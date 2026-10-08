package dev.javaquest.basics.inheritance;

/**
 * 一个「值相等」的对象：金额一样就算相等，不管是不是同一个对象。
 *
 * <p>这要靠重写 {@code equals} 和 {@code hashCode}。第 04 章说过不重写的话
 * 比的是身份；这里把两对方法一起写对。
 */
public final class Money {

    /** 用分存，避免浮点误差。 */
    private final int cents;

    public Money(int cents) {
        this.cents = cents;
    }

    public int getCents() {
        return cents;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Money that)) {
            return false;
        }
        return this.cents == that.cents;
    }

    /**
     * 和 {@code equals} 配套：equals 判相等的两个对象，hashCode 必须一样。
     *
     * <p>漏掉这个方法时，{@link java.util.HashSet} 会把两个相等的金额当成两个元素。
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(cents);
    }

    @Override
    public String toString() {
        return (cents / 100) + "元" + (cents % 100) + "分";
    }
}
