package com.java8.features.patterns;

public class Patterns_Singleton {

    private static final Patterns_Singleton INSTANCE = new Patterns_Singleton();

    private Patterns_Singleton(){

    }

    public static Patterns_Singleton getInstance() {
        return INSTANCE;
    }

}
