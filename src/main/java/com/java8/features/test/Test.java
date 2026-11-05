package com.java8.features.test;


import java.util.*;

public class Test {
    static void main() {

        /*============================================*/
        int[] arr1 = {0, 2, 0, 4, 0, 6, 7, 8, 0, 10};
        int ind = 0;
        for (int i = 0; i < arr1.length; i++) {
            if (arr1[i] != 0) {
                int temp = arr1[i];
                arr1[i] = arr1[ind];
                arr1[ind] = temp;
                ind++;
            }
        }
        System.out.println(Arrays.toString(arr1));
        /*============================================*/

        /*============================================*/
        int[] arr2 = {0, 2, 0, 4, 0, 6, 7, 8, 0, 10};
        int index = arr2.length - 1;
        for (int i = arr2.length - 1; i >= 0; i--) {
            if (arr2[i] != 0) {
                int temp = arr2[index];
                arr2[index] = arr2[i];
                arr2[i] = temp;
                index--;
            }
        }
        System.out.println(Arrays.toString(arr2));
        /*============================================*/

        /*============================================*/
        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int left = 0;
        int right = arr.length - 1;
        while (left < right) {
            int temp = arr[left];
            arr[left] = arr[right];
            arr[right] = temp;
            left++;
            right--;
        }
        System.out.println(Arrays.toString(arr));
        /*============================================*/

        /*============================================*/
        Map<Integer, Integer> countIntMap = new HashMap<>();
        for (int i : arr1) {
            countIntMap.put(i, countIntMap.getOrDefault(i, 0) + 1);
        }
        System.out.println(countIntMap);
        /*============================================*/

        /*============================================*/
        String s = "TENET";
        char[] charArray = s.toCharArray();
        Map<Character, Integer> countMap = new HashMap<>();
        for (char c : charArray) {
            countMap.put(c, countMap.getOrDefault(c, 0) + 1);
        }
        System.out.println(countMap);
        /*============================================*/

        /*============================================*/
        String test = "programming";
        Set<Character> ch = new HashSet<>();
        List<Character> string = test.chars().mapToObj(c -> (char) c).filter(c -> !ch.add(c)).toList();
        System.out.println(string);
        char[] charArray1 = test.toCharArray();
        Map<Character, Integer> map = new HashMap<>();
        for (char c : charArray1) {
            map.put(c, map.getOrDefault(c, 0) + 1);
        }

        for (Map.Entry<Character, Integer> entry : map.entrySet()) {
            if (entry.getValue() == 1) {
                System.out.println(entry.getKey());
                break;
            }
        }
        /*============================================*/


        /*============================================*/
        int StrLeft = 0;
        int sRight = charArray.length - 1;
        while (StrLeft < sRight) {
            if (s.charAt(StrLeft) != s.charAt(sRight)) {
                System.out.println("Not Palindrome");
                break;
            }
            StrLeft++;
            sRight--;
        }
        /*============================================*/
    }


    static boolean isPrime(int n) {

        if (n < 2) {
            return false;
        }
        for (int i = 2; i * i <= n; i++) {

            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }
}
