package com.demo.cloud.test;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lqq
 * @date 2020/5/7
 */
public class CommonTest {

    @Test
    public void test() {
        String content = "ab\ncc\r\ndd\r\n\r\nee";

        List<String> lines = Stream.of(content.split("[\r\n]+")).collect(Collectors.toList());
        String lastLine;
        final int remainedLineSize = 3;
        if (lines.size() > remainedLineSize) {
            lastLine = lines.stream().skip(lines.size() - remainedLineSize)
                    .collect(Collectors.joining("\n"));
        } else {
            lastLine = String.join("\n", lines);
        }

        System.out.println(lastLine);
    }


}
