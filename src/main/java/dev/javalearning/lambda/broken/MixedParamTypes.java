package dev.javalearning.lambda.broken;

import dev.javalearning.lambda.Calculator;

/**
 * 第 07 课的<b>反面教材</b>：lambda 的参数类型写一半。
 *
 * <p><b>这是故意写错的，编译一定失败。</b> 放在 {@code broken/} 包里是因为
 * {@code build.gradle.kts} 把整个 broken 子包排除出了构建。
 *
 * <p>参数类型只有两种合法状态：<b>全写</b>（{@code (int a, int b)}）或者
 * <b>全省</b>（{@code (a, b)}）。{@code (int a, b)} 这种混着写编译器不认——
 * 它没法判断你是想显式声明还是想让它推断。
 */
public class MixedParamTypes {

    public static void main(String[] args) {
        // ← 编译错误：cannot mix implicitly-typed and explicitly-typed parameters
        Calculator mixed = (int a, b) -> a + b;
        System.out.println(mixed.calculate(3, 4));
    }
}
