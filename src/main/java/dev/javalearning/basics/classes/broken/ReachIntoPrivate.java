package dev.javalearning.basics.classes.broken;

import dev.javalearning.basics.classes.Person;

/**
 * 第 04 课的反面教材：从外面直接读 private 字段。
 *
 * <p>这个类是<b>故意写错的</b>，放在 {@code broken/} 包里，已经被
 * {@code build.gradle.kts} 排除出构建。编译它，看看报什么错——
 * {@code docs/04-类与对象.md} 里贴的就是这个错误的真实输出。
 */
public class ReachIntoPrivate {

    public static void main(String[] args) {
        Person albert = new Person("Albert", 30);

        // name 是 private 的，只有 Person 自己能碰
        System.out.println(albert.name);

        // 想拿数据只能走它开的那扇门
        System.out.println(albert.name());
    }
}
