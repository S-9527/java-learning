package dev.javalearning.stream.broken;

import java.util.stream.Stream;

/**
 * 这是<b>故意写错</b>的反面教材：以为对象流也有 {@code sum()}。
 *
 * <p>它<b>编译不过</b>，所以住在 {@code broken/} 包里，看到标红是正常的。
 *
 * <p>{@code sum()} / {@code average()} / {@code summaryStatistics()} 都长在
 * {@code IntStream} 这些<b>数值流</b>上，{@code Stream<Integer>} 一个都没有。
 * 想让对象流求和，得先 {@code mapToInt(...)} 转过去。
 *
 * <p>真实报错（完整输出连同行号和插入符见 docs/10-Stream.md，那里是逐字复制的）：
 * <pre>
 * error: cannot find symbol  →  symbol: method sum()  /  location: interface Stream<Integer>
 * </pre>
 */
public class StreamSum {

    public static void main(String[] args) {
        // 编译不过：Stream<Integer> 没有 sum()
        int total = Stream.of(1, 2, 3).sum();
        System.out.println(total);
    }
}
