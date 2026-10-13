package dev.javalearning.concurrency.threads;

/**
 * 第 12 课的补充示例：{@code stop()} 已经废弃到「调了会抛异常」。
 *
 * <p>单独放一个类，是因为它需要 {@code System.exit(0)} 收尾：那个被
 * {@code stop()} 的线程其实**还活着**，用户线程不结束 JVM 就不退出。
 * 放在 {@link ThreadsDemo} 里会把后面几节全堵死。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/threads/StopIsGoneDemo.java}
 *
 * <p>类名里带 {@code Stop} 所以它是 {@code StopIsGoneDemo.java}，
 * 文件名和类名一致，能直接单文件运行。
 */
public class StopIsGoneDemo {

    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(() -> {
            long deadline = System.nanoTime() + 5_000_000_000L;
            long n = 0;
            while (System.nanoTime() < deadline) {
                n++;
            }
        }, "victim");
        t.start();
        Thread.sleep(100);

        try {
            t.stop();
            System.out.println("stop() 没抛异常，线程还活着吗 = " + t.isAlive());
        } catch (UnsupportedOperationException e) {
            System.out.println("stop() → java.lang.UnsupportedOperationException");
        }
        t.join(300);
        System.out.println("join(300) 之后 isAlive() = " + t.isAlive() + " —— 它根本没被停掉");
        System.out.println("它还在空转，JVM 不会退出，所以这里手动收尾");

        // 顺带把编译期的警告原话带出来：javac 对 stop() 的用词是 removal
        System.out.println("javac 会警告：[removal] stop() in Thread has been deprecated and marked for removal");

        System.exit(0);
    }
}
