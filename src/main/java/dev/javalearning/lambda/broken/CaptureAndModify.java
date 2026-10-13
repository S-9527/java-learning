package dev.javalearning.lambda.broken;

import dev.javalearning.lambda.Message;

/**
 * 第 07 课的<b>反面教材</b>：在 lambda 里修改外层的局部变量。
 *
 * <p><b>这是故意写错的，编译一定失败。</b> 放在 {@code broken/} 包里是因为
 * {@code build.gradle.kts} 把整个 broken 子包排除出了构建。
 *
 * <p>要证明的是：lambda 能<b>读</b>外面的局部变量，但那个变量必须是
 * <b>事实上的 final</b>（effectively final）——声明之后没再被重新赋值过。
 * 下面这个 {@code prefix} 先被 lambda 读、又被 lambda 改，于是编译器直接拦住。
 */
public class CaptureAndModify {

    public static void main(String[] args) {
        String prefix = "[捕获] ";          // 这里还没问题

        Message m = text -> {
            prefix = prefix + text;          // ← 编译错误就在这一行
            System.out.println(prefix);
        };

        m.send("试试能不能编译");
    }
}
