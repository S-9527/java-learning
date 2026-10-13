package dev.javalearning.reflection;

import java.lang.reflect.Method;

/**
 * 第 09 课的性能对比：同一个 {@code hello()}，直接调用 vs 反射调用。
 *
 * <p>单独跑：{@code java src/main/java/dev/javalearning/reflection/ReflectionCost.java}
 *
 * <p><b>这个类的输出天生不稳定</b>——耗时每次运行都不一样，所以它没有被
 * {@link ReflectionDemo} 打印出来（那会把 demo 的输出弄脏）。文档
 * {@code docs/09-反射.md} 里贴的这组数字是某一次真实运行的结果，看量级就行。
 *
 * <p>写这个对比时踩了两个坑，都写在代码里了，因为它们比结论更有用：
 * <ol>
 *   <li>结果不消费，JIT 会把整个循环优化掉，直接调用测得 0 ms（见 {@link #sink}）；</li>
 *   <li>只测一轮，谁先被 JIT 编译谁吃亏，数字能差几十倍（见 {@link #TRIES}）。</li>
 * </ol>
 */
public class ReflectionCost {

    /** 预热：先让 JIT 把两边都编译了，否则比的是"谁先被编译"，不是"谁更快"。 */
    private static final int WARMUP = 200_000;

    /** 每轮调用的次数。 */
    private static final int ROUNDS = 5_000_000;

    /** 每边各测几轮，最后取最小值——最小值最接近"没有额外干扰"的那次。 */
    private static final int TRIES = 6;

    /**
     * 黑洞：把结果攒起来，防止 JIT 发现"这个循环白算"就把它删了。
     *
     * <p>第一版没写这个，直接调用的耗时是 {@code 0 ms}——不是它快到测不出来，
     * 是那段代码压根没执行。
     */
    private static long sink;

    public static void main(String[] args) throws ReflectiveOperationException {
        User user = new User("小明", 18);
        Method hello = User.class.getMethod("hello");

        for (int i = 0; i < WARMUP; i++) {
            sink += user.hello().length();
            sink += ((String) hello.invoke(user)).length();
        }

        long bestDirect = Long.MAX_VALUE;
        long bestReflection = Long.MAX_VALUE;
        for (int round = 0; round < TRIES; round++) {
            long direct = timeDirect(user);
            long reflection = timeReflection(user, hello);
            bestDirect = Math.min(bestDirect, direct);
            bestReflection = Math.min(bestReflection, reflection);
            System.out.println("第 " + (round + 1) + " 轮：直接 " + ms(direct)
                    + " ms，反射 " + ms(reflection) + " ms");
        }

        System.out.println();
        System.out.println("取各边最快的一轮（调用 " + ROUNDS + " 次 user.hello()）：");
        System.out.println("  直接调用：" + ms(bestDirect) + " ms");
        System.out.println("  反射调用：" + ms(bestReflection) + " ms");
        System.out.println("  反射大约是直接调用的 "
                + String.format("%.1f", bestReflection * 1.0 / bestDirect) + " 倍");
        System.out.println();
        System.out.println("换算成每次调用：");
        System.out.println("  直接调用：" + perCall(bestDirect) + " 纳秒");
        System.out.println("  反射调用：" + perCall(bestReflection) + " 纳秒");
        System.out.println();
        System.out.println("（每次运行数字都不同，看量级：反射确实更慢，"
                + "但慢的是几十纳秒这个级别）");
    }

    private static long timeDirect(User user) {
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            sink += user.hello().length();
        }
        return System.nanoTime() - start;
    }

    private static long timeReflection(User user, Method hello) throws ReflectiveOperationException {
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            sink += ((String) hello.invoke(user)).length();
        }
        return System.nanoTime() - start;
    }

    private static String ms(long nanos) {
        return String.format("%.1f", nanos / 1_000_000.0);
    }

    private static String perCall(long nanos) {
        return String.format("%.0f", nanos * 1.0 / ROUNDS);
    }
}
