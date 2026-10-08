package dev.javaquest.basics.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 01 章：变量与类型。
 *
 * <p>这个类本身就是文档。方法名是英文（方便 IDE 跳转和搜索），
 * 中文句子放在 {@code @DisplayName} 里，在测试报告和控制台上看得到。
 *
 * <p>每条断言都会用 {@code .as("...")} 再说明一次「如果它失败了，说明什么」。
 * 这样断言失败时，报错信息本身就是一段解释。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("01 变量与类型")
class VariableTest {

    @Test
    @Order(1)
    @DisplayName("整数除法直接丢掉余数，不做四舍五入")
    void integerDivisionDropsTheRemainder() {
        assertThat(VariableDemo.divide7By2())
                .as("7 / 2 得到 3 而不是 3.5：Java 不猜你的意图，整数除以整数就是整数")
                .isEqualTo(3);
    }

    @Test
    @Order(2)
    @DisplayName("负数除法是向零截断，不是向下取整")
    void negativeDivisionTruncatesTowardZero() {
        assertThat(VariableDemo.divideNegative7By2())
                .as("-7 / 2 得到 -3 而不是 -4。这是 Java 和 Python 最容易被坑的差别")
                .isEqualTo(-3);
    }

    @Test
    @Order(3)
    @DisplayName("取模结果的符号跟被除数走，不跟除数走")
    void moduloSignFollowsTheDividend() {
        assertThat(VariableDemo.moduloNegative7By2())
                .as("-7 % 2 是 -1 而不是 1。所以「负数是不是偶数」要写 n % 2 == 0 而不是 == 1")
                .isEqualTo(-1);
    }

    @Test
    @Order(4)
    @DisplayName("int 溢出会静默回绕成最小值，不报错")
    void integerOverflowWrapsAroundSilently() {
        assertThat(VariableDemo.addOneToMaxInt())
                .as("最大值加一不会抛异常，而是变成最小值。这是 Java 里最危险的一类 bug")
                .isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @Order(5)
    @DisplayName("两个 byte 相加自动提升成 int，不会挤回 byte")
    void byteAdditionPromotesToInt() {
        assertThat(VariableDemo.addTwoBytes((byte) 100, (byte) 100))
                .as("byte + byte 的结果类型是 int。如果硬塞回 byte 必须显式转换，编译器会拦你")
                .isEqualTo(200);
    }

    @Test
    @Order(6)
    @DisplayName("浮点数天生不精确，0.1 + 0.2 不等于 0.3")
    void floatingPointIsNotExact() {
        double sum = VariableDemo.addPointOneAndPointTwo();

        assertThat(sum)
                .as("0.1 + 0.2 在二进制里无法精确表示，结果是 0.30000000000000004")
                .isNotEqualTo(0.3);

        assertThat(sum)
                .as("正确的比较方式是给一个容差，而不是用 ==")
                .isCloseTo(0.3, withPrecision(1e-9));
    }

    @Test
    @Order(7)
    @DisplayName("两个完全一样的字符串字面量共享同一个对象")
    void stringLiteralsShareOneObject() {
        assertThat(VariableDemo.twoLiteralsAreTheSameObject())
                .as("字面量存在常量池里，长得一样就复用同一个对象。所以 == 对字面量能凑巧对")
                .isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("手工 new 出来的字符串内容和字面量相同，但不是同一个对象")
    void freshlyBuiltStringIsNotTheSameObject() {
        String literal = "java";
        String built = new String("java");

        assertThat(built).as("内容当然相同").isEqualTo(literal);
        assertThat(built == literal)
                .as("但 == 比的是地址。== 只能用于字面量比较，判断内容必须用 equals")
                .isFalse();
    }

    @Test
    @Order(9)
    @DisplayName("String 不可变：拼接不会修改左边的对象")
    void concatenationLeavesLeftOperandUntouched() {
        assertThat(VariableDemo.concatenationChangesTheLeftOperand())
                .as("s + \"c\" 产生了一个新对象，s 自己一个字都没变")
                .isFalse();

        assertThat(VariableDemo.leftOperandAfterConcatenation())
                .as("验证 s 还是原来的 ab，没有变成 abc")
                .isEqualTo("ab");
    }

    @Test
    @Order(10)
    @DisplayName("final 锁的是引用，不是对象内容")
    void finalLocksTheReferenceNotTheObject() {
        assertThat(VariableDemo.finalReferenceBlocksAddingToList())
                .as("final List 不能重新指向别的 List，但里面的元素随便加")
                .isFalse();
    }

    @Test
    @Order(11)
    @DisplayName("char 背后就是一个 16 位无符号整数")
    void charIsAnUnsignedNumber() {
        assertThat(VariableDemo.numericValueOfCharA())
                .as("char 可以直接当 int 用，'A' 就是 65")
                .isEqualTo(65);

        assertThat(VariableDemo.nextCharAfterA())
                .as("字符能做算术，但结果类型是 int，塞回 char 必须显式转换")
                .isEqualTo('B');
    }

    @Test
    @Order(12)
    @DisplayName("装箱后的 Integer 在 -128 到 127 之间会复用同一个对象")
    void boxedIntegerIdentityIsCachedUpTo127() {
        assertThat(VariableDemo.boxed127IsTheSameObjectAsAnotherBoxed127())
                .as("小整数有缓存池，所以 == 凑巧能通过。这是巧合，不是规则")
                .isTrue();
    }

    @Test
    @Order(13)
    @DisplayName("超出缓存范围的装箱对象每次都是新的，== 会失败")
    void boxedIntegerIdentityIsNotCachedAbove127() {
        assertThat(VariableDemo.boxed200IsTheSameObjectAsAnotherBoxed200())
                .as("200 超出缓存范围，两次装箱是两个对象。所以包装类型永远用 equals 比较")
                .isFalse();
    }
}
