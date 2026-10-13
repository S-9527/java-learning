package dev.javalearning.concurrency.safety;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 第 13 课的示例代码：演示<b>有序性</b>——指令重排。
 *
 * <p>经典的「标记位」场景：线程 A 先准备数据 {@code data = 42}，再立起标记
 * {@code ready = true}；线程 B 等看到 {@code ready} 才去用 {@code data}。
 * 直觉上 B 只要看到标记，数据就一定准备好了。
 *
 * <p>但这两句写操作可能被重排成「先立标记，再写数据」，于是 B 会看到
 * {@code ready == true} 而 {@code data} 还是旧的。本类用一个更精简的
 * <b>litmus test</b>（石蕊试纸测试）来抓这个现象：
 *
 * <pre>
 *   A: x = 1; r1 = y;      B: y = 1; r2 = x;
 * </pre>
 *
 * <p>如果两个线程都规规矩矩按顺序执行，那么 {@code (r1, r2)} 不可能同时是
 * {@code (0, 0)}——总有一个人的写发生在另一个人的读之前。真出现了 {@code (0, 0)}，
 * 就说明重排确实发生了。
 *
 * <p>把 {@code x}、{@code y} 换成 {@code volatile}，重排被禁止，{@code (0, 0)}
 * 应该彻底消失。这就是「{@code volatile} 保证有序性」的实证。
 *
 * <p>实现上有个讲究：每轮都 {@code new Thread} 的话，跑几万轮要几分钟。
 * 这里让两个线程一直活着，用 {@code round} 这个 {@link AtomicInteger}
 * （它自带 {@code volatile} 语义）当发令枪，几十毫秒就能跑完 20 万轮。
 *
 * <p>对应 {@code docs/13-线程安全与同步.md}。
 */
final class Reordering {

    /** 跑 {@code rounds} 轮，{@code anomalies} 是出现 {@code (0, 0)} 的轮数。 */
    record Result(int rounds, int anomalies) {

        boolean sawAnomaly() {
            return anomalies > 0;
        }
    }

    private static int plainX;
    private static int plainY;
    private static volatile int volatileX;
    private static volatile int volatileY;

    private Reordering() {
    }

    /** 普通字段：允许重排，能抓到 {@code (0, 0)}。 */
    static Result runPlain(int rounds) throws InterruptedException {
        return run(rounds, false);
    }

    /** {@code volatile} 字段：禁止重排，抓不到 {@code (0, 0)}。 */
    static Result runVolatile(int rounds) throws InterruptedException {
        return run(rounds, true);
    }

    private static Result run(int rounds, boolean useVolatile) throws InterruptedException {
        AtomicInteger round = new AtomicInteger();      // 发令枪，volatile 语义
        AtomicInteger finished = new AtomicInteger();
        AtomicInteger stopped = new AtomicInteger();
        int[] r1 = {-1};
        int[] r2 = {-1};

        Thread a = new Thread(() -> loop(round, finished, stopped, r1, r2, true, useVolatile));
        Thread b = new Thread(() -> loop(round, finished, stopped, r1, r2, false, useVolatile));
        a.setDaemon(true);
        b.setDaemon(true);
        a.start();
        b.start();

        int anomalies = 0;
        for (int i = 1; i <= rounds; i++) {
            if (useVolatile) {
                volatileX = 0;
                volatileY = 0;
            } else {
                plainX = 0;
                plainY = 0;
            }
            r1[0] = -1;
            r2[0] = -1;
            finished.set(0);
            round.set(i);
            while (finished.get() < 2) {
                Thread.onSpinWait();
            }
            if (r1[0] == 0 && r2[0] == 0) {
                anomalies++;
            }
        }

        stopped.set(1);     // 放这两个线程走，别让它们继续烧 CPU
        round.incrementAndGet();
        a.join(1000);
        b.join(1000);
        return new Result(rounds, anomalies);
    }

    /** 一个线程反复做「写自己的、读对方的」。 */
    private static void loop(AtomicInteger round, AtomicInteger finished, AtomicInteger stopped,
                             int[] r1, int[] r2, boolean isA, boolean useVolatile) {
        int lastSeen = 0;
        while (stopped.get() == 0) {
            while (round.get() == lastSeen && stopped.get() == 0) {
                Thread.onSpinWait();
            }
            if (stopped.get() != 0) {
                return;
            }
            lastSeen = round.get();

            if (useVolatile) {
                if (isA) {
                    volatileX = 1;
                    r1[0] = volatileY;
                } else {
                    volatileY = 1;
                    r2[0] = volatileX;
                }
            } else {
                if (isA) {
                    plainX = 1;
                    r1[0] = plainY;
                } else {
                    plainY = 1;
                    r2[0] = plainX;
                }
            }
            finished.incrementAndGet();
        }
    }
}
