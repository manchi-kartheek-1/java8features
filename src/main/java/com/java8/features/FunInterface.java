package com.java8.features;

@FunctionalInterface
public interface FunInterface {

    abstract void test();

    static void test1() {
        System.out.println("Test 1");
    }

    static void test2() {
        System.out.println("Test 2");
    }

}
