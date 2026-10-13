package dev.javalearning.concurrency.threads;

import java.util.concurrent.CountDownLatch;

/**
 * 第 12 课的补充示例：守护线程被 JVM 直接掐掉。
 *
 * <p>这个实验也必须让 main <b>真的返回</b>——只有进程退出才能看出
 * 「守护线程的下一次心跳永远不会出现」。所以它单独一个类，
 * 而不是塞进 {@link ThreadsDemo}（那里后面还有别的实验要跑）。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/threads/DaemonExitDemo.java}
 *
 * <p><b>关于稳定性</b>：这里有两处刻意的设计，否则输出会抖。
 * <ol>
 *   <li>守护线程先打印、再 {@code countDown()}，用户线程 {@code await()} 之后才开始计时。
 *       这样「第 1 次心跳」一定排在「用户线程干完了」前面，不靠 sleep 猜。</li>
 *   <li>心跳间隔取 1500ms，而用户线程 300ms 就结束了。中间这 1200ms 的余量
 *       保证 JVM 一定来得及退出，不会冷不丁多冒一次心跳出来。</li>
 * </ol>
 */
public class DaemonExitDemo {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch firstBeat = new CountDownLatch(1);

        Thread daemon = new Thread(() -> {
            int beats = 0;
            while (true) {
                beats++;
                System.out.println("[守护线程] 第 " + beats + " 次心跳");
                if (beats == 1) {
                    firstBeat.countDown();
                }
                sleepQuietly(1500);            // 下一次心跳在 1.5 秒之后 —— JVM 早退了
            }
        }, "daemon-heartbeat");
        daemon.setDaemon(true);

        Thread user = new Thread(() -> {
            awaitQuietly(firstBeat);           // 等第 1 次心跳打完再开始计时
            sleepQuietly(300);
            System.out.println("[用户线程] 我干完了，我是用户线程");
        }, "user-worker");

        daemon.start();
        user.start();
        user.join();                           // 只等用户线程，不等守护线程

        System.out.println("[main] 用户线程结束了，现在只剩守护线程");
        System.out.println("[main] 我结束了 —— JVM 这就退出，守护线程被直接掐掉");
        System.out.println("[main] 所以第 2 次心跳永远不会出现");
        // main 返回后 JVM 发现「用户线程都结束了」，立刻退出。
        // 守护线程此刻正睡在那 1500ms 里，连醒都醒不过来。
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
