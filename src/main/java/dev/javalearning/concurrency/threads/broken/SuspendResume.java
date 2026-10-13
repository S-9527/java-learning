package dev.javalearning.concurrency.threads.broken;

/**
 * 这是<b>故意写错的反面教材</b>：{@code suspend()} / {@code resume()} 已经被移除。
 *
 * <p>放在 {@code broken/} 里是因为它<b>编译不过</b>——{@code build.gradle.kts}
 * 把整个 {@code broken/} 包排除了，不参与构建。在 IDE 里看到它标红是正常的。
 *
 * <p>{@code suspend()} 会挂起线程但<b>不释放它手里的锁</b>，别的线程一等就是死等，
 * 是死锁的经典来源；{@code resume()} 一旦比 {@code suspend()} 先到，线程就永远醒不过来。
 * 所以 JDK 把它们直接删了，只留下 {@code stop()} 还在（废弃、调用抛异常）。
 *
 * <p>真实编译错误见 {@code docs/12-多线程基础.md}，那里贴的是 javac 的原话。
 */
public class SuspendResume {

    public static void main(String[] args) {
        Thread t = new Thread(() -> System.out.println("干活"));
        t.start();
        t.suspend();
        t.resume();
    }
}
