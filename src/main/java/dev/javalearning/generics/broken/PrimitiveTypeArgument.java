package dev.javalearning.generics.broken;

import dev.javalearning.generics.Box;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>类型参数不能是基本类型。{@code Box<int>} 编译不过，
 * 必须写 {@code Box<Integer>}（装箱类型）。
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class PrimitiveTypeArgument {

    public static void main(String[] args) {
        Box<int> box = new Box<>(42);   // ← int 不能当类型参数
        System.out.println(box.get());
    }
}
