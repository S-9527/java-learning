package dev.javalearning.concurrency.jmm;

/**
 * 第 14 课的类：双重检查锁定（Double-Checked Locking）单例。
 *
 * <p>{@code instance} 前面的 {@code volatile} 不是可有可无的装饰。
 *
 * <p>{@code new Singleton()} 至少三步：分配内存 → 初始化对象 → 把引用赋给 instance。
 * 没有 volatile 时，第 2 步和第 3 步可能被重排成"先赋值、后初始化"，
 * 于是别的线程可能通过第一次 null 检查、拿到一个<b>还没初始化完的对象</b>。
 * volatile 禁止这种重排。
 */
public final class Singleton {

    /** volatile 在这里的作用是禁止 new 的三步被重排。 */
    private static volatile Singleton instance;

    private final String createdAt;

    private Singleton() {
        this.createdAt = "初始化完成";
    }

    public static Singleton getInstance() {
        if (instance == null) {                 // 第一次检查：不加锁，图快
            synchronized (Singleton.class) {
                if (instance == null) {         // 第二次检查：防止多个线程都通过了第一次
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }

    @Override
    public String toString() {
        return "Singleton[" + createdAt + "]";
    }
}
