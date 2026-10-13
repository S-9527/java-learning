package dev.javalearning.reflection;

/**
 * 和 {@link InitProbe} 配对的状态容器，<b>故意分开写</b>。
 *
 * <p>为什么不能把 {@code initialized} 直接写在 {@link InitProbe} 里？
 * 因为读一个类的 static 字段<b>本身就会触发这个类初始化</b>——
 * 那样探针一被读就把自己要测的东西给弄脏了，测出来永远是 {@code true}。
 *
 * <p>所以标志位放在这个没有 static 块的类里，读它不会牵动 {@link InitProbe}。
 * 这是写这个测试时才发现的坑，比结论本身更值得记一笔。
 */
public final class InitProbeLog {

    /** {@link InitProbe} 的 static 块跑过没有。 */
    public static boolean probeInitialized;

    private InitProbeLog() {
    }
}
