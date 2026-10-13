package dev.javalearning.reflection;

/**
 * 只在「谁触发了类初始化」这一节里被引用，本身没有别的用途。
 *
 * <p>它存在的理由：{@code User} 在第 1 节就已经被 {@code new User(...)} 初始化了，
 * 之后再拿 {@code User.class} 或 {@code Class.forName} 都看不到区别。
 * 要演示「{@code .class} 不初始化、{@code Class.forName} 初始化」，
 * 必须有一个<b>从头到尾没被碰过</b>的类，所以就有了这个 {@code HeavyInit}。
 *
 * <p>构造器故意写成 private，顺便给「{@code getConstructors()} 只看 public」当例子。
 */
public class HeavyInit {

    static {
        // 这行输出就是「类被初始化了」的证据，一辈子只打一次
        System.out.println("  → [HeavyInit 的 static 块执行了：这个类被初始化了]");
    }

    private HeavyInit() {
    }

    /** 只是让这个类看起来有点用，本课不会调它。 */
    public static String describe() {
        return "HeavyInit";
    }
}
