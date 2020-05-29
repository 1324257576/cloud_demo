package com.demo.cloud.code;

import java.util.Arrays;
import java.util.Scanner;

/**
 * @author lqq
 * @date 2020/4/17
 */
public class NOnce {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int N = scanner.nextInt();
        int V = scanner.nextInt();

        int[] vArr = new int[N + 1];
        int[] wArr = new int[N + 1];
        for (int i = 1; i <= N; i++) {
            int v = scanner.nextInt();
            int w = scanner.nextInt();

            vArr[i] = v;
            wArr[i] = w;
        }


        System.out.println(Arrays.toString(vArr));
        System.out.println(Arrays.toString(wArr));


        int[] rArr = new int[V + 1];

        for (int i = 1; i <= N; i++) {
            for (int j = V; j >= vArr[i]; j--) {

                rArr[j] = Math.max(rArr[j], rArr[j - vArr[i]] + wArr[i]);

            }

            System.out.println(Arrays.toString(rArr));
        }
        System.out.println(rArr[V]);
    }
}
