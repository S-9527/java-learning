package dev.javalearning.stream;

import java.util.List;

/**
 * 第 10 课的类：订单。一个订单里有一串商品。
 *
 * <p>它是为了讲 {@code flatMap} 准备的：{@code Order} 里套着一层 {@code List}，
 * 于是"所有订单的所有商品"这种需求，用 {@code map} 只能得到
 * {@code Stream<List<String>>}，必须用 {@code flatMap} 才能压平。
 */
public class Order {

    private final String id;
    private final List<String> items;

    public Order(String id, List<String> items) {
        this.id = id;
        this.items = List.copyOf(items);
    }

    public String id() {
        return id;
    }

    public List<String> items() {
        return items;
    }

    @Override
    public String toString() {
        return id + items;
    }
}
