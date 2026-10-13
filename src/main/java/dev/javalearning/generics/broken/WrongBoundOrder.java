package dev.javalearning.generics.broken;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>多个上界用 {@code &} 连接时，<b>类必须写在接口前面</b>。
 * 下面把接口 {@code Comparable} 写在了类 {@code Number} 前面，
 * 编译器直接拒绝：类型变量的第一个上界必须是类。
 *
 * <p>正确的写法是 {@code <T extends Number & Comparable<T>>}。
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class WrongBoundOrder {

    /** ← 顺序写反了：接口排在了类前面。 */
    public static <T extends Comparable<T> & Number> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static void main(String[] args) {
        System.out.println(max(1, 2));
    }
}
