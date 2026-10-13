package dev.javalearning.lambda.broken;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * 第 07 课的<b>反面教材</b>：往没声明异常的 lambda 里抛受检异常。
 *
 * <p><b>这是故意写错的，编译一定失败。</b> 放在 {@code broken/} 包里是因为
 * {@code build.gradle.kts} 把整个 broken 子包排除出了构建。
 *
 * <p>想说的是：lambda 的方法体受<b>目标接口那个方法</b>的签名约束。
 * {@code Consumer.accept} 没写 {@code throws}，所以 lambda 里就不能抛受检异常。
 * 对比 {@code dev.javalearning.lambda.TextReader}——它声明了 {@code throws IOException}，
 * 同样的 lambda 在那边就能编译过。
 */
public class CheckedExceptionInLambda {

    public static void main(String[] args) {
        // ← 编译错误：unreported exception IOException; must be caught or declared to be thrown
        Consumer<String> reader = path -> {
            throw new IOException("文件不存在：" + path);
        };
        reader.accept("/tmp/不存在.txt");
    }
}
