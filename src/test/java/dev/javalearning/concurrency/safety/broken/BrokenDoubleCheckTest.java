package dev.javalearning.concurrency.safety.broken;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 13/14 课的断言：漏掉 volatile 的双重检查锁定。
 *
 * <p>这个测试的重点<b>不是</b>"证明它出错"——那个 bug 依赖 JIT 重排，
 * 在 x86 上极难稳定复现，硬断言会变成 flaky 测试。
 *
 * <p>重点是记录<b>为什么它危险</b>：
 * 代码能编译、能跑、并发拿到的还是同一个对象、结果看起来完全正常。
 * 问题只在极端时机下才暴露。所以真正该做的是"写对"，
 * 而不是"等测试变红"。
 */
class BrokenDoubleCheckTest {

    @Test
    @DisplayName("漏掉 volatile 的 DCL：能编译、能跑、并发下拿到的还是同一个对象")
    void looksFineButIsNot() throws Exception {
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        List<BrokenDoubleCheck> got = Collections.synchronizedList(new ArrayList<>());

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                got.add(BrokenDoubleCheck.getInstance());
            });
            workers[i].start();
        }
        start.countDown();
        for (Thread w : workers) {
            w.join();
        }

        // 看起来一切正常：16 个线程拿到的是同一个实例
        assertThat(got).hasSize(threads);
        assertThat(got).allMatch(s -> s == got.get(0));
        // 而且数据也是完整的（这就是它危险的地方——测试环境根本发现不了）
        assertThat(got.get(0).checksum()).isPositive();
    }
}
