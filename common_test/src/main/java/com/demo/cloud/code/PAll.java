package com.demo.cloud.code;

import java.util.Arrays;
import java.util.Scanner;

/**
 * @author lqq
 * @date 2020/4/16
 * 完全背包
 */
public class PAll {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        //物品数量
        int n = scanner.nextInt();
        //最大容积
        int v = scanner.nextInt();

        //每个物品的体积
        int[] vArr = new int[n + 1];
        //每个物品的价值
        int[] wArr = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            int vi = scanner.nextInt();
            int wi = scanner.nextInt();

            vArr[i] = vi;
            wArr[i] = wi;

        }

        int[][] r = new int[n + 1][v + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= v; j++) {
                for (int k = 0; k * vArr[i] <= j; k++) {
                    r[i][j] = Math.max(r[i][j], r[i - 1][j - k * vArr[i]] + k * wArr[i]);
                }
            }
            System.out.println(Arrays.toString(r[i]));
        }

        System.out.println(r[n][v]);
    }
}
