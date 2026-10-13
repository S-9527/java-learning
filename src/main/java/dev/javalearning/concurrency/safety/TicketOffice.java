package dev.javalearning.concurrency.safety;

import java.util.concurrent.CountDownLatch;

/**
 * 第 13 课的示例代码：100 张票、4 个窗口卖——超卖是怎么发生的。
 *
 * <p>卖一张票是「看一眼还有没有，然后拿走一张」两个动作。这两个动作之间如果
 * 别人插进来，就会两个窗口都看到「还剩 1 张」，然后都拿走——卖出 101 张，
 * 余票变成 -1。这就是<b>检查后行动</b>（check-then-act）竞态。
 *
 * <p>{@link #widen} 参数会在「看」和「拿」之间插一次 {@link Thread#yield()}，
 * 把这个窗口撑开，好让超卖几乎每次都出现。不加的话窗口只有几个指令宽，
 * 要跑很多次才碰上一次——这正是这类 bug 在生产环境「偶发」的原因。
 *
 * <p>对应 {@code docs/13-线程安全与同步.md}。
 */
final class TicketOffice {

    /** 一轮卖票的结果。{@code remaining} 为负就是超卖。 */
    record Result(int sold, int remaining) {

        boolean oversold() {
            return remaining < 0;
        }
    }

    private final int total;
    private final int windows;
    private final boolean widen;
    private final Object lock = new Object();

    private int remaining;
    private int sold;

    TicketOffice(int total, int windows, boolean widen) {
        this.total = total;
        this.windows = windows;
        this.widen = widen;
    }

    /**
     * 不加锁地卖：检查（{@code remaining > 0}）和行动（{@code remaining--}）
     * 之间没有任何保护。
     */
    Result sellWithoutLock() throws InterruptedException {
        remaining = total;
        sold = 0;
        sell(false);
        return new Result(sold, remaining);
    }

    /** 加锁地卖：检查加行动一起放进 {@code synchronized} 块，变成一个不可分割的动作。 */
    Result sellWithLock() throws InterruptedException {
        remaining = total;
        sold = 0;
        sell(true);
        return new Result(sold, remaining);
    }

    private void sell(boolean safe) throws InterruptedException {
        CountDownLatch go = new CountDownLatch(1);
        Thread[] sellers = new Thread[windows];

        for (int i = 0; i < windows; i++) {
            sellers[i] = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                while (true) {
                    if (safe) {
                        synchronized (lock) {
                            if (remaining <= 0) {
                                return;
                            }
                            remaining--;
                            sold++;
                        }
                    } else {
                        if (remaining <= 0) {
                            return;                 // ① 看
                        }
                        if (widen) {
                            Thread.yield();         // 把「看」和「拿」之间的缝撑开
                        }
                        remaining--;                // ② 拿
                        sold++;
                    }
                }
            });
            sellers[i].start();
        }

        go.countDown();
        for (Thread seller : sellers) {
            seller.join();
        }
    }
}
