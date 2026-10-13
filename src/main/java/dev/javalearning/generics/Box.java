package dev.javalearning.generics;

/**
 * 第 06 课的泛型类：一个能装任何类型值的盒子。
 *
 * <p>类名后面的 {@code <T>} 是<b>类型参数</b>（type parameter）。T 只是个占位符，
 * 具体是什么类型，由调用方在 {@code new} 的时候决定：
 * {@code new Box<String>("hello")} 里的 T 就是 String，
 * {@code new Box<Integer>(42)} 里的 T 就是 Integer。
 *
 * <p>对比一下 {@link IntegerPrinter} 和 {@link StringPrinter}——那两个类做的是同一件事，
 * 只是类型不同就得各写一份。有了 {@code Box<T>}，一份就够。
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
