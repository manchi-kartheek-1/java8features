package com.dsa;

import java.util.Arrays;

public class TwoSum {

    static void main() {
        int[] indexes = TwoSum.getIndexes();
        System.out.println(" " + Arrays.toString(indexes));
    }

    public static int[] getIndexes() {
        int[] a = {1, 2, 3, 4, 5, 6, 7, 8};
        int target = 14;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a.length; j++) {
                if (a[i] == a[j]) {
                    continue;
                }
                if (a[i] + a[j] == target) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{0, 0};
    }

}
