package com.java8.features.medium;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class StringPrograms {

    public static String string = "ea principle + charges = installment amount or else report and don't add customer installment";


    static void main() {
        StringPrograms.findAllWordsGNNdSort(6);
        StringPrograms.countOccurrencesOfString();
        StringPrograms.groupStringsByLength();
        StringPrograms.findDuplicateStrings();
        StringPrograms.findUniqueStrings();
        StringPrograms.findWordsContainsVowels();
        StringPrograms.sortStringsByLength();
        StringPrograms.findMostOccurringString();
        StringPrograms.convertListOfStringsToMapOfStringsAndLen();
    }

    public static void findAllWordsGNNdSort(int i) {
        String collect = Arrays.stream(string.split(" "))
                .map(String::valueOf)
                .filter(word -> word.length() > i)
                .sorted()
                .collect(Collectors.joining(" "));
        System.out.println(collect);
    }

    public static void countOccurrencesOfString() {
        Map<String, Long> collect = Arrays.stream(string.split(" "))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        System.out.println(collect);
    }

    public static void groupStringsByLength() {
        Map<Integer, List<String>> collect = Arrays.stream(string.split(" "))
                .collect(Collectors.groupingBy(String::length));
        System.out.println(collect);
    }

    public static void findDuplicateStrings() {
        Set<String> stringArrayList = new HashSet<>();
        List<String> list = Arrays.stream(string.split(" "))
                .filter(w -> !stringArrayList.add(w))
                .toList();
        System.out.println(list);
    }

    public static void findUniqueStrings() {
        List<String> collect = Arrays.stream(string.split(" "))
                .distinct()
                .toList();
        System.out.println(collect);
    }

    public static void findWordsContainsVowels() {
        List<String> list = Arrays.stream(string.split(" "))
                .filter(w -> w.matches("(?i)[aeiou]+"))
                .toList();
        System.out.println(list);
    }

    public static void sortStringsByLength() {
        List<String> list = Arrays.stream(string.split(" "))
                .sorted(Comparator.comparingInt(String::length))
                .toList();
        System.out.println(list);
    }

    public static void findMostOccurringString() {
        Optional<String> s = Arrays.stream(string.split(" "))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
        System.out.println(s);
    }

    public static void convertListOfStringsToMapOfStringsAndLen() {
        Map<String, Integer> collect = Arrays.stream(string.split(" "))
                .collect(Collectors.toMap(String::valueOf, String::length, (existing, replacement) -> existing));
        System.out.println(collect);
    }


}
