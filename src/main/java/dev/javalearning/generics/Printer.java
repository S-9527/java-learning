package dev.javalearning.generics;

/**
 * 第 06 课的泛型方法：{@code <T>} 写在返回类型之前。
 *
 * <p>类上的类型参数属于整个类，方法上的类型参数属于这个方法。
 * 一个方法需要"能打印任何类型"时，就给它自己声明一个 T。
 */
public final class Printer {

    private Printer() {
    }

    /** 泛型方法：方法自己声明 T，跟类没关系。 */
    public static <T> void print(T content) {
        System.out.println(content);
    }

    /**
     * 有界的泛型方法：T 必须是 {@link Number} 的子类。
     *
     * <p>约束之后，方法体里就能放心调用 {@code Number} 的方法了。
     */
    public static <T extends Number> double half(T number) {
        return number.doubleValue() / 2;
    }
}
