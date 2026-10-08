package dev.javaquest.basics.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 01 章的练习题。
 *
 * <p>用法：把方法上的 {@code @Disabled} 删掉，重新运行，就能看到答案跑起来。
 * {@code @Disabled} 里写着答案，不看答案先自己写。
 *
 * <p>本类同样被 {@code docs/01-变量与类型.md} 覆盖，所以
 * {@link dev.javaquest.meta.DocumentationSyncTest} 会检查这里的每条答案都在文档里被点名。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("01 练习题：变量与类型")
class VariableExercisesTest {

    @Test
    @Order(1)
    @Disabled("答案：-1。Java 里 % 的符号跟被除数走，所以 -7 % 2 是 -1，不是 1。")
    @DisplayName("练习 1：-7 % 2 等于几？")
    void exerciseModuloOfNegativeNumbers() {
        assertThat(-7 % 2).isEqualTo(-1);
    }

    @Test
    @Order(2)
    @Disabled("答案：写 n % 2 == 0。因为 -4 % 2 等于 0，所以「等于 1 才算偶数」这种判断对负数是错的。")
    @DisplayName("练习 2：写一个判断负数是否偶数、不会出错的表达式")
    void exerciseEvenCheckThatWorksForNegatives() {
        assertThat(-4 % 2 == 0).as("负偶数也是偶数").isTrue();
    }

    @Test
    @Order(3)
    @Disabled("答案：不能直接用 ==。要么给一个容差，要么改用 BigDecimal。这里用 AssertJ 的 isCloseTo。")
    @DisplayName("练习 3：怎么正确断言 0.1 + 0.2 等于 0.3？")
    void exerciseComparingFloatingPoint() {
        assertThat(0.1 + 0.2).isCloseTo(0.3, within(1e-9));
    }

    @Test
    @Order(4)
    @Disabled("答案：用 equals()。== 比的是两个对象是不是同一个，equals() 比的才是内容。")
    @DisplayName("练习 4：两个 new 出来的 \"java\" 怎么判断内容相等？")
    void exerciseComparingTwoBuiltStrings() {
        assertThat(new String("java")).isEqualTo(new String("java"));
        assertThat(new String("java") == new String("java")).as("两个新对象，地址当然不同").isFalse();
    }

    @Test
    @Order(5)
    @Disabled("答案：2147483648。先转成 long 再加，运算全程在 long 的范围里，不会回绕。")
    @DisplayName("练习 5：怎么算出 Integer.MAX_VALUE + 1 的真实值而不是 -2147483648？")
    void exerciseAvoidingOverflowByWideningFirst() {
        assertThat((long) Integer.MAX_VALUE + 1).isEqualTo(2_147_483_648L);
    }

    @Test
    @Order(6)
    @Disabled("答案：'a' - 'A' 等于 32。ASCII 里大写字母排在小写前面，所以大写减小写是负数。")
    @DisplayName("练习 6：'A' - 'a' 等于几？'a' - 'A' 呢？")
    void exerciseCharacterSubtractionOrderMatters() {
        assertThat('a' - 'A').isEqualTo(32);
        assertThat('A' - 'a').isEqualTo(-32);
    }
}
