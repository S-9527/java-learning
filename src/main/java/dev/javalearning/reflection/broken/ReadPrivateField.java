package dev.javalearning.reflection.broken;

import dev.javalearning.reflection.User;

/**
 * 第 09 课的反面教材：直接读 {@code User} 的 private 字段。
 *
 * <p><b>这是故意写错的</b>，用来演示"常规访问碰不到 private"，
 * 它编译不过，所以放在 {@code broken/} 里（{@code build.gradle.kts} 已整包排除）。
 * 文档 {@code docs/09-反射.md} 里贴的是真实的 {@code javac} 错误。
 */
public class ReadPrivateField {

    public static void main(String[] args) {
        User user = new User("小明", 18);
        // age 是 private 的，这行编译不过——这正是反射要解决的问题
        System.out.println(user.age);
    }
}
