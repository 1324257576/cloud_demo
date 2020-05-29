package com.demo.cloud.code;

import java.util.Arrays;
import java.util.Scanner;

/**
 * @author lqq
 * @date 2020/4/16
 * 完全背包
 */
public class NAll {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        //物品数量
        int N = scanner.nextInt();
        //最大容积
        int V = scanner.nextInt();

        //每个物品的体积
        int[] vArr = new int[N + 1];
        //每个物品的价值
        int[] wArr = new int[N + 1];

        for (int i = 1; i <= N; i++) {
            int vi = scanner.nextInt();
            int wi = scanner.nextInt();

            vArr[i] = vi;
            wArr[i] = wi;

        }

        int[] rArr = new int[V + 1];
        for (int i = 1; i <= N; i++) {
            for (int j = vArr[i]; j <= V; j++) {
                rArr[j] = Math.max(rArr[j], rArr[j - vArr[i]] + wArr[i]);
            }
            System.out.println(Arrays.toString(rArr));
        }

        System.out.println(rArr[V]);
    }
}
