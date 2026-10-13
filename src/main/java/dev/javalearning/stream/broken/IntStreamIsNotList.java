package dev.javalearning.stream.broken;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 这是<b>故意写错</b>的反面教材：以为 {@code IntStream} 就是 {@code List<Integer>}。
 *
 * <p>它<b>编译不过</b>，所以住在 {@code broken/} 包里，看到标红是正常的。
 *
 * <p>流不是集合：{@code IntStream} 既不是 {@code List}，也不是
 * {@code Stream<Integer>}。它是一条「只能被消费一次」的数据管道，
 * 想变成集合必须走一次终端操作（{@code boxed().collect(...)} 或 {@code toArray()}）。
 *
 * <p>真实报错（完整输出连同行号和插入符见 docs/10-Stream.md，那里是逐字复制的）：
 * <pre>
 * error: incompatible types: IntStream cannot be converted to List<Integer>
 * </pre>
 */
public class IntStreamIsNotList {

    public static void main(String[] args) {
        // 编译不过：IntStream 不是 List<Integer>
        List<Integer> numbers = IntStream.range(1, 5);
        System.out.println(numbers);
    }
}
