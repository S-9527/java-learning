package dev.javaquest.basics.classes;

/**
 * 一个没有写任何构造器的类。
 *
 * <p>字段故意保持包内可见（不写修饰符），这样测试能直接读到它们的默认值。
 * 真实代码里字段应该写成 {@code private}，再配 getter——见
 * {@link Point}。
 */
public final class Bare {

    int number;
    boolean flag;
    String text;
    int[] numbers;
}
