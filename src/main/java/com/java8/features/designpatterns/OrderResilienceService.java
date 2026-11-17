package com.java8.features.designpatterns;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One service, one fault-tolerance pattern per method.
 *
 * <pre>
 *  #  Pattern                        Method(s)
 *  1  Circuit Breaker                checkInventory()
 *  2  Bulkhead (semaphore + pool)    processPayment(), generateInvoicePdf()
 *  3  Retry                          createShipment()
 *  4  Timeout                        getPriceQuote()
 *  5  Fallback (multi-level chain)   getRecommendations()
 *  6  Rate Limiting                  sendNotification(), sendNotificationPerTenant()
 *  7  Failover & Replication         getOrderWithFailover(), saveOrderReplicated()
 *  8  Health Checks & Self-Healing   health(), selfHeal()
 *  9  Dead Letter Queue              consumeOrderEvent(), handleDeadLetter(), sendToDeadLetterQueue()
 * 10  All combined                   placeOrder()
 * </pre>
 *
 * All tuning parameters live in application.yml (resilience4j.* and app.*).
 * Annotation "name" values below must match the instance names in that file.
 *
 * NOTE: Resilience4j annotations work through Spring AOP proxies, so calling one
 * annotated method from another method in THIS class bypasses the pattern
 * (self-invocation). Call them from a controller or another bean.
 */
