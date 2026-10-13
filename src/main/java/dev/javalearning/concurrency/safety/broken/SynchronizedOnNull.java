package dev.javalearning.concurrency.safety.broken;

/**
 * 这是<b>故意写错的</b>反面教材：把 {@code null} 当锁。
 *
 * <p>看起来像「没有锁」，但 {@code null} 连对象都不是，编译器直接拦下来。
 * 就算能过编译，运行期也会抛 {@code NullPointerException}——
 * 拿不到对象就无从「持有」它。
 *
 * <p>放 {@code broken/} 子包是因为它<b>编译不过</b>。
 */
public class SynchronizedOnNull {

    public void doSomething() {
        synchronized (null) {   // ← 编译错误：required: reference, found: <null>
            System.out.println("永远走不到这里");
        }
    }
}
