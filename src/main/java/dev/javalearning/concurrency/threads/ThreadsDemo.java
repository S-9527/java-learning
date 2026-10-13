package dev.javalearning.concurrency.threads;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 第 12 课的示例代码：多线程基础。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/threads/ThreadsDemo.java}
 * 每一节对应 {@code docs/12-多线程基础.md} 里的一节，输出都是真跑出来的。
 *
 * <p>线程的输出顺序天生不确定，所以凡是有先后要求的地方，都用
 * {@code join} / {@code sleep} / 计数器把顺序钉死，保证每次跑出来一样。
 * 唯一不稳定的数字（比如计数线程数了多少下）干脆不打印。
 *
 * <p>这一课故意只用一个类讲完：第 13 节（守护线程）必须放在最后，
 * 因为 JVM 要退出才能看出守护线程被掐掉。「主线程结束 ≠ 进程退出」
 * 那个实验要主线程真的返回，所以单独放在 {@link MainExitDemo} 里。
 */
public class ThreadsDemo {

    public static void main(String[] args) throws Exception {
        section("1. 并发 vs 并行");
        concurrencyVsParallelism();

        section("2. 主线程");
        mainThread();

        section("3. 创建线程的方式");
        byExtendingThread();
        byImplementingRunnable();
        byLambda();
        byCallableAndFutureTask();

        section("4. start() 和 run() 的区别");
        startVersusRun();

        section("5. 线程是异步执行的：主线程不会等子线程");
        mainDoesNotWait();

        section("6. join()：等另一个线程做完");
        joinBlocksTheCaller();
        joinFromAnotherThread();

        section("7. 两个线程同时跑 vs 一个一个跑");
        overlapVersusSequential();

        section("8. 线程的生命周期六种状态");
        lifecycleStates();

        section("9. 中断机制：interrupt() 只是发个信号");
        interruptIsJustASignal();

        section("10. 正确的停止方式");
        stopByCheckingFlag();
        stopByCatchingInterruptedException();

        section("11. interrupted() 和 isInterrupted() 的区别");
        interruptedVersusIsInterrupted();

        section("12. 不推荐的用法");
        ignoringInterruptedException();

        section("13. 用户线程 vs 守护线程");
        daemonThreadGetsKilled();
    }

    // ── 1. 并发 vs 并行 ────────────────────────────────────────────

    /** 并发是「交替推进」，并行是「同时进行」。这里演示前者。 */
    private static void concurrencyVsParallelism() throws InterruptedException {
        System.out.println("并发 Concurrency：同一时间段内处理多个任务，靠调度快速切换");
        System.out.println("                  单核也能并发 —— 逻辑上同时，物理上轮流");
        System.out.println("并行 Parallelism：同一时刻真的在跑，需要多核");
        System.out.println("本机可用核心数 = " + Runtime.getRuntime().availableProcessors()
                + "（这个数取决于你的机器）");
        System.out.println();
        System.out.println("-- 两个任务在同一段时间里交替推进");
        // 等 A 真的开跑了再启动 B，纯粹为了让输出顺序每次都一样
        CountDownLatch aStarted = new CountDownLatch(1);
        Thread a = new Thread(() -> {
            System.out.println("  A 第 1 次推进");
            aStarted.countDown();
            sleepQuietly(400);
            System.out.println("  A 第 2 次推进");
        }, "A");
        Thread b = new Thread(() -> {
            System.out.println("  B 第 1 次推进");
            sleepQuietly(200);
            System.out.println("  B 第 2 次推进");
        }, "B");
        a.start();
        aStarted.await();
        b.start();
        a.join();
        b.join();
        System.out.println("  B 的两次都在 A 的两次之间 —— 它们在同一段时间里各自推进");
    }

    // ── 2. 主线程 ──────────────────────────────────────────────────

    /** main 方法跑在主线程上，它的名字就叫 main。 */
    private static void mainThread() throws InterruptedException {
        Thread main = Thread.currentThread();
        System.out.println("执行 main 的线程：" + main.getName());
        System.out.println("它的线程 id = " + main.threadId() + "（JVM 内部编号，换台机器可能是别的数）");

        // 改名字只影响显示（日志、jstack、调试器），对行为没有任何影响
        Thread worker = new Thread(
                () -> System.out.println("  线程里看到自己的名字 = " + Thread.currentThread().getName()),
                "worker-1");
        System.out.println("启动之前叫：" + worker.getName());
        worker.setName("搬砖工");
        System.out.println("setName 之后叫：" + worker.getName());
        worker.start();
        worker.join();
    }

