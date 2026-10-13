package dev.javalearning.generics.broken;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>{@code ? extends Number} 是<b>生产者</b>：编译器只知道里面装的是「某种
 * Number 的子类」，但不知道到底是 Integer 还是 Double，所以不允许往里写。
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class ProducerCannotAdd {

    public static void main(String[] args) {
        List<? extends Number> producer = new ArrayList<Integer>();
        producer.add(42);   // ← ? extends Number 不让写
        System.out.println(producer);
    }
}
