package dev.javalearning.concurrency.safety;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 第 13 课的示例代码：同一个「自增」用几种方式实现，放在一起对比。
 *
 * <p>它们做的事一模一样（把计数器加一），区别只在<b>怎么保证不被打断</b>：
 *
 * <ul>
 *   <li>{@link #incrementUnsafe()} —— 裸的 {@code count++}，会丢更新</li>
 *   <li>{@link #incrementWithWideWindow()} —— 把「读」和「写」之间的窗口撑开，
 *       让丢失更新几乎每次都出现</li>
 *   <li>{@link #incrementSynchronized()} —— 方法级 {@code synchronized}，锁的是 {@code this}</li>
 *   <li>{@link #incrementWithLockObject()} —— 同步代码块，锁的是 {@code lock} 那个对象</li>
 *   <li>{@link #incrementLocked()} —— {@link ReentrantLock}，注意 {@code finally} 里解锁</li>
 *   <li>{@link #incrementWithFreshLock()} —— <b>故意写错</b>：每次 new 一把新锁，等于没锁</li>
 * </ul>
 *
 * <p>对应 {@code docs/13-线程安全与同步.md}。
 */
public class Counter {

    private int unsafe;
    private int wideWindow;
    private volatile int volatileWideWindow;
    private int synchronizedCount;
    private int blockCount;
    private int locked;
    private int freshLock;

    private final Object lock = new Object();
    private final ReentrantLock reentrantLock = new ReentrantLock();

    /** 裸的 {@code count++}：读、加、写三步，中间谁都能插进来。 */
    public void incrementUnsafe() {
        unsafe++;
    }

    /**
     * 故意把「读」和「写」之间的窗口撑开，让丢失更新几乎每次都出现。
     *
     * <p>{@link Thread#yield()} 在这里是个<b>教学道具</b>：真实业务里这两步之间
     * 可能是查余额、算利息、写日志，窗口比这宽得多。窗口越宽，别人越容易挤进来。
     *
     * <p>每 8 次才让一次（而不是每次都让），是因为每次都让的话一个线程要让
     * 1000 次，整个演示要跑几十秒。每 8 次让一次同样能让 20 次统计里次次都错，
     * 但快了两个数量级。
     *
     * <p>为什么裸的 {@code count++} 常常复现不出来、这个却几乎每次都错？
     * 因为裸的自增只有几条机器指令，线程跑得太快、压根没重叠（见第 2 节实测）。
     *
     * @param step 调用方传进来的循环下标，只用来决定「这次要不要让」
     */
    public void incrementWithWideWindow(int step) {
        int current = wideWindow;               // ① 读
        if ((step & 7) == 0) {
            Thread.yield();                     // ② 每 8 次撑开一次窗口
        }
        wideWindow = current + 1;               // ③ 写
    }

    /**
     * {@code volatile} 版的三步写法。
     *
     * <p>字段加了 {@code volatile}（保证可见性），写法也是显式的「读—加—写」，
     * 但<b>照样丢更新</b>——因为 {@code volatile} 不保证原子性。
     * 文档里专门用这个对比来打掉「加了 volatile 就线程安全」这个误会。
     */
    public void incrementVolatileWideWindow(int step) {
        int current = volatileWideWindow;       // ① 读
        if ((step & 7) == 0) {
            Thread.yield();                     // ② 每 8 次撑开一次窗口
        }
        volatileWideWindow = current + 1;       // ③ 写
    }

    /** {@code synchronized} 修饰方法：整段不可打断，锁的是当前对象 {@code this}。 */
    public synchronized void incrementSynchronized() {
        synchronizedCount++;
    }

    /** 同步代码块：锁的是 {@code lock} 指向的那个对象。 */
    public void incrementWithLockObject() {
        synchronized (lock) {
            blockCount++;
        }
    }

    /** {@link ReentrantLock}：{@code unlock} 必须写在 {@code finally} 里，否则出异常就永远不还锁。 */
    public void incrementLocked() {
        reentrantLock.lock();
        try {
            locked++;
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * <b>故意写错</b>：每次进方法都 {@code new} 一把新锁。
     *
     * <p>每个线程拿到的是<b>不同的对象</b>，也就是不同的锁，互不排斥，等于没加锁。
     * 文档里专门演示了这个坑。
     */
    public void incrementWithFreshLock() {
        synchronized (new Object()) {
            freshLock++;
        }
    }

    public int unsafe() {
        return unsafe;
    }

    public int wideWindow() {
        return wideWindow;
    }

    public int volatileWideWindow() {
        return volatileWideWindow;
    }

    public int synchronizedCount() {
        return synchronizedCount;
    }

    public int blockCount() {
        return blockCount;
    }

    public int locked() {
        return locked;
    }

    public int freshLock() {
        return freshLock;
    }
}
