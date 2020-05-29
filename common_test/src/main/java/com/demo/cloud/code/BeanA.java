package com.demo.cloud.code;

import java.util.ArrayList;

/**
 * @author lqq
 * @date 2020/4/18
 */
public class BeanA extends BeanAP {

    private float f = 1.0f;


    public class B {

    }


    public static void main(String args[]) throws Exception {
        test("1");
    }


    private static final ArrayList<String> list = new ArrayList<>();

    public static String test(String j) {
        int i = 1, s = 1, f = 1, a = 1, b = 1, c = 1, d = 1, e = 1;
        list.add(new String("11111111111111111111111111111"));
        return test(s + i + f + a + b + c + d + e + "");
    }

}

