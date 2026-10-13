package dev.javalearning.generics;

/**
 * 第 06 课：没有泛型之前，人们是怎么解决问题的。
 *
 * <p>需求是「打印一个 Integer」。于是写一个 {@code IntegerPrinter}。
 * 要打印 String 呢？再复制一个 {@code StringPrinter}。
 * 每加一种类型就得加一个类——这就是泛型要解决的第一个问题。
 */
public class IntegerPrinter {

    private final Integer content;

    public IntegerPrinter(Integer content) {
        this.content = content;
    }

    public void print() {
        System.out.println(content);
    }
}
