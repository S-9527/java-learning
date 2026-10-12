package dev.javalearning.basics.methods.broken;

/**
 * 第 03 课的反面教材之一：只有返回值不同的「重载」。
 *
 * <p>这个类是<b>故意写错的</b>，放在 {@code broken/} 包里，已经被
 * {@code build.gradle.kts} 排除出构建。编译它，看看报什么错——
 * {@code docs/03-方法.md} 里贴的就是这个错误的真实输出。
 */
public class ReturnTypeOnly {

    /** 只看这一行：和下面那个方法参数列表一模一样，只有返回类型不同。 */
    public int twice(int x) {
        return x * 2;
    }

    public long twice(int x) {   // ← 编译不过：twice(int) 已经被定义过了
        return x * 2L;
    }
}
