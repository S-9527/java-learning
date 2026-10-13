package dev.javalearning.generics.broken;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>泛型集合的 {@code add} 参数类型就是类型实参，往里塞别的东西编译不过。
 * 这正是 {@code List<Object>} 换不来的东西：错误在编译期就被挡住了，
 * 不用等到运行期强转才炸。
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class AddWrongType {

    public static void main(String[] args) {
        List<String> names = new ArrayList<>();
        names.add("Alice");
        names.add(42);   // ← List<String> 里塞不进 int
        System.out.println(names);
    }
}
