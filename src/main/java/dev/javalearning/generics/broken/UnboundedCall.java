package dev.javalearning.generics.broken;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>没有上界的类型参数会被当成 {@code Object} 用，而 {@code Object}
 * 上没有 {@code doubleValue()} 这个方法，所以编译不过。
 * 加上 {@code <T extends Number>} 就能编译了——这正是「有界泛型」的意义：
 * 约束换来了调用 T 身上方法的能力。
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class UnboundedCall {

    public static <T> double half(T number) {
        return number.doubleValue() / 2;   // ← T 没约束，被当成 Object
    }

    public static void main(String[] args) {
        System.out.println(half(10));
    }
}
