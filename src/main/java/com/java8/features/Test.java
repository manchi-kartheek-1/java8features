package com.java8.features;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;


public class Test {
    public static void main(String[] args) {
        Employee1 employee1 = new Employee1(1, "IT", 500.0, 5);
        Employee1 employee2 = new Employee1(2, "NON-IT", 150.0, 6);
        Employee1 employee3 = new Employee1(3, "NON-IT", 250.0, 7);
        Employee1 employee4 = new Employee1(4, "IT", 550.0, 6);
        List<Employee1> empList = new ArrayList<>();
        empList.add(employee1);
        empList.add(employee2);
        empList.add(employee3);
        empList.add(employee4);
        System.out.println(empList);
        List<Employee1> list = empList.stream().sorted(Comparator.comparingDouble(Employee1::getSalary).reversed()).toList();
        Map<String, Long> collect = list.stream().collect(Collectors.groupingBy(Employee1::getDept, Collectors.counting()));

        System.out.println(collect);

    }
}

/*
* public class Employee{

find the total no of employees in the company
calculate the avg sal of employee
filter the emp based on yoe and count
count the emp > 5 yrs
* */

@Data
@NoArgsConstructor
class Employee1 {
    int id;
    String dept;
    Double salary;
    int yoe;

    public Employee1(int id, String dept, Double salary, int yoe) {
        this.id = id;
        this.dept = dept;
        this.salary = salary;
        this.yoe = yoe;
    }
}



