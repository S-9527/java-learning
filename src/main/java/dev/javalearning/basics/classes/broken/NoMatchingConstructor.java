package dev.javalearning.basics.classes.broken;

import dev.javalearning.basics.classes.Person;

/**
 * 第 04 课的反面教材：调用不存在的无参构造器。
 *
 * <p>这个类是<b>故意写错的</b>，放在 {@code broken/} 包里，已经被
 * {@code build.gradle.kts} 排除出构建。编译它，看看报什么错——
 * {@code docs/04-类与对象.md} 里贴的就是这个错误的真实输出。
 *
 * <p>想说明的规则：一旦你自己写了任何构造器，编译器就不再送无参的那个。
 */
public class NoMatchingConstructor {

    public static void main(String[] args) {
        Person p = new Person();   // ← Person 只有 Person(String, int) 这一个构造器
    }
}
