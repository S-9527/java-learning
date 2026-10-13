package dev.javalearning.generics.broken;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>类型擦除之后，运行期没有 {@code List<String>} 这个东西，只有 {@code List}。
 * 所以 {@code instanceof} 没法检查泛型实参——面对一个 {@code Object}，
 * 编译器不允许你写 {@code instanceof List<String>}。
 *
 * <p>（注意：如果变量的静态类型<b>已经</b>是 {@code List<String>}，
 * 那 {@code list instanceof List<String>} 反而能编译——因为编译器已经从
 * 声明里知道了，这个检查没有任何意义。真正的规则是「编译器必须能证明
 * 这个转型是安全的」，而面对 {@code Object} 它证明不了。）
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class InstanceofErased {

    public static void main(String[] args) {
        Object o = new ArrayList<String>();
        System.out.println(o instanceof List<String>);   // ← 擦除后检查不了
    }
}