    // ── 3. 创建线程的方式 ──────────────────────────────────────────

    /** 方式一：继承 Thread，重写 run。 */
    private static void byExtendingThread() throws InterruptedException {
        Thread t = new NamedTask("extends-thread");
        t.start();
        t.join();
        System.out.println("  写法最直接，但任务逻辑写死在类里，而且 Java 不支持多继承，扩展性差");
    }

    /** 方式二：实现 Runnable，把任务和线程分开。 */
    private static void byImplementingRunnable() throws InterruptedException {
        PrintTask shared = new PrintTask("同一个任务对象");
        // 这里刻意一个一个跑，好让输出顺序稳定；同一个任务对象当然也能被两个线程同时跑
        Thread first = new Thread(shared, "worker-1");
        first.start();
        first.join();
        Thread second = new Thread(shared, "worker-2");
        second.start();
        second.join();
        System.out.println("  任务和线程解耦了：一个 Runnable 对象可以被多个线程执行");
    }

    /** 方式三：Runnable 是函数式接口，Lambda 直接写。 */
    private static void byLambda() throws InterruptedException {
        Thread t = new Thread(() -> System.out.println(
                "  Lambda 版：跑在 " + Thread.currentThread().getName() + " 上"), "lambda-worker");
        t.start();
        t.join();
        // 顺便看一眼：不给名字的话 JVM 叫它什么
        Thread unnamed = new Thread(
                () -> System.out.println("  不给名字的线程，JVM 自动叫它：" + Thread.currentThread().getName()));
        unnamed.start();
        unnamed.join();
        System.out.println("  编号按「创建顺序」发，本课其余地方都显式命名，所以这里每次都是 Thread-0");
    }

