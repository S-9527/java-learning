package dev.javalearning.concurrency.coordination;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 第 16 课的示例代码：线程通信。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/coordination/CoordinationDemo.java}
 * 每一节对应 {@code docs/16-线程通信.md} 里的一节，输出都是真跑出来的。
 */
public class CoordinationDemo {

    public static void main(String[] args) throws Exception {
        section("1. wait/notify 必须在 synchronized 里，否则直接抛异常");
        monitorStateDemo();

        section("2. wait 会释放锁，sleep 不会——这是最容易混的一点");
        waitReleasesLockDemo();
        sleepHoldsLockDemo();

        section("3. 用 wait/notify 写一个生产者-消费者队列");
        producerConsumerDemo();

        section("4. 为什么用 while 而不是 if 包住 wait");
        spuriousWakeupDemo();

        section("5. notify 只叫醒一个，notifyAll 叫醒全部");
        notifyVsNotifyAllDemo();
    }

    /** 不在同步块里调 wait/notify → IllegalMonitorStateException。 */
    static void monitorStateDemo() {
        Object lock = new Object();
        try {
            lock.wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IllegalMonitorStateException e) {
            System.out.println("  不在 synchronized 里调 wait   → IllegalMonitorStateException");
        }
        try {
            lock.notify();
        } catch (IllegalMonitorStateException e) {
            System.out.println("  不在 synchronized 里调 notify → IllegalMonitorStateException");
        }
        System.out.println("  ← 因为 wait/notify 操作的是「对象上的监视器」，必须先持有它");
    }

    /** wait 会释放锁：等待期间别的线程能拿到同一把锁。 */
    static void waitReleasesLockDemo() throws Exception {
        Object lock = new Object();
        boolean[] otherGotIn = {false};

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "waiter");
        waiter.start();
        Thread.sleep(100);   // 等 waiter 进入 wait