@Service
public class OrderResilienceService implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(OrderResilienceService.class);

    // ---- Resilience4j instance names (must match application.yml) ----
    public static final String INVENTORY_CB      = "inventoryService";
    public static final String PAYMENT_BULKHEAD  = "paymentService";
    public static final String INVOICE_POOL      = "invoiceService";
    public static final String SHIPPING_RETRY    = "shippingService";
    public static final String PRICING_TIMEOUT   = "pricingService";
    public static final String RECOMMENDATION_CB = "recommendationService";
    public static final String NOTIFICATION_RL   = "notificationService";
    public static final String CHECKOUT          = "checkoutService";

    // ---- Collaborators ----
    private final RestClient.Builder restClientBuilder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    // ---- Downstream base URLs ----
    @Value("${app.downstream.inventory-url}")       private String inventoryUrl;
    @Value("${app.downstream.payment-url}")         private String paymentUrl;
    @Value("${app.downstream.invoice-url}")         private String invoiceUrl;
    @Value("${app.downstream.shipping-url}")        private String shippingUrl;
    @Value("${app.downstream.pricing-url}")         private String pricingUrl;
    @Value("${app.downstream.recommendation-url}")  private String recommendationUrl;
    @Value("${app.downstream.catalog-url}")         private String catalogUrl;
    @Value("${app.downstream.notification-url}")    private String notificationUrl;
    @Value("${app.downstream.checkout-url}")        private String checkoutUrl;

    // ---- HTTP-level timeouts (first line of defence, below TimeLimiter) ----
    @Value("${app.http.connect-timeout-ms:1000}")   private int connectTimeoutMs;
    @Value("${app.http.read-timeout-ms:3000}")      private int readTimeoutMs;

    // ---- Failover & replication ----
    @Value("${app.order-cluster.nodes}")                      private String clusterNodesCsv;
    @Value("${app.order-cluster.max-attempts-per-node:2}")    private int maxAttemptsPerNode;
    @Value("${app.order-cluster.quarantine-duration-ms:30000}") private long quarantineMs;
    @Value("${app.order-cluster.replication.write-quorum:2}")  private int writeQuorum;
    @Value("${app.order-cluster.replication.timeout-ms:2000}") private long replicationTimeoutMs;
    @Value("${app.order-cluster.replication.pool-size:8}")     private int replicationPoolSize;

    // ---- Fallback / caching ----
    @Value("${app.fallback.cache-ttl-ms:300000}")             private long cacheTtlMs;
    @Value("${app.fallback.default-recommendations}")         private String defaultRecommendationsCsv;

    // ---- Rate limiting (programmatic, per tenant) ----
    @Value("${app.rate-limit.per-tenant.limit-for-period:20}")     private int tenantLimitForPeriod;
    @Value("${app.rate-limit.per-tenant.refresh-period-ms:1000}")  private long tenantRefreshPeriodMs;
    @Value("${app.rate-limit.per-tenant.timeout-ms:0}")            private long tenantTimeoutMs;

    // ---- Self-healing ----
    @Value("${app.self-healing.probe-timeout-ms:500}")        private int probeTimeoutMs;
    @Value("${app.self-healing.probe-path:/actuator/health}") private String probePath;

    // ---- Kafka topics ----
    @Value("${app.kafka.topics.order-events}")            private String orderEventsTopic;
    @Value("${app.kafka.topics.deferred-notifications}")  private String deferredNotificationsTopic;
    @Value("${app.kafka.topics.dead-letter}")             private String deadLetterTopic;

    // ---- Runtime state ----
    private RestClient inventoryClient, paymentClient, invoiceClient, shippingClient, pricingClient,
            recommendationClient, catalogClient, notificationClient, checkoutClient;
    private final Map<String, RestClient> nodeClients = new LinkedHashMap<>();   // keeps primary first
    private final Map<String, RestClient> probeClients = new ConcurrentHashMap<>();
    private final Map<String, String> circuitBreakerToBaseUrl = new LinkedHashMap<>();
    private final Map<String, Instant> quarantinedUntil = new ConcurrentHashMap<>();

    private final Map<String, InventoryStatus> inventoryCache = new ConcurrentHashMap<>();
    private final Map<String, PriceQuote> priceCache = new ConcurrentHashMap<>();
    private final Map<String, CachedValue<List<String>>> recommendationCache = new ConcurrentHashMap<>();

    private final AtomicLong deadLetterCount = new AtomicLong();
    private final AtomicLong deferredNotificationCount = new AtomicLong();

    private ExecutorService timeLimiterExecutor;
    private ExecutorService replicationExecutor;

    public OrderResilienceService(RestClient.Builder restClientBuilder,
                                  KafkaTemplate<String, Object> kafkaTemplate,
                                  CircuitBreakerRegistry circuitBreakerRegistry,
                                  RetryRegistry retryRegistry,
                                  RateLimiterRegistry rateLimiterRegistry) {
        this.restClientBuilder = restClientBuilder;
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @PostConstruct
    void init() {
        inventoryClient      = client(inventoryUrl, connectTimeoutMs, readTimeoutMs);
        paymentClient        = client(paymentUrl, connectTimeoutMs, readTimeoutMs);
        invoiceClient        = client(invoiceUrl, connectTimeoutMs, 10_000);  // PDFs are slow
        shippingClient       = client(shippingUrl, connectTimeoutMs, readTimeoutMs);
        pricingClient        = client(pricingUrl, connectTimeoutMs, readTimeoutMs);
        recommendationClient = client(recommendationUrl, connectTimeoutMs, readTimeoutMs);
        catalogClient        = client(catalogUrl, connectTimeoutMs, readTimeoutMs);
        notificationClient   = client(notificationUrl, connectTimeoutMs, readTimeoutMs);
        checkoutClient       = client(checkoutUrl, connectTimeoutMs, readTimeoutMs);

        for (String node : clusterNodesCsv.split(",")) {
            String url = node.trim();
            nodeClients.put(url, client(url, connectTimeoutMs, readTimeoutMs));
        }

        // Which downstream each circuit breaker protects (used by self-healing probes)
        circuitBreakerToBaseUrl.put(INVENTORY_CB, inventoryUrl);
        circuitBreakerToBaseUrl.put(RECOMMENDATION_CB, recommendationUrl);
        circuitBreakerToBaseUrl.put(CHECKOUT, checkoutUrl);

        timeLimiterExecutor = Executors.newFixedThreadPool(16);
        replicationExecutor = Executors.newFixedThreadPool(replicationPoolSize);

        // Observability: log state changes and retries
        circuitBreakerToBaseUrl.keySet().forEach(name ->
                circuitBreakerRegistry.circuitBreaker(name).getEventPublisher()
                        .onStateTransition(e -> log.warn("[CB] {} : {}", e.getCircuitBreakerName(), e.getStateTransition()))
                        .onCallNotPermitted(e -> log.debug("[CB] {} short-circuited a call", e.getCircuitBreakerName())));

        retryRegistry.retry(SHIPPING_RETRY).getEventPublisher()
                .onRetry(e -> log.warn("[RETRY] {} attempt #{} in {} ms, cause: {}",
                        e.getName(), e.getNumberOfRetryAttempts(), e.getWaitInterval().toMillis(),
                        String.valueOf(e.getLastThrowable())));
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        timeLimiterExecutor.shutdown();
        replicationExecutor.shutdown();
        timeLimiterExecutor.awaitTermination(5, TimeUnit.SECONDS);
        replicationExecutor.awaitTermination(5, TimeUnit.SECONDS);
    }

    // =====================================================================
    // 1. CIRCUIT BREAKER
    //    CLOSED -> (failure/slow-call rate over threshold) -> OPEN
    //    OPEN -> (waitDurationInOpenState) -> HALF_OPEN -> trial calls -> CLOSED / OPEN
    //    Config: resilience4j.circuitbreaker.instances.inventoryService
    // =====================================================================
    @CircuitBreaker(name = INVENTORY_CB, fallbackMethod = "inventoryFallback")
    public InventoryStatus checkInventory(String productId) {
        InventoryStatus status = inventoryClient.get()
                .uri("/api/v1/inventory/{productId}", productId)
                .retrieve()
                .body(InventoryStatus.class);
        if (status != null) {
            inventoryCache.put(productId, status);
        }
        return status;
    }

    /** Circuit is OPEN: downstream was not called at all. Most specific exception wins. */
    private InventoryStatus inventoryFallback(String productId, CallNotPermittedException ex) {
        log.warn("Inventory circuit OPEN, serving cached stock for {}", productId);
        return Optional.ofNullable(inventoryCache.get(productId))
                .map(s -> s.withSource("CACHE"))
                .orElse(InventoryStatus.unknown(productId));
    }

    /** Circuit is CLOSED/HALF_OPEN but this call failed. */
    private InventoryStatus inventoryFallback(String productId, Exception ex) {
        log.warn("Inventory call failed for {}: {}", productId, ex.toString());
        return Optional.ofNullable(inventoryCache.get(productId))
                .map(s -> s.withSource("CACHE"))
                .orElse(InventoryStatus.unknown(productId));
    }

    // =====================================================================
    // 2a. BULKHEAD (SEMAPHORE): caps concurrent calls on the caller's thread.
    //     Config: resilience4j.bulkhead.instances.paymentService
    // =====================================================================
    @Bulkhead(name = PAYMENT_BULKHEAD, type = Bulkhead.Type.SEMAPHORE,
            fallbackMethod = "paymentBulkheadFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        return paymentClient.post()
                .uri("/api/v1/payments")
                .header("Idempotency-Key", request.orderId())
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }

    /** All permits taken: fail fast instead of piling up threads. */
    private PaymentResponse paymentBulkheadFallback(PaymentRequest request, BulkheadFullException ex) {
        log.warn("Payment bulkhead full, rejecting order {}", request.orderId());
        return new PaymentResponse(request.orderId(), "RETRY_LATER", "Payment system busy, please retry");
    }

    private PaymentResponse paymentBulkheadFallback(PaymentRequest request, Exception ex) {
        return new PaymentResponse(request.orderId(), "FAILED", ex.getMessage());
    }

    // =====================================================================
    // 2b. BULKHEAD (THREAD POOL): runs the call in its own isolated pool + queue.
    //     Method MUST return CompletableFuture / CompletionStage.
    //     Config: resilience4j.thread-pool-bulkhead.instances.invoiceService
    // =====================================================================
    @Bulkhead(name = INVOICE_POOL, type = Bulkhead.Type.THREADPOOL,
            fallbackMethod = "invoiceFallback")
    public CompletableFuture<String> generateInvoicePdf(String orderId) {
        // This body already runs on a thread from the bulkhead's dedicated pool
        String url = invoiceClient.post()
                .uri("/api/v1/invoices/{orderId}/pdf", orderId)
                .retrieve()
                .body(String.class);
        return CompletableFuture.completedFuture(url);
    }

    private CompletableFuture<String> invoiceFallback(String orderId, BulkheadFullException ex) {
        publishOrderEvent(new OrderEvent(UUID.randomUUID().toString(), orderId, "INVOICE_REQUESTED", Instant.now()));
        return CompletableFuture.completedFuture("QUEUED: invoice for " + orderId + " will be emailed");
    }

    private CompletableFuture<String> invoiceFallback(String orderId, Throwable ex) {
        return CompletableFuture.completedFuture("UNAVAILABLE: " + ex.getMessage());
    }

    // =====================================================================
    // 3. RETRY: exponential backoff on transient errors only.
    //    Make the downstream call idempotent (Idempotency-Key) before retrying a POST.
    //    Config: resilience4j.retry.instances.shippingService
    // =====================================================================
    @Retry(name = SHIPPING_RETRY, fallbackMethod = "shippingFallback")
    public ShipmentResponse createShipment(ShipmentRequest request) {
        return shippingClient.post()
                .uri("/api/v1/shipments")
                .header("Idempotency-Key", request.orderId())
                .body(request)
                .retrieve()
                .body(ShipmentResponse.class);
    }

    /** Called only after maxAttempts are exhausted (or a non-retryable exception). */
    private ShipmentResponse shippingFallback(ShipmentRequest request, Exception ex) {
        sendToDeadLetterQueue("createShipment", request, ex);
        return new ShipmentResponse(request.orderId(), null, "SCHEDULED_FOR_RETRY");
    }

    // =====================================================================
    // 4. TIMEOUT: TimeLimiter bounds total wait time of an async call.
    //    Two layers: HTTP connect/read timeout (RestClient) + TimeLimiter.
    //    Config: resilience4j.timelimiter.instances.pricingService
    // =====================================================================
    @TimeLimiter(name = PRICING_TIMEOUT, fallbackMethod = "pricingFallback")
    public CompletableFuture<PriceQuote> getPriceQuote(String productId) {
        return CompletableFuture.supplyAsync(() -> {
            PriceQuote quote = pricingClient.get()
                    .uri("/api/v1/prices/{productId}", productId)
                    .retrieve()
                    .body(PriceQuote.class);
            if (quote != null) {
                priceCache.put(productId, quote);
            }
            return quote;
        }, timeLimiterExecutor);
        // cancelRunningFuture=true cancels the future, but supplyAsync does not interrupt
        // the worker thread, which is why the HTTP read timeout is still needed.
    }

    private CompletableFuture<PriceQuote> pricingFallback(String productId, TimeoutException ex) {
        log.warn("Pricing timed out for {}, serving last known price", productId);
        PriceQuote cached = priceCache.get(productId);
        return CompletableFuture.completedFuture(
                cached != null ? cached.asStale() : new PriceQuote(productId, null, "INR", true));
    }

    private CompletableFuture<PriceQuote> pricingFallback(String productId, Throwable ex) {
        return pricingFallback(productId, new TimeoutException(ex.getMessage()));
    }

    // =====================================================================
    // 5. FALLBACK: graceful degradation chain
    //    primary service -> fresh cache -> secondary service -> stale cache -> static default
    //    Config: resilience4j.circuitbreaker.instances.recommendationService, app.fallback.*
    // =====================================================================
    @CircuitBreaker(name = RECOMMENDATION_CB, fallbackMethod = "recommendationFallback")
    public List<String> getRecommendations(String customerId) {
        List<String> items = recommendationClient.get()
                .uri("/api/v1/recommendations/{customerId}", customerId)
                .retrieve()
                .body(StringList.class);
        recommendationCache.put(customerId, new CachedValue<>(items, Instant.now()));
        return items;
    }

    private List<String> recommendationFallback(String customerId, Throwable ex) {
        Duration ttl = Duration.ofMillis(cacheTtlMs);

        // Level 1: fresh personalised cache
        CachedValue<List<String>> cached = recommendationCache.get(customerId);
        if (cached != null && !cached.isOlderThan(ttl)) {
            log.info("Recommendations L1 fallback (fresh cache) for {}", customerId);
            return cached.value();
        }
        // Level 2: generic "popular items" from a different service
        try {
            List<String> popular = catalogClient.get()
                    .uri("/api/v1/products/popular?limit=10")
                    .retrieve()
                    .body(StringList.class);
            if (popular != null && !popular.isEmpty()) {
                log.info("Recommendations L2 fallback (popular items) for {}", customerId);
                return popular;
            }
        } catch (Exception secondaryFailure) {
            log.warn("Secondary catalog service also failed: {}", secondaryFailure.toString());
        }
        // Level 3: stale cache beats nothing
        if (cached != null) {
            log.info("Recommendations L3 fallback (stale cache) for {}", customerId);
            return cached.value();
        }
        // Level 4: static default, never fails
        log.info("Recommendations L4 fallback (static default) for {}", customerId);
        return Arrays.stream(defaultRecommendationsCsv.split(",")).map(String::trim).toList();
    }

    // =====================================================================
    // 6a. RATE LIMITING (global, declarative)
    //     Config: resilience4j.ratelimiter.instances.notificationService
    // =====================================================================
    @RateLimiter(name = NOTIFICATION_RL, fallbackMethod = "notificationRateLimitFallback")
    public NotificationResult sendNotification(String customerId, String message) {
        return doSendNotification(customerId, message);
    }

    /** No permit within timeoutDuration: defer through Kafka instead of dropping. */
    private NotificationResult notificationRateLimitFallback(String customerId, String message,
                                                             RequestNotPermitted ex) {
        return deferNotification(customerId, message);
    }

    // =====================================================================
    // 6b. RATE LIMITING (per tenant, programmatic, shows every builder parameter)
    // =====================================================================
    public NotificationResult sendNotificationPerTenant(String tenantId, String customerId, String message) {
        RateLimiterConfig tenantConfig = RateLimiterConfig.custom()
                .limitForPeriod(tenantLimitForPeriod)                         // permits per refresh period
                .limitRefreshPeriod(Duration.ofMillis(tenantRefreshPeriodMs)) // window length
                .timeoutDuration(Duration.ofMillis(tenantTimeoutMs))          // wait for a permit (0 = fail fast)
                .writableStackTraceEnabled(false)                             // cheaper RequestNotPermitted
                .build();

        // Registry caches one limiter per name. With unbounded tenant ids, evict old ones
        // (rateLimiterRegistry.remove(name)) or use a distributed limiter (Redis / API gateway).
        io.github.resilience4j.ratelimiter.RateLimiter limiter =
                rateLimiterRegistry.rateLimiter("tenant-" + tenantId, tenantConfig);

        if (!limiter.acquirePermission()) {
            log.warn("Tenant {} exceeded {} req/{}ms", tenantId, tenantLimitForPeriod, tenantRefreshPeriodMs);
            return deferNotification(customerId, message);
        }
        return doSendNotification(customerId, message);
    }

    private NotificationResult doSendNotification(String customerId, String message) {
        notificationClient.post()
                .uri("/api/v1/notifications")
                .body(Map.of("customerId", customerId, "message", message))
                .retrieve()
                .toBodilessEntity();
        return new NotificationResult(customerId, "SENT");
    }

    private NotificationResult deferNotification(String customerId, String message) {
        kafkaTemplate.send(deferredNotificationsTopic, customerId,
                Map.of("customerId", customerId, "message", message, "deferredAt", Instant.now().toString()));
        deferredNotificationCount.incrementAndGet();
        return new NotificationResult(customerId, "QUEUED");
    }

    // =====================================================================
    // 7a. FAILOVER (reads): try primary, then replicas; quarantine bad nodes.
    //     Config: app.order-cluster.*
    // =====================================================================
    public OrderResponse getOrderWithFailover(String orderId) {
        List<String> candidates = availableNodes();
        Exception lastError = null;

        for (String node : candidates) {
            RestClient client = nodeClients.get(node);
            for (int attempt = 1; attempt <= maxAttemptsPerNode; attempt++) {
                try {
                    OrderResponse response = client.get()
                            .uri("/api/v1/orders/{orderId}", orderId)
                            .retrieve()
                            .body(OrderResponse.class);
                    quarantinedUntil.remove(node);
                    return response == null ? null : response.servedBy(node);
                } catch (ResourceAccessException | HttpServerErrorException e) {
                    // Connection refused, timeout or 5xx: candidate for failover
                    lastError = e;
                    log.warn("Node {} attempt {}/{} failed: {}", node, attempt, maxAttemptsPerNode, e.toString());
                }
                // 4xx errors propagate: failing over will not fix a bad request
            }
            quarantine(node);
        }
        throw new ServiceUnavailableException("All order nodes failed for " + orderId, lastError);
    }

    // =====================================================================
    // 7b. REPLICATION (writes): write to all nodes in parallel and require a quorum.
    //     N = nodes, W = write-quorum. W > N/2 gives strong-ish durability.
    // =====================================================================
    public OrderResponse saveOrderReplicated(OrderRequest request) {
        List<String> nodes = new ArrayList<>(nodeClients.keySet());

        List<CompletableFuture<Boolean>> writes = nodes.stream()
                .map(node -> CompletableFuture
                        .supplyAsync(() -> writeToNode(node, request), replicationExecutor)
                        .completeOnTimeout(false, replicationTimeoutMs, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> false))
                .toList();

        CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).join();
        long acks = writes.stream().filter(CompletableFuture::join).count();

        if (acks >= writeQuorum) {
            if (acks < nodes.size()) {
                // Lagging replicas get repaired asynchronously (read-repair / reconciliation job)
                publishOrderEvent(new OrderEvent(UUID.randomUUID().toString(), request.orderId(),
                        "REPLICA_REPAIR_NEEDED", Instant.now()));
            }
            log.info("Order {} replicated to {}/{} nodes (quorum {})", request.orderId(), acks, nodes.size(), writeQuorum);
            return new OrderResponse(request.orderId(), "SAVED", acks + "/" + nodes.size() + " replicas");
        }

        ReplicationQuorumException ex = new ReplicationQuorumException(
                "Only " + acks + "/" + nodes.size() + " acks, quorum is " + writeQuorum);
        sendToDeadLetterQueue("saveOrderReplicated", request, ex);
        throw ex;
    }

    private boolean writeToNode(String node, OrderRequest request) {
        try {
            nodeClients.get(node).put()
                    .uri("/api/v1/orders/{orderId}", request.orderId())
                    .header("Idempotency-Key", request.orderId())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Replication to {} failed: {}", node, e.toString());
            quarantine(node);
            return false;
        }
    }

    /** Healthy nodes in priority order; if every node is quarantined, try them all anyway. */
    private List<String> availableNodes() {
        Instant now = Instant.now();
        List<String> healthy = nodeClients.keySet().stream()
                .filter(n -> quarantinedUntil.getOrDefault(n, Instant.MIN).isBefore(now))
                .toList();
        return healthy.isEmpty() ? new ArrayList<>(nodeClients.keySet()) : healthy;
    }

    private void quarantine(String node) {
        quarantinedUntil.put(node, Instant.now().plusMillis(quarantineMs));
        log.warn("Node {} quarantined for {} ms", node, quarantineMs);
    }

    // =====================================================================
    // 8a. HEALTH CHECK: exposed at /actuator/health/orderResilienceService
    //     and wired into the readiness group in application.yml.
    // =====================================================================
    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();

        Map<String, String> breakers = new LinkedHashMap<>();
        boolean anyOpen = false;
        for (String name : circuitBreakerToBaseUrl.keySet()) {
            var state = circuitBreakerRegistry.circuitBreaker(name).getState();
            breakers.put(name, state.name());
            anyOpen |= state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
        }
        details.put("circuitBreakers", breakers);

        Instant now = Instant.now();
        long healthyNodes = nodeClients.keySet().stream()
                .filter(n -> quarantinedUntil.getOrDefault(n, Instant.MIN).isBefore(now))
                .count();
        details.put("orderCluster", healthyNodes + "/" + nodeClients.size() + " nodes healthy");
        details.put("quarantinedNodes", quarantinedUntil.keySet());
        details.put("deadLetters", deadLetterCount.get());
        details.put("deferredNotifications", deferredNotificationCount.get());

        if (healthyNodes == 0) {
            return Health.down().withDetails(details).build();           // -> readiness fails, no traffic
        }
        if (anyOpen || healthyNodes < writeQuorum) {
            return Health.status(new Status("DEGRADED")).withDetails(details).build(); // still serving (HTTP 200)
        }
        return Health.up().withDetails(details).build();
    }

    // =====================================================================
    // 8b. SELF-HEALING: periodic job that repairs what it can automatically.
    //     Kubernetes liveness probes restart the pod for what it cannot (see k8s yaml).
    // =====================================================================
    @Scheduled(fixedDelayString = "${app.self-healing.interval-ms:30000}",
            initialDelayString = "${app.self-healing.initial-delay-ms:15000}")
    public void selfHeal() {
        Instant now = Instant.now();

        // 1) Bring quarantined nodes back once their health endpoint responds
        quarantinedUntil.forEach((node, until) -> {
            if (now.isAfter(until) && probe(node)) {
                quarantinedUntil.remove(node);
                log.info("[SELF-HEAL] Node {} healthy again, back in rotation", node);
            }
        });

        // 2) If an OPEN circuit's downstream is healthy again, move to HALF_OPEN early
        circuitBreakerToBaseUrl.forEach((cbName, baseUrl) -> {
            var cb = circuitBreakerRegistry.circuitBreaker(cbName);
            if (cb.getState() == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN && probe(baseUrl)) {
                cb.transitionToHalfOpenState();
                log.info("[SELF-HEAL] {} probe OK, circuit OPEN -> HALF_OPEN", cbName);
            }
        });

        // 3) Evict very stale fallback cache entries so memory doesn't grow forever
        Duration maxAge = Duration.ofMillis(cacheTtlMs * 10);
        recommendationCache.entrySet().removeIf(e -> e.getValue().isOlderThan(maxAge));
    }

    private boolean probe(String baseUrl) {
        try {
            probeClients.computeIfAbsent(baseUrl, u -> client(u, probeTimeoutMs, probeTimeoutMs))
                    .get().uri(probePath).retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // =====================================================================
    // 9. DEAD LETTER QUEUE
    //    order-events -> order-events-retry-0 (1s) -> -retry-1 (2s) -> -retry-2 (4s) -> order-events-dlt
    //    Non-retryable exceptions (validation) skip the retry topics and go straight to the DLT.
    // =====================================================================
    @RetryableTopic(
            attempts = "${app.kafka.dlq.attempts:4}",                    // 1 original + 3 retries
            backoff = @Backoff(
                    delayExpression = "${app.kafka.dlq.initial-delay-ms:1000}",
                    multiplierExpression = "${app.kafka.dlq.multiplier:2.0}",
                    maxDelayExpression = "${app.kafka.dlq.max-delay-ms:10000}"),
            numPartitions = "${app.kafka.dlq.partitions:3}",
            replicationFactor = "${app.kafka.dlq.replication-factor:1}",
            autoCreateTopics = "true",
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,                     // if DLT handler fails, stop (don't loop)
            exclude = {BusinessValidationException.class, IllegalArgumentException.class}, // never retried
            traversingCauses = "true",                                   // look at wrapped causes too
            timeout = "${app.kafka.dlq.global-timeout-ms:120000}",       // give up on retries after this
            kafkaTemplate = "kafkaTemplate")
    @KafkaListener(
            id = "order-events-listener",
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "${app.kafka.listener-concurrency:3}")
    public void consumeOrderEvent(ConsumerRecord<String, OrderEvent> record) {
        OrderEvent event = record.value();
        log.info("Consuming {} from {}-{}@{}", event.type(), record.topic(), record.partition(), record.offset());

        if (event.orderId() == null || event.orderId().isBlank()) {
            throw new BusinessValidationException("orderId missing in event " + event.eventId()); // -> DLT directly
        }
        // Transient failures here (ResourceAccessException, 5xx) go through the retry topics
        checkoutClient.post()
                .uri("/api/v1/orders/{orderId}/events", event.orderId())
                .header("Idempotency-Key", event.eventId())
                .body(event)
                .retrieve()
                .toBodilessEntity();
    }

    @DltHandler
    public void handleDeadLetter(ConsumerRecord<String, OrderEvent> record) {
        deadLetterCount.incrementAndGet();
        log.error("[DLT] topic={} key={} originalTopic={} originalOffset={} exception={} message={}",
                record.topic(), record.key(),
                header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                header(record, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                header(record, KafkaHeaders.DLT_EXCEPTION_FQCN),
                header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE));
        // Typical next steps: persist to a "failed_events" table, alert on-call,
        // expose an admin endpoint that re-publishes fixed messages to the original topic.
    }

    /** Manual DLQ for non-Kafka flows (HTTP calls that exhausted retries / failed quorum). */
    public void sendToDeadLetterQueue(String source, Object payload, Throwable error) {
        DeadLetter letter = new DeadLetter(source, payload, error.getClass().getName(),
                String.valueOf(error.getMessage()), Instant.now());
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(deadLetterTopic, source, letter);
        producerRecord.headers()
                .add("dlq-source", source.getBytes(StandardCharsets.UTF_8))
                .add("dlq-exception", error.getClass().getName().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(producerRecord).whenComplete((result, ex) -> {
            if (ex != null) {
                // Last resort: DLQ itself is down. Log everything so nothing is silently lost.
                log.error("DLQ publish FAILED for {} payload={}", source, payload, ex);
            }
        });
        deadLetterCount.incrementAndGet();
    }

    public void publishOrderEvent(OrderEvent event) {
        kafkaTemplate.send(orderEventsTopic, event.orderId(), event);
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        var h = record.headers().lastHeader(name);
        if (h == null) return null;
        byte[] v = h.value();
        // Partition/offset headers are binary int/long; everything else is UTF-8 text
        if (v.length == 8 && name.equals(KafkaHeaders.DLT_ORIGINAL_OFFSET)) {
            return String.valueOf(java.nio.ByteBuffer.wrap(v).getLong());
        }
        return new String(v, StandardCharsets.UTF_8);
    }

    // =====================================================================
    // 10. ALL PATTERNS STACKED on one critical call.
    //     Default aspect order (outermost first):
    //       Retry( CircuitBreaker( RateLimiter( TimeLimiter( Bulkhead( method ) ) ) ) )
    //     Put fallbackMethod on the OUTERMOST aspect (Retry). If the CircuitBreaker had
    //     the fallback, Retry would see a "successful" fallback result and never retry.
    //     Config: every resilience4j.*.instances.checkoutService block
    // =====================================================================
    @Retry(name = CHECKOUT, fallbackMethod = "placeOrderFallback")
    @CircuitBreaker(name = CHECKOUT)
    @RateLimiter(name = CHECKOUT)
    @TimeLimiter(name = CHECKOUT)
    @Bulkhead(name = CHECKOUT, type = Bulkhead.Type.SEMAPHORE)
    public CompletableFuture<OrderResponse> placeOrder(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> checkoutClient.post()
                .uri("/api/v1/checkout")
                .header("Idempotency-Key", request.orderId())
                .body(request)
                .retrieve()
                .body(OrderResponse.class), timeLimiterExecutor);
    }

    /** Accept the order asynchronously; Kafka consumer (pattern 9) finishes it later. */
    private CompletableFuture<OrderResponse> placeOrderFallback(OrderRequest request, Throwable ex) {
        log.warn("Checkout degraded for {} ({}), accepting asynchronously", request.orderId(), ex.getClass().getSimpleName());
        publishOrderEvent(new OrderEvent(UUID.randomUUID().toString(), request.orderId(), "ORDER_PLACED", Instant.now()));
        return CompletableFuture.completedFuture(new OrderResponse(request.orderId(), "ACCEPTED", "async"));
    }

    // =====================================================================
    // Helpers, DTOs and exceptions (nested to keep the sample in one file)
    // =====================================================================
    private RestClient client(String baseUrl, int connectMs, int readMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectMs);
        factory.setReadTimeout(readMs);
        return restClientBuilder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public record InventoryStatus(String productId, int availableQuantity, String source) {
        static InventoryStatus unknown(String productId) { return new InventoryStatus(productId, -1, "UNKNOWN"); }
        InventoryStatus withSource(String s) { return new InventoryStatus(productId, availableQuantity, s); }
    }
    public record PaymentRequest(String orderId, String customerId, BigDecimal amount, String currency) {}
    public record PaymentResponse(String orderId, String status, String message) {}
    public record ShipmentRequest(String orderId, String address) {}
    public record ShipmentResponse(String orderId, String trackingId, String status) {}
    public record PriceQuote(String productId, BigDecimal price, String currency, boolean stale) {
        PriceQuote asStale() { return new PriceQuote(productId, price, currency, true); }
    }
    public record NotificationResult(String customerId, String status) {}
    public record OrderRequest(String orderId, String customerId, List<String> productIds, BigDecimal total) {}
    public record OrderResponse(String orderId, String status, String servedBy) {
        OrderResponse servedBy(String node) { return new OrderResponse(orderId, status, node); }
    }
    public record OrderEvent(String eventId, String orderId, String type, Instant createdAt) {}
    public record DeadLetter(String source, Object payload, String errorClass, String errorMessage, Instant failedAt) {}
    record CachedValue<T>(T value, Instant cachedAt) {
        boolean isOlderThan(Duration age) { return cachedAt.plus(age).isBefore(Instant.now()); }
    }
    /** Concrete type so RestClient can deserialize a JSON array of strings. */
    static class StringList extends ArrayList<String> {}

    /** Transient: retried and counted as a failure by circuit breakers. */
    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String msg, Throwable cause) { super(msg, cause); }
    }
    /** Business error: never retried, ignored by circuit breakers (it's not the downstream's fault). */
    public static class BusinessValidationException extends RuntimeException {
        public BusinessValidationException(String msg) { super(msg); }
    }
    public static class ReplicationQuorumException extends RuntimeException {
        public ReplicationQuorumException(String msg) { super(msg); }
    }
}
