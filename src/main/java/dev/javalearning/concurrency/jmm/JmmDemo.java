package dev.javalearning.concurrency.jmm;

import java.util.concurrent.CountDownLatch;

/**
 * 第 14 课的示例代码：JMM 与 volatile。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/jmm/JmmDemo.java}
 *
 * <p><b>这一课有两个方法需要特殊参数运行</b>（可见性演示依赖 JIT 优化）：
 * <pre>
 * java -Xint src/main/java/dev/javalearning/concurrency/jmm/JmmDemo.java nojit
 * </pre>
 * 详见 {@code docs/14-JMM与volatile.md}。
 */
public class JmmDemo {

    /** 普通 boolean，不加任何修饰——可见性问题就出在它身上。 */
    static boolean running = true;

    /** 加了 volatile 的版本，用来对比。 */
    static volatile boolean volatileRunning = true;

    /** 计数器，用来证明 volatile 不保证原子性。 */
    static volatile int volatileCount = 0;

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("nojit")) {
            // 只跑第一节，用来看「不做 JIT 优化」时它会不会正常退出
            section("1. 可见性（-Xint，不做 JIT 优化）");
            visibilityDemo("nojit");
            return;
        }

        section("1. 可见性：一个线程改了值，另一个线程看不见");
        visibilityDemo("default");

        section("2. volatile 保证可见性：同样的代码，加了就正常");
        volatileVisibilityDemo();

        section("3. volatile 不保证原子性：两个线程各加 100 万");
        volatileAtomicityDemo();

        section("4. 锁规则：synchronized 也能保证可见性");
        lockVisibilityDemo();

        section("5. volatile 的正确用法：状态标志位");
        flagDemo();

        section("6. 双重检查锁定单例：instance 为什么必须是 volatile");
        System.out.println("第一次拿: " + Singleton.getInstance());
        System.out.println("第二次拿: " + Singleton.getInstance());
        System.out.println("是同一个对象吗 → " + (Singleton.getInstance() == Singleton.getInstance()));

        section("7. happens-before：线程启动和 join");
        happensBeforeDemo();
    }

    /**
     * 可见性演示。线程在 while 里读 running，主线程一秒后设为 false。
     *
     * <p>默认（JIT 优化）下，JIT 可能把 running 提升到循环外只读一次，
     * 于是这个线程永远停不下来——这就是可见性问题。
     * 用 {@code -Xint}（不做 JIT 优化）跑，它就能正常退出。
     */
    static void visibilityDemo(String mode) throws Exception {
        running = true;
        Thread counter = new Thread(() -> {
            long i = 0;
            while (running) {
                i++;
            }
            System.out.println("  counter 线程退出了");
        }, "counter");
        // 这个线程可能永远停不下来，设成守护线程，主线程跑完 JVM 就能退出
        counter.setDaemon(true);
        counter.start();
        Thread.sleep(1000);
        System.out.println("  主线程把 running 设成了 false");
        running = false;              // 先打印再设标志，保证输出顺序确定
        counter.join(3000);
        System.out.println("  等了 3 秒，counter 还活着吗 → " + counter.isAlive());
        if (counter.isAlive()) {
            System.out.println("  ← 它永远看不到 running 变成了 false（JIT 把读优化掉了）");
            System.out.println("  ← 这就是可见性问题：一个线程改了值，另一个线程看不见");
        } else {
            System.out.println("  ← 这次它看见了（" + mode + " 模式下没有把读提升到循环外）");
        }
    }

    /** 同样的代码，只是给 running 加了 volatile。 */
    static void volatileVisibilityDemo() throws Exception {
        volatileRunning = true;
        Thread counter = new Thread(() -> {
            long i = 0;
            while (volatileRunning) {
                i++;
            }
            System.out.println("  counter 线程退出了");
        }, "volatile-counter");
        counter.start();
        Thread.sleep(1000);
        System.out.println("  主线程把 volatileRunning 设成了 false");
        volatileRunning = false;      // 同样先打印再设标志
        counter.join(3000);
        System.out.println("  等了 3 秒，counter 还活着吗 → " + counter.isAlive());
    }

    /**
     * volatile 不保证原子性：count++ 依然是"读-改-写"三步。
     *
     * <p>为了让输出稳定，跑 10 轮取最好的一次和最差的一次。
     */
    static void volatileAtomicityDemo() throws Exception {
        // 关键设计：不去断言「第 N 轮一定丢」，而是「循环直到观测到一次丢」。
        //
        // 为什么？因为两个线程偶尔会恰好串行执行（一个跑完另一个才开始），
        // 那一轮就不会丢更新——我实测过，20 轮里有 3~6 轮是这种巧合。
        // 但「循环足够多轮之后总会出现一次丢失」是必然的：
        // 只要有一轮出现 count < 2000000，volatile 不保证原子性就被证明了。
        //
        // 这样输出就稳定了：结论永远是 true，只是花了多少轮不确定（所以不打印轮数）。
        int rounds = 0;
        boolean observedLoss = false;
        while (rounds < 50 && !observedLoss) {
            rounds++;
            volatileCount = 0;
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 1_000_000; i++) {
                    volatileCount++;
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 1_000_000; i++) {
                    volatileCount++;
                }
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            if (volatileCount < 2_000_000) {
                observedLoss = true;
            }
        }
        System.out.println("  两个线程各加 100 万，期望 2000000");
        System.out.println("  观测到「加少了」吗 → " + observedLoss);
        System.out.println("  ← 加了 volatile 依然丢更新，因为它只管可见性不管原子性");
        System.out.println("  ← 某一轮恰好没丢是可能的（两个线程凑巧串行了），所以这里循环到出现为止");
    }

    /** 用一个普通 int（不是 volatile），靠锁来保证可见性。 */
    static int lockedValue = 0;

    static void lockVisibilityDemo() throws Exception {
        lockedValue = 0;
        CountDownLatch written = new CountDownLatch(1);
        final Object lock = new Object();

        Thread writer = new Thread(() -> {
            synchronized (lock) {
                lockedValue = 42;
            }
            written.countDown();
        });
        writer.start();
        written.await();
        writer.join();

        synchronized (lock) {
            System.out.println("  writer 在锁里写了 lockedValue = 42（它不是 volatile）");
            System.out.println("  reader 在同一个锁里读到 → " + lockedValue);
            System.out.println("  ← 解锁 happens-before 后续加锁，所以看得见");
        }
    }

    /** 状态标志位——volatile 最典型的正确用法。 */
    static void flagDemo() throws Exception {
        Thread worker = new Thread(() -> {
            long i = 0;
            while (volatileRunning) {
                i++;
            }
            System.out.println("  worker 看到标志位变成 false，干净退出");
        });
        volatileRunning = true;
        worker.start();
        Thread.sleep(500);
        volatileRunning = false;
        worker.join(2000);
        System.out.println("  还活着吗 → " + worker.isAlive());
    }

    /** 线程启动 / join 的 happens-before：start 前的写对子线程可见。 */
    static void happensBeforeDemo() throws Exception {
        StringBuilder shared = new StringBuilder();

        // start() happens-before 新线程里的一切，所以子线程能看见 start 前的写
        shared.append("start 之前写的");
        Thread t = new Thread(() -> {
            shared.append("，子线程看到了");
        });
        t.start();
        t.join();   // 线程里的所有操作 happens-before join() 返回
        shared.append("，join 之后主线程也看到了");

        System.out.println("  " + shared);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
