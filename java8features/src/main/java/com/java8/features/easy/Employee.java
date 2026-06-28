package com.java8.features.easy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
    int empNo;
    String name;
    Double salary;
    String department;
}
