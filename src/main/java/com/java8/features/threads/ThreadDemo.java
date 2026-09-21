package com.java8.features.threads;

public class ThreadDemo implements Runnable {
    int i = 0;

    static void main() {
        ThreadDemo threadDemo = new ThreadDemo();
//        ThreadDemo threadDemo1 = new ThreadDemo();
        Thread thread = new Thread(threadDemo);
        Thread thread1 = new Thread(threadDemo);
        thread.start();
        thread1.start();
    }

    @Override
    public void run() {


        this.increment();
        System.out.println("inside run method");

    }

    public synchronized void increment(){
        i++;
        System.out.println(i);
    }
}
