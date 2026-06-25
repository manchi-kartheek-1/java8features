package com.java8.features.easy;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class StringPrograms {

    public static String testString = "Java Programming";

    static void main() {
        StringPrograms.countOccurrenceInString();
        StringPrograms.convertingToUpperCase();
        StringPrograms.findStringStartsWithChar();
        StringPrograms.findRepeatedWordCountInList();
        StringPrograms.countTheWordsLengthIsGE5();
        StringPrograms.groupTheStringsWithLength();
        StringPrograms.sortListFfStringsAlphabetically();
        StringPrograms.removeDuplicateStringsFromList();
        StringPrograms.FindFirstStringThatStartsWith();
        StringPrograms.joinAllStringsIntoSingleCommaSeparatedString();
        StringPrograms.findLongestStringInList();
        StringPrograms.findShortestStringInList();
        StringPrograms.filterStringsHavingLengthExactly();
        StringPrograms.filterAllStringsAreNonEmpty();
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
        StringPrograms.convertToLowerCase(string);

    }

    public static void convertToLowerCase(String s) {
        String collect = s.chars()
                .mapToObj(c -> (char) Character.toLowerCase(c))
                .map(String::valueOf)
                .collect(Collectors.joining());
        System.out.println(collect);
    }

    public static void findStringStartsWithChar() {
        String string = "principle + charges = installment amount or else report and don't add customer installment";
        String[] split = string.split(" ");
        List<String> list = Arrays.stream(split).filter(s -> s.startsWith("i")).toList();
        System.out.println(list);
    }

    public static void findRepeatedWordCountInList() {
        String string = "principle + charges = installment amount or else report and don't add customer installment";
        String[] split = string.split(" ");
        Map<String, Long> collect = Arrays.stream(split).collect(Collectors.groupingBy(word -> word, Collectors.counting()));
        System.out.println(collect);
    }

    public static void countTheWordsLengthIsGE5() {
        String string = "principle + charges = installment amount or else report and don't add customer installment";
        List<String> list = Arrays.stream(string.split(" ")).filter(word -> word.length() >= 5).toList();
        System.out.println(list);
    }

    public static void groupTheStringsWithLength() {
        String string = "principle + charges = installment amount or else report and don't add customer installment";
        Map<Integer, List<String>> collect = Arrays.stream(string.split(" ")).collect(Collectors.groupingBy(String::length));
        System.out.println(collect);
    }

    public static void sortListFfStringsAlphabetically() {
        String string = "principle + charges = installment amount or else report and don't add customer installment";
        List<String> sorted = Arrays.stream(string.split(" ")).sorted().toList();
        System.out.println(sorted);
    }

    public static void removeDuplicateStringsFromList() {
        String string = "principle + charges = installment amount or else report and don't add customer installment";
        List<String> distinct = Arrays.stream(string.split(" ")).distinct().toList();
        System.out.println(distinct);
    }

    public static void FindFirstStringThatStartsWith() {
        String string = "principle + charges = installment amount or else report and don't add customer installments";
        String s = Arrays.stream(string.split(" ")).filter(word -> word.startsWith("i")).findFirst().get();
        System.out.println(s);
    }

    public static void joinAllStringsIntoSingleCommaSeparatedString() {
        String string = "principle + charges = installment amount or else report and don't add customer installments";
        String collect = Arrays.stream(string.split(" ")).map(String::valueOf).collect(Collectors.joining(","));
        System.out.println(collect);
    }

    public static void findLongestStringInList() {
        String string = "principle + charges = installment amount or else report and don't add customer installmentsss";
        Map<String, Integer> stringIntegerMap = Arrays.stream(string.split(" ")).max(Comparator.comparingInt(String::length))
                .map(word -> Collections.singletonMap(word, word.length()))
                .orElse(Collections.emptyMap());
        System.out.println(stringIntegerMap);
    }

    public static void findShortestStringInList() {
        String string = "principle charges installment amount or else report and don't add customer installmentsss";
        Map<String, Integer> stringIntegerMap = Arrays.stream(string.split(" "))
                .min(Comparator.comparingInt(String::length))
                .map(word -> Collections.singletonMap(word, word.length()))
                .orElse(Collections.emptyMap());
        System.out.println(stringIntegerMap);
    }

    public static void filterStringsHavingLengthExactly() {
        String string = "principle charges installment amount or else report and don't add customer installments";
        List<String> list = Arrays.stream(string.split(" ")).filter(word -> word.length() == 5).toList();
        System.out.println(list);
    }

    public static void filterAllStringsAreNonEmpty() {
        List<String> list = List.of("principle", "charges", "installment", "", " ", "else", "report", "and", "don't", "add", "customer");
        List<String> collect = list.stream().filter(word -> word.isEmpty() && word.isBlank()).collect(Collectors.toList());
        System.out.println(collect);
    }

}
