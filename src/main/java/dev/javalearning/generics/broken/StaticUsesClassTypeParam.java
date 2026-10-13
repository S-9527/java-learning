package dev.javalearning.generics.broken;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>类上的类型参数属于<b>对象</b>（每个 {@code Box<String>} 和
 * {@code Box<Integer>} 的 T 不一样），而 static 方法没有对象，
 * 所以 static 方法里不能引用类上的 T。
 *
 * <p>要写泛型 static 方法，必须给它<b>自己</b>声明一份类型参数——
 * 就是 {@code Pair.of} 和 {@code Printer.print} 的写法：
 * {@code public static <T> T create(...)}。
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class StaticUsesClassTypeParam<T> {

    private T content;

    public static T create() {   // ← static 方法里没有 T
        return null;
    }

    public static void main(String[] args) {
        System.out.println(create());
    }
}
