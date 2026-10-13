package dev.javalearning.concurrency.safety;

import java.util.concurrent.CountDownLatch;

/**
 * 第 13 课的示例代码：让一批线程「同时」开始跑同一段逻辑。
 *
 * <p>并发 bug 是抢出来的。线程如果一个个错开启动，先启动的那个可能已经跑完了，
 * 根本没有机会交错，bug 就复现不出来——这就是「测试环境跑一百次都对，
 * 生产环境偶发」的一半原因。这里用两个 {@link CountDownLatch}：一个等所有线程
 * 都就位（{@code ready}），一个当发令枪（{@code go}），尽量让它们同时冲出去。
 *
 * <p>包私有：它只是本课 demo 和断言的脚手架，不是要教的知识点。
 */
final class Threads {

    private Threads() {
    }

    /** 起 {@code threadCount} 个线程，等它们全部就位后同时执行 {@code body}，再全部结束才返回。 */
    static void runTogether(int threadCount, Runnable body) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                ready.countDown();          // 我准备好了
                try {
                    go.await();             // 等发令枪
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                body.run();
            });
            threads[i].start();
        }

        ready.await();      // 等到齐
        go.countDown();     // 开枪
        for (Thread thread : threads) {
            thread.join();  // 等它们全跑完
        }
    }

    /**
     * 一个接一个地跑：起一个、等它彻底结束、再起下一个。
     *
     * <p>用它来当「不重叠」的对照组——同样的代码、同样的次数，只因为线程没机会碰上，
     * 结果就永远是对的。这是理解「为什么这个 bug 有时不出现」的关键。
     */
    static void runSequentially(int threadCount, Runnable body) throws InterruptedException {
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(body);
            thread.start();
            thread.join();      // 下一个必须等这一个结束
        }
    }
}
