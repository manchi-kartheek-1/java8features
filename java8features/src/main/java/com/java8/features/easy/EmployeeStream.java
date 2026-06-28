package com.java8.features.easy;

import java.util.ArrayList;
import java.util.List;

public class EmployeeStream {

    static void main() {
        EmployeeStream employeeStream = new EmployeeStream();
        Employee employee = new Employee(0, "Sai", 10.0, "Dev");
        Employee employee1 = new Employee(1, "Sai 1", 100.0, "Dev");
        Employee employee2 = new Employee(2, "Sai 2", 101.0, "Testing");
        Employee employee3 = new Employee(3, "Sai 3", 104.0, "Dev");
        Employee employee4 = new Employee(4, "Sai 4", 15.0, "Testing");
        Employee employee5 = new Employee(5, "Sai 5", 150.0, "Dev");
        Employee employee6 = new Employee(5, "Sai 6", 80.0, "Testing");
        List<Employee> employeeList = List.of(employee, employee1, employee3, employee2, employee4, employee5, employee6);
        employeeStream.findEmpGGivenSalary(150.00, employeeList);
    }

    public void findEmpGGivenSalary(double salary, List<Employee> employeeList) {
        List<Employee> list = employeeList.stream()
                .filter(emp -> emp.getSalary() > 100.00)
                .toList();
        System.out.println(list);
    }

}
