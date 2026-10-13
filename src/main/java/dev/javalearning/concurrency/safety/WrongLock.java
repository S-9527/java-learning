package dev.javalearning.concurrency.safety;

import java.util.concurrent.CountDownLatch;

/**
 * 第 13 课的示例代码：演示「锁错对象」的后果。
 *
 * <p>两个线程各自写 {@code synchronized (new Object())}。它们以为自己在互斥，
 * 其实每次 {@code new} 出来的都是<b>不同的对象</b>，也就是两把不同的锁——
 * 于是两个人可以同时站在临界区里。这就是那句话：
 * <b>锁住的是「对象」，不是「代码」</b>。
 *
 * <p>正确写法是把锁存成一个字段，让所有线程用同一个对象：
 *
 * <pre>
 *   private final Object lock = new Object();
 *   ...
 *   synchronized (lock) { ... }
 * </pre>
 *
 * <p>对应 {@code docs/13-线程安全与同步.md}。
 */
final class WrongLock {

    /** 两个线程有没有同时待在各自的临界区里。 */
    record Result(boolean bothInsideAtOnce) {
    }

    private WrongLock() {
    }

    /**
     * 线程 A 先进自己那把新锁并待着，线程 B 随后进它自己那把新锁。
     *
     * <p>如果锁真的起作用，B 会被挡在外面；实际结果 B 直接进去了。
     */
    static Result run() throws InterruptedException {
        CountDownLatch aInside = new CountDownLatch(1);
        CountDownLatch bInside = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        boolean[] overlap = {false};

        Thread a = new Thread(() -> {
            synchronized (new Object()) {       // A 的那把锁
                aInside.countDown();
                await(releaseA);
            }
        });

        Thread b = new Thread(() -> {
            await(aInside);
            synchronized (new Object()) {       // B 的另一把锁，和 A 的完全无关
                overlap[0] = true;              // 两把不同的锁，可以同时进
                bInside.countDown();
            }
        });

        a.start();
        b.start();
        bInside.await();                        // 等 B 确实进来了
        boolean bothInside = overlap[0];
        releaseA.countDown();
        a.join();
        b.join();

        return new Result(bothInside);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
