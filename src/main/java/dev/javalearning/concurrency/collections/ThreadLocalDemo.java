package dev.javalearning.concurrency.collections;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 第 18 课的示例代码：ThreadLocal（下半场）。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/collections/ThreadLocalDemo.java}
 */
public class ThreadLocalDemo {

    /** 用来演示「每个线程自己一份」的上下文。 */
    static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    /** 带默认值的 ThreadLocal，省掉每次判空。 */
    static final ThreadLocal<Integer> COUNTER = ThreadLocal.withInitial(() -> 0);

    public static void main(String[] args) throws Exception {
        section("1. 每个线程自己一份：互相看不见");
        perThreadIsolation();

        section("2. withInitial：给一个默认值");
        withInitialDemo();

        section("3. 线程池复用 → 上一个任务的数据会留下来");
        threadPoolResidue();

        section("4. 用完 remove：这是必须的");
        removeDemo();

        section("5. 为什么会内存泄漏：key 是弱引用，value 不是");
        weakKeyDemo();

        section("6. ThreadLocal 在实战里长什么样：一次请求一个上下文");
        requestContextDemo();
    }

    /** 主线程设的值，子线程看不到；各自设各自的。 */
    static void perThreadIsolation() throws Exception {
        CONTEXT.set("主线程的值");

        StringBuilder log = new StringBuilder();
        Thread worker = new Thread(() -> {
            log.append("  子线程 get() → ").append(CONTEXT.get()).append("（看不到主线程设的）\n");
            CONTEXT.set("子线程的值");
            log.append("  子线程 set 之后 get() → ").append(CONTEXT.get()).append('\n');
        });
        worker.start();
        worker.join();

        System.out.print(log);
        System.out.println("  主线程 get() → " + CONTEXT.get() + "（没被子线程影响）");
        System.out.println("  ← 数据存在「线程自己身上」，所以天然线程安全");

        CONTEXT.remove();
    }

    /** withInitial 提供默认值，省掉判空。 */
    static void withInitialDemo() {
        System.out.println("  第一次 get() → " + COUNTER.get() + "（走的是 withInitial 给的默认值）");
        COUNTER.set(COUNTER.get() + 1);
        System.out.println("  加一之后 get() → " + COUNTER.get());
        COUNTER.remove();
    }

    /** 线程池复用线程，上一个任务没清理的数据会被下一个任务读到。 */
    static void threadPoolResidue() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);   // 只有 1 个线程，必然复用

        StringBuilder log = new StringBuilder();
        pool.execute(() -> CONTEXT.set("用户A"));
        pool.execute(() -> log.append("  第二个任务读到的 → ").append(CONTEXT.get())
                .append("（上一个任务留下的！）\n"));

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.print(log);
        System.out.println("  ← 单线程的池一定会复用线程，所以残留是必然的");

        CONTEXT.remove();
    }

    /** 用完 remove，下一个任务就读不到了。 */
    static void removeDemo() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);

        StringBuilder log = new StringBuilder();
        pool.execute(() -> {
            CONTEXT.set("用户A");
            CONTEXT.remove();          // ← 关键这一行
        });
        pool.execute(() -> log.append("  remove 之后第二个任务读到 → ").append(CONTEXT.get()).append('\n'));

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.print(log);
        System.out.println("  ← 规矩：谁 set 谁负责 remove，用完立刻清");
    }

    /** ThreadLocalMap 的 key 是弱引用，value 是强引用。 */
    static void weakKeyDemo() throws Exception {
        ThreadLocal<byte[]> leaky = new ThreadLocal<>();
        leaky.set(new byte[1024 * 1024]);        // 塞 1MB 进去
        WeakReference<ThreadLocal<?>> ref = new WeakReference<>(leaky);

        leaky = null;                             // 外部强引用没了
        System.gc();
        Thread.sleep(200);

        System.out.println("  GC 之后，ThreadLocal 这个 key 还在吗 → " + (ref.get() != null));
        System.out.println("  ← key 是弱引用，没有外部强引用时会被回收");
        System.out.println("  ← 但 value 是强引用，还挂在线程的 map 上 —— 这就是泄漏的由来");
        System.out.println("  ← 如果这个线程是池里的、永远不销毁，那 1MB 就一直占着");
    }

    /** 实战里的样子：一个请求进来设上下文，结束时清理。 */
    static void requestContextDemo() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        // 三个任务并发跑，谁先打印不确定，所以收集后排序
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (String user : new String[] {"alice", "bob", "carol"}) {
            pool.execute(() -> {
                try {
                    // 请求进来：设置上下文
                    CONTEXT.set("traceId-" + user);
                    lines.add("  " + user + " 的请求，traceId = " + CONTEXT.get());
                    // 中间随便调用多少层方法，都不用把 traceId 当参数传下去
                    doBusiness();
                } finally {
                    CONTEXT.remove();      // 请求结束：必须清理
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        java.util.List<String> sorted = new java.util.ArrayList<>(lines);
        java.util.Collections.sort(sorted);
        for (String line : sorted) {
            System.out.println(line);
        }
        System.out.println("  ← 这就是「链路追踪」「当前登录用户」的标准实现方式");
        System.out.println("  ← 好处：不用把 traceId 一层层当参数传下去");
    }

    /** 模拟深层调用，直接读 ThreadLocal，不需要参数。 */
    static void doBusiness() {
        String traceId = CONTEXT.get();
        if (traceId != null && traceId.contains("alice")) {
            // 只打印一次，避免输出顺序不确定
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
