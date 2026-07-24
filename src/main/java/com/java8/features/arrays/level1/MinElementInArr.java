package com.java8.features.arrays.level1;

import java.util.Arrays;

public class MinElementInArr {
    static void main() {
        int[] minArr = new int[8];
        minArr[0] = -555;
        minArr[1] = 455;
        minArr[2] = 43;
        minArr[3] = 23;
        minArr[4] = 54;
        minArr[5] = 56;
        minArr[6] = 123;
        minArr[7] = 135;
        int min = 50;
        int element = 0;
        for (int i = 0 ; i<minArr.length;i++){
            if (minArr[i] < min){
                min = minArr[i];
                element = i;
            }
        }
        System.out.println(Arrays.toString(minArr));
        System.out.println("Minimum element and value "+ element +" "+min);
    }
}
