package com.java8.features.easy;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EmployeeStream {

    static void main() {
        EmployeeStream employeeStream = new EmployeeStream();
        Employee employee = new Employee(0, "Sai", 10.0, "Dev");
        Employee employee1 = new Employee(1, "Sai", 100.0, "Dev");
        Employee employee2 = new Employee(2, "Sai 2", 101.0, "Testing");
        Employee employee3 = new Employee(3, "Sai 3", 104.0, "Dev");
        Employee employee4 = new Employee(4, "Sai 4", 15.0, "Testing");
        Employee employee5 = new Employee(5, "Sai 5", 150.0, "Dev");
        Employee employee6 = new Employee(6, "M Sai 6", 80.0, "Testing");
        List<Employee> employeeList = List.of(employee, employee1, employee3, employee2,
                employee4, employee5, employee6);
        employeeStream.findEmpGGivenSalary(150.00, employeeList);
        employeeStream.findHighestPaidSalary(employeeList);
        employeeStream.findLowestPaidSalary(employeeList);
        employeeStream.groupEmpByDep(employeeList);
        employeeStream.getAvgSalByDept(employeeList);
        employeeStream.getMaxSalByDept(employeeList);
        employeeStream.getMinSalByDept(employeeList);
        employeeStream.getCountOfEmpByDept(employeeList);
        employeeStream.findSecondHighestSal(employeeList);
        employeeStream.findTop_3_Highest_Salary_Emp(employeeList);
        employeeStream.sortEmpBySalaryDes(employeeList);
        employeeStream.sortEmpByDeptSalary(employeeList);
        employeeStream.findDeptWithHighestSal(employeeList);
        employeeStream.findDeptWittMostEmp(employeeList);
        employeeStream.convertEmployeeListToMap(employeeList);
        employeeStream.partitionEmpBySalaryG_150(employeeList);
    }

    public void findEmpGGivenSalary(double salary, List<Employee> employeeList) {
        List<Employee> list = employeeList.stream()
                .filter(emp -> emp.getSalary() > 100.00)
                .toList();
        System.out.println(list);
    }


    public void findHighestPaidSalary(List<Employee> employeeList) {
        Employee employee = employeeList.stream()
                .max(Comparator.comparingDouble(Employee::getSalary))
                .orElse(null);
        System.out.println(employee);
    }

    public void findLowestPaidSalary(List<Employee> employeeList) {
        Employee employee = employeeList.stream()
                .min(Comparator.comparingDouble(Employee::getSalary))
                .orElse(null);
        System.out.println(employee);
    }

    public void groupEmpByDep(List<Employee> employeeList) {
        Map<String, List<Employee>> collect = employeeList.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment));
        System.out.println(collect);
    }

    public void getAvgSalByDept(List<Employee> employeeList) {
        Map<String, Double> collect = employeeList.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment,
                        Collectors.averagingDouble(Employee::getSalary)));
        System.out.println(collect);
    }

    public void getMaxSalByDept(List<Employee> employeeList) {
        Map<String, Optional<Employee>> collect = employeeList.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment,
                        Collectors.maxBy(Comparator.comparingDouble(Employee::getSalary))));
        System.out.println(collect);
    }

    public void getMinSalByDept(List<Employee> employeeList) {
        Map<String, Optional<Employee>> collect = employeeList.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment,
                        Collectors.minBy(Comparator.comparingDouble(Employee::getSalary))));
        System.out.println(collect);
    }

    public void getCountOfEmpByDept(List<Employee> employeeList) {
        Map<String, Long> collect = employeeList.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment,
                        Collectors.counting()));
        System.out.println(collect);
    }

    public void findDuplicateNames(List<Employee> employeeList) {
        Set<String> strings = new HashSet<>();
        List<String> list = employeeList.stream()
                .map(Employee::getName)
                .filter(name -> !strings.add(name))
                .toList();
        System.out.println(list);
    }

    public void findSecondHighestSal(List<Employee> employeeList) {
        Employee employee = employeeList.stream().sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
                .skip(1)
                .findFirst().get();
        System.out.println(employee);
    }

    public void findTop_3_Highest_Salary_Emp(List<Employee> employeeList) {
        List<Employee> list = employeeList.stream().sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
                .limit(3)
                .toList();
        System.out.println(list);
    }

    public void sortEmpBySalaryDes(List<Employee> employeeList) {
        List<Employee> list = employeeList.stream().sorted(Comparator.comparingDouble(Employee::getSalary).reversed()).toList();
        System.out.println(list);
    }

    public void sortEmpByDeptSalary(List<Employee> employeeList) {
        List<Employee> list = employeeList.stream()
                .sorted(Comparator.comparing(Employee::getDepartment)
                        .thenComparing(Employee::getSalary))
                .toList();
        System.out.println(list);
    }

    public void findDeptWithHighestSal(List<Employee> employeeList) {
        String dept = employeeList.stream()
                .min(Comparator.comparing(Employee::getSalary)
                        .reversed()
                        .thenComparing(Employee::getDepartment))
                .get()
                .getDepartment();
        System.out.println(dept);
    }

    public void findDeptWittMostEmp(List<Employee> employeeList) {
        Optional<Map.Entry<String, Long>> max = employeeList.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue());
        System.out.println(max);
    }

    public void convertEmployeeListToMap(List<Employee> employeeList) {
        Map<Integer, Employee> collect = employeeList.stream()
                .collect(Collectors.toMap(Employee::getEmpNo, Function.identity()));
        System.out.println(collect);
    }

    public void partitionEmpBySalaryG_150(List<Employee> employeeList) {
        Map<Boolean, List<Employee>> collect = employeeList.stream()
                .collect(Collectors.partitioningBy(employee -> employee.getSalary() > 10));
        System.out.println(collect);
    }

}
