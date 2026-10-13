package dev.javalearning.generics;

/**
 * 第 06 课：带<b>两个</b>类型参数的泛型类。
 *
 * <p>{@code <K, V>} 里 K 是 key 的类型、V 是 value 的类型，两个都是占位符。
 * {@code Pair<String, Integer>} 和 {@code Pair<Integer, String>} 是两个不同的类型，
 * 虽然用的是同一个类。
 *
 * <p>{@link #of} 是个泛型方法：<b>static 方法不能使用类上的类型参数</b>
 * （类上的 T 是跟着对象走的，static 方法没有对象），所以它自己声明了一份
 * {@code <K, V>}，编译器从参数里把类型推出来。
 */
public class Pair<K, V> {

    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /** 静态工厂：调用时不用写 {@code Pair.<String, Integer>of(...)}，编译器自己推。 */
    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }

    public K key() {
        return key;
    }

    public V value() {
        return value;
    }

    @Override
    public String toString() {
        return "Pair[" + key + " = " + value + "]";
    }
}
