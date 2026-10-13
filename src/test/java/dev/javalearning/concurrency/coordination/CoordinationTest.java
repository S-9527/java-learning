package dev.javalearning.concurrency.coordination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 16 课的断言。
 *
 * <p>每一条对应 {@code docs/16-线程通信.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*coordination*'}
 */
class CoordinationTest {

    // ── 1. 必须持有监视器 ──────────────────────────────────────────

    @Test
    @DisplayName("不在 synchronized 里调 wait 会抛 IllegalMonitorStateException")
    void waitRequiresTheMonitor() {
        Object lock = new Object();
        assertThatThrownBy(lock::wait)
                .isInstanceOf(IllegalMonitorStateException.class);
    }

    @Test
    @DisplayName("不在 synchronized 里调 notify 也一样")
    void notifyRequiresTheMonitor() {
        Object lock = new Object();
        assertThatThrownBy(lock::notify)
                .isInstanceOf(IllegalMonitorStateException.class);
    }

    // ── 2. wait 释放锁，sleep 不释放 ───────────────────────────────

    @Test
    @DisplayName("wait 会释放锁：等待期间别的线程能进同一个同步块")
    void waitReleasesTheLock() throws Exception {
        Object lock = new Object();
        AtomicBoolean otherGotIn = new AtomicBoolean(false);
        AtomicBoolean waiting = new AtomicBoolean(false);

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                waiting.set(true);
                try {
                    lock.wait(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        waiter.start();
        while (!waiting.get()) {
            Thread.sleep(5);
        }
        Thread.sleep(50);

        // 如果 wait 没释放锁，这个同步块会一直等下去
        synchronized (lock) {
            otherGotIn.set(true);
        }
        assertThat(otherGotIn).isTrue();

        waiter.interrupt();
        waiter.join();
    }

    @Test
    @DisplayName("sleep 不释放锁：睡眠期间别的线程进不来")
    void sleepDoesNotReleaseTheLock() throws Exception {
        Object lock = new Object();
        AtomicBoolean otherGotIn = new AtomicBoolean(false);
        AtomicBoolean sleeping = new AtomicBoolean(false);

        Thread sleeper = new Thread(() -> {
            synchronized (lock) {
                sleeping.set(true);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        sleeper.start();
        while (!sleeping.get()) {
            Thread.sleep(5);
        }

        Thread probe = new Thread(() -> {
            synchronized (lock) {
                otherGotIn.set(true);
            }
        });
        probe.start();
        Thread.sleep(100);   // 此时 sleeper 还在睡

        assertThat(otherGotIn).isFalse();   // 锁还被握着
        sleeper.join();
        probe.join();
        assertThat(otherGotIn).isTrue();    // 睡完释放后才进去
    }

    // ── 3. 生产者-消费者 ───────────────────────────────────────────

    @Test
    @DisplayName("生产者-消费者：用 wait/notify 实现，容量满了会真的阻塞")
    void producerConsumerBlocksWhenFull() throws Exception {
        CoordinationDemo.SimpleQueue<Integer> queue = new CoordinationDemo.SimpleQueue<>(1);
        AtomicBoolean producerFinished = new AtomicBoolean(false);

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                queue.put(i);
            }
            producerFinished.set(true);
        });
        producer.start();
        Thread.sleep(200);

        // 队列容量 1，生产者放了第一个就卡住了
        assertThat(producerFinished).isFalse();

        // 每取走一个，生产者才能再放一个。容量 1 意味着要取三次它才跑得完。
        assertThat(queue.take()).isEqualTo(1);   // 腾出位置，生产者放入 2
        assertThat(queue.take()).isEqualTo(2);   // 再腾出位置，生产者放入 3
        assertThat(queue.take()).isEqualTo(3);   // 最后一次
        producer.join(3000);
        assertThat(producerFinished).isTrue();
    }

    @Test
    @DisplayName("队列空了，take 会等在那里，直到有人放进来")
    void takeWaitsWhenEmpty() throws Exception {
        CoordinationDemo.SimpleQueue<Integer> queue = new CoordinationDemo.SimpleQueue<>(2);
        AtomicReference<Integer> taken = new AtomicReference<>();

        Thread consumer = new Thread(() -> taken.set(queue.take()));
        consumer.start();
        Thread.sleep(150);

        assertThat(taken.get()).isNull();     // 还在等

        queue.put(42);
        consumer.join(2000);
        assertThat(taken.get()).isEqualTo(42);
    }

    @Test
    @DisplayName("顺序进出：放进去什么，取出来还是什么")
    void queueKeepsFifoOrder() throws Exception {
        CoordinationDemo.SimpleQueue<String> queue = new CoordinationDemo.SimpleQueue<>(10);
        queue.put("a");
        queue.put("b");
        queue.put("c");
        assertThat(queue.take()).isEqualTo("a");
        assertThat(queue.take()).isEqualTo("b");
        assertThat(queue.take()).isEqualTo("c");
    }

    // ── 4. while 而不是 if ─────────────────────────────────────────

    @Test
    @DisplayName("用 while 包住 wait：条件没满足时会继续等，不会被空通知骗走")
    void waitMustBeWrappedInWhile() throws Exception {
        Object lock = new Object();
        boolean[] condition = {false};
        AtomicInteger wakeUps = new AtomicInteger();
        AtomicBoolean finished = new AtomicBoolean(false);

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                while (!condition[0]) {
                    try {
                        lock.wait(2000);
                        wakeUps.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                finished.set(true);
            }
        });
        waiter.start();
        Thread.sleep(100);

        // 第一次通知，但条件仍然是 false —— 用 while 的话它会接着睡
        synchronized (lock) {
            lock.notify();
        }
        Thread.sleep(150);
        assertThat(finished).isFalse();
        assertThat(wakeUps.get()).isEqualTo(1);   // 醒过一次，但条件不满足

        // 这次真正满足条件
        synchronized (lock) {
            condition[0] = true;
            lock.notify();
        }
        waiter.join(3000);
        assertThat(finished).isTrue();
    }

    // ── 5. notify vs notifyAll ─────────────────────────────────────

    @Test
    @DisplayName("notify 只叫醒一个，notifyAll 把剩下的都叫醒")
    void notifyWakesOneNotifyAllWakesTheRest() throws Exception {
        Object lock = new Object();
        int waiters = 3;
        AtomicInteger woken = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < waiters; i++) {
            Thread t = new Thread(() -> {
                synchronized (lock) {
                    try {
                        lock.wait(3000);
                        woken.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            t.start();
            threads.add(t);
        }
        Thread.sleep(200);

        synchronized (lock) {
            lock.notify();          // 只叫一个
        }
        Thread.sleep(200);
        assertThat(woken.get()).isEqualTo(1);

        synchronized (lock) {
            lock.notifyAll();       // 把剩下的叫醒
        }
        for (Thread t : threads) {
            t.join(3000);
        }
        assertThat(woken.get()).isEqualTo(3);
    }
}
