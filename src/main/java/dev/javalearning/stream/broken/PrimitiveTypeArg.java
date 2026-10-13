package dev.javalearning.stream.broken;

import java.util.stream.Stream;

/**
 * 这是<b>故意写错</b>的反面教材：想让流直接装基本类型 {@code int}。
 *
 * <p>它<b>编译不过</b>，所以住在 {@code broken/} 包里，看到标红是正常的。
 *
 * <p>泛型的类型参数只能是<b>引用类型</b>，{@code int} 不是。这就是
 * {@code Stream<Integer>} 和 {@code IntStream} 同时存在的原因：
 * 后者是 Java 为了避免大量装箱专门给基本类型开的直通车。
 * 想深挖这条规则，见 {@code docs/06-泛型.md}。
 *
 * <p>真实报错（完整输出连同行号和插入符见 docs/10-Stream.md，那里是逐字复制的）：
 * <pre>
 * error: unexpected type  →  required: reference  /  found: int
 * </pre>
 */
public class PrimitiveTypeArg {

    public static void main(String[] args) {
        Stream<int> numbers = Stream.of(1, 2, 3);   // ← 编译不过：类型参数不能是基本类型
        System.out.println(numbers.count());
    }
}
