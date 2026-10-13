package dev.javalearning.generics;

/**
 * 第 06 课：{@code IntegerPrinter} 的复制品，只是类型换成了 String。
 *
 * <p>它和 {@code IntegerPrinter} 的逻辑一模一样，只有类型不同。
 * 再加一个类型就再加一个类——这就是"代码重复"，泛型要解决的就是它。
 */
public class StringPrinter {

    private final String content;

    public StringPrinter(String content) {
        this.content = content;
    }

    public void print() {
        System.out.println(content);
    }
}
