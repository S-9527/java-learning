package dev.javalearning.generics;

/**
 * 第 06 课的泛型类：一个能装任何类型值的盒子。
 *
 * <p>类名后面的 {@code <T>} 是类型参数（type parameter）。T 只是个占位符，
 * 具体是什么类型，由调用方在 {@code new} 的时候决定。
 */
public class Box<T> {

    private T content;

    public Box(T content) {
        this.content = content;
    }

    /** 取出来直接就是 T，调用方不用强转。 */
    public T get() {
        return content;
    }

    public void set(T content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Box[" + content + "]";
    }
}
