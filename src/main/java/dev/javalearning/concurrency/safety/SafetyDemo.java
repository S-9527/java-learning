package dev.javalearning.concurrency.safety;

/**
 * 第 13 课的示例代码：线程安全与同步。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/safety/SafetyDemo.java}
 * 每一节对应 {@code docs/13-线程安全与同步.md} 里的一节。
 *
 * <p><b>关于输出为什么是稳定的</b>：本课演示的是竞态条件，而竞态的输出天然不稳定。
 * 如果直接打印「跑 200 次，有 7 次不是 10000」，这个 7 每次跑都不一样，文档里的
 * 输出就没法逐字核对。所以这里分两种模式：
 *
 * <ul>
 *   <li>默认模式：只打印<b>定性结论</b>（「至少出现过一次错误 = 是」），
 *       以及那些本来就确定的结果（加了锁之后恒等于 10000）。这样输出逐字稳定。</li>
 *   <li>{@code sample} 模式：把原始数字（9987、9965 这种）也打出来，
 *       每次跑都不一样，用来让读者亲眼看见竞态。文档里会标明它是「某一次真实运行」。</li>
 * </ul>
 *
 * <p>两种模式跑的是同一批代码，没有为了「好看」而挑结果。
 */
public class SafetyDemo {

    /** 10 个线程，每个加 1000 次，期望 10000。 */
    private static final int THREADS = 10;
    private static final int PER_THREAD = 1000;
    private static final int EXPECTED = THREADS * PER_THREAD;

    /** 默认模式统计用 200 次：次数够多，「至少错过一次」这个结论才稳得住。 */
    private static final int TRIALS = 200;

    /** sample 模式打印用 20 次：够看出错，又不至于刷屏。 */
    private static final int SAMPLE_TRIALS = 20;

    /** 第 1 节冷启动时测到的裸 count++ 结果，第 9 节要拿出来跟别的写法对比。 */
    private static Stats coldBareStats;

    public static void main(String[] args) throws Exception {
        boolean sample = args.length > 0 && args[0].equals("sample");
        if (sample) {
            System.out.println("（sample 模式：下面是某一次真实运行的原始数字，每次跑都不一样）");
        }

        section("1. 不安全的计数器：10 个线程各加 1000 次");
        unsafeCounter(sample);

        section("2. 为什么它有时又不翻车：线程根本没抢起来");
        whyItSometimesWorks(sample);

        section("3. 为什么 count++ 会错：丢失更新 lost update");
        lostUpdate();

        section("4. synchronized：让那三步变成不可打断的一段");
        synchronizedCounter(sample);

        section("5. 坑一：锁住的是「对象」，不是「代码」");
        wrongLock(sample);

        section("6. 坑二：synchronized 是可重入的 reentrant");
        reentrant();

        section("7. 锁对象是谁：Thread.holdsLock 当场问出来");
        lockIdentity();

        section("8. ReentrantLock：unlock 必须写在 finally 里");
        lockLeak();

        section("9. 三大特性之一：原子性 atomicity");
        atomicity(sample);

        section("10. 三大特性之二：可见性 visibility");
        visibility();

        section("11. 三大特性之三：有序性 ordering");
        ordering(sample);

        section("12. 实战：100 张票，4 个窗口卖");
        ticketOffice(sample);
    }

    // ── 1. 不安全的计数器 ──────────────────────────────────────────

