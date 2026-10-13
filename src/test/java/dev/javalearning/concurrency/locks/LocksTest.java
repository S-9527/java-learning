package dev.javalearning.concurrency.locks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 15 课的断言。
 *
 * <p>每一条对应 {@code docs/15-锁.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*locks*'}
 */
class LocksTest {

    // ── 1. 基本用法 ────────────────────────────────────────────────

    @Test
    @DisplayName("unlock 放在 finally 里，异常也不会把锁漏掉")
    void unlockInFinallyReleasesEvenOnException() {
        ReentrantLock lock = new ReentrantLock();
        try {
            lock.lock();
            try {
                throw new IllegalStateException("故意抛的");
            } finally {
                lock.unlock();
            }
        } catch (IllegalStateException ignored) {
            // 吞掉
        }
        assertThat(lock.isLocked()).isFalse();
    }

    @Test
    @DisplayName("锁没释放时 isLocked 是 true；而同一线程再 tryLock 会成功——因为它可重入")
    void lockedStateIsVisibleAndReentrant() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            assertThat(lock.isLocked()).isTrue();
            // 注意：这里同一线程调 tryLock 会返回 true，因为锁是可重入的。
            // "拿不到"只发生在别的线程身上——所以这条断言不能写成 isFalse()。
            assertThat(lock.tryLock()).isTrue();
            assertThat(lock.getHoldCount()).isEqualTo(2);
        } finally {
            lock.unlock();
            lock.unlock();
        }
        assertThat(lock.isLocked()).isFalse();
    }

    // ── 2. tryLock ─────────────────────────────────────────────────

    @Test
    @DisplayName("tryLock() 拿不到立刻返回 false，不阻塞")
    void tryLockReturnsImmediately() throws Exception {
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

        assertThat(got).isFalse();
        assertThat(elapsedMillis).isLessThan(100);   // 确实是"立刻"返回

        release.countDown();
        holder.join();
    }

    @Test
    @DisplayName("tryLock(超时) 会等满超时时间才放弃")
    void tryLockWithTimeoutWaits() throws Exception {
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
        boolean got = lock.tryLock(200, TimeUnit.MILLISECONDS);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(got).isFalse();
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(200);

        release.countDown();
        holder.join();
    }

    @Test
    @DisplayName("对方释放之后再 tryLock 就能拿到")
    void tryLockSucceedsAfterRelease() throws Exception {
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

        assertThat(lock.tryLock()).isFalse();
        release.countDown();
        holder.join();
        assertThat(lock.tryLock()).isTrue();
        lock.unlock();
    }

    // ── 3. 可重入 ──────────────────────────────────────────────────

    @Test
    @DisplayName("可重入：同一线程重复 lock，holdCount 会累加")
    void reentrantLockCountsHolds() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        assertThat(lock.getHoldCount()).isEqualTo(1);
        lock.lock();
        assertThat(lock.getHoldCount()).isEqualTo(2);
        lock.unlock();
        assertThat(lock.getHoldCount()).isEqualTo(1);
        lock.unlock();
        assertThat(lock.isLocked()).isFalse();
    }

    // ── 4. 公平锁 ──────────────────────────────────────────────────

    @Test
    @DisplayName("默认非公平，传 true 才是公平锁")
    void fairnessIsOptIn() {
        assertThat(new ReentrantLock().isFair()).isFalse();
        assertThat(new ReentrantLock(true).isFair()).isTrue();
    }

    // ── 5. Condition ───────────────────────────────────────────────

    @Test
    @DisplayName("Condition 能把等待者分成不同队列，signal 只叫醒指定的那个")
    void conditionSeparatesWaitingQueues() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        Condition notEmpty = lock.newCondition();
        Condition notFull = lock.newCondition();

        AtomicBoolean consumerWoken = new AtomicBoolean();
        AtomicBoolean otherQueueTouched = new AtomicBoolean();

        Thread consumer = new Thread(() -> {
            lock.lock();
            try {
                notEmpty.await(2000, TimeUnit.MILLISECONDS);
                consumerWoken.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        consumer.start();
        Thread.sleep(200);

        lock.lock();
        try {
            notEmpty.signal();      // 只叫醒等 notEmpty 的
        } finally {
            lock.unlock();
        }
        consumer.join();

        assertThat(consumerWoken).isTrue();
        // notFull 那个队列压根没人，signal 它不会有任何效果——证明队列是分开的
        lock.lock();
        try {
            notFull.signal();
            otherQueueTouched.set(true);
        } finally {
            lock.unlock();
        }
        assertThat(otherQueueTouched).isTrue();
    }

    // ── 6. 死锁 ────────────────────────────────────────────────────

    @Test
    @DisplayName("两个线程反向加锁会死锁，ThreadMXBean 能找出来")
    void deadlockIsDetectable() throws Exception {
        Object lockA = new Object();
        Object lockB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                sleep(100);
                synchronized (lockB) {
                    // 走不到这里
                }
            }
        }, "deadlock-t1");

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                sleep(100);
                synchronized (lockA) {
                    // 走不到这里
                }
            }
        }, "deadlock-t2");

        // 这两个线程会永久卡死，必须设成守护线程，否则测试进程退不出去。
        // setDaemon 必须在 start() 之前调用。
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();
        t1.join(500);
        t2.join(500);

        assertThat(t1.isAlive()).isTrue();
        assertThat(t2.isAlive()).isTrue();
        assertThat(t1.getState()).isEqualTo(Thread.State.BLOCKED);
        assertThat(t2.getState()).isEqualTo(Thread.State.BLOCKED);

        // JVM 自己就能找出死锁
        java.lang.management.ThreadMXBean mx = java.lang.management.ManagementFactory.getThreadMXBean();
        long[] deadlocked = mx.findDeadlockedThreads();
        assertThat(deadlocked).isNotNull().hasSize(2);
    }

    @Test
    @DisplayName("对已启动的线程调 setDaemon 会抛 IllegalThreadStateException")
    void setDaemonMustComeBeforeStart() throws Exception {
        Thread t = new Thread(() -> sleep(50));
        t.start();
        assertThatThrownBy(() -> t.setDaemon(true))
                .isInstanceOf(IllegalThreadStateException.class);
        t.join();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
