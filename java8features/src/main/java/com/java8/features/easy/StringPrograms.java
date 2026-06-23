package com.java8.features.easy;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringPrograms {

    public static String testString = "Java Programming";

    static void main() {
        StringPrograms.countOccurrenceInString();
        StringPrograms.convertingToUpperCase();
    }

    public static void countOccurrenceInString() {
        Map<Character, Long> occurrences = testString.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        System.out.println(occurrences);
    }

    public static void convertingToUpperCase() {
        String string = testString.chars()
                .mapToObj(c -> (char) Character.toUpperCase(c))
                .map(String::valueOf)
                .collect(Collectors.joining());
        System.out.println(string);
    }
}
