package dev.javalearning.lesson01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 01 课的断言。
 *
 * <p>每一条对应 {@code docs/01-变量与类型.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*lesson01*'}
 */
class VariablesTest {

    @Test
    @DisplayName("整数除法丢掉余数，不四舍五入：7 / 2 是 3")
    void integerDivisionDropsTheRemainder() {
        assertThat(7 / 2).isEqualTo(3);
    }

    @Test
    @DisplayName("想让结果带小数，随便让一边变成小数就行：7 / 2.0 是 3.5")
    void oneDecimalOperandIsEnough() {
        assertThat(7 / 2.0).isEqualTo(3.5);
    }

    @Test
    @DisplayName("负数是向零截断，不是向下取整：-7 / 2 是 -3 而不是 -4")
    void negativeDivisionTruncatesTowardZero() {
        assertThat(-7 / 2).isEqualTo(-3);
    }

    @Test
    @DisplayName("int 溢出静默回绕成最小值，不报错")
    void intOverflowWrapsAroundSilently() {
        assertThat(Integer.MAX_VALUE + 1).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @DisplayName("想要溢出就报错，用 Math.addExact")
    void addExactThrowsOnOverflow() {
        assertThatThrownBy(() -> Math.addExact(Integer.MAX_VALUE, 1))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("integer overflow");
    }

    @Test
    @DisplayName("浮点天生不精确：0.1 + 0.2 不是 0.3")
    void floatingPointIsNotExact() {
        assertThat(0.1 + 0.2).isNotEqualTo(0.3);
        assertThat(0.1 + 0.2).isEqualTo(0.30000000000000004);
    }

    @Test
    @DisplayName("算钱用 BigDecimal，而且要用字符串那个构造器")
    void bigDecimalGivesTheExpectedAnswer() {
        assertThat(new BigDecimal("0.1").add(new BigDecimal("0.2")))
                .isEqualByComparingTo("0.3");
        // 用 double 那个构造器，二进制里那点误差会被原样带进来
        assertThat(new BigDecimal(0.1).toString())
                .startsWith("0.100000000000000");
    }

    @Test
    @DisplayName("可交换可结合才敢说「数字相等」：浮点数的坑也在这里")
    void floatingPointEqualityNeedsATolerance() {
        // 工程上的做法：比差值，不比相等
        double diff = Math.abs(0.1 + 0.2 - 0.3);
        assertThat(diff).isLessThan(1e-9);
    }

    @Test
    @DisplayName("两个字面量共享同一个 String 对象")
    void twoLiteralsAreTheSameObject() {
        assertThat(VariablesDemo.twoLiteralsAreTheSameObject()).isTrue();
    }

    @Test
    @DisplayName("new 出来的 String 内容相同，但不是同一个对象")
    void newStringIsADifferentObject() {
        assertThat(VariablesDemo.literalEqualsNewString()).isFalse();
        assertThat(VariablesDemo.literalEqualsNewStringByContent()).isTrue();
    }

    @Test
    @DisplayName("装地址的变量：两个引用指向同一个数组，改一个另一个看得见")
    void referenceVariablesShareTheArray() {
        int[] original = {1, 2, 3};
        int[] alias = original;
        alias[0] = 99;

        assertThat(alias).isSameAs(original);
        assertThat(original[0]).isEqualTo(99);
    }

    @Test
    @DisplayName("final 锁的是引用，不是对象内容")
    void finalLocksTheReferenceNotTheContent() {
        final int[] locked = {1, 2, 3};
        locked[0] = 99;                       // 编译得过：引用没变，变的是内容

        assertThat(locked[0]).isEqualTo(99);
        // locked = new int[3];               // 这一行才编译不过
    }

    @Test
    @DisplayName("装箱的小整数（-128 ~ 127）会被缓存复用")
    void smallIntegersAreCached() {
        assertThat(VariablesDemo.smallIntegersAreCached()).isTrue();
    }

    @Test
    @DisplayName("超出缓存范围的装箱对象每次都是新的，== 会失败")
    void bigIntegersAreNotCached() {
        assertThat(VariablesDemo.bigIntegersAreNotCached()).isFalse();
        assertThat(VariablesDemo.bigIntegersAreEqualByContent()).isTrue();
    }

    @Test
    @DisplayName("Integer 拆箱成 null 会炸 NPE，这是自动装箱最阴的坑")
    void unboxingNullThrowsNpe() {
        Integer boxed = null;

        assertThatThrownBy(() -> {
            int unboxed = boxed;   // 自动拆箱
        }).isInstanceOf(NullPointerException.class);
    }
}
