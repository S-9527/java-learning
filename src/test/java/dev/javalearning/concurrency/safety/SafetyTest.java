package dev.javalearning.concurrency.safety;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 13 课的断言。
 *
 * <p>每一条对应 {@code docs/13-线程安全与同步.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*safety*'}
 *
 * <p><b>这些断言为什么是稳的</b>：竞态条件的输出天然不稳定，所以这里
 * <b>绝不断言「某一次跑出来的具体数字」</b>。凡是要证明「这段代码不安全」的，
 * 都写成「跑 N 次，至少有一次不对」——N 取得足够大（200 次），这个结论就稳了。
 * 凡是要证明「修好了」的，才断言恒等于期望值。
 *
 * <p>反过来，任何「跑一次看它错不错」的写法都不能进测试：实测中，
 * {@code synchronized (new Object())} 单跑一次有 11/15 的概率<b>碰巧全对</b>。
 */
class SafetyTest {

    private static final int THREADS = 10;
    private static final int PER_THREAD = 1000;
    private static final int EXPECTED = THREADS * PER_THREAD;

    /** 要证明「不安全」时跑的轮数。够大才稳得住。 */
    private static final int TRIALS = 200;

    // ── 1. 不安全的计数器 ──────────────────────────────────────────

    @Test
    @DisplayName("裸 count++ 会丢更新——跑 200 次里至少错一次")
    void unsafeCounterLosesUpdates() throws InterruptedException {
        boolean everWrong = false;
        for (int round = 0; round < TRIALS && !everWrong; round++) {
            Counter counter = new Counter();
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementUnsafe();
                }
            });
            everWrong = counter.unsafe() != EXPECTED;
        }
        assertThat(everWrong)
                .as("裸 count++ 是「读—加—写」三步，跑 %d 次至少该错一次", TRIALS)
                .isTrue();
    }

    @Test
    @DisplayName("把「读」和「写」之间的窗口撑开，丢失更新几乎必现")
    void widenedWindowAlmostAlwaysLosesUpdates() throws InterruptedException {
        int wrong = 0;
        int trials = 20;
        for (int round = 0; round < trials; round++) {
            Counter counter = new Counter();
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementWithWideWindow(i);
                }
            });
            if (counter.wideWindow() != EXPECTED) {
                wrong++;
            }
        }
        // 实测这个写法 20/20 次都错；这里留一点余量，要求「绝大多数都错」
        assertThat(wrong).as("窗口撑开后应该几乎每次都丢更新").isGreaterThanOrEqualTo(trials - 2);
    }

    @Test
    @DisplayName("线程不重叠时，同一段不安全代码跑 200 次一次都不错")
    void noOverlapMeansNoRace() throws InterruptedException {
        int wrong = 0;
        for (int round = 0; round < TRIALS; round++) {
            Counter counter = new Counter();
            // 起一个、join 一个：下一个线程必须等前一个彻底结束
            Threads.runSequentially(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementUnsafe();
                }
            });
            if (counter.unsafe() != EXPECTED) {
                wrong++;
            }
        }
        assertThat(wrong)
                .as("线程不重叠就没有交错，跑 %d 次应当一次都不错——这才是竞态「偶发」的真相", TRIALS)
                .isZero();
    }

    // ── 2. 丢失更新 ────────────────────────────────────────────────

    @Test
    @DisplayName("手工编排 A读→B读→B写→A写，两个线程各加一次只涨了 1")
    void lostUpdateIsDeterministic() throws InterruptedException {
        Interleaving.Result result = Interleaving.run(new Interleaving.PlainCell());
        assertThat(result.aRead()).isZero();
        assertThat(result.bRead()).isZero();
        // 加了两次，结果只有 1 —— 丢失更新
        assertThat(result.finalValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("volatile 保证可见性，但 volatile 的 count++ 照样丢更新")
    void volatileDoesNotMakeIncrementAtomic() throws InterruptedException {
        Interleaving.Result result = Interleaving.run(new Interleaving.VolatileCell());
        assertThat(result.finalValue())
                .as("volatile 不保证原子性，读—加—写三步依然会丢")
                .isEqualTo(1);
    }

    // ── 3. synchronized 修好了 ─────────────────────────────────────

    @Test
    @DisplayName("方法级 synchronized：跑 200 次，次次都是 10000")
    void synchronizedMethodIsAlwaysCorrect() throws InterruptedException {
        for (int round = 0; round < TRIALS; round++) {
            Counter counter = new Counter();
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementSynchronized();
                }
            });
            assertThat(counter.synchronizedCount())
                    .as("第 %d 次", round + 1)
                    .isEqualTo(EXPECTED);
        }
    }

    @Test
    @DisplayName("同步代码块 synchronized (lock)：锁对了一个共用的对象，次次都对")
    void synchronizedBlockOnSharedObjectIsAlwaysCorrect() throws InterruptedException {
        for (int round = 0; round < TRIALS; round++) {
            Counter counter = new Counter();
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementWithLockObject();
                }
            });
            assertThat(counter.blockCount())
                    .as("第 %d 次", round + 1)
                    .isEqualTo(EXPECTED);
        }
    }

    @Test
    @DisplayName("ReentrantLock 正确写法（finally 里 unlock）：次次都是 10000")
    void reentrantLockIsAlwaysCorrect() throws InterruptedException {
        for (int round = 0; round < TRIALS; round++) {
            Counter counter = new Counter();
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementLocked();
                }
            });
            assertThat(counter.locked())
                    .as("第 %d 次", round + 1)
                    .isEqualTo(EXPECTED);
        }
    }

    // ── 4. 锁错对象 ────────────────────────────────────────────────

    @Test
    @DisplayName("锁住的是「对象」不是「代码」：synchronized (new Object()) 等于没锁")
    void freshLockEveryTimeIsNoLockAtAll() throws InterruptedException {
        boolean everWrong = false;
        for (int round = 0; round < TRIALS && !everWrong; round++) {
            Counter counter = new Counter();
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementWithFreshLock();
                }
            });
            everWrong = counter.freshLock() != EXPECTED;
        }
        assertThat(everWrong)
                .as("每次 new 一把新锁 = 每个线程各锁各的，跑 %d 次至少该错一次", TRIALS)
                .isTrue();
    }

    @Test
    @DisplayName("两把不同的锁，两个线程可以同时待在各自的临界区里")
    void twoDifferentLocksDoNotExcludeEachOther() throws InterruptedException {
        WrongLock.Result result = WrongLock.run();
        assertThat(result.bothInsideAtOnce())
                .as("各锁各的 new Object()，B 不会被 A 挡住")
                .isTrue();
    }

    // ── 5. 可重入 ──────────────────────────────────────────────────

    @Test
    @DisplayName("synchronized 可重入：a() 里调同步方法 b() 不会自己锁死")
    void synchronizedIsReentrant() {
        Reentrancy.Result result = Reentrancy.run();
        assertThat(result.trace()).isEqualTo("a→b(持有锁=true)");
        assertThat(result.calls()).isEqualTo(2);
    }

    // ── 6. 锁对象是谁 ──────────────────────────────────────────────

    @Test
    @DisplayName("实例同步方法锁 this，不锁类对象")
    void instanceMethodLocksThis() {
        LockIdentity.Holds holds = new LockIdentity().inspectInstanceMethod();
        assertThat(holds.self()).isTrue();
        assertThat(holds.classObject()).isFalse();
    }

    @Test
    @DisplayName("静态同步方法锁类对象，不锁 this")
    void staticMethodLocksClassObject() {
        LockIdentity.Holds holds = LockIdentity.inspectStaticMethod();
        assertThat(holds.self()).isFalse();
        assertThat(holds.classObject()).isTrue();
    }

    @Test
    @DisplayName("synchronized (this) 和实例同步方法锁的是同一把")
    void thisBlockMatchesInstanceMethod() {
        LockIdentity target = new LockIdentity();
        assertThat(target.inspectThisBlock(target)).isEqualTo(
                new LockIdentity.Holds(true, false));
    }

    @Test
    @DisplayName("synchronized (Xxx.class) 和静态同步方法锁的是同一把")
    void classBlockMatchesStaticMethod() {
        assertThat(LockIdentity.inspectClassBlock()).isEqualTo(
                new LockIdentity.Holds(false, true));
    }

    @Test
    @DisplayName("锁别的对象时，既不持有 this 也不持有类对象")
    void otherObjectLockIsUnrelated() {
        assertThat(LockIdentity.inspectOtherBlock()).isEqualTo(
                new LockIdentity.Holds(true, false));
        // 注意第一个字段在这里表示「持有被锁的那个对象」，
        // 而这个对象是 LockIdentity.OTHER，不是 this —— 详见下面的断言
        assertThat(Thread.holdsLock(LockIdentity.OTHER)).isFalse();
    }

    // ── 7. ReentrantLock 的 finally ────────────────────────────────

    @Test
    @DisplayName("写了 finally 的 unlock：抛异常也照样还锁")
    void lockReleasedInFinally() throws InterruptedException {
        assertThat(LockLeak.withFinally().otherThreadGotLock()).isTrue();
    }

    @Test
    @DisplayName("漏了 finally 的 unlock：抛异常后锁永远不还，别的线程拿不到")
    void lockLeakedWithoutFinally() throws InterruptedException {
        assertThat(LockLeak.withoutFinally().otherThreadGotLock())
                .as("锁没还回去，300ms 内别的线程拿不到 → 这就是死锁的前身")
                .isFalse();
    }

    // ── 8. 可见性 ──────────────────────────────────────────────────

    @Test
    @DisplayName("volatile 保证可见性：改了标志位，那个线程就停得下来")
    void volatileMakesTheChangeVisible() throws InterruptedException {
        assertThat(Visibility.volatileStops(500)).isTrue();
    }

    @Test
    @DisplayName("普通字段不保证可见性：那个线程可能一直看不见 running 变了")
    void plainFieldMayNeverSeeTheChange() throws InterruptedException {
        // 这个现象依赖 JIT 优化：本机默认分层编译下稳定复现，
        // 但 -Xint 纯解释执行时不会出现（解释器每次都去读内存）。
        // 所以这里不硬断言「一定卡住」，而是断言「这件事在某些环境下会发生」——
        // 只要它发生过，就说明内存模型允许它发生，代码就是不可靠的。
        boolean everStuck = Visibility.plainEverStuck(3, 500);
        assertThat(everStuck)
                .as("普通字段下，跑 3 次里至少该出现一次「看不见」；"
                        + "如果这条挂了，多半是换了 JVM 参数（比如 -Xint），"
                        + "不代表那个写法就安全了")
                .isTrue();
    }

    // ── 9. 有序性 ──────────────────────────────────────────────────

    @Test
    @DisplayName("普通字段能抓到 (0, 0)：指令重排真实发生了")
    void plainFieldsShowReordering() throws InterruptedException {
        Reordering.Result result = Reordering.runPlain(200_000);
        assertThat(result.sawAnomaly())
                .as("200000 轮里该抓到 (0, 0)；抓不到说明换了平台或 JVM 参数")
                .isTrue();
    }

    @Test
    @DisplayName("volatile 禁止这种重排：(0, 0) 一次都抓不到")
    void volatileForbidsReordering() throws InterruptedException {
        Reordering.Result result = Reordering.runVolatile(200_000);
        assertThat(result.anomalies()).isZero();
    }

    // ── 10. 卖票 ───────────────────────────────────────────────────

    @Test
    @DisplayName("100 张票 4 个窗口不加锁：会超卖，余票能卖成负数")
    void ticketsOversellWithoutLock() throws InterruptedException {
        boolean everOversold = false;
        boolean everWrongCount = false;
        for (int round = 0; round < TRIALS; round++) {
            TicketOffice office = new TicketOffice(100, 4, true);
            TicketOffice.Result result = office.sellWithoutLock();
            everOversold |= result.oversold();
            everWrongCount |= result.sold() != 100;
        }
        assertThat(everOversold).as("检查后行动之间有窗口，会超卖").isTrue();
        assertThat(everWrongCount).as("卖出的张数也会对不上").isTrue();
    }

    @Test
    @DisplayName("加上 synchronized：跑 200 次，次次正好卖出 100 张、余票 0")
    void ticketsAreCorrectWithLock() throws InterruptedException {
        for (int round = 0; round < TRIALS; round++) {
            TicketOffice office = new TicketOffice(100, 4, true);
            TicketOffice.Result result = office.sellWithLock();
            assertThat(result.sold()).as("第 %d 次卖出的张数", round + 1).isEqualTo(100);
            assertThat(result.remaining()).as("第 %d 次余票", round + 1).isZero();
        }
    }

    // ── 11. volatile 不保证原子性 ──────────────────────────────────

    @Test
    @DisplayName("字段加 volatile 之后，撑开窗口的三步写法照样丢更新")
    void volatileFieldStillLosesUpdates() throws InterruptedException {
        boolean everWrong = false;
        for (int round = 0; round < 20 && !everWrong; round++) {
            Counter counter = new Counter();
            Threads.runTogether(THREADS, () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    counter.incrementVolatileWideWindow(i);
                }
            });
            everWrong = counter.volatileWideWindow() != EXPECTED;
        }
        assertThat(everWrong)
                .as("volatile 只保证可见性和有序性，不保证原子性")
                .isTrue();
    }
}
