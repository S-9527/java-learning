package dev.javaquest.basics.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 02 章的练习题。
 *
 * <p>用法：把方法上的 {@code @Disabled} 删掉，重新运行，就能看到答案跑起来。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("02 练习题：流程控制")
class ControlFlowExercisesTest {

    @Test
    @Order(1)
    @Disabled("答案：7。case 4 加 4，没有 break 就继续往下走到 case 3 又加 3。要只加 4 就得补 break。")
    @DisplayName("练习 1：一个 case 值为 4 的传统 switch，漏写 break 会算出什么？")
    void exerciseSwitchFallThroughResult() {
        int grade = 4;
        int sum = 0;
        switch (grade) {
            case 4:
                sum += 4;
            case 3:
                sum += 3;
                break;
            default:
                sum += 0;
        }
        assertThat(sum).isEqualTo(7);
    }

    @Test
    @Order(2)
    @Disabled("答案：0 次。while 先判断再执行，5 < 3 为假，循环体一次都没进去。换成 do-while 就会跑 1 次。")
    @DisplayName("练习 2：while 循环在条件一开始就为 false 时，循环体跑几次？")
    void exerciseWhileRunsZeroTimes() {
        int count = 0;
        int i = 5;
        while (i < 3) {
            count++;
        }
        assertThat(count).isZero();
    }

    @Test
    @Order(3)
    @Disabled("答案：2。两层循环，j 一到 2 就 break outer，所以外层也一起结束，只留下第一轮的两个组合。")
    @DisplayName("练习 3：两层循环，内层在 j==2 时 break 外层，一共访问多少次？")
    void exerciseLabeledBreakVisits() {
        int visited = 0;
        outer:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 2) {
                    break outer;
                }
                visited++;
            }
        }
        assertThat(visited).isEqualTo(2);
    }

    @Test
    @Order(4)
    @Disabled("答案：1、3、5。增强 for 改的是局部变量，改不回集合。要删元素得用 list.removeIf(...) 或者按下标遍历。")
    @DisplayName("练习 4：在增强 for 里把每个元素乘 10，原列表会变吗？")
    void exerciseEnhancedForCannotWriteBack() {
        var numbers = new java.util.ArrayList<>(java.util.List.of(1, 2, 3));
        for (Integer x : numbers) {
            x = x * 10;
        }
        assertThat(numbers).containsExactly(1, 2, 3);
    }
}
