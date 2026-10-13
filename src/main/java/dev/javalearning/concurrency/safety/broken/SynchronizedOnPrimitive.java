package dev.javalearning.concurrency.safety.broken;

/**
 * 这是<b>故意写错的</b>反面教材：把 synchronized 用在基本类型上。
 *
 * <p>锁必须是一个<b>对象</b>（引用类型）。{@code int} 是基本类型，没有对象身份，
 * 编译器直接拦下来。
 *
 * <p>放 {@code broken/} 子包是因为它<b>编译不过</b>——{@code build.gradle.kts}
 * 已经整包排除，不参与构建。文档里贴的是真实的 javac 错误。
 */
public class SynchronizedOnPrimitive {

    private int count;

    public void increment() {
        synchronized (1) {      // ← 编译错误：required: reference, found: int
            count++;
        }
    }
}
