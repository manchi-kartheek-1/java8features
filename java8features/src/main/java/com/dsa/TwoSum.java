package com.dsa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TwoSum {

    static void main() {
        int[] a = {1, 2, 3, 4, 5, 6, 7, 8};
        int target = 13;
        int[] indexes = TwoSum.getByBruteForce(a, target);
        int[] indexByHashing = TwoSum.getByHashing(a, target);
        System.out.println(" " + Arrays.toString(indexes));
        System.out.println(" " + Arrays.toString(indexByHashing));
    }

    //Bruteforce Solution O(n2)
    public static int[] getByBruteForce(int[] a, int target) {
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

    //By Hashing Technique O(n)
    public static int[] getByHashing(int[] a, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < a.length; i++) {
            int complement = target - a[i];
            if (map.containsKey(complement)) {
                return new int[]{map.get(complement), i};
            }
            map.put(a[i], i);
        }
        return new int[]{0, 0};
    }

}
