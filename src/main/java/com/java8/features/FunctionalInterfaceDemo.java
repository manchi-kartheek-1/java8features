package com.java8.features;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FunctionalInterfaceDemo {
    static void main() {
        Predicate<Integer> p = x -> x > 18;
        System.out.println("Predicate "+ p.test(15));

        Function<Integer, Integer> f = x -> x * 2;
        System.out.println("Function "+f.apply(20));

        Consumer<String> c = System.out::println;
        c.accept("Hii, This is Consumer");
        System.gc();

        Supplier<String> s = () -> "Kartheek";
        System.out.println(s.get());
    }
}
