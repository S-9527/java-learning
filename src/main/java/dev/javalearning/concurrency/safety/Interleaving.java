package dev.javalearning.concurrency.safety;

import java.util.concurrent.CountDownLatch;

/**
 * 第 13 课的示例代码：手工编排两个线程的交错顺序，把「丢失更新」<b>确定性地</b>演出来。
 *
 * <p>竞态条件的输出天然不稳定：直接起两个线程各加一次，多半是 2，看不出问题。
 * 但并发 bug 的危险恰恰在于「多半没事」。所以这里不去碰运气，而是用
 * {@link CountDownLatch} 把两个线程的步子钉死成：
 *
 * <pre>
 *   A 读 → B 读 → B 写 → A 写
 * </pre>
 *
 * <p>这个顺序是线程调度<b>允许</b>的，所以它是真实存在的 bug，不是造出来的假象。
 * 这样每次跑都得到同一个结果：加了两次，只涨了 1。
 *
 * <p>对应 {@code docs/13-线程安全与同步.md}。
 */
final class Interleaving {

    /** 一个可以被读写的整数格子。普通字段和 {@code volatile} 字段各给一个实现。 */
    interface Cell {

        int get();

        void set(int value);

        String name();
    }

    /** 普通字段：读和写都没有任何同步。 */
    static final class PlainCell implements Cell {

        private int value;

        @Override
        public int get() {
            return value;
        }

        @Override
        public void set(int value) {
            this.value = value;
        }

        @Override
        public String name() {
            return "普通字段";
        }
    }

    /**
     * {@code volatile} 字段：保证可见性，但 {@code volatile} 的 {@code value++}
     * 仍然是「读—加—写」三步，照样会丢更新。
     */
    static final class VolatileCell implements Cell {

        private volatile int value;

        @Override
        public int get() {
            return value;
        }

        @Override
        public void set(int value) {
            this.value = value;
        }

        @Override
        public String name() {
            return "volatile 字段";
        }
    }

    /** 一次交错的结果。{@code finalValue} 恒为 1（期望是 2）。 */
    record Result(String cellName, int aRead, int bRead, int finalValue) {
    }

    private Interleaving() {
    }

    /**
     * 让两个线程按 {@code A 读 → B 读 → B 写 → A 写} 的顺序各做一次自增。
     *
     * @param cell 用哪种格子（普通字段 / volatile 字段）
     * @return 这次交错读到和写回的值
     */
    static Result run(Cell cell) throws InterruptedException {
        cell.set(0);

        CountDownLatch aRead = new CountDownLatch(1);
        CountDownLatch bWrote = new CountDownLatch(1);
        int[] aSaw = {0};
        int[] bSaw = {0};

        Thread a = new Thread(() -> {
            int read = cell.get();          // ① A 读
            aSaw[0] = read;
            aRead.countDown();
            await(bWrote);                  // 等 B 先写完
            cell.set(read + 1);             // ④ A 写回
        });

        Thread b = new Thread(() -> {
            await(aRead);                   // 等 A 读完
            int read = cell.get();          // ② B 读（读到的是同一个旧值）
            bSaw[0] = read;
            cell.set(read + 1);             // ③ B 写回
            bWrote.countDown();
        });

        a.start();
        b.start();
        a.join();
        b.join();

        return new Result(cell.name(), aSaw[0], bSaw[0], cell.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
