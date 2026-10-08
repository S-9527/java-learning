package dev.javaquest.basics.classes;

/**
 * 用来区分「属于类的东西」和「属于对象的东西」。
 *
 * <p>{@code created} 是 {@code static} 的：不管造多少个对象，它只有一份。
 * {@code id} 和 {@code value} 是实例字段：每个对象各有一份。
 */
public final class Counter {

    /** 属于类的计数器，被所有实例共享。 */
    private static int created;

    /** 属于本对象的编号，造对象时自动分配。 */
    private final int id;

    /** 属于本对象的计数值。 */
    private int value;

    public Counter() {
        this.id = ++created;   // ++created 先加后赋，所以第一个对象拿到 1
    }

    public void increment() {
        value++;
    }

    public int getValue() {
        return value;
    }

    public int getId() {
        return id;
    }

    /** static 方法：不依赖任何对象，直接通过类名调用。 */
    public static int getCreated() {
        return created;
    }

    /** 测试专用：把静态计数器归零，避免用例之间互相影响。 */
    public static void resetCreated() {
        created = 0;
    }
}
