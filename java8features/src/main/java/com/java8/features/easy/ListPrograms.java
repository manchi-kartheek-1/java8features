package com.java8.features.easy;

import java.util.Comparator;
import java.util.List;

public class ListPrograms {

    public static List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 22, 3, 44, 5, 666, 22);

    static void main() {
        ListPrograms.maxNumber();
        ListPrograms.minNumber();
        ListPrograms.findAllEvenNum();
        ListPrograms.findAllEvenOdd();
        ListPrograms.squareEachElementInList();
        ListPrograms.sumEachElementInList();
        ListPrograms.findMaxInList();
        ListPrograms.findMinInList();
        ListPrograms.countTotElements();
        ListPrograms.removeDuplicatesInList();
        ListPrograms.ascList();
        ListPrograms.dscList();
        ListPrograms.findNumGGivenNum();
        ListPrograms.findNumLGivenNumber();
        ListPrograms.doubleNumInList();
        ListPrograms.findAnyNumIsNeg();
        ListPrograms.findAllNumIsPov();
    }

    public static void maxNumber() {

        Integer max = list.stream().distinct().max(Comparator.naturalOrder()).get();
        Integer secondHighest = list.stream().distinct().sorted(Comparator.reverseOrder()).skip(1).findFirst().get();
        System.out.println(max);
        System.out.println(secondHighest);
    }

    public static void minNumber() {

        Integer min = list.stream().distinct().sorted().findFirst().get();
        Integer secondMin = list.stream().distinct().sorted().skip(1).findFirst().get();
        System.out.println(min);
        System.out.println(secondMin);
    }

    public static void findAllEvenNum(){
        List<Integer> list1 = list.stream().filter(i -> i % 2 == 0).toList();
        System.out.println(list1);
    }

    public static void findAllEvenOdd(){
        List<Integer> list1 = list.stream().filter(i -> i % 2 == 1).toList();
        System.out.println(list1);
    }

    public static void squareEachElementInList() {
        List<Integer> list1 = list.stream().map(i -> i * i).toList();
        System.out.println(list1);
    }

    public static void sumEachElementInList() {
        int sum = list.stream().mapToInt(Integer::valueOf).sum();
        Integer reduce = list.stream().reduce(0, Integer::sum);
        System.out.println(reduce);
        System.out.println(sum);
    }

    public static void findMaxInList(){
        Integer i = list.stream().sorted(Comparator.reverseOrder()).findFirst().get();
        Integer i1 = list.stream().max(Comparator.naturalOrder()).get();
        System.out.println(i);
        System.out.println(i1);
    }

    public static void findMinInList(){
        Integer i = list.stream().sorted().findFirst().get();
        Integer i1 = list.stream().min(Comparator.naturalOrder()).get();
        System.out.println(i);
        System.out.println(i1);
    }

    public static void countTotElements(){
        long count = list.stream().count();
        int size = list.size();
        System.out.println(count);
        System.out.println(size);
    }

    public static void removeDuplicatesInList(){
        List<Integer> list1 = list.stream().distinct().toList();
        System.out.println(list1);
    }

    public static void ascList(){
        List<Integer> list1 = list.stream().distinct().sorted().toList();
        System.out.println(list1);
    }

    public static void dscList(){
        List<Integer> list1 = list.stream().distinct().sorted(Comparator.reverseOrder()).toList();
        System.out.println(list1);
    }

    public static void findNumGGivenNum(){
        List<Integer> list1 = list.stream().filter(i -> i > 50).toList();
        System.out.println(list1);
    }

    public static void findNumLGivenNumber(){
        List<Integer> list1 = list.stream().filter(i -> i < 50).toList();
        System.out.println(list1);
    }

    public static void doubleNumInList(){
        List<Integer> list1 = list.stream().map(i -> i + 1).toList();
        System.out.println(list1);
    }

    public static void findAnyNumIsNeg(){
        boolean b = list.stream().anyMatch(i -> i < 0);
        System.out.println(b);
    }

    public static void findAllNumIsPov(){
        boolean b = list.stream().allMatch(i -> i > 0);
        System.out.println(b);
    }


}
