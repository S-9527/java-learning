package dev.javalearning.concurrency.safety;

/**
 * 第 13 课的示例代码：演示 {@code synchronized} 的<b>可重入</b>（reentrant）。
 *
 * <p>{@link Nested#a()} 和 {@link Nested#b()} 都是同步方法，锁的都是同一个对象
 * （{@code this}）。{@code a()} 里又调了 {@code b()}——也就是说，同一个线程
 * 在已经持有这把锁的情况下，<b>再进一次</b>这把锁保护的代码。
 *
 * <p>如果锁不可重入，这里会当场死锁：线程自己把自己挡在门外。实际结果是一路进去，
 * 因为 JVM 会记「这把锁是这个线程拿的，而且拿了两次」。
 *
 * <p>{@code b()} 里用 {@link Thread#holdsLock(Object)} 确认自己确实持有锁，
 * 证明它不是「碰巧跑进来的」。
 *
 * <p>对应 {@code docs/13-线程安全与同步.md}。
 */
final class Reentrancy {

    /** {@code trace} 是调用轨迹，{@code calls} 是一共进了几次同步方法。 */
    record Result(String trace, int calls) {
    }

    static final class Nested {

        private int calls;

        synchronized String a() {
            calls++;
            // 同一个线程再次进入这把锁保护的代码——可重入，所以直接放行
            return "a→" + b();
        }

        synchronized String b() {
            calls++;
            // 用 Thread.holdsLock 证明确实持有锁，不是碰巧跑进来的
            return "b(持有锁=" + Thread.holdsLock(this) + ")";
        }

        int calls() {
            return calls;
        }
    }

    private Reentrancy() {
    }

    static Result run() {
        Nested nested = new Nested();
        String trace = nested.a();
        return new Result(trace, nested.calls());
    }
}
