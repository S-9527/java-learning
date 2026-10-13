package dev.javalearning.lambda.broken;

import java.util.function.Supplier;

/**
 * 第 07 课的<b>反面教材</b>：在匿名内部类里直接拿 {@code this} 和外层对象比。
 *
 * <p><b>这是故意写错的，编译一定失败。</b> 放在 {@code broken/} 包里是因为
 * {@code build.gradle.kts} 把整个 broken 子包排除出了构建。
 *
 * <p>这个错误是写本课时真的撞上的——本来只想演示「匿名内部类里的 {@code this}
 * 不是外层对象」，顺手写了个 {@code this == outer} 想打印 {@code false}，
 * 结果编译器直接报 {@code incomparable types}。
 *
 * <p>它比 {@code false} 更有说服力：编译器认为「匿名 Supplier」和
 * 「Outer」是两种毫不相干的类型，<b>连比较的资格都没有</b>。
 * 想比只能先都退化成 {@code Object}，那时才会得到 {@code false}。
 */
public class IncomparableThis {

    private final String name = "Outer";

    public Supplier<String> task() {
        IncomparableThis outer = this;
        return new Supplier<>() {
            @Override
            public String get() {
                // ← 编译错误：incomparable types
                return "和外层是同一个对象：" + (this == outer);
            }
        };
    }
}
