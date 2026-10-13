package dev.javalearning.generics.broken;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>{@code List<String>} <b>不是</b> {@code List<Object>} 的子类型，
 * 哪怕 String 是 Object 的子类。这两个类型之间没有任何继承关系，
 * 所以直接赋值编译不过。
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class StringListIsNotObjectList {

    public static void main(String[] args) {
        List<String> names = new ArrayList<>();
        names.add("Alice");

        List<Object> objects = names;   // ← 编译不过：泛型不协变

        objects.add(42);                // 如果上面那行放行了，这一行就混进来了
        String first = names.get(0);    // 然后这里会炸 ClassCastException
        System.out.println(first);
    }
}