    private static void unsafeCounter(boolean sample) throws InterruptedException {
        System.out.println("期望 " + EXPECTED + "（" + THREADS + " 个线程 × " + PER_THREAD + " 次）");

        int trials = sample ? SAMPLE_TRIALS : TRIALS;
        Stats stats = measure(trials, counter -> {
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementUnsafe();
                }
            });
            return counter.unsafe();
        });
        if (!sample) {
            coldBareStats = stats;      // 第 9 节还要用
        }

        System.out.println("跑了 " + trials + " 次，至少出现过一次不是 " + EXPECTED + " 的情况："
                + yesNo(stats.wrong() > 0));
        System.out.println("但它不是每次都错：这 " + trials + " 次里绝大多数都跑出了正确的 "
                + EXPECTED + "。");
        System.out.println("跑对的那几次不代表代码是对的，只是那几次线程没抢起来（见下一节）。");

        if (sample) {
            System.out.println();
            System.out.println("（某一次真实运行：不是 " + EXPECTED + " 的有 " + stats.wrong()
                    + " 次，最小值 " + stats.min() + "，最大值 " + stats.max() + "）");
            System.out.println("（最大值就是 " + EXPECTED + "，说明有一大半次是「碰巧对」的）");
        } else {
            System.out.println();
            System.out.println("想看具体的错误数字（9987、9965 这种）？加个 sample 参数：");
            System.out.println("  java src/main/java/dev/javalearning/concurrency/safety/SafetyDemo.java sample");
        }
    }

    // ── 2. 为什么有时不翻车 ────────────────────────────────────────

    private static void whyItSometimesWorks(boolean sample) throws InterruptedException {
        System.out.println("同一段不安全的代码，换一种跑法，结果完全不同：");
        System.out.println();

        // 对照组：线程一个接一个跑，压根没机会重叠
        int sequentialWrong = 0;
        for (int round = 0; round < TRIALS; round++) {
            Counter counter = new Counter();
            Threads.runSequentially(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementUnsafe();
                }
            });
            if (counter.unsafe() != EXPECTED) {
                sequentialWrong++;
            }
        }

        // 实验组：同时开始，并且把「读」和「写」之间的窗口撑开
        int togetherWrong = 0;
        for (int round = 0; round < TRIALS; round++) {
            Counter counter = new Counter();
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementWithWideWindow(i);
                }
            });
            if (counter.wideWindow() != EXPECTED) {
                togetherWrong++;
            }
        }

        System.out.println("  一个接一个跑（线程不重叠）：跑 " + TRIALS + " 次，出现过错误吗 = "
                + yesNo(sequentialWrong > 0));
        System.out.println("  同时开始 + 撑开窗口      ：跑 " + TRIALS + " 次，出现过错误吗 = "
                + yesNo(togetherWrong > 0));
        System.out.println();
        System.out.println("同样是那段会丢更新的代码，线程只要不重叠，跑 " + TRIALS + " 次一次都不错。");
        System.out.println("竞态不是「代码有毒」，是「代码 + 时机」凑到一起才发作。");
        System.out.println();
        System.out.println("那为什么第 1 节里裸 count++ 只错了少数几次？两个原因：");
        System.out.println("  1. 一个线程几十微秒就跑完了，而 10 个线程光是启动就错开了几百微秒，");
        System.out.println("     先启动的早收工了，后启动的还没开始，它们压根没碰上。");
        System.out.println("  2. JIT 编译之后那个自增循环快到极致，线程重叠的时间窗更短了。");
        System.out.println("换句话说：你测试环境里「跑一百次都对」，可能只是机器太快、没抢起来。");

        if (sample) {
            System.out.println();
            System.out.println("（某一次真实运行：不重叠时错 " + sequentialWrong + " 次，");
            System.out.println("  同时开始 + 撑开窗口时错 " + togetherWrong + " 次）");
        }
    }

    // ── 3. 丢失更新 ────────────────────────────────────────────────

    private static void lostUpdate() throws InterruptedException {
        System.out.println("把两个线程的步子钉死成：A 读 → B 读 → B 写 → A 写");
        System.out.println("（这个顺序是线程调度允许的，所以它是真 bug，不是造出来的假象）");
        System.out.println();

        Interleaving.Result plain = Interleaving.run(new Interleaving.PlainCell());
        System.out.println("【" + plain.cellName() + "】");
        System.out.println("  A 读到 " + plain.aRead());
        System.out.println("  B 读到 " + plain.bRead());
        System.out.println("  两个线程各加一次，最终 = " + plain.finalValue() + "（期望 2）");

        Interleaving.Result vol = Interleaving.run(new Interleaving.VolatileCell());
        System.out.println("【" + vol.cellName() + "】");
        System.out.println("  A 读到 " + vol.aRead());
        System.out.println("  B 读到 " + vol.bRead());
        System.out.println("  两个线程各加一次，最终 = " + vol.finalValue() + "（期望 2）");
        System.out.println();
        System.out.println("两个人各读到 0，各自加一得 1，先后写回——加了两次只涨了 1。");
        System.out.println("volatile 保证了可见性，但 count++ 依然是三步，照样丢更新。");
    }

    // ── 4. synchronized ────────────────────────────────────────────

    private static void synchronizedCounter(boolean sample) throws InterruptedException {
        int trials = sample ? SAMPLE_TRIALS : TRIALS;

        System.out.println("方法级 synchronized（锁的是 this）：");
        System.out.println("  " + verdict(trials, counter -> {
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementSynchronized();
                }
            });
            return counter.synchronizedCount();
        }, sample));

        System.out.println("同步代码块 synchronized (lock)（锁的是 lock 那个对象）：");
        System.out.println("  " + verdict(trials, counter -> {
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementWithLockObject();
                }
            });
            return counter.blockCount();
        }, sample));
        System.out.println();
        System.out.println("跑多少遍都是 " + EXPECTED + "。");
    }

    // ── 5. 锁错对象 ────────────────────────────────────────────────

    private static void wrongLock(boolean sample) throws InterruptedException {
        int trials = sample ? SAMPLE_TRIALS : TRIALS;

        System.out.println("错误写法：synchronized (new Object()) —— 每次进方法都 new 一把新锁");
        System.out.println("  " + verdict(trials, counter -> {
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementWithFreshLock();
                }
            });
            return counter.freshLock();
        }, sample));
        System.out.println();

        WrongLock.Result result = WrongLock.run();
        System.out.println("另一个更直接的证据：两个线程各自 synchronized (new Object())");
        System.out.println("  它们能同时待在各自的临界区里吗 = " + result.bothInsideAtOnce());
        System.out.println();
        System.out.println("每次 new 出来的都是不同的对象，也就是不同的锁，互不排斥。");
        System.out.println("锁必须是一个大家共用的对象——存成字段，或者用 类.class。");
    }

    // ── 6. 可重入 ──────────────────────────────────────────────────

    private static void reentrant() {
        Reentrancy.Result result = Reentrancy.run();
        System.out.println("a() 和 b() 都是 synchronized，a() 里又调了 b()：");
        System.out.println("  调用轨迹 = " + result.trace());
        System.out.println("  一共进了同步方法 " + result.calls() + " 次");
        System.out.println();
        System.out.println("b() 里用 Thread.holdsLock(this) 确认了自己确实持有锁：");
        System.out.println("  所以不是「碰巧跑进来的」，是同一个线程合法地又进了一次。");
        System.out.println("如果锁不可重入，a() 调 b() 的那一刻就自己把自己锁死了。");
    }

    // ── 7. 锁对象是谁 ──────────────────────────────────────────────

    private static void lockIdentity() {
        LockIdentity target = new LockIdentity();
        LockIdentity.Holds instance = target.inspectInstanceMethod();
        LockIdentity.Holds staticM = LockIdentity.inspectStaticMethod();
        LockIdentity.Holds thisBlock = LockIdentity.inspectThisBlock(target);
        LockIdentity.Holds classBlock = LockIdentity.inspectClassBlock();
        LockIdentity.Holds otherBlock = LockIdentity.inspectOtherBlock();

        System.out.println("写法                        持有目标对象  持有类对象");
        row("实例同步方法 m()", instance);
        row("静态同步方法 static m()", staticM);
        row("synchronized (this)", thisBlock);
        row("synchronized (Xxx.class)", classBlock);
        row("synchronized (别的对象)", otherBlock);
        System.out.println();
        System.out.println("静态同步方法锁的是「类的 Class 对象」，全局唯一——");
        System.out.println("它保护的范围大得多，会影响这个类的所有实例。");
    }

    private static void row(String label, LockIdentity.Holds holds) {
        System.out.println(pad(label, 28) + pad(String.valueOf(holds.self()), 12)
                + holds.classObject());
    }

    // ── 8. ReentrantLock ───────────────────────────────────────────

    private static void lockLeak() throws InterruptedException {
        LockLeak.Result good = LockLeak.withFinally();
        LockLeak.Result bad = LockLeak.withoutFinally();

        System.out.println("方法里抛了个异常，然后：");
        System.out.println("  写了 finally { unlock() } 的：别的线程还能拿到锁吗 = " + good.otherThreadGotLock());
        System.out.println("  漏了 finally 的              ：别的线程还能拿到锁吗 = " + bad.otherThreadGotLock());
        System.out.println();
        System.out.println("synchronized 出异常 JVM 会自动还锁；Lock 必须自己还。");
        System.out.println("漏一次 unlock，这把锁就永远不还了，其他线程全部卡死 → 死锁。");

        Counter counter = new Counter();
        Threads.runTogether(THREADS, () -> {
            for (int i = 0; i < PER_THREAD; i++) {
                counter.incrementLocked();
            }
        });
        System.out.println();
        System.out.println("ReentrantLock 正常用也能保护计数：" + counter.locked()
                + "（期望 " + EXPECTED + "）");
    }

    // ── 9. 原子性 ──────────────────────────────────────────────────

    private static void atomicity(boolean sample) throws InterruptedException {
        int trials = sample ? SAMPLE_TRIALS : TRIALS;

        System.out.println("原子性 Atomicity：一个或多个操作要么全做完、要么不做，中间不被打断。");
        System.out.println("count++ 是「读—加—写」三步，原子性被破坏，所以会丢更新。");
        System.out.println("手段：synchronized / Lock / 原子类。");
        System.out.println();
        System.out.println("同一件事的五种写法，每种各跑 " + trials + " 次：");

        Stats bare = coldBareStats != null ? coldBareStats : measure(trials, counter -> {
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementUnsafe();
                }
            });
            return counter.unsafe();
        });
        Stats wide = measure(trials, counter -> {
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementWithWideWindow(i);
                }
            });
            return counter.wideWindow();
        });
        Stats volatileWide = measure(trials, counter -> {
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementVolatileWideWindow(i);
                }
            });
            return counter.volatileWideWindow();
        });
        Stats fresh = measure(trials, counter -> {
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementWithFreshLock();
                }
            });
            return counter.freshLock();
        });
        Stats sync = measure(trials, counter -> {
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementSynchronized();
                }
            });
            return counter.synchronizedCount();
        });

        System.out.println(pad("  写法", 32) + "出现过错误吗");
        System.out.println(pad("  裸 count++", 32) + yesNo(bare.wrong() > 0) + "（冷启动时测的，见第 1 节）");
        System.out.println(pad("  撑开窗口的三步写法", 32) + yesNo(wide.wrong() > 0));
        System.out.println(pad("  字段加 volatile + 撑开窗口", 32) + yesNo(volatileWide.wrong() > 0)
                + "   ← volatile 不保证原子性！");
        System.out.println(pad("  synchronized (new Object())", 32) + yesNo(fresh.wrong() > 0));
        System.out.println(pad("  synchronized 方法", 32) + yesNo(sync.wrong() > 0) + "   ← 每次都对");
        System.out.println();
        System.out.println("前四行都是原子性被破坏的写法，最后一行才是修好的。");
        System.out.println("特别注意第三行：光加 volatile 只解决了可见性，count++ 照样丢更新。");
        System.out.println("顺便注意前两行：同一个 bug，窗口一宽，出错率就从「偶尔」跳到「几乎必现」。");

        if (sample) {
            System.out.println();
            System.out.println("（某一次真实运行：裸 " + bare.wrong() + "/" + trials
                    + "，撑开窗口 " + wide.wrong() + "/" + trials
                    + "，volatile " + volatileWide.wrong() + "/" + trials
                    + "，新锁 " + fresh.wrong() + "/" + trials
                    + "，synchronized 方法 " + sync.wrong() + "/" + trials + "）");
        }
    }

    // ── 10. 可见性 ─────────────────────────────────────────────────

    private static void visibility() throws InterruptedException {
        System.out.println("可见性 Visibility：一个线程改了共享变量，另一个线程可能看不见。");
        System.out.println();
        System.out.println("一个线程 while (running) 空转，主线程 1 秒后把 running 设成 false。");
        System.out.println("  普通字段     ：停不下来？" + Visibility.plainGetsStuck(1000));
        System.out.println("  volatile 字段：停下来了吗？" + Visibility.volatileStops(1000));
        System.out.println();
        System.out.println("普通字段那一行显示 true，说明那个线程压根没看见 running 被改了。");
        System.out.println("这个现象依赖 JIT 优化，用 -Xint 跑就不出现——但内存模型上是允许的。");
    }

    // ── 11. 有序性 ─────────────────────────────────────────────────

    private static void ordering(boolean sample) throws InterruptedException {
        int rounds = 200_000;
        System.out.println("有序性 Ordering：编译器和 CPU 为了性能可能调整指令顺序。");
        System.out.println();
        System.out.println("litmus test（" + rounds + " 轮）：");
        System.out.println("  A: x = 1; r1 = y;      B: y = 1; r2 = x;");
        System.out.println("  顺序执行的话 (r1, r2) 不可能同时是 (0, 0)");
        System.out.println();

        Reordering.Result plain = Reordering.runPlain(rounds);
        Reordering.Result vol = Reordering.runVolatile(rounds);

        System.out.println("  普通字段      ：抓到 (0, 0) 了吗 = " + yesNo(plain.sawAnomaly()));
        System.out.println("  volatile 字段 ：抓到 (0, 0) 了吗 = " + yesNo(vol.sawAnomaly()));
        if (sample) {
            System.out.println();
            System.out.println("  （某一次真实运行：普通字段 " + plain.anomalies()
                    + " 次 / " + rounds + " 轮，volatile " + vol.anomalies() + " 次）");
        }
        System.out.println();
        System.out.println("普通字段抓到 (0, 0) → 重排真实发生了。");
        System.out.println("volatile 之后一次都抓不到 → volatile 禁止这种重排。");
    }

    // ── 12. 卖票 ───────────────────────────────────────────────────

    private static void ticketOffice(boolean sample) throws InterruptedException {
        int total = 100;
        int windows = 4;
        int trials = sample ? SAMPLE_TRIALS : TRIALS;

        int badRuns = 0;
        int oversoldRuns = 0;
        int worstRemaining = total;
        int maxSold = 0;

        for (int round = 0; round < trials; round++) {
            TicketOffice office = new TicketOffice(total, windows, true);
            TicketOffice.Result result = office.sellWithoutLock();
            if (result.sold() != total) {
                badRuns++;
            }
            if (result.oversold()) {
                oversoldRuns++;
            }
            worstRemaining = Math.min(worstRemaining, result.remaining());
            maxSold = Math.max(maxSold, result.sold());
        }

        System.out.println(total + " 张票、" + windows + " 个窗口，不加锁跑 " + trials + " 次：");
        System.out.println(pad("  出现过卖出张数不是 " + total + " 的情况吗", 46) + "= " + yesNo(badRuns > 0));
        System.out.println(pad("  出现过余票是负数（超卖）的情况吗", 46) + "= " + yesNo(oversoldRuns > 0));
        if (sample) {
            System.out.println("  （某一次真实运行：卖出不是 " + total + " 的有 " + badRuns
                    + " 次，超卖 " + oversoldRuns + " 次，最差余票 " + worstRemaining
                    + "，最多一次卖出 " + maxSold + " 张）");
        }
        System.out.println();

        int safeBad = 0;
        for (int round = 0; round < trials; round++) {
            TicketOffice office = new TicketOffice(total, windows, true);
            TicketOffice.Result result = office.sellWithLock();
            if (result.sold() != total || result.remaining() != 0) {
                safeBad++;
            }
        }
        System.out.println("加上 synchronized 之后再跑 " + trials + " 次：");
        System.out.println("  卖出张数不是 " + total + " 或余票不是 0 的有 " + safeBad + " 次");
        System.out.println();
        System.out.println("最后一定卖出 " + total + " 张，任何窗口都不会卖成负数。");
    }

    // ── 小工具 ─────────────────────────────────────────────────────

    /** 一批试验的结果统计。 */
    private record Stats(int trials, int wrong, int min, int max) {
    }

    /** 一个计数器接口，用来把「跑很多次统计」的样板抽出来。 */
    private interface CountAction {

        int run(Counter counter) throws InterruptedException;
    }

    /** 跑 {@code trials} 次，统计结果不是 {@link #EXPECTED} 的次数和极值。 */
    private static Stats measure(int trials, CountAction action) throws InterruptedException {
        int wrong = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int round = 0; round < trials; round++) {
            int value = action.run(new Counter());
            if (value != EXPECTED) {
                wrong++;
            }
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return new Stats(trials, wrong, min, max);
    }

    /** 默认模式只给「有没有错」这个稳定结论；sample 模式才印原始次数。 */
    private static String verdict(int trials, CountAction action, boolean sample)
            throws InterruptedException {
        Stats stats = measure(trials, action);
        if (sample) {
            return "跑 " + trials + " 次，不是 " + EXPECTED + " 的有 " + stats.wrong()
                    + " 次（最小 " + stats.min() + "）";
        }
        return "跑 " + trials + " 次，出现过不是 " + EXPECTED + " 的情况："
                + yesNo(stats.wrong() > 0);
    }

    private static String yesNo(boolean value) {
        return value ? "是" : "否";
    }

    /** 按「显示宽度」补空格。中文在终端里占两格，直接 {@code printf} 对不齐。 */
    private static String pad(String text, int width) {
        int display = 0;
        for (int i = 0; i < text.length(); i++) {
            display += isWide(text.charAt(i)) ? 2 : 1;
        }
        return text + " ".repeat(Math.max(1, width - display));
    }

    /** 粗略判断一个字符在等宽终端里是不是占两格（够本课用）。 */
    private static boolean isWide(char c) {
        return (c >= 0x1100 && c <= 0x115F)      // 韩文字母
                || (c >= 0x2E80 && c <= 0xA4CF)  // 中日韩部首、汉字
                || (c >= 0xAC00 && c <= 0xD7A3)  // 韩文音节
                || (c >= 0xF900 && c <= 0xFAFF)  // 兼容汉字
                || (c >= 0xFE30 && c <= 0xFE6F)  // 兼容形式
                || (c >= 0xFF00 && c <= 0xFF60)  // 全角
                || (c >= 0xFFE0 && c <= 0xFFE6);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