    /** 方式四：实现 Callable + FutureTask，能返回结果、能抛异常。 */
    private static void byCallableAndFutureTask() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> {
            Thread.sleep(200);
            return "Callable 的返回值（来自 " + Thread.currentThread().getName() + "）";
        });
        Thread t = new Thread(task, "callable-worker");
        t.start();
        System.out.println("  还没跑完时 isDone() = " + task.isDone());
        System.out.println("  get() 拿到的结果 = " + task.get());
        System.out.println("  跑完之后 isDone() = " + task.isDone());

        // Callable 允许抛受检异常，异常会被包在 ExecutionException 里带回来
        FutureTask<Integer> boom = new FutureTask<>(() -> {
            throw new IllegalStateException("Callable 里可以抛异常");
        });
        new Thread(boom, "callable-throws").start();
        try {
            boom.get();
        } catch (ExecutionException e) {
            System.out.println("  get() 抛了 " + e.getClass().getSimpleName()
                    + "，真正的原因在 getCause() 里：" + e.getCause().getMessage());
        }
        System.out.println("  这是线程池那一课的主角，这里先见个面");
    }

    // ── 4. start() 和 run() 的区别 ─────────────────────────────────

    /** 调 run() 只是普通方法调用，跑在当前线程里。 */
    private static void startVersusRun() throws InterruptedException {
        Thread t = new Thread(() -> System.out.println(
                "    run() 里打印的线程名 = " + Thread.currentThread().getName()), "worker");

        System.out.println("直接调 t.run()：");
        t.run();                       // ← 就是一次普通的方法调用
        System.out.println("  上面打印的是 main —— 根本没有新线程");

        System.out.println("再调 t.start()：");
        t.start();                     // ← 这才是「开一个新线程」
        t.join();
        System.out.println("  同一段 run()，start() 之后就跑到 worker 上了");
    }

    // ── 5. 线程是异步执行的 ────────────────────────────────────────

    /** 主线程启动子线程之后继续往下走，不会等它。 */
    private static void mainDoesNotWait() throws InterruptedException {
        Thread t = new Thread(() -> {
            sleepQuietly(800);         // 先睡，好让主线程那几行一定排在前面
            System.out.println("子线程：我睡醒了，现在才轮到我的输出");
        }, "sleeper");
        t.start();
        System.out.println("主线程：子线程已经启动，但我不等它");
        System.out.println("主线程：我自己的活已经干完了");
        System.out.println("主线程：上面这两行都排在子线程那一行前面 —— 谁也没等谁");
        t.join();                      // 只是为了不让它的输出串到后面几节里去
        System.out.println("主线程：这一节末尾我 join 了一下，纯属为了排版整齐");
    }

    // ── 6. join ────────────────────────────────────────────────────

    /** 在主线程里 join：阻塞的是主线程。 */
    private static void joinBlocksTheCaller() throws InterruptedException {
        Thread t = new Thread(() -> {
            sleepQuietly(300);
            System.out.println("  子线程：开始干活");
            sleepQuietly(300);
            System.out.println("  子线程：干完了");
        }, "worker");
        t.start();
        System.out.println("  主线程：调 join() 之前");
        System.out.println("  主线程：下面这行 join() 会把我卡住");
        t.join();
        System.out.println("  主线程：join() 返回了，说明子线程已经结束");
    }

    /** 在 A 线程里 join B：阻塞的是 A。 */
    private static void joinFromAnotherThread() throws InterruptedException {
        Thread b = new Thread(() -> {
            sleepQuietly(300);
            System.out.println("    B：我结束了");
        }, "B");
        Thread a = new Thread(() -> {
            System.out.println("    A：我要等 B，所以我先卡住");
            joinQuietly(b);
            System.out.println("    A：B 结束了，我接着跑");
        }, "A");
        b.start();
        a.start();
        joinQuietly(a);
        System.out.println("    main：我只等了 A，B 是 A 等的");
    }

    // ── 7. 重叠 vs 串行 ────────────────────────────────────────────

    /** 先全部 start 再 join，和「启动一个 join 一个」完全不一样。 */
    private static void overlapVersusSequential() throws InterruptedException {
        System.out.println("-- 方式一：两个都 start，再依次 join");
        CountDownLatch aStarted = new CountDownLatch(1);
        Thread a1 = new Thread(() -> {
            System.out.println("  A 开始（要 900ms）");
            aStarted.countDown();
            sleepQuietly(900);
            System.out.println("  A 结束");
        }, "A");
        Thread b1 = new Thread(() -> {
            System.out.println("  B 开始（要 400ms）");
            sleepQuietly(400);
            System.out.println("  B 结束");
        }, "B");
        long start1 = System.nanoTime();
        a1.start();
        aStarted.await();
        b1.start();
        a1.join();
        b1.join();
        long elapsed1 = (System.nanoTime() - start1) / 1_000_000;
        System.out.println("  B 开始的时候 A 还没结束 —— 两个线程是重叠跑的");
        System.out.println("  总耗时约 " + round100(elapsed1) + " ms，比 900 + 400 = 1300 少");

        System.out.println("-- 方式二：启动一个、join 一个，再启动下一个");
        Thread a2 = new Thread(() -> {
            System.out.println("  A 开始（要 900ms）");
            sleepQuietly(900);
            System.out.println("  A 结束");
        }, "A");
        Thread b2 = new Thread(() -> {
            System.out.println("  B 开始（要 400ms）");
            sleepQuietly(400);
            System.out.println("  B 结束");
        }, "B");
        long start2 = System.nanoTime();
        a2.start();
        a2.join();                     // ← 不等完 A，B 根本不会启动
        b2.start();
        b2.join();
        long elapsed2 = (System.nanoTime() - start2) / 1_000_000;
        System.out.println("  A 结束之后 B 才开始 —— 严格串行");
        System.out.println("  总耗时约 " + round100(elapsed2) + " ms，差不多是 900 + 400");

        System.out.println("  注意：join() 只是「等」，不是「排队」。线程一旦 start 就已经在跑了");
    }

    // ── 8. 生命周期 ────────────────────────────────────────────────

    /** NEW → RUNNABLE → TIMED_WAITING → TERMINATED，用 getState() 实时看。 */
    private static void lifecycleStates() throws InterruptedException {
        System.out.println("Thread.State 一共六个值：");
        for (Thread.State state : Thread.State.values()) {
            System.out.println("  " + state);
        }
        System.out.println("没有 RUNNING —— 真正在跑的时候，状态也是 RUNNABLE");
        System.out.println();

        // 先原地打转，主线程才能稳稳地看到 RUNNABLE；主线程说「走」它才去睡
        AtomicInteger gate = new AtomicInteger(0);
        Thread t = new Thread(() -> {
            while (gate.get() == 0) {
                Thread.onSpinWait();
            }
            sleepQuietly(600);          // 进入 TIMED_WAITING
        }, "lifecycle");

        System.out.println("start 之前：" + t.getState());
        t.start();

        Thread.State last = Thread.State.NEW;
        boolean released = false;
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            Thread.State now = t.getState();
            if (now != last) {          // 只在变化时打印，免得刷屏
                System.out.println("状态变成　：" + now);
                last = now;
            }
            if (now == Thread.State.RUNNABLE && !released) {
                released = true;
                gate.set(1);            // 放它去 sleep
            }
            if (now == Thread.State.TERMINATED) {
                break;
            }
            Thread.sleep(1);
        }
        System.out.println("结束之后　：" + t.getState());
        System.out.println("BLOCKED 和 WAITING 要等锁和线程通信，后面几课再演示");
    }

    // ── 9. 中断 ────────────────────────────────────────────────────

    /** interrupt() 只置一个标志位，线程停不停由它自己决定。 */
    private static void interruptIsJustASignal() throws InterruptedException {
        AtomicInteger count = new AtomicInteger();
        Thread t = new Thread(() -> {
            // 自己定一个终点：跑到 1.2 秒就停。全程没看过中断标志
            long deadline = System.nanoTime() + 1_200_000_000L;
            while (System.nanoTime() < deadline) {
                count.incrementAndGet();
            }
            System.out.println("  计数线程：我自己跑满了 1.2 秒才停，全程没人理中断");
        }, "counter");

        t.start();
        sleepQuietly(300);
        System.out.println("  主线程：调 interrupt() 之前，它活着吗 = " + t.isAlive());
        t.interrupt();
        System.out.println("  主线程：interrupt() 调完了，它立刻返回 —— 这个方法不阻塞");
        System.out.println("  主线程：中断标志已经置上了 = " + t.isInterrupted());
        System.out.println("  主线程：可它还在跑，isAlive() = " + t.isAlive() + " —— 信号收到了，但没人理");
        t.join();
        System.out.println("  主线程：join() 之后才是真的结束，isAlive() = " + t.isAlive());
        System.out.println("  主线程：它数了很多下（具体数字每次都不同，所以不打印）");
    }

    // ── 10. 正确的停止方式 ─────────────────────────────────────────

    /** 正确做法一：循环里检查中断标志，主动退出。 */
    private static void stopByCheckingFlag() throws InterruptedException {
        Thread t = new Thread(() -> {
            long n = 0;
            // 循环条件里就检查中断标志：收到信号，循环自然结束
            while (!Thread.currentThread().isInterrupted()) {
                n++;
            }
            System.out.println("  检查标志的线程：看到中断标志了，我自己退出"
                    + "（数了多少下每次都不同，所以不打印）");
        }, "checks-flag");

        t.start();
        sleepQuietly(300);
        t.interrupt();
        t.join();
        System.out.println("  主线程：join 之后 isAlive() = " + t.isAlive()
                + " —— 它是自己退出的，没人强杀它");
    }

    /** 正确做法二：sleep / wait 被中断会抛 InterruptedException，在 catch 里收场。 */
    private static void stopByCatchingInterruptedException() throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(10_000);
                System.out.println("  睡满了（不该出现这行）");
            } catch (InterruptedException e) {
                System.out.println("  睡觉的线程：被 InterruptedException 吵醒了");
                // 关键细节：抛异常的同时，中断标志已经被 JVM 清掉了
                System.out.println("  睡觉的线程：catch 里 isInterrupted() = "
                        + Thread.currentThread().isInterrupted() + " —— 标志被清掉了");
            }
            System.out.println("  睡觉的线程：我退出");
        }, "sleeper-interrupted");

        t.start();
        sleepQuietly(300);
        t.interrupt();
        t.join();
        System.out.println("  主线程：它被吵醒之后就结束了，isAlive() = " + t.isAlive());
    }

    // ── 11. interrupted() vs isInterrupted() ───────────────────────

    /** 一个读并清除，一个只读。 */
    private static void interruptedVersusIsInterrupted() throws InterruptedException {
        Thread t = new Thread(() -> {
            Thread me = Thread.currentThread();
            System.out.println("  刚进来：isInterrupted() = " + me.isInterrupted());
            me.interrupt();            // 给自己发一个信号
            System.out.println("  interrupt() 之后：isInterrupted() = " + me.isInterrupted());
            System.out.println("  Thread.interrupted() 第一次 = " + Thread.interrupted()
                    + "（读到了，顺便把标志清掉）");
            System.out.println("  Thread.interrupted() 第二次 = " + Thread.interrupted()
                    + "（已经清掉了）");
            System.out.println("  isInterrupted() = " + me.isInterrupted() + "（它只读，不清）");
        }, "flag-probe");
        t.start();
        t.join();
        System.out.println("  一句话：interrupted() 会清除标志，isInterrupted() 不会");
    }

    // ── 12. 不推荐的用法 ───────────────────────────────────────────

    /** catch 里什么都不做，等于把中断信号吞掉。 */
    private static void ignoringInterruptedException() throws InterruptedException {
        Thread t = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    // 反面教材：什么都不做。信号被吞掉，循环接着睡下一个 10 秒
                }
            }
        }, "swallows-interrupt");
        t.setDaemon(true);             // 它是守护线程，JVM 退出时会被直接掐掉，不会拖住程序

        t.start();
        sleepQuietly(200);
        t.interrupt();
        sleepQuietly(200);
        System.out.println("  被 interrupt() 之后它还在睡：isAlive() = " + t.isAlive()
                + "（catch 里什么都没做，信号被吞了）");
        System.out.println("  正确做法：要么在 catch 里退出，要么 Thread.currentThread().interrupt()");
        System.out.println("  把标志放回去，交给上层处理");
        System.out.println("  另外 stop() 已经废弃到被移除，在 JDK 25 上调用会抛 UnsupportedOperationException");
        System.out.println("  （真实结果见 StopIsGoneDemo，本课不在这里演示）");
    }

    // ── 13. 守护线程（必须是最后一节） ─────────────────────────────

    /**
     * 用户线程跑完，JVM 立刻退出，守护线程被直接掐掉。
     *
     * <p>心跳间隔故意取 1500ms：用户线程数到第 2 次心跳就结束，
     * 那之后 JVM 有整整 1500ms 的时间退出，守护线程的第 3 次心跳
     * 不可能冒出来。间隔要是取 100ms，偶尔就会多打一行——这是本课
     * 最容易翻车的地方，详见文档里「关于输出稳定性」那段。
     */
    private static void daemonThreadGetsKilled() throws InterruptedException {
        AtomicInteger beats = new AtomicInteger();   // 已经打完的心跳次数
        Thread daemon = new Thread(() -> {
            while (true) {
                sleepQuietly(1500);
                System.out.println("  守护线程：第 " + (beats.get() + 1) + " 次心跳");
                beats.incrementAndGet();             // 先打印再计数，顺序才是确定的
            }
        }, "daemon-heartbeat");

        System.out.println("  刚创建：isDaemon() = " + daemon.isDaemon() + "（默认就是用户线程）");
        daemon.setDaemon(true);
        System.out.println("  setDaemon(true) 之后：isDaemon() = " + daemon.isDaemon());

        Thread user = new Thread(() -> {
            while (beats.get() < 2) {                // 数到第 2 次心跳
                sleepQuietly(10);                    // 轮询用 sleep，别空转烧 CPU
            }
            System.out.println("  用户线程：数到第 2 次心跳了，我是用户线程，我这就结束");
        }, "user-worker");

        daemon.start();
        user.start();
        user.join();
        System.out.println("  主线程：用户线程都结束了，只剩守护线程");
        System.out.println("  主线程：main 一返回 JVM 就退出，守护线程的第 3 次心跳永远不会出现");
    }

    // ── 辅助 ───────────────────────────────────────────────────────

    /** 方式一：继承 Thread，把任务写在 run() 里。 */
    static class NamedTask extends Thread {

        NamedTask(String name) {
            super(name);
        }

        @Override
        public void run() {
            System.out.println("  继承 Thread：run() 跑在 " + Thread.currentThread().getName() + " 上");
        }
    }

    /** 方式二：实现 Runnable，任务本身不是线程。 */
    static class PrintTask implements Runnable {

        private final String tag;

        PrintTask(String tag) {
            this.tag = tag;
        }

        @Override
        public void run() {
            System.out.println("  实现 Runnable：" + tag + " 跑在 "
                    + Thread.currentThread().getName() + " 上");
        }
    }

    /** 四舍五入到 100ms。毫秒数每次都会抖，抹掉零头才能写进文档。 */
    static long round100(long millis) {
        return Math.round(millis / 100.0) * 100;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // 被中断就恢复标志，别吞掉
        }
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
