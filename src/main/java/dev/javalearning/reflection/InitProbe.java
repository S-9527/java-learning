package dev.javalearning.reflection;

/**
 * 只给 {@code ReflectionTest} 用的初始化探针：static 块一跑就登记到
 * {@link InitProbeLog} 里，用来证明「{@code .class} 不触发初始化、
 * {@code Class.forName} 触发」。
 *
 * <p>{@link HeavyInit} 是同一个道理的演示版，但它给 demo 用；
 * 测试需要一个<b>只有那一条断言会碰</b>的类，否则别的测试先把它初始化了，
 * 断言就永远看到 {@code true}。
 */
public class InitProbe {

    static {
        InitProbeLog.probeInitialized = true;
    }

    /** 公开的无参构造器：测试里要能直接 {@code new} 出来。 */
    public InitProbe() {
    }
}
