package com.java8.features.entity;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class Employee {

    Integer id;
    String name;
    String department;
    Double salary;
    Integer age;


    public static List<Employee> getDummyEmployeeList() {
        Employee employee = Employee.builder().id(1).name("Sai").age(23).department("IT").salary(12000.0).build();
        Employee employee1 = Employee.builder().id(2).name("Jack").age(33).department("IT").salary(150000.0).build();
        Employee employee2 = Employee.builder().id(3).name("Sri").age(24).department("DEV").salary(25000.0).build();
        Employee employee3 = Employee.builder().id(4).name("Guru").age(26).department("DEV").salary(28000.0).build();
        Employee employee4 = Employee.builder().id(5).name("Vivek").age(29).department("DEV").salary(30000.0).build();
        Employee employee5 = Employee.builder().id(6).name("Sasi").age(25).department("Testing").salary(45000.0).build();
        return List.of(employee, employee1, employee2, employee3, employee4, employee5);
    }

}
