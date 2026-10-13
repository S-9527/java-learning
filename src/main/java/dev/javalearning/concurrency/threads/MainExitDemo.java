package dev.javalearning.concurrency.threads;

/**
 * 第 12 课的补充示例：主线程结束 ≠ 进程退出。
 *
 * <p>这个实验必须让 main <b>真的返回</b>，才能看出「主线程结束了，子线程还在跑」。
 * 如果塞进 {@link ThreadsDemo} 里，它后面还有 13 节要演，根本没法返回。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/threads/MainExitDemo.java}
 *
 * <p>注意最后那个「子线程收尾」是在 main 返回之后打印的 —— 那就是 JVM
 * 在等这个用户线程。
 */
public class MainExitDemo {

    public static void main(String[] args) {
        Thread t = new Thread(() -> {
            sleepQuietly(600);
            System.out.println("[子线程] 主线程早就返回了，我还在跑");
            sleepQuietly(600);
            System.out.println("[子线程] 我结束了，JVM 现在才退出");
        }, "lingering");

        t.start();
        System.out.println("[main] 执行 main 的线程叫：" + Thread.currentThread().getName());
        System.out.println("[main] 子线程启动了");
        System.out.println("[main] 我不 join，我直接返回");
        System.out.println("[main] main 方法到此结束");
        // main 返回之后 JVM 不会退出，因为 lingering 还是个用户线程
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