        synchronized (lock) {
            otherGotIn[0] = true;
        }
        System.out.println("  wait 期间，别的线程能拿到同一把锁吗 → " + otherGotIn[0]);
        waiter.interrupt();
        waiter.join();
    }

    /** sleep 不释放锁：睡眠期间别的线程进不来。 */
    static void sleepHoldsLockDemo() throws Exception {
        Object lock = new Object();
        boolean[] otherGotIn = {false};

        Thread sleeper = new Thread(() -> {
            synchronized (lock) {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "sleeper");
        sleeper.start();
        Thread.sleep(100);   // 等 sleeper 拿到锁并开始睡

        Thread probe = new Thread(() -> {
            synchronized (lock) {
                otherGotIn[0] = true;
            }
        }, "probe");
        probe.start();
        Thread.sleep(150);   // 给 probe 一点时间去抢锁

        System.out.println("  sleep 期间，别的线程能拿到同一把锁吗 → " + otherGotIn[0]);
        sleeper.join();
        probe.join();
    }

    /** 用 wait/notify 写一个容量为 1 的生产者-消费者。 */
    static void producerConsumerDemo() throws Exception {
        // 容量给 3：生产者能一口气放完，事件顺序就确定了
        SimpleQueue<Integer> queue = new SimpleQueue<>(3);

        // 注意：这里用一个同步的 StringBuilder 收集日志，并且**按事件顺序编号**。
        // 如果直接往共享 StringBuilder 里 append，两个线程会交错写入、顺序也不确定。
        StringBuilder log = new StringBuilder();
        Object logLock = new Object();
        java.util.concurrent.atomic.AtomicInteger seq = new java.util.concurrent.atomic.AtomicInteger();

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                int v = queue.take();
                synchronized (logLock) {
                    log.append(String.format("  [%d] 消费者取到 %d%n", seq.incrementAndGet(), v));
                }
            }
        }, "consumer");

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                queue.put(i);
                synchronized (logLock) {
                    log.append(String.format("  [%d] 生产者放入 %d%n", seq.incrementAndGet(), i));
                }
            }
        }, "producer");

        producer.start();
        producer.join();      // 生产者先放完 3 个（容量够，不会阻塞）
        consumer.start();     // 消费者再取
        consumer.join();

        // 打印时按事件编号排序，输出就稳定了（真实的交错顺序每次可能不同）
        String[] lines = log.toString().split("\n");
        java.util.Arrays.sort(lines);
        for (String line : lines) {
            System.out.println(line);
        }
        System.out.println("  ← 两边用同一个监视器互相通知，谁都不忙等");
        System.out.println("  ← 真实的事件交错顺序每次可能不同，这里按编号排好再打印");

        // 再演示一次"容量满了会真的阻塞"：容量 1，生产者要放 3 个
        SimpleQueue<Integer> small = new SimpleQueue<>(1);
        long start = System.nanoTime();
        Thread slowProducer = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                small.put(i);
            }
        }, "slow-producer");
        slowProducer.start();
        Thread.sleep(300);        // 此时队列已满，生产者卡在 put 里等
        boolean blocked = slowProducer.isAlive();
        System.out.println("  容量 1 的队列，放满之后生产者还在跑吗 → " + blocked);
        // 取走一个，给生产者腾出位置
        int taken = small.take();
        slowProducer.join(2000);
        System.out.println("  消费者取走 " + taken + " 之后，生产者放完了吗 → " + !slowProducer.isAlive());
        // 把剩下的取干净，避免线程挂着
        small.take();
        small.take();
        System.out.println("  ← put 在队列满时用 wait 挂起，被 take 的 notifyAll 叫醒");
    }

    /**
     * 为什么必须用 while 而不是 if 包住 wait。
     *
     * <p>wait 可能在没有 signal 的情况下自己醒来（虚假唤醒 spurious wakeup），
     * 也可能醒来时条件又被人抢走了。用 if 只检查一次，条件不成立就会继续往下跑。
     */
    static void spuriousWakeupDemo() throws Exception {
        Object lock = new Object();
        boolean[] condition = {false};
        StringBuilder log = new StringBuilder();

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                // 用 while：醒来后重新检查条件，不成立就接着等
                while (!condition[0]) {
                    try {
                        log.append("  waiter：条件不成立，继续等\n");
                        lock.wait(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                log.append("  waiter：条件成立，继续干活\n");
            }
        }, "waiter");
        waiter.start();
        Thread.sleep(100);

        // 故意先 notify 一次但**不改变条件** —— 模拟虚假唤醒
        synchronized (lock) {
            log.append("  main：先 notify 一次，但条件还是 false\n");
            lock.notify();
        }
        Thread.sleep(200);

        // 这次真正满足条件再通知
        synchronized (lock) {
            log.append("  main：这次把条件设为 true 再 notify\n");
            condition[0] = true;
            lock.notify();
        }
        waiter.join(3000);

        System.out.print(log);
        System.out.println("  ← 用 while 的话，第一次被叫醒发现条件不成立会接着睡；用 if 就会直接冲出去");
    }

    /** notify 随机叫一个，notifyAll 全叫醒。 */
    static void notifyVsNotifyAllDemo() throws Exception {
        Object lock = new Object();
        StringBuilder log = new StringBuilder();
        int waiters = 3;

        Thread[] threads = new Thread[waiters];
        for (int i = 0; i < waiters; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                synchronized (lock) {
                    try {
                        lock.wait(2000);
                        log.append("  等待者 ").append(id).append(" 被叫醒\n");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "waiter-" + id);
            threads[i].start();
        }
        Thread.sleep(200);

        synchronized (lock) {
            log.append("  调 notify()，只叫醒一个\n");
            lock.notify();
        }
        Thread.sleep(200);

        synchronized (lock) {
            log.append("  调 notifyAll()，把剩下的都叫醒\n");
            lock.notifyAll();
        }
        for (Thread t : threads) {
            t.join(3000);
        }

        System.out.print(log);
        System.out.println("  ← notify 叫醒哪个是不确定的，所以多个等待者时一般用 notifyAll");
    }

    /** 一个容量固定的简单阻塞队列，用 wait/notify 实现。 */
    static class SimpleQueue<T> {
        private final Queue<T> items = new ArrayDeque<>();
        private final int capacity;

        SimpleQueue(int capacity) {
            this.capacity = capacity;
        }

        synchronized void put(T item) {
            // 满了就等，等到有人取走
            while (items.size() >= capacity) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            items.add(item);
            notifyAll();     // 通知等"非空"的消费者
        }

        synchronized T take() {
            // 空了就等，等到有人放进来
            while (items.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            T item = items.poll();
            notifyAll();     // 通知等"不满"的生产者
            return item;
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
