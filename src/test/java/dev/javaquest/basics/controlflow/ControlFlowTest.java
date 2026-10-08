package dev.javaquest.basics.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 02 章：流程控制。
 *
 * <p>这个类本身就是文档。方法名是英文（方便 IDE 跳转和搜索），
 * 中文句子放在 {@code @DisplayName} 里。
 *
 * <p>每个 {@code @Nested} 内部分组对应文档里的一节，测试报告里能直接看出层级。
 */
@DisplayName("02 流程控制")
class ControlFlowTest {

    @Nested
    @DisplayName("短路求值")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ShortCircuit {

        @Test
        @Order(1)
        @DisplayName("&& 左边为 false 时，右边根本不会执行")
        void andDoesNotEvaluateTheRightSide() {
            assertThat(ControlFlowDemo.andShortCircuits())
                    .as("如果右半边被求值，这里会抛 AssertionError。能跑通就说明短路生效了")
                    .isFalse();
        }

        @Test
        @Order(2)
        @DisplayName("|| 左边为 true 时，右边不会执行")
        void orDoesNotEvaluateTheRightSide() {
            assertThat(ControlFlowDemo.orShortCircuits())
                    .as("这是 null 安全检查的写法：左边已经确定答案，就不去碰右边那个可能为 null 的对象")
                    .isTrue();
        }

        @Test
        @Order(3)
        @DisplayName("先判断非空再取值，是 Java 里最常见的短路用法")
        void nullCheckGuardsTheDereference() {
            assertThat(ControlFlowDemo.lengthOfMaybeNull(null))
                    .as("传 null 返回 -1，而不是抛 NullPointerException")
                    .isEqualTo(-1);

            assertThat(ControlFlowDemo.lengthOfMaybeNull("java"))
                    .as("传正常字符串返回长度")
                    .isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("switch")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Switches {

        @Test
        @Order(1)
        @DisplayName("传统 switch 漏写 break 会静默串到下一个 case")
        void classicSwitchFallsThroughWhenBreakIsMissing() {
            assertThat(ControlFlowDemo.classicSwitchFallsThrough())
                    .as("case 4 本该只加 4，但因为漏了 break 又加了 3，得到 7。这类 bug 编译期不报错")
                    .isEqualTo(7);
        }

        @Test
        @Order(2)
        @DisplayName("switch 表达式每个分支返回值，漏写会编译报错")
        void switchExpressionReturnsDirectly() {
            assertThat(ControlFlowDemo.gradeToLabel(4)).isEqualTo("优秀");
            assertThat(ControlFlowDemo.gradeToLabel(3)).isEqualTo("良好");
            assertThat(ControlFlowDemo.gradeToLabel(2)).isEqualTo("及格");
            assertThat(ControlFlowDemo.gradeToLabel(1))
                    .as("其他值走 default 分支")
                    .isEqualTo("待改进");
        }

        @Test
        @Order(3)
        @DisplayName("switch 表达式对 enum 做穷尽匹配，不用写 default")
        void switchExpressionIsExhaustiveOverEnum() {
            assertThat(ControlFlowDemo.classifySign(-42))
                    .as("Signum.NEGATIVE 对应 -1")
                    .isEqualTo(-1);
            assertThat(ControlFlowDemo.classifySign(0)).isZero();
            assertThat(ControlFlowDemo.classifySign(42)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("循环")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Loops {

        @Test
        @Order(1)
        @DisplayName("for 的三段式：初始化、条件、更新")
        void forLoopHasThreeParts() {
            assertThat(ControlFlowDemo.sumWithFor(5))
                    .as("0+1+2+3+4 等于 10")
                    .isEqualTo(10);
            assertThat(ControlFlowDemo.sumWithFor(0)).as("n 为 0 时循环一次都不进").isZero();
        }

        @Test
        @Order(2)
        @DisplayName("条件为 false 时 while 一次都不跑，do-while 至少跑一次")
        void whileAndDoWhileDifferOnTheFirstCheck() {
            assertThat(ControlFlowDemo.whileRunsHowManyTimesIfConditionIsFalse())
                    .as("while 先判断后执行，条件一开始就假，循环体一次都不进")
                    .isZero();

            assertThat(ControlFlowDemo.doWhileRunsHowManyTimesIfConditionIsFalse())
                    .as("do-while 先执行后判断，所以哪怕条件为假也一定会跑一次")
                    .isEqualTo(1);
        }

        @Test
        @Order(3)
        @DisplayName("continue 只跳过本次剩余部分，不终止整个循环")
        void continueSkipsOnlyTheCurrentIteration() {
            assertThat(ControlFlowDemo.countBreaksAndContinues(10))
                    .as("1 到 10 里 5 和 10 被 continue 跳过，剩 8 次")
                    .isEqualTo(8);
            assertThat(ControlFlowDemo.countBreaksAndContinues(4))
                    .as("1 到 4 没有 5 的倍数，一次都不跳过")
                    .isEqualTo(4);
        }

        @Test
        @Order(4)
        @DisplayName("break 只跳出最内层那一层循环")
        void breakExitsOnlyTheInnermostLoop() {
            assertThat(ControlFlowDemo.breakOnlyExitsTheInnermostLoop())
                    .as("每次外层循环进到 j=2 就 break，只跑了 j=0 和 j=1。三次外层一共 6 次")
                    .isEqualTo(6);
        }

        @Test
        @Order(5)
        @DisplayName("给循环打标签后，break 标签能一次跳出外层")
        void labeledBreakExitsTheOuterLoop() {
            assertThat(ControlFlowDemo.labeledBreakExitsTheOuterLoop())
                    .as("直接结束整个外层循环，所以只跑了第一轮外层的两次内层")
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("增强 for 与集合")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class EnhancedFor {

        @Test
        @Order(1)
        @DisplayName("增强 for 里的变量是副本，改它不会改回集合")
        void enhancedForCannotWriteBack() {
            List<Integer> numbers = ControlFlowDemo.enhancedForCannotReplaceElements();

            assertThat(numbers)
                    .as("循环里 x 被乘了 10，但那是局部变量。集合里的元素一个都没变")
                    .containsExactly(1, 2, 3);
        }

        @Test
        @Order(2)
        @DisplayName("想在遍历中删元素，用 removeIf 而不是增强 for")
        void removeIfIsTheSafeWayToDelete() {
            List<Integer> numbers = ControlFlowDemo.removeEvenSafely();

            assertThat(numbers)
                    .as("删掉 2、4、6，剩下奇数。直接在增强 for 里 remove 会抛 ConcurrentModificationException")
                    .containsExactly(1, 3, 5);
        }
    }

    @Nested
    @DisplayName("三目运算符")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Ternary {

        @Test
        @Order(1)
        @DisplayName("两个分支类型不同时，自动提升到更宽的那个")
        void ternaryPromotesToTheWiderType() {
            long value = ControlFlowDemo.ternaryPicksTheWiderType(true);

            assertThat(value)
                    .as("int 分支和 long 分支混在一起，整条表达式的结果类型是 long")
                    .isEqualTo(1L);
        }

        @Test
        @Order(2)
        @DisplayName("结果要变窄时，必须显式转换")
        void ternaryRequiresAnExplicitCastToNarrow() {
            assertThat(ControlFlowDemo.ternaryRequiresExplicitNarrowing(true))
                    .as("没有 (int) 这一步就编译不过：long 不能隐式缩回 int")
                    .isEqualTo(1);
        }
    }
}
