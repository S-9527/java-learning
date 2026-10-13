package dev.javalearning.concurrency.locks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 第 15 课的示例代码：锁。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/locks/LocksDemo.java}
 * 每一节对应 {@code docs/15-锁.md} 里的一节，输出都是真跑出来的。
 */
public class LocksDemo {

    public static void main(String[] args) throws Exception {
        section("1. Lock 的基本用法：unlock 必须放 finally");
        basicLock();

        section("2. tryLock：拿不到就走，不干等");
        tryLockDemo();

        section("3. tryLock 超时：等一会儿就走");
        tryLockTimeoutDemo();

        section("4. 可重入：同一线程能重复拿同一把锁");
        reentrancyDemo();

        section("5. 公平锁 vs 非公平锁");
        fairnessDemo();

        section("6. Condition：一个锁配多个等待队列");
        conditionDemo();

        section("7. 死锁：两个线程互相等对方的锁");
        deadlockDemo();
    }

    /** 最简单的用法，重点是 finally。 */
    static void basicLock() {
        ReentrantLock lock = new ReentrantLock();
        int[] count = {0};

        lock.lock();
        try {
            count[0]++;
            System.out.println("  拿到锁，count = " + count[0]);
        } finally {
            lock.unlock();   // ← 不管上面怎么退出，锁一定还回去
        }
        System.out.println("  释放后 isLocked = " + lock.isLocked());
    }

    /** tryLock() 无参：拿到返回 true，拿不到立刻返回 false，不阻塞。 */
    static void tryLockDemo() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                held.countDown();
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        holder.start();
        held.await();

        long start = System.nanoTime();
        boolean got = lock.tryLock();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        System.out.println("  另一个线程持有锁时，tryLock() → " + got);
        System.out.println("  耗时 " + elapsedMillis + " 毫秒 ← 立刻返回，没干等（不到 1 毫秒）");

        release.countDown();
        holder.join();
        System.out.println("  对方释放后再 tryLock() → " + lock.tryLock());
        lock.unlock();
    }

    /** tryLock(超时)：等一段时间，还是拿不到就放弃。 */
    static void tryLockTimeoutDemo() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                held.countDown();
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        holder.start();
        held.await();

        long start = System.nanoTime();
        boolean got = lock.tryLock(300, TimeUnit.MILLISECONDS);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        System.out.println("  tryLock(300ms) → " + got);
        // 精确耗时会在 300 毫秒上下小幅抖动，所以只判断量级
        System.out.println("  等了 " + (elapsedMillis >= 300 ? "至少 300" : elapsedMillis) + " 毫秒才放弃 ← 它真的等满了超时时间");

        release.countDown();
        holder.join();
    }

    /** 可重入：同一线程再次拿同一把锁不会死锁，但计数会涨。 */
    static void reentrancyDemo() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        System.out.println("  第一次 lock 之后 holdCount = " + lock.getHoldCount());
        lock.lock();
        System.out.println("  第二次 lock 之后 holdCount = " + lock.getHoldCount());
        lock.unlock();
        System.out.println("  释放一次之后 holdCount = " + lock.getHoldCount());
        lock.unlock();
        System.out.println("  全部释放之后 isLocked = " + lock.isLocked());
    }

    /** 公平锁：按排队顺序拿；非公平锁：允许插队（默认，吞吐更高）。 */
    static void fairnessDemo() {
        System.out.println("  new ReentrantLock()      isFair = " + new ReentrantLock().isFair());
        System.out.println("  new ReentrantLock(true)  isFair = " + new ReentrantLock(true).isFair());
        System.out.println("  ← 默认是非公平的，吞吐更高但可能饿死某个线程");
    }

    /**
     * Condition：一个锁可以配多个等待队列。
     *
     * <p>这是 {@code synchronized + wait/notify} 做不到的——
     * 那个只有一个等待队列，notifyAll 会把所有等待者都叫醒。
     */
    static void conditionDemo() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        Condition notFull = lock.newCondition();
        Condition notEmpty = lock.newCondition();

        StringBuilder log = new StringBuilder();
        Thread consumer = new Thread(() -> {
            lock.lock();
            try {
                log.append("消费者：仓库空了，去 notEmpty 队列上等\n");
                notEmpty.await(2000, TimeUnit.MILLISECONDS);
                log.append("消费者：被叫醒了\n");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }, "consumer");
        consumer.start();

        Thread.sleep(200);   // 让消费者先进入等待

        lock.lock();
        try {
            log.append("生产者：放了一个元素，只唤醒 notEmpty 队列\n");
            notEmpty.signal();          // 只叫醒等"非空"的那些，不动 notFull
        } finally {
            lock.unlock();
        }
        consumer.join();

        System.out.print(log.toString().indent(2));
        System.out.println("  ← 两个队列互不干扰，这就是 Condition 比 wait/notify 强的地方");
    }

    /** 死锁：两个线程各持一把锁，互相等对方那把。 */
    static void deadlockDemo() throws Exception {
        Object lockA = new Object();
        Object lockB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                sleep(100);
                synchronized (lockB) {
                    System.out.println("  t1 同时拿到 A 和 B");
                }
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                sleep(100);
                synchronized (lockA) {
                    System.out.println("  t2 同时拿到 A 和 B");
                }
            }
        }, "t2");

        // 这两个线程会永久卡死。设成守护线程，这样主线程结束后 JVM 能正常退出。
        // 注意：setDaemon 必须在 start() 之前调用，启动后再设会抛
        // IllegalThreadStateException。
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();
        t1.join(1000);
        t2.join(1000);

        System.out.println("  1 秒后 t1 还活着吗 → " + t1.isAlive());
        System.out.println("  1 秒后 t2 还活着吗 → " + t2.isAlive());
        System.out.println("  两个线程的状态：t1=" + t1.getState() + "，t2=" + t2.getState());
        System.out.println("  ← 两个都卡住了：t1 拿着 A 等 B，t2 拿着 B 等 A");
        System.out.println("  ← 死锁四条件：互斥、持有并等待、不可剥夺、循环等待");

        // 用 ThreadMXBean 让 JVM 自己找出死锁——这是排查死锁的正规手段
        java.lang.management.ThreadMXBean mx = java.lang.management.ManagementFactory.getThreadMXBean();
        long[] deadlocked = mx.findDeadlockedThreads();
        System.out.println("  ThreadMXBean 找到的死锁线程数 → "
                + (deadlocked == null ? 0 : deadlocked.length));
        System.out.println("  （线上排查死锁用 jstack，它内部就是查这个）");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
