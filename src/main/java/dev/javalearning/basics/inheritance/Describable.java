package dev.javalearning.basics.inheritance;

/**
 * 第 05 课的接口示例：能描述自己的东西。
 *
 * <p>抽象类是"是什么"，接口是"能做到什么"。{@link Shape} 继承了这个接口，
 * 于是所有图形都保证会 {@link #name()}，也就白拿到 {@link #label()}。
 */
public interface Describable {

    /** 抽象方法：不写修饰符，编译器会补上 public abstract。 */
    String name();

    /** 默认方法：给了现成的做法，实现者可以不重写。 */
    default String label() {
        return "<" + name() + ">";
    }
}
