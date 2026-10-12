package dev.javalearning.lesson01.broken;

/**
 * 第 01 课的反面教材：两个 byte 相加。
 *
 * <p>这个类是<b>故意写错的</b>，放在 {@code broken/} 包里，已经被
 * {@code build.gradle.kts} 排除出构建。编译它，看看报什么错——
 * {@code docs/01-变量与类型.md} 里贴的就是这个错误的真实输出。
 */
public class ByteSum {

    public static void main(String[] args) {
        byte a = 100;
        byte b = 100;

        byte sum = a + b;   // ← 就是这一行编译不过

        System.out.println(sum);
    }
}
