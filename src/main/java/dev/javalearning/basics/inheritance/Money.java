package dev.javalearning.basics.inheritance;

import java.util.Objects;

/**
 * 第 05 课的类：金额。<b>正确</b>重写了 equals 和 hashCode。
 *
 * <p>它和第 04 课的 {@code Person} 是同一类东西，区别只在于：
 * 这里把 Object 继承来的 equals 换成了「按内容比」。
 * 而 hashCode 必须跟着一起改——理由是这一课要讲的「契约」。
 */
public class Money {

    private final int cents;

    public Money(int cents) {
        this.cents = cents;
    }

    public int cents() {
        return cents;
    }

    /** equals 的三步：先比身份，再比类型，最后比内容。 */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;                    // ① 同一个对象，直接相等
        }
        if (!(other instanceof Money that)) {
            return false;                   // ② 类型不对就不等（模式匹配）
        }
        return this.cents == that.cents;    // ③ 内容一样才算相等
    }

    /**
     * hashCode 必须和 equals 一致：equals 判相等的两个对象，hashCode 必须一样。
     *
     * <p>反过来不要求——hashCode 相等只代表可能相等，还得靠 equals 确认。
     * 这就是为什么 HashMap 先用 hashCode 分桶、再在桶里用 equals 比。
     */
    @Override
    public int hashCode() {
        return Objects.hash(cents);
    }

    @Override
    public String toString() {
        return "Money{" + cents + " 分}";
    }
}
