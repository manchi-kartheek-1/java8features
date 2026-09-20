package com.java8.features.threads;

import java.util.concurrent.*;

public class CallableDemo implements Callable<Integer> {

    int n;

    public CallableDemo(int i) {
        this.n = i;
    }

    @Override
    public Integer call() throws Exception {
        int fact = 1;
        for (int i = n; i > 1; i--) {
            fact *= i;
        }
        return fact;
    }

    static void main() throws ExecutionException, InterruptedException {
        CallableDemo callableDemo = new CallableDemo(5);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Integer> submit = executorService.submit(callableDemo);
        Integer i = submit.get();
        System.out.println(i);
        executorService.shutdown();
    }
}
