package com.java8.features.easy;

import java.util.Comparator;
import java.util.List;

public class ListPrograms {

    public static List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 22, 3, 44, 5, 666, 22);

    static void main() {
        ListPrograms.maxNumber();
        ListPrograms.minNumber();
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


}
