package dev.javalearning.generics;

/**
 * 第 06 课的有界泛型：{@code <T extends Number>}。
 *
 * <p>不加约束时 T 被当成 Object，只能调 Object 的方法。
 * 写成 {@code T extends Number} 之后，T 就被当成 Number 用，
 * {@code doubleValue()}、{@code intValue()} 这些方法就能调了——
 * 代价是调用方只能传 Number 的子类（Integer、Double、Long……），
 * 传 String 会直接编译不过。
 */
public class NumberBox<T extends Number> {

    private final T content;

    public NumberBox(T content) {
        this.content = content;
    }

    public T get() {
        return content;
    }

    /** T 被约束成 Number，所以这里能放心调用 Number 的方法。 */
    public double doubleValue() {
        return content.doubleValue();
    }

    /**
     * 参数用通配符：另一个盒子里的 T 可以是别的 Number 子类。
     *
     * <p>写成 {@code NumberBox<T>} 就要求两边 T 完全一样，
     * {@code NumberBox<Integer>} 就没法和 {@code NumberBox<Double>} 相加了。
     */
    public double plus(NumberBox<? extends Number> other) {
        return doubleValue() + other.doubleValue();
    }

    @Override
    public String toString() {
        return "NumberBox[" + content + "]";
    }
}
