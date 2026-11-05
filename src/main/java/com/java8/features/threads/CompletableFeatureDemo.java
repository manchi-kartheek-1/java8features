package com.java8.features.threads;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletableFeatureDemo{

    static void main() throws Exception {
        CompletableFeatureDemo completableFeatureDemo = new CompletableFeatureDemo();
        completableFeatureDemo.orderSelected();

    }


    public void orderSelected() throws ExecutionException, InterruptedException {
        System.out.println("Order Selected");
        this.orderPlaced();
//        ExecutorService executorService = Executors.newFixedThreadPool(2);
//        executorService.submit(() -> {
//            paymentProcessing();
//        });
         CompletableFuture.supplyAsync(this::paymentProcessing)
                .thenRun(this::sendMail)
                .get();
    }

    public String paymentProcessing(){
        sleep(5000);
        System.out.println("Payment Processing :: Success");
        return "SUCCESS";
    }

    public String orderPlaced() {
        System.out.println("Order Placed");
        return "SUCCESS";
    }

    public void sendMail(){
        System.out.println("Sending Mail...");
    }

    private void sleep(long sec){
        try {
            Thread.sleep(sec);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
