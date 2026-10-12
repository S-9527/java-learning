package dev.javalearning.basics.methods.broken;

/**
 * 第 03 课的反面教材之二：用了离开作用域的局部变量。
 *
 * <p>这个类是<b>故意写错的</b>，放在 {@code broken/} 包里，已经被
 * {@code build.gradle.kts} 排除出构建。编译它，看看报什么错——
 * {@code docs/03-方法.md} 里贴的就是这个错误的真实输出。
 */
public class OutOfScope {

    public static void main(String[] args) {
        {
            int inner = 1;
            System.out.println("花括号里 inner = " + inner);
        }
        System.out.println(inner);   // ← 编译不过：inner 只在上面那个花括号里活着
    }
}
