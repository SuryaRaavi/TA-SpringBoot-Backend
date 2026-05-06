package com.ta.managementproject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FunctionalExample {

    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // map() — mengalikan setiap elemen dengan 2
        List<Integer> doubled = numbers.stream()
                .map(n -> n * 2)
                .collect(Collectors.toList());

        // filter() + map() — ambil bilangan genap lalu ubah ke String
        List<String> evenStrings = numbers.stream()
                .filter(n -> n % 2 == 0)
                .map(n -> "Number: " + n)
                .collect(Collectors.toList());

        // reduce() — menjumlahkan semua elemen
        int sum = numbers.stream()
                .reduce(0, (acc, n) -> acc + n);

        // reduce() — mencari nilai maksimum
        int max = numbers.stream()
                .reduce(Integer.MIN_VALUE, (a, b) -> a > b ? a : b);

        // forEach() — mencetak setiap elemen
        doubled.forEach(n -> System.out.println("Doubled: " + n));

        // forEach() dengan method reference (bentuk anonymous function)
        evenStrings.forEach(System.out::println);

        // groupingBy — mengelompokkan berdasarkan genap/ganjil
        Map<String, List<Integer>> grouped = numbers.stream()
                .collect(Collectors.groupingBy(n -> n % 2 == 0 ? "EVEN" : "ODD"));

        grouped.forEach((key, values) ->
                System.out.println(key + " -> " + values));

        System.out.println("Sum: " + sum);
        System.out.println("Max: " + max);
    }
}