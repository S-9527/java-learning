package dev.javalearning.reflection.broken;

import java.util.Map;

/**
 * 第 09 课的反面教材：类名和方法名只存在于运行时。
 *
 * <p><b>这是故意写错的</b>。{@code config.get("className")} 是运行时才拿到的字符串，
 * 可 {@code new} 后面必须跟一个编译期就存在的类名，方法名也一样。
 * 这就是"反射为什么必须存在"最直接的证据。
 *
 * <p>先赋值给局部变量再 {@code new}，是为了让 {@code javac} 报出清楚的那条错
 * （{@code cannot find symbol}）。直接写成 {@code new config.get("className")()}
 * 编译器会先卡在语法上，报 {@code ';' expected}，反而看不出真正的原因。
 *
 * <p>它编译不过，所以放在 {@code broken/} 里。文档里贴真实的 {@code javac} 错误。
 */
public class DynamicNew {

    public static void main(String[] args) {
        Map<String, String> config = Map.of(
                "className", "dev.javalearning.reflection.User",
                "methodName", "hello");

        String className = config.get("className");
        // 编译错误：className 是变量，不是类名
        Object obj = new className();

        String methodName = config.get("methodName");
        // 编译错误：方法名也得在源码里写死
        obj.methodName();
    }
}
