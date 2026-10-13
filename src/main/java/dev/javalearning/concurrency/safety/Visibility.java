package dev.javalearning.concurrency.safety;

/**
 * 第 13 课的示例代码：演示<b>可见性</b>问题。
 *
 * <p>一个线程死循环等一个标志位变 {@code false}，另一个线程把它设成 {@code false}。
 * 用普通字段时，跑着的线程<b>可能永远看不到这次修改</b>，于是永远停不下来。
 * 换成 {@code volatile} 字段，它每次都去主内存重新读，就能停下来。
 *
 * <p><b>注意</b>：这个现象依赖 JIT 优化。本机（JDK 25，默认分层编译）实测稳定复现；
 * 但换成 {@code -Xint}（纯解释执行）就不会出现，因为解释器每次都老老实实去读内存。
 * 内存模型上「看不到」是允许的，所以不能因为某次没复现就以为程序是对的。
 *
 * <p>还有一个实现上的坑，值得单独说一句：如果循环写成
 * {@code while (running && !escape)}、而 {@code escape} 是个 {@code volatile} 字段，
 * 那这个 {@code volatile} 读会<b>顺手把 {@code running} 也带成每次都读</b>，
 * 现象就不出现了。所以本类故意不给那个卡住的线程留逃生门——它是守护线程，
 * 主线程一结束 JVM 直接退出，不会拖住程序。
 *
 * <p>对应 {@code docs/13-线程安全与同步.md}。
 */
final class Visibility {

    private static boolean plainRunning;
    private static volatile boolean volatileRunning;

    private Visibility() {
    }

    /**
     * 普通字段版：把 {@code plainRunning} 改成 {@code false}，但那个线程很可能看不见。
     *
     * @return 线程有没有「停不下来」（true = 卡住了）
     */
    static boolean plainGetsStuck(long millis) throws InterruptedException {
        plainRunning = true;
        Thread worker = new Thread(() -> {
            while (plainRunning) {
                // 空转：故意什么都不做，就为了等标志位
            }
        });
        worker.setDaemon(true);     // 卡住了也不拖住 JVM
        worker.start();

        Thread.sleep(millis);
        plainRunning = false;       // 改是改了，但对面看不见
        worker.join(500);
        return worker.isAlive();
    }

    /**
     * {@code volatile} 字段版：改了就看得见。
     *
     * @return 线程有没有停下来
     */
    static boolean volatileStops(long millis) throws InterruptedException {
        volatileRunning = true;
        Thread worker = new Thread(() -> {
            while (volatileRunning) {
                // 空转
            }
        });
        worker.setDaemon(true);
        worker.start();

        Thread.sleep(millis);
        volatileRunning = false;    // 这次对面看得见
        worker.join(2000);
        return !worker.isAlive();
    }

    /** 普通字段版连跑 {@code runs} 次，只要有<b>一次</b>卡住就算复现了。 */
    static boolean plainEverStuck(int runs, long millis) throws InterruptedException {
        boolean ever = false;
        for (int i = 0; i < runs; i++) {
            ever |= plainGetsStuck(millis);
        }
        return ever;
    }
}
