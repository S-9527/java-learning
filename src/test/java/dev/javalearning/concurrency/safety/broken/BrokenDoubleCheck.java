package dev.javalearning.concurrency.safety.broken;

/**
 * 第 13/14 课的反面教材：漏掉 {@code volatile} 的双重检查锁定单例。
 *
 * <p><b>它住在 src/test 而不是 src/main 的 broken/</b>——因为它编译得过，
 * 只是行为错。这类"静默错误"必须能被测试引用才能钉住。
 *
 * <p>错误在于：{@code new BrokenDoubleCheck()} 的三步
 * （分配内存 → 初始化 → 赋值引用）可能被重排成"先赋值、后初始化"，
 * 于是别的线程可能拿到一个<b>还没初始化完的对象</b>。
 *
 * <p>对比正确版本：{@code dev.javalearning.concurrency.jmm.Singleton}
 * （那里的 {@code instance} 有 volatile）。
 *
 * <p><b>注意</b>：这个 bug 在 x86 上极难复现（内存模型比较强），
 * 所以这里不断言"一定能观测到"，只作为反面教材保留代码。
 * 测试里只验证"能编译、能跑、行为看起来正常"——这正是它危险的地方。
 */
public final class BrokenDoubleCheck {

    /** 少了 volatile。 */
    private static BrokenDoubleCheck instance;

    private final int[] data;

    private BrokenDoubleCheck() {
        // 模拟"初始化需要一点时间"，让重排的后果有机会显现
        this.data = new int[1000];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
    }

    public static BrokenDoubleCheck getInstance() {
        if (instance == null) {
            synchronized (BrokenDoubleCheck.class) {
                if (instance == null) {
                    instance = new BrokenDoubleCheck();
                }
            }
        }
        return instance;
    }

    /** 读一下内部数据，用来检测"是不是拿到了半成品"。 */
    public int checksum() {
        int sum = 0;
        for (int v : data) {
            sum += v;
        }
        return sum;
    }
}
