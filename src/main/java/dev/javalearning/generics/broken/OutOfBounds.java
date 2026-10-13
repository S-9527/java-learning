package dev.javalearning.generics.broken;

import dev.javalearning.generics.NumberBox;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>{@code NumberBox<T extends Number>} 规定了 T 必须是 Number 的子类，
 * 传 String 就违反了上界，编译器直接拒绝。
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class OutOfBounds {

    public static void main(String[] args) {
        NumberBox<String> box = new NumberBox<>("不是数字");   // ← String 不在 Number 的范围内
        System.out.println(box.get());
    }
}
