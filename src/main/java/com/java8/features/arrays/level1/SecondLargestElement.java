package com.java8.features.arrays.level1;

import java.util.Arrays;

public class SecondLargestElement {
    static void main() {
        int[] maxArr = new int[8];
        maxArr[1] = 100;
        maxArr[0] = 9999;
        maxArr[2] = 30;
        maxArr[3] = 20;
        maxArr[4] = 23;
        maxArr[5] = 45;
        maxArr[6] = 231;
        maxArr[7] = 2;
        System.out.println(Arrays.toString(maxArr));
        // Sort the array in ascending order
        Arrays.sort(maxArr);
        System.out.println(Arrays.toString(maxArr));
        int secondLargestElement = 0;
        int n = maxArr.length;
        int largest = maxArr[n - 1];

        // Traverse backward to find the first element distinct from the largest
        for (int i = n - 2; i >= 0; i--) {
            if (maxArr[i] != largest) {
                secondLargestElement = maxArr[i];
                break;
            }
        }
        System.out.println(secondLargestElement);
    }
}
