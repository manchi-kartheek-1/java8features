package com.java8.features.arrays.level1;

import java.util.Arrays;

public class SecondSmallestElement {
    static void main() {
        int[] minArr = new int[8];
        minArr[1] = 100;
        minArr[0] = 9999;
        minArr[2] = 30;
        minArr[3] = 20;
        minArr[4] = 23;
        minArr[5] = 45;
        minArr[6] = 231;
        minArr[7] = 2;

        Arrays.sort(minArr);
        System.out.println(Arrays.toString(minArr));
        System.out.println(minArr[1]);
        int secondSmallest = 0;
        // Loop to bypass duplicate smallest elements
        int smallest = minArr[0];
        for (int i = 1; i < minArr.length; i++) {
            if (minArr[i] != smallest) {
                secondSmallest = minArr[i]; // Return the first distinct element greater than smallest
                break;
            }
        }
        System.out.println(secondSmallest);
    }
}
