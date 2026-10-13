package dev.javalearning.concurrency.safety;

/**
 * 第 13 课的示例代码：用 {@link Thread#holdsLock(Object)} 当场问出
 * 「我现在到底持有哪把锁」。
 *
 * <p>这张表光背没用，跑一遍就记住了：
 *
 * <pre>
 *   实例同步方法          → 持有 this，不持有 类对象
 *   静态同步方法          → 持有 类对象，不持有 this
 *   synchronized (this)   → 持有 this
 *   synchronized (类.class) → 持有 类对象
 * </pre>
 *
 * <p>对应 {@code docs/13-线程安全与同步.md}。
 */
final class LockIdentity {

    /** {@code self} = 持有当前实例；{@code classObject} = 持有这个类的 {@code Class} 对象。 */
    record Holds(boolean self, boolean classObject) {
    }

    /** 另一个对象，用来验证「没锁它就不持有它」。 */
    static final Object OTHER = new Object();

    /** 要能造实例才能验证「实例同步方法锁的是 this」，所以这里不能用 private。 */
    LockIdentity() {
    }

    synchronized Holds inspectInstanceMethod() {
        return new Holds(Thread.holdsLock(this), Thread.holdsLock(LockIdentity.class));
    }

    static synchronized Holds inspectStaticMethod() {
        return new Holds(false, Thread.holdsLock(LockIdentity.class));
    }

    static Holds inspectThisBlock(LockIdentity target) {
        synchronized (target) {
            return new Holds(Thread.holdsLock(target), Thread.holdsLock(LockIdentity.class));
        }
    }

    static Holds inspectClassBlock() {
        synchronized (LockIdentity.class) {
            return new Holds(false, Thread.holdsLock(LockIdentity.class));
        }
    }

    static Holds inspectOtherBlock() {
        synchronized (OTHER) {
            // 锁的是 OTHER，跟 this、跟类对象都没关系
            return new Holds(Thread.holdsLock(OTHER), Thread.holdsLock(LockIdentity.class));
        }
    }
}
