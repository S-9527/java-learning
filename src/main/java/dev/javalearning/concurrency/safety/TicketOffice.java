package dev.javalearning.concurrency.safety;

/**
 * 第 13 课的实战例子：100 张票，4 个窗口卖。
 *
 * <p>不加锁时会超卖（剩余票数变成负数），加锁后正好卖完。
 *
 * <p><b>为什么这里要留一个"间距"</b>：真实的售票业务里，
 * "查库存"和"扣库存"之间是有耗时的（查数据库、发远程调用）。
 * 如果没有这个间距，几个线程几乎不可能同时通过判断，
 * 超卖就很难复现。代码里用几次 {@code Thread.onSpinWait()} 模拟这个间距——
 * 它让多个窗口有机会都读到"还剩 1 张"。
 */
public class TicketOffice {

    private int tickets = 100;
    private final boolean locked;

    public TicketOffice(boolean locked) {
        this.locked = locked;
    }

    /** 卖票。窗口会一直卖到没票为止。 */
    public void sell() {
        while (true) {
            if (locked) {
                synchronized (this) {
                    if (tickets <= 0) {
                        return;
                    }
                    tickets--;
                }
            } else {
                // 故意不加锁：判断和扣减是两步，中间可能被别的窗口插进来
                if (tickets <= 0) {
                    return;
                }
                // 模拟"判断完到扣减之间"那点耗时（真实业务里是远程调用）
                for (int k = 0; k < 50; k++) {
                    Thread.onSpinWait();
                }
                tickets--;      // 多个窗口可能都通过了上面的判断，于是都减了
            }
        }
    }

    public int remaining() {
        return tickets;
    }
}
