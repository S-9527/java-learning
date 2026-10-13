package dev.javalearning.concurrency.safety;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 第 13 课的示例代码：线程安全与同步。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/safety/SafetyDemo.java}
 * 每一节对应 {@code docs/13-线程安全与同步.md} 里的一节。
 *
 * <p><b>这一课演示的就是「并发导致结果不确定」，所以输出必须小心处理。</b>
 * 凡是竞态相关的地方，都不打印具体数字，只打印「有没有出问题」这种稳定结论；
 * 需要展示具体数字的地方（比如加锁后的 100000），那本身就是确定的值。
 */
public class SafetyDemo {

    /** 不安全的计数器：count++ 不是原子操作。 */
    static int unsafeCount = 0;

    /** 安全的计数器：方法加了 synchronized。 */
    static int safeCount = 0;

    /** 用一把共享的锁保护的计数器。 */
    static int lockedCount = 0;

    /** 用「每次 new 一把新锁」保护的计数器——等于没锁。 */
    static int badLockCount = 0;

    static final Object SHARED_LOCK = new Object();

    public static void main(String[] args) throws Exception {
        section("1. 先让代码疼一次：不安全的计数器");
        unsafeCounterDemo();

        section("2. synchronized 方法：不管跑多少遍都对");
        synchronizedMethodDemo();

        section("3. 同步代码块：锁对象要自己指定");
        synchronizedBlockDemo();

        section("4. 坑一：锁 new Object() 等于没锁");
        badLockDemo();

        section("5. 坑二：synchronized 是可重入的");
        reentrancyDemo();

        section("6. 锁到底锁的是谁");
        whatIsLocked();

        section("7. Lock：更灵活的锁，但 unlock 必须放 finally");
        lockDemo();

        section("8. 线程安全的三个特性");
        threeProperties();

        section("9. 实战：100 张票 4 个窗口卖");
        ticketDemo();

        section("10. 不推荐的用法：锁字符串常量");
        stringLockDemo();
    }

    /**
     * 10 个线程各加 1 万次，期望 10 万。
     *
     * <p>关键设计：不去打印「这次少了多少」（每次运行都不同），
     * 而是「循环到观测到丢失为止」——这必然会发生，所以结论稳定。
     */
    static void unsafeCounterDemo() throws Exception {
        int threads = 10;
        int perThread = 10_000;
        int expected = threads * perThread;

        int rounds = 0;
        boolean observedLoss = false;
        while (rounds < 20 && !observedLoss) {
            rounds++;
            if (countWithThreads(threads, perThread, false, false) < expected) {
                observedLoss = true;
            }
        }
        System.out.println("  " + threads + " 个线程各加 " + perThread + " 次，期望 " + expected);
        System.out.println("  观测到「加少了」吗 → " + observedLoss);
        System.out.println("  ← count++ 不是一步：读 → 改 → 写，中间可能被别人插进来");
        System.out.println("  ← 这叫丢失更新 lost update。少多少、哪一轮少，每次运行都不同");
    }

    /** 加 synchronized 之后，跑多少遍都是精确的 100000。 */
    static void synchronizedMethodDemo() throws Exception {
        int threads = 10;
        int perThread = 10_000;
        int expected = threads * perThread;

        // 这里可以放心打印具体数字——加锁之后它就是确定的值
        int result = countWithThreads(threads, perThread, true, false);
        System.out.println("  同样的测试，给方法加上 synchronized");
        System.out.println("  实际结果 → " + result + "（期望 " + expected + "）");
        System.out.println("  精确相等吗 → " + (result == expected));
        System.out.println("  ← 任意时刻只允许一个线程进入这个方法，别人排队等");
        System.out.println("  ← 「读-改-写」三步完整做完，中间没人能插队");
    }

