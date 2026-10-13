package dev.javalearning.basics.inheritance.broken;

import java.util.HashSet;
import java.util.Set;

/**
 * 第 05 课的反面教材：只重写 equals，漏掉 hashCode。
 *
 * <p><b>它住在 src/test 而不是 src/main</b>——这不是编译错误，而是行为错误：
 * 编译通过、运行不报错，但 HashSet 的去重会静默失效。
 * 正因为它编译得过，才必须能被测试引用，才能在断言里把这个静默错误钉住。
 * （编译不过的反面教材才放 main 的 broken/ 里。）
 */
public class BrokenMoney {

    private final int cents;

    public BrokenMoney(int cents) {
        this.cents = cents;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BrokenMoney that)) {
            return false;
        }
        return this.cents == that.cents;
    }

    // ← 就是漏了 hashCode。Object 那个默认实现按对象身份算，
    //   于是每个对象的 hashCode 都不一样。

    public static void main(String[] args) {
        Set<BrokenMoney> wallet = new HashSet<>();
        wallet.add(new BrokenMoney(100));
        wallet.add(new BrokenMoney(100));
        wallet.add(new BrokenMoney(100));

        System.out.println("放进去 3 个相同的 100 分，HashSet 里剩 " + wallet.size() + " 个");
        System.out.println("（正确的话应该剩 1 个）");
    }
}
