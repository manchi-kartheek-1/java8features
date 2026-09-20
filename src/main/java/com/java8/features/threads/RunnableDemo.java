package com.java8.features.threads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunnableDemo implements Runnable {
    @Override
    public void run() {
        System.out.println("This is Run Method");
    }

    static void main() {
        RunnableDemo runnableDemo = new RunnableDemo();
        Thread thread = new Thread(runnableDemo);
        thread.start();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(runnableDemo);
        executorService.shutdown();
    }
}