    /** 同步代码块，锁对象自己指定。 */
    static void synchronizedBlockDemo() throws Exception {
        int threads = 10;
        int perThread = 10_000;
        int expected = threads * perThread;

        lockedCount = 0;
        Thread[] ts = new Thread[threads];
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                await(start);
                for (int j = 0; j < perThread; j++) {
                    synchronized (SHARED_LOCK) {
                        lockedCount++;
                    }
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        start.countDown();
        for (Thread t : ts) {
            t.join();
        }

        System.out.println("  用同步代码块 + 一把共享的锁");
        System.out.println("  实际结果 → " + lockedCount + "（期望 " + expected + "）");
        System.out.println("  精确相等吗 → " + (lockedCount == expected));
        System.out.println("  ← 关键是「同一把锁」：SHARED_LOCK 是所有线程共享的那个对象");
    }

    /** 锁 new Object()：每次都是新锁，等于完全没锁。 */
    static void badLockDemo() throws Exception {
        int threads = 10;
        int perThread = 10_000;
        int expected = threads * perThread;

        int rounds = 0;
        boolean observedLoss = false;
        while (rounds < 20 && !observedLoss) {
            rounds++;
            badLockCount = 0;
            Thread[] ts = new Thread[threads];
            CountDownLatch start = new CountDownLatch(1);
            for (int i = 0; i < threads; i++) {
                ts[i] = new Thread(() -> {
                    await(start);
                    for (int j = 0; j < perThread; j++) {
                        // 每次进来都 new 一把新锁——别的线程拿的是另一把，互不排斥
                        synchronized (new Object()) {
                            badLockCount++;
                        }
                    }
                });
            }
            for (Thread t : ts) {
                t.start();
            }
            start.countDown();
            for (Thread t : ts) {
                t.join();
            }
            if (badLockCount < expected) {
                observedLoss = true;
            }
        }

        System.out.println("  把锁换成 synchronized (new Object())");
        System.out.println("  观测到「加少了」吗 → " + observedLoss);
        System.out.println("  ← 每次进来都是一把新锁，worker-0 拿锁 A、worker-1 拿锁 B，谁都不挡谁");
        System.out.println("  ← 这是最典型的「看起来加了锁其实没加」");
    }

    /** 可重入：同一线程能重复拿到自己已持有的锁。 */
    static void reentrancyDemo() {
        System.out.println("  a() 和 b() 都是 synchronized，a() 里面调 b()");
        String result = outerMethod();
        System.out.println("  结果 → " + result);
        System.out.println("  ← 没有死锁。同一线程再次进入这把锁保护的代码可以直接进");
        System.out.println("  ← 否则 a() 拿到锁再调 b() 就会自己把自己锁死");
    }

    static synchronized String outerMethod() {
        return "a() 进去了 → " + innerMethod();
    }

    static synchronized String innerMethod() {
        return "b() 也进去了";
    }

    /** 锁对象是谁：实例方法锁 this，静态方法锁 Class。 */
    static void whatIsLocked() {
        System.out.println("  public synchronized void m()          → 锁 this（当前对象）");
        System.out.println("  public static synchronized void m()   → 锁该类的 Class 对象（全局唯一）");
        System.out.println("  synchronized (this) { }               → 同实例方法");
        System.out.println("  synchronized (Xxx.class) { }          → 同类静态方法");
        System.out.println("  synchronized (lock) { }               → lock 指向的那个对象");
        System.out.println("  ← 静态方法锁的范围大得多，会影响这个类的所有实例");
        System.out.println("  ← 加锁范围越大，并发能力越差，所以要想清楚在保护什么");
    }

    /** Lock：更灵活，但 unlock 必须放 finally。 */
    static void lockDemo() throws Exception {
        int threads = 10;
        int perThread = 10_000;
        int expected = threads * perThread;

        Lock lock = new ReentrantLock();
        int[] count = {0};
        Thread[] ts = new Thread[threads];
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                await(start);
                for (int j = 0; j < perThread; j++) {
                    lock.lock();
                    try {
                        count[0]++;
                    } finally {
                        lock.unlock();   // ← 必须在 finally
                    }
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        start.countDown();
        for (Thread t : ts) {
            t.join();
        }

        System.out.println("  用 ReentrantLock 保护，实际结果 → " + count[0] + "（期望 " + expected + "）");
        System.out.println("  精确相等吗 → " + (count[0] == expected));
        System.out.println("  ← unlock 必须放 finally：中间抛异常又没释放，锁就永远不还了");
        System.out.println("  ← 那是死锁，其他线程全部卡死");
        System.out.println("  ← synchronized 出异常 JVM 会自动释放，Lock 不会");
    }

    /** 三个特性：原子性、可见性、有序性。 */
    static void threeProperties() {
        System.out.println("  原子性 Atomicity：操作不可分割。count++ 破坏了它 → synchronized / Lock / 原子类");
        System.out.println("  可见性 Visibility：一个线程改了值，别的线程可能看不见 → volatile / synchronized");
        System.out.println("  有序性 Ordering：编译器和 CPU 可能重排指令 → volatile / synchronized");
        System.out.println("  ← synchronized 三个都能解决，是「一刀切」的武器，代价是性能");
        System.out.println("  ← 可见性和有序性的细节在 [JMM 与 volatile] 那一课（见文档链接）");
    }

    /** 100 张票、4 个窗口。 */
    static void ticketDemo() throws Exception {
        // 先看不加锁：会超卖
        // 超卖是概率性的（4 个窗口卖 100 张，撞上要运气），所以循环到观测到为止
        boolean oversold = false;
        int rounds = 0;
        while (rounds < 200 && !oversold) {
            rounds++;
            if (sellTickets(false) < 0) {      // 剩余票数为负 = 超卖
                oversold = true;
            }
        }
        System.out.println("  不加锁：观测到「卖成负数」吗 → " + oversold);
        System.out.println("  （超卖是概率性的：要靠多个窗口恰好都读到「还剩 1 张」）");

        // 再加锁：一定是 0
        int remaining = sellTickets(true);
        System.out.println("  加锁后：剩余票数 → " + remaining + "（期望 0）");
        System.out.println("  正好卖完 100 张吗 → " + (remaining == 0));
        System.out.println("  ← 不加锁时两个窗口可能同时看到「还剩 1 张」，于是都卖了");
    }

    /** 返回剩余票数。加锁版本一定返回 0；不加锁版本可能返回负数。 */
    static int sellTickets(boolean locked) throws Exception {
        TicketOffice office = new TicketOffice(locked);
        Thread[] windows = new Thread[4];
        for (int i = 0; i < 4; i++) {
            windows[i] = new Thread(office::sell);
        }
        for (Thread w : windows) {
            w.start();
        }
        for (Thread w : windows) {
            w.join();
        }
        return office.remaining();
    }

    /** 不推荐：锁字符串常量。 */
    static void stringLockDemo() {
        System.out.println("  synchronized (\"abc\") { }  ← 不推荐");
        System.out.println("  ← 字符串常量在常量池里是共享的，别的代码可能也锁同一个 \"abc\"");
        System.out.println("  ← 于是两段毫不相干的代码莫名其妙互相阻塞");
        System.out.println("  ← 而且你根本不知道有没有别人在锁它");
    }

    // ── 工具方法 ────────────────────────────────────────────────────

    /** 跑一轮计数测试。synchronizedMethod=true 时用同步方法，否则裸加。 */
    static int countWithThreads(int threads, int perThread, boolean synchronizedMethod, boolean unused) throws Exception {
        if (synchronizedMethod) {
            safeCount = 0;
        } else {
            unsafeCount = 0;
        }
        Thread[] ts = new Thread[threads];
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                await(start);
                for (int j = 0; j < perThread; j++) {
                    if (synchronizedMethod) {
                        safeIncrement();
                    } else {
                        unsafeCount++;
                    }
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        start.countDown();
        for (Thread t : ts) {
            t.join();
        }
        return synchronizedMethod ? safeCount : unsafeCount;
    }

    static synchronized void safeIncrement() {
        safeCount++;
    }

    static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
