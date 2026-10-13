package dev.javalearning.concurrency.threads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 12 课的断言。
 *
 * <p>每一条对应 {@code docs/12-多线程基础.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*threads*'}
 *
 * <p>多线程的测试<b>一律不断言调度顺序</b>：要么用 latch / join 把先后钉死，
 * 要么断言「两者重叠」这种与顺序无关的事实。会抖的耗时只用来做宽松比较。
 */
class ThreadsTest {

    // ── 1. 并发 vs 并行 ────────────────────────────────────────────

    @Test
    @DisplayName("两个线程可以在同一段时间里各自推进，这就是并发")
    void twoThreadsCanMakeProgressInTheSameTimeSpan() throws Exception {
        CountDownLatch aStarted = new CountDownLatch(1);
        CountDownLatch bStarted = new CountDownLatch(1);
        AtomicBoolean bRanWhileAWasBusy = new AtomicBoolean(false);

        Thread a = new Thread(() -> {
            aStarted.countDown();
            sleep(400);
        });
        Thread b = new Thread(() -> {
            sleep(150);                                    // A 还在睡的时候我跑
            bRanWhileAWasBusy.set(a.isAlive());
            bStarted.countDown();
        });

        a.start();
        aStarted.await();
        b.start();
        bStarted.await();
        a.join();
        b.join();

        // B 动手的时候 A 还活着 —— 两个任务的时间段是重叠的
        assertThat(bRanWhileAWasBusy).isTrue();
    }

    // ── 2. 主线程 ──────────────────────────────────────────────────

    @Test
    @DisplayName("测试方法自己跑在名为 Test worker 的线程上——它不是 main")
    void testsRunOnTheirOwnThreadNotMain() {
        // 这条断言解释了一个坑：JUnit 是另起一个 Test worker 线程来跑测试方法的，
        // 测试里根本没有 main 线程。所以「main 线程叫 main」只能去子进程里验，
        // 见下面 userThreadsKeepTheJvmAlive 那条。
        assertThat(Thread.currentThread().getName()).isEqualTo("Test worker");
        assertThat(Thread.currentThread().getName()).isNotEqualTo("main");
    }

    @Test
    @DisplayName("setName 只换显示用的标签，换不掉线程本身")
    void setNameOnlyChangesTheLabel() throws Exception {
        Thread t = new Thread(() -> { }, "原名");
        t.setName("新名");
        assertThat(t.getName()).isEqualTo("新名");

        AtomicReference<String> seen = new AtomicReference<>();
        t = new Thread(() -> seen.set(Thread.currentThread().getName()), "另一个名字");
        t.start();
        t.join();
        assertThat(seen.get()).isEqualTo("另一个名字");
    }

    @Test
    @DisplayName("不给名字的线程由 JVM 按创建顺序编号")
    void unnamedThreadsAreNumberedInCreationOrder() {
        // 编号是 JVM 全局计数器，所以只断言「两个相邻创建的线程编号相差 1」，
        // 不断言具体数字——别的测试也可能创建过线程。
        Thread first = new Thread(() -> { });
        Thread second = new Thread(() -> { });
        int n1 = numberSuffix(first.getName());
        int n2 = numberSuffix(second.getName());
        assertThat(first.getName()).startsWith("Thread-");
        assertThat(n2).isEqualTo(n1 + 1);
    }

    // ── 3. 创建线程的方式 ──────────────────────────────────────────

    @Test
    @DisplayName("继承 Thread 重写 run，跑在新线程上")
    void extendingThreadRunsOnTheNewThread() throws Exception {
        AtomicReference<String> where = new AtomicReference<>();
        Thread t = new Thread(() -> where.set(Thread.currentThread().getName()), "extends-thread");
        t.start();
        t.join();
        assertThat(where.get()).isEqualTo("extends-thread");
        // 顺带确认样板里的那个类确实是 Thread 的子类
        assertThat(new ThreadsDemo.NamedTask("x")).isInstanceOf(Thread.class);
    }

    @Test
    @DisplayName("一个 Runnable 对象可以被多个线程执行——任务和线程是分开的")
    void oneRunnableCanBeRunByManyThreads() throws Exception {
        List<String> names = new CopyOnWriteArrayList<>();
        Runnable task = () -> names.add(Thread.currentThread().getName());

        Thread first = new Thread(task, "worker-1");
        Thread second = new Thread(task, "worker-2");
        first.start();
        second.start();
        first.join();
        second.join();

        // 同一个 task 对象，被两个不同的线程各自跑了一遍
        assertThat(names).containsExactlyInAnyOrder("worker-1", "worker-2");
        // 样板里那个实现 Runnable 的类，确实就是个普通类，不是线程
        assertThat(new ThreadsDemo.PrintTask("同一个任务对象")).isNotInstanceOf(Thread.class);
    }

    @Test
    @DisplayName("Runnable 是函数式接口，Lambda 就能当任务")
    void lambdaIsAValidRunnable() throws Exception {
        Runnable task = () -> { };
        assertThat(task).isInstanceOf(Runnable.class);

        AtomicReference<String> where = new AtomicReference<>();
        Thread t = new Thread(() -> where.set(Thread.currentThread().getName()), "lambda-worker");
        t.start();
        t.join();
        assertThat(where.get()).isEqualTo("lambda-worker");
    }

    @Test
    @DisplayName("Callable + FutureTask 能拿到返回值")
    void callableReturnsAValue() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> "结果");
        new Thread(task, "callable-worker").start();
        assertThat(task.get()).isEqualTo("结果");
        assertThat(task.isDone()).isTrue();
    }

    @Test
    @DisplayName("Callable 抛的异常被包进 ExecutionException，原因在 getCause 里")
    void callableExceptionsArriveWrapped() {
        FutureTask<Integer> task = new FutureTask<>(() -> {
            throw new IllegalStateException("故意抛的");
        });
        new Thread(task, "callable-throws").start();

        assertThatThrownBy(task::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("故意抛的");
    }

    // ── 4. start() 和 run() ───────────────────────────────────────

    @Test
    @DisplayName("直接调 run() 只是普通方法调用，跑在当前线程上")
    void callingRunDirectlyStaysOnTheCallerThread() {
        AtomicReference<String> where = new AtomicReference<>();
        Thread t = new Thread(() -> where.set(Thread.currentThread().getName()), "worker");
        t.run();                                   // ← 没有新线程
        // 跑在「调 run() 的那个线程」上。这里是 Test worker，
        // 在 ThreadsDemo 里就是 main —— 反正都不是 worker。
        assertThat(where.get()).isEqualTo(Thread.currentThread().getName());
        assertThat(where.get()).isNotEqualTo("worker");
        assertThat(t.getState()).isEqualTo(Thread.State.NEW);   // 压根没启动过
    }

    @Test
    @DisplayName("start() 才开新线程，同一个 run() 就跑在别的线程上")
    void startCreatesANewThread() throws Exception {
        AtomicReference<String> where = new AtomicReference<>();
        Thread t = new Thread(() -> where.set(Thread.currentThread().getName()), "worker");
        t.start();
        t.join();
        assertThat(where.get()).isEqualTo("worker");
        assertThat(t.getState()).isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    @DisplayName("同一个线程 start 两次会抛 IllegalThreadStateException")
    void startingTwiceThrows() throws Exception {
        Thread t = new Thread(() -> { });
        t.start();
        t.join();
        assertThatThrownBy(t::start).isInstanceOf(IllegalThreadStateException.class);
    }

    // ── 5. 异步执行 ────────────────────────────────────────────────

    @Test
    @DisplayName("主线程不会等子线程：子线程还卡着，主线程已经往下走了")
    void mainDoesNotWaitForTheChildThread() throws Exception {
        CountDownLatch childStarted = new CountDownLatch(1);
        CountDownLatch letChildFinish = new CountDownLatch(1);
        AtomicBoolean childFinished = new AtomicBoolean(false);

        Thread t = new Thread(() -> {
            childStarted.countDown();
            await(letChildFinish);
            childFinished.set(true);
        }, "child");
        t.start();
        childStarted.await();

        // 子线程明明还没干完，主线程照样读得到、走得下去
        assertThat(childFinished).isFalse();
        assertThat(t.isAlive()).isTrue();

        letChildFinish.countDown();
        t.join();
        assertThat(childFinished).isTrue();
    }

    // ── 6. join ────────────────────────────────────────────────────

    @Test
    @DisplayName("join 返回时，被等的线程一定已经结束")
    void joinReturnsOnlyAfterTheOtherThreadFinishes() throws Exception {
        AtomicBoolean childDone = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            sleep(200);
            childDone.set(true);
        }, "child");
        t.start();
        t.join();

        assertThat(childDone).isTrue();
        assertThat(t.isAlive()).isFalse();
        assertThat(t.getState()).isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    @DisplayName("在 A 线程里 join B，被阻塞的是 A，不是 main")
    void joinBlocksTheThreadThatCallsIt() throws Exception {
        CountDownLatch bDone = new CountDownLatch(1);
        AtomicReference<Long> bCountWhenAResumes = new AtomicReference<>();

        Thread b = new Thread(() -> {
            sleep(200);
            bDone.countDown();
        }, "B");
        Thread a = new Thread(() -> {
            joinQuietly(b);
            // A 恢复时 B 已经跑完，所以这个计数器必然是 0
            bCountWhenAResumes.set(bDone.getCount());
        }, "A");

        b.start();
        a.start();
        a.join();

        assertThat(bCountWhenAResumes.get()).isZero();
    }

    @Test
    @DisplayName("join 一个还没 start 的线程会立刻返回，不报错")
    void joiningANewThreadReturnsImmediately() throws Exception {
        Thread t = new Thread(() -> { });
        long start = System.nanoTime();
        t.join();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMillis).isLessThan(500);
        assertThat(t.getState()).isEqualTo(Thread.State.NEW);
    }

    // ── 7. 重叠 vs 串行 ────────────────────────────────────────────

    @Test
    @DisplayName("先都 start 再依次 join：两个线程重叠跑，总耗时接近较长的那个")
    void startingBothFirstMakesThemOverlap() throws Exception {
        CountDownLatch aDone = new CountDownLatch(1);
        AtomicBoolean bSawABusy = new AtomicBoolean(false);

        Thread a = new Thread(() -> {
            sleep(900);
            aDone.countDown();
        }, "A");
        Thread b = new Thread(() -> {
            sleep(300);
            bSawABusy.set(aDone.getCount() == 1);   // A 那 900ms 还没到点
        }, "B");

        long start = System.nanoTime();
        a.start();
        b.start();
        a.join();
        b.join();
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        assertThat(bSawABusy).isTrue();          // B 动手时 A 还在忙
        assertThat(elapsed).isLessThan(1150);    // 串行的话至少要 1200
    }

    @Test
    @DisplayName("启动一个 join 一个：严格串行，总耗时是两者相加")
    void joiningBeforeStartingTheNextMakesItSequential() throws Exception {
        CountDownLatch aDone = new CountDownLatch(1);
        AtomicBoolean bSawABusy = new AtomicBoolean(false);

        Thread a = new Thread(() -> {
            sleep(900);
            aDone.countDown();
        }, "A");
        Thread b = new Thread(() -> {
            sleep(300);
            bSawABusy.set(aDone.getCount() == 1);
        }, "B");

        long start = System.nanoTime();
        a.start();
        a.join();                                 // ← B 等 A 结束才出生
        b.start();
        b.join();
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        assertThat(bSawABusy).isFalse();          // B 动手时 A 早就结束了
        assertThat(elapsed).isGreaterThanOrEqualTo(1100);
    }

    // ── 8. 生命周期 ────────────────────────────────────────────────

    @Test
    @DisplayName("Thread.State 只有六个值，没有 RUNNING")
    void thereIsNoRunningState() {
        assertThat(Thread.State.values()).hasSize(6);
        assertThat(Thread.State.values()).containsExactly(
                Thread.State.NEW,
                Thread.State.RUNNABLE,
                Thread.State.BLOCKED,
                Thread.State.WAITING,
                Thread.State.TIMED_WAITING,
                Thread.State.TERMINATED);
        assertThat(Thread.State.values()).noneMatch(s -> s.name().equals("RUNNING"));
    }

    @Test
    @DisplayName("start 之前是 NEW，start 之后是 RUNNABLE")
    void newThenRunnable() throws Exception {
        CountDownLatch running = new CountDownLatch(1);
        AtomicBoolean release = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            running.countDown();
            while (!release.get()) {
                Thread.onSpinWait();              // 原地打转，主线程随时能看到 RUNNABLE
            }
        }, "lifecycle");

        assertThat(t.getState()).isEqualTo(Thread.State.NEW);
        t.start();
        running.await();

        // 真正在跑的时候，状态也是 RUNNABLE —— 没有单独的 RUNNING
        assertThat(t.getState()).isEqualTo(Thread.State.RUNNABLE);

        release.set(true);
        t.join();
    }

    @Test
    @DisplayName("sleep 中是 TIMED_WAITING，结束后是 TERMINATED")
    void timedWaitingThenTerminated() throws Exception {
        Thread t = new Thread(() -> sleep(2000), "sleeper");
        t.start();

        awaitUntil(() -> t.getState() == Thread.State.TIMED_WAITING, 2000);
        assertThat(t.getState()).isEqualTo(Thread.State.TIMED_WAITING);

        t.interrupt();                            // 别真等两秒
        t.join();
        assertThat(t.getState()).isEqualTo(Thread.State.TERMINATED);
    }

    // ── 9. 中断只是一个信号 ────────────────────────────────────────

    @Test
    @DisplayName("没人理中断标志时，interrupt() 之后线程照跑不误")
    void interruptDoesNotStopAThreadThatIgnoresIt() throws Exception {
        CountDownLatch running = new CountDownLatch(1);
        AtomicBoolean release = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            running.countDown();
            while (!release.get()) {              // 循环里从不看中断标志
                Thread.onSpinWait();
            }
        }, "ignores-interrupt");

        t.start();
        running.await();
        t.interrupt();

        Thread.sleep(100);                        // 给它机会「停下来」，它不会
        assertThat(t.isInterrupted()).isTrue();   // 标志确实置上了
        assertThat(t.isAlive()).isTrue();         // 但线程还活着

        release.set(true);
        t.join();
    }

    @Test
    @DisplayName("interrupt() 是非阻塞的，调完立刻返回")
    void interruptReturnsImmediately() throws Exception {
        CountDownLatch running = new CountDownLatch(1);
        AtomicBoolean release = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            running.countDown();
            while (!release.get()) {
                Thread.onSpinWait();
            }
        });
        t.start();
        running.await();

        long start = System.nanoTime();
        t.interrupt();
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsed).isLessThan(200);
        assertThat(t.isAlive()).isTrue();          // 所以中断后立刻看 isAlive 可能还是 true

        release.set(true);
        t.join();
        assertThat(t.isAlive()).isFalse();         // 配合 join 才能确认结束
    }

    @Test
    @DisplayName("循环里检查标志，线程自己就能干净地退出")
    void checkingTheFlagStopsTheThread() throws Exception {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.onSpinWait();
            }
        }, "checks-flag");
        t.start();
        awaitUntil(t::isAlive, 2000);

        t.interrupt();
        t.join(2000);

        assertThat(t.isAlive()).isFalse();
        assertThat(t.getState()).isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    @DisplayName("sleep 被中断会抛 InterruptedException，同时中断标志被清掉")
    void interruptedSleepClearsTheFlag() throws Exception {
        AtomicReference<Boolean> flagInCatch = new AtomicReference<>();
        CountDownLatch sleeping = new CountDownLatch(1);

        Thread t = new Thread(() -> {
            try {
                sleeping.countDown();
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                // 抛异常的同时，JVM 已经把标志清掉了
                flagInCatch.set(Thread.currentThread().isInterrupted());
            }
        }, "sleeper");
        t.start();
        sleeping.await();
        Thread.sleep(50);                          // 确保它真的进到 sleep 里
        t.interrupt();
        t.join(2000);

        assertThat(t.isAlive()).isFalse();
        assertThat(flagInCatch.get()).isFalse();
    }

    // ── 10. interrupted() vs isInterrupted() ───────────────────────

    @Test
    @DisplayName("interrupted() 读了就清，isInterrupted() 只读不清")
    void interruptedClearsButIsInterruptedDoesNot() throws Exception {
        AtomicReference<Boolean> first = new AtomicReference<>();
        AtomicReference<Boolean> second = new AtomicReference<>();
        AtomicReference<Boolean> after = new AtomicReference<>();

        Thread t = new Thread(() -> {
            Thread.currentThread().interrupt();
            first.set(Thread.interrupted());                     // 读到 true，并清掉
            second.set(Thread.interrupted());                    // 已经清掉了 → false
            Thread.currentThread().interrupt();                  // 再来一次
            Thread.interrupted();                                // 读掉
            after.set(Thread.currentThread().isInterrupted());   // isInterrupted 不清，但上面已清
        }, "flag-probe");
        t.start();
        t.join();

        assertThat(first.get()).isTrue();
        assertThat(second.get()).isFalse();
        assertThat(after.get()).isFalse();
    }

    @Test
    @DisplayName("isInterrupted() 反复读都还是 true，它不清标志")
    void isInterruptedKeepsReportingTrue() throws Exception {
        AtomicReference<Boolean> first = new AtomicReference<>();
        AtomicReference<Boolean> second = new AtomicReference<>();
        Thread t = new Thread(() -> {
            Thread.currentThread().interrupt();
            first.set(Thread.currentThread().isInterrupted());
            second.set(Thread.currentThread().isInterrupted());
        });
        t.start();
        t.join();
        assertThat(first.get()).isTrue();
        assertThat(second.get()).isTrue();
    }

    // ── 11. 用户线程 vs 守护线程 ───────────────────────────────────

    @Test
    @DisplayName("新建的线程默认是用户线程")
    void threadsAreUserThreadsByDefault() {
        assertThat(new Thread(() -> { }).isDaemon()).isFalse();
        assertThat(Thread.currentThread().isDaemon()).isFalse();
    }

    @Test
    @DisplayName("setDaemon 必须在线程「还活着」之前调，活着时调会抛异常")
    void setDaemonMustHappenBeforeStart() throws Exception {
        Thread t = new Thread(() -> { });
        t.setDaemon(true);
        assertThat(t.isDaemon()).isTrue();
        t.start();
        t.join();

        // 这个坑比想象的细：拦你的条件是 isAlive()，不是「有没有 start 过」。
        // 所以线程还活着时调 → 抛异常；
        Thread spinning = new Thread(() -> {
            long deadline = System.nanoTime() + 2_000_000_000L;
            while (System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
        });
        spinning.setDaemon(true);
        spinning.start();
        awaitUntil(spinning::isAlive, 2000);
        assertThatThrownBy(() -> spinning.setDaemon(false))
                .isInstanceOf(IllegalThreadStateException.class);
        spinning.interrupt();
        spinning.join();

        // 而线程已经结束后再调，它一声不吭地改了标志 —— 但那时线程早死了，毫无意义
        Thread dead = new Thread(() -> { });
        dead.start();
        dead.join();
        assertThat(dead.getState()).isEqualTo(Thread.State.TERMINATED);
        dead.setDaemon(true);                     // 不抛
        assertThat(dead.isDaemon()).isTrue();     // 标志确实改了，可是没有任何用
    }

    @Test
    @DisplayName("守护线程不拖住 JVM：主线程一结束，进程就退出")
    void daemonThreadsDoNotKeepTheJvmAlive() throws Exception {
        Process process = startDemo("dev.javalearning.concurrency.threads.DaemonExitDemo");
        boolean exited = process.waitFor(15, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
        }
        String output = readOutput(process);

        // 守护线程那个死循环没有拖住进程 —— 它跑得再久 JVM 也照样退出
        assertThat(exited).as("守护线程不该拖住 JVM").isTrue();
        // 但它确实跑过（说明不是没启动）
        assertThat(output).contains("[守护线程] 第 1 次心跳");
        assertThat(output).contains("[main] 我结束了");
        // 而且心跳没有无限打下去：用户线程一结束，守护线程就被掐了
        assertThat(output).doesNotContain("[守护线程] 第 3 次心跳");
    }

    @Test
    @DisplayName("用户线程会拖住 JVM：main 返回之后进程还活着，直到子线程跑完")
    void userThreadsKeepTheJvmAlive() throws Exception {
        Process process = startDemo("dev.javalearning.concurrency.threads.MainExitDemo");
        boolean exited = process.waitFor(15, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
        }
        String output = readOutput(process);

        assertThat(exited).isTrue();
        // 「执行 main 的线程叫 main」这句得在真的 main 里才验得到
        assertThat(output).contains("[main] 执行 main 的线程叫：main");
        // 这两行是 main 返回之后才打印的，说明 JVM 一直在等这个用户线程
        assertThat(output).contains("[main] main 方法到此结束");
        assertThat(output).contains("[子线程] 我结束了，JVM 现在才退出");
        // 而且顺序是：main 先说完，子线程才收尾
        assertThat(output.indexOf("[main] main 方法到此结束"))
                .isLessThan(output.indexOf("[子线程] 我结束了，JVM 现在才退出"));
    }

    // ── 12. 不推荐的用法 ───────────────────────────────────────────

    @Test
    @SuppressWarnings("removal")
    @DisplayName("stop() 已废弃到被移除，JDK 25 上调它会抛 UnsupportedOperationException")
    void stopThrowsInsteadOfStopping() throws Exception {
        AtomicBoolean stillRunning = new AtomicBoolean(true);
        Thread t = new Thread(() -> {
            while (stillRunning.get()) {
                Thread.onSpinWait();
            }
        }, "victim");
        t.setDaemon(true);
        t.start();
        awaitUntil(t::isAlive, 2000);

        assertThatThrownBy(t::stop).isInstanceOf(UnsupportedOperationException.class);
        // 关键：它没有被停掉。JVM 里已经没有「安全地强杀一个线程」这种操作了
        assertThat(stillRunning.get()).isTrue();
        assertThat(t.isAlive()).isTrue();

        stillRunning.set(false);
        t.join();
    }

    @Test
    @DisplayName("catch 里什么都不做，中断信号就被吞了，线程接着跑")
    void swallowingInterruptedExceptionKeepsTheThreadGoing() throws Exception {
        CountDownLatch inSleep = new CountDownLatch(1);
        AtomicInteger rounds = new AtomicInteger();

        Thread t = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    inSleep.countDown();
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    // 反面教材：什么都不做，连标志都不恢复
                }
                rounds.incrementAndGet();
            }
        }, "swallows-interrupt");
        t.setDaemon(true);
        t.start();
        inSleep.await();

        t.interrupt();
        awaitUntil(() -> rounds.get() >= 1, 2000);

        // 正确做法下线程这时候已经退出了；吞掉信号的结果是它换一轮接着睡
        assertThat(rounds.get()).isGreaterThanOrEqualTo(1);
        assertThat(t.isAlive()).isTrue();
    }

    // ── 辅助 ───────────────────────────────────────────────────────

    private static Process startDemo(String mainClass) throws IOException {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        return new ProcessBuilder(java.toString(), "-Dfile.encoding=UTF-8",
                "-cp", System.getProperty("java.class.path"), mainClass)
                .redirectErrorStream(true)
                .start();
    }

    private static String readOutput(Process process) throws IOException {
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static int numberSuffix(String name) {
        return Integer.parseInt(name.substring(name.indexOf('-') + 1));
    }

    private static void awaitUntil(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(5);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
