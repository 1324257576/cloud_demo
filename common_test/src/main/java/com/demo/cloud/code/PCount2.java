package com.demo.cloud.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @author lqq
 * @date 2020/4/16
 * 2的幂次分解的多重背包
 */
public class PCount2 {

    public static void main(String[] args) {

        int MAX = 2000;
        Scanner scanner = new Scanner(System.in);
        //物品数量
        int n = scanner.nextInt();
        //最大容积
        int v = scanner.nextInt();

        //每个物品的体积
        List<Integer> vList = new ArrayList<>();
        //每个物品的价值
        List<Integer> wList = new ArrayList<>();


        for (int i = 1; i <= n; i++) {
            int vi = scanner.nextInt();
            int wi = scanner.nextInt();
            int si = scanner.nextInt();


            for (int k = 1; k <= si; k <<= 1) {
                vList.add(vi * k);
                wList.add(wi * k);
                si -= k;
            }
            if (si > 0) {
                vList.add(vi * si);
                wList.add(wi * si);
            }

        }

        int[] vArr = new int[vList.size() + 1];
        int[] wArr = new int[wList.size() + 1];

        for (int i = 0; i < vList.size(); i++) {
            vArr[i + 1] = vList.get(i);
            wArr[i + 1] = wList.get(i);
        }


        int[][] r = new int[vArr.length][v + 1];
        for (int i = 1; i <= r.length - 1; i++) {
            for (int j = 1; j <= v; j++) {
                r[i][j] = r[i - 1][j];

                if (vArr[i] <= j) {
                    r[i][j] = Math.max(r[i][j], r[i - 1][j - vArr[i]] + wArr[i]);
                }
            }
            System.out.println(Arrays.toString(r[i]));
        }

        System.out.println(r[n][v]);
    }
}
