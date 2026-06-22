package com.java8.features.easy;

import java.util.Map;
import java.util.stream.Collectors;

public class StringPrograms {

    public static String testString = "Java Programming";

    static void main() {
        StringPrograms.countOccurrenceInString();
    }

    public static void countOccurrenceInString() {
        Map<Character, Long> occurrences = testString.chars().mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        System.out.println(occurrences);
    }
}
