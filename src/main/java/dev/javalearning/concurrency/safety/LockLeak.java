package dev.javalearning.concurrency.safety;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 第 13 课的示例代码：{@link ReentrantLock} 忘了在 {@code finally} 里解锁的后果。
 *
 * <p>{@code synchronized} 出异常时 JVM 会自动把锁还回去，所以写起来省心；
 * {@code Lock} 必须手动 {@code unlock()}。一旦中间抛异常又没走 {@code finally}，
 * 这把锁就<b>永远不还了</b>——其他线程全部卡死，也就是死锁。
 *
 * <p>本类不真的让程序卡住（那没法写测试），而是用
 * {@link ReentrantLock#tryLock(long, TimeUnit)} 去问一句「别的线程还拿得到吗」，
 * 把「拿不到」这个事实钉下来。
 *
 * <p>对应 {@code docs/13-线程安全与同步.md}。
 */
final class LockLeak {

    /** 锁正确释放 / 泄漏，用一个 300 毫秒的超时去探。 */
    record Result(boolean otherThreadGotLock) {
    }

    private static final ReentrantLock RELEASED = new ReentrantLock();
    private static final ReentrantLock LEAKED = new ReentrantLock();

    private LockLeak() {
    }

    /** 正确写法：{@code unlock} 在 {@code finally} 里，抛异常也照样还锁。 */
    static Result withFinally() throws InterruptedException {
        RELEASED.lock();
        try {
            throw new IllegalStateException("业务异常");
        } catch (IllegalStateException e) {
            // 吞掉异常继续走——重点是 finally 仍然会执行
        } finally {
            RELEASED.unlock();
        }
        return new Result(otherThreadCanLock(RELEASED));
    }

    /** 错误写法：没有 {@code finally}，异常一抛，锁就再也没人还了。 */
    static Result withoutFinally() throws InterruptedException {
        LEAKED.lock();
        try {
            throw new IllegalStateException("业务异常");
        } catch (IllegalStateException e) {
            // 吞掉异常——注意这里<b>没有</b> unlock
        }
        return new Result(otherThreadCanLock(LEAKED));
    }

    private static boolean otherThreadCanLock(ReentrantLock lock) throws InterruptedException {
        boolean[] got = {false};
        CountDownLatch done = new CountDownLatch(1);
        Thread other = new Thread(() -> {
            try {
                got[0] = lock.tryLock(300, TimeUnit.MILLISECONDS);
                if (got[0]) {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });
        other.start();
        done.await();
        return got[0];
    }
}
