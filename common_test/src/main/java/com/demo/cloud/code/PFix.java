package com.demo.cloud.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @author lqq
 * @date 2020/4/16
 * <p>
 * //混合背包
 */
public class PFix {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        //物品数量
        int n = scanner.nextInt();
        //最大容积
        int v = scanner.nextInt();

        //每个物品的体积
        List<Integer> vList = new ArrayList<>();
        //每个物品的价值
        List<Integer> wList = new ArrayList<>();

        //每个物品的个数
        List<Integer> sList = new ArrayList<>();

        for (int i = 1; i <= n; i++) {
            int wi = scanner.nextInt();
            int vi = scanner.nextInt();
            //si -1:只能用1次 0：无限 >0：可以使用si次
            int si = scanner.nextInt();
            if (si == -1) {
                vList.add(vi);
                wList.add(wi);
                sList.add(-1);
            } else if (si == 0) {
                vList.add(vi);
                wList.add(wi);
                sList.add(0);
            } else {
                //多重背包 分解成01背包
                for (int k = 1; k <= si; k <<= 1) {
                    vList.add(vi * k);
                    wList.add(wi * k);
                    si -= k;
                    sList.add(-1);
                }
                if (si > 0) {
                    vList.add(vi * si);
                    wList.add(wi * si);
                    sList.add(-1);
                }
            }
        }

        int[] vArr = new int[vList.size() + 1];
        int[] wArr = new int[wList.size() + 1];
        int[] sArr = new int[sList.size() + 1];

        for (int i = 0; i < vList.size(); i++) {
            vArr[i + 1] = vList.get(i);
            wArr[i + 1] = wList.get(i);
            sArr[i + 1] = sList.get(i);
        }

        System.out.println(Arrays.toString(vArr));
        System.out.println(Arrays.toString(wArr));
        System.out.println(Arrays.toString(sArr));

        int[][] r = new int[vArr.length][v + 1];
        for (int i = 1; i < vArr.length; i++) {


            if (sArr[i] == -1) {
                //01

                for (int j = 1; j <= v; j++) {
                    r[i][j] = r[i - 1][j];
                    if (wArr[i] <= j) {
                        r[i][j] = Math.max(r[i][j], r[i - 1][j - wArr[i]] + vArr[i]);
                    }
                }
            } else {
                //完全

                for (int j = 1; j <= v; j++) {
                    for (int k = 0; k * wArr[i] <= j; k++) {
                        r[i][j] = Math.max(r[i][j], r[i - 1][j - wArr[i] * k] + vArr[i] * k);
                    }
                }
            }

            System.out.println(Arrays.toString(r[i]));
        }

        System.out.println(r[n][v]);
    }
}
