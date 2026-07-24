package com.java8.features.arrays.level1;

import java.util.Arrays;

public class MaxElementInArr {
    static void main() {
        int[] maxArr = new int[8];
        maxArr[1] = 1000;
        maxArr[0] = 1000;
        maxArr[2] = 30;
        maxArr[3] = 20;
        maxArr[4] = 23;
        maxArr[5] = 45;
        maxArr[6] = 231;
        maxArr[7] = 2;
        int max = 0;
        int elemment = 0;
        System.out.println(Arrays.toString(maxArr));
        for (int i = 0; i < maxArr.length; i++) {
            if (maxArr[i] > max) {
                max = maxArr[i];
                elemment = i;
            }
        }
        System.out.println("Maximum element and value "+ elemment + " " + max);
    }
}
