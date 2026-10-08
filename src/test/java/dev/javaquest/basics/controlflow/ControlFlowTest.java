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
 * <p>分三组：<b>选择</b>、<b>重复</b>、<b>跳转</b>。这三件事就是改变执行顺序的全部手段。
 */
@DisplayName("02 流程控制")
class ControlFlowTest {

    @Nested
    @DisplayName("选择：条件怎么决定走哪条路")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Choosing {

        @Test
        @Order(1)
        @DisplayName("&& 左边为 false 时，右边不执行")
        void andSkipsTheRightSide() {
            assertThat(ControlFlowDemo.andSkipsTheRightSide())
                    .as("右边会抛 AssertionError。能跑完就说明 && 短路生效")
                    .isFalse();
        }

        @Test
        @Order(2)
        @DisplayName("|| 左边为 true 时，右边不执行")
        void orSkipsTheRightSide() {
            assertThat(ControlFlowDemo.orSkipsTheRightSide())
                    .as("这是 null 安全检查的写法：左边已有答案，就不去碰右边那个可能为 null 的对象")
                    .isTrue();
        }

        @Test
        @Order(3)
        @DisplayName("非短路的 | 会让两边都执行，所以副作用会发生两次")
        void bitwiseOrAlwaysEvaluatesBothSides() {
            assertThat(ControlFlowDemo.orShortCircuitsIncrement())
                    .as("|| 左边已经是 true，右边被跳过，i 只加了 1 次")
                    .isEqualTo(4);

            assertThat(ControlFlowDemo.bitwiseOrAlwaysIncrements())
                    .as("换成单个 | ，右边一定执行，i 加了 2 次。结果都是 true，但副作用次数不同")
                    .isEqualTo(5);
        }

        @Test
        @Order(4)
        @DisplayName("传统 switch 漏写 break 会一路往下走")
        void classicSwitchFallsThroughWhenBreakIsMissing() {
            assertThat(ControlFlowDemo.classicSwitchFallsThrough())
                    .as("case 4 本该只加 4，但因为漏了 break 又加了 3，得到 7")
                    .isEqualTo(7);
        }

        @Test
        @Order(5)
        @DisplayName("switch 表达式处理 int 时必须写 default")
        void switchExpressionOnIntNeedsDefault() {
            assertThat(ControlFlowDemo.gradeToLabel(4)).isEqualTo("优秀");
            assertThat(ControlFlowDemo.gradeToLabel(1))
                    .as("int 有无穷多个取值，编译器无法确认你覆盖完了，所以 default 不能省")
                    .isEqualTo("待改进");
        }

        @Test
        @Order(6)
        @DisplayName("switch 表达式处理枚举时不需要 default")
        void switchExpressionOnEnumIsExhaustive() {
            assertThat(ControlFlowDemo.dayTypeToLabel(ControlFlowDemo.DayOfWeek.MONDAY)).isEqualTo("工作日");
            assertThat(ControlFlowDemo.dayTypeToLabel(ControlFlowDemo.DayOfWeek.SATURDAY)).isEqualTo("休息日");
            assertThat(ControlFlowDemo.dayTypeToLabel(ControlFlowDemo.DayOfWeek.SUNDAY))
                    .as("枚举的取值是封闭的，编译器能确认你覆盖完了。删掉某个分支它就会报错")
                    .isEqualTo("休息日");
        }

        @Test
        @Order(7)
        @DisplayName("三目运算符两个分支类型不同时会提升到更宽的那个")
        void ternaryPromotesToTheWiderType() {
            long value = ControlFlowDemo.ternaryPromotesToTheWiderType(true);

            assertThat(value)
                    .as("int 分支和 long 分支混在一起，整条表达式的结果类型是 long")
                    .isEqualTo(1L);

            assertThat(ControlFlowDemo.ternaryRequiresExplicitNarrowing(true))
                    .as("结果要变窄时必须显式转换，否则 long 塞不进 int")
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("重复：同一段代码怎么跑多次")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Repeating {

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
        @DisplayName("while 和 do-while 的区别只在「什么时候判断」")
        void whileAndDoWhileDifferOnWhenTheyCheck() {
            assertThat(ControlFlowDemo.whileRunsZeroTimesWhenConditionIsFalse())
                    .as("while 先判断后执行，条件一开始就假，循环体一次都不进")
                    .isZero();

            assertThat(ControlFlowDemo.doWhileRunsOnceWhenConditionIsFalse())
                    .as("do-while 先执行后判断，所以哪怕条件为假也一定会跑一次")
                    .isEqualTo(1);
        }

        @Test
        @Order(3)
        @DisplayName("增强 for 里的变量是副本，改它不会改回集合")
        void enhancedForCannotWriteBack() {
            List<Integer> numbers = ControlFlowDemo.enhancedForCannotWriteBack();

            assertThat(numbers)
                    .as("循环里 x 被乘了 10，但那是局部变量。集合里的元素一个都没变")
                    .containsExactly(1, 2, 3);
        }

        @Test
        @Order(4)
        @DisplayName("在增强 for 里删元素会抛 ConcurrentModificationException")
        void removingWhileIteratingThrows() {
            assertThat(ControlFlowDemo.removingWhileIterating())
                    .as("增强 for 底层是 Iterator，迭代期间改集合结构会被检测到并抛异常")
                    .isEqualTo("ConcurrentModificationException");
        }

        @Test
        @Order(5)
        @DisplayName("remove(int) 按下标删，remove(Object) 按值删")
        void removeHasTwoOverloadsAndIntPicksTheIndexOne() {
            List<Integer> result = ControlFlowDemo.removeByIndexVersusByValue();

            assertThat(result)
                    .as("第一段 [1,2,3,4].remove(2) 删掉的是下标 2（值 3），剩 [1,2,4]")
                    .startsWith(1, 2, 4)
                    .contains(-1);

            assertThat(result)
                    .as("第二段删掉值为 2 的元素，剩 [1,3,4]。传 int 会选到 remove(int) 那个重载")
                    .endsWith(1, 3, 4);
        }

        @Test
        @Order(6)
        @DisplayName("要删元素请用 removeIf")
        void removeIfIsTheSafeWayToDelete() {
            List<Integer> numbers = ControlFlowDemo.removeEvenSafely();

            assertThat(numbers)
                    .as("removeIf 不违反迭代期间不改结构的约定，所以能安全删掉 2、4、6")
                    .containsExactly(1, 3, 5);
        }
    }

    @Nested
    @DisplayName("跳转：怎么提前离开当前的位置")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Jumping {

        @Test
        @Order(1)
        @DisplayName("continue 只跳过本次，循环继续")
        void continueSkipsOnlyTheCurrentIteration() {
            assertThat(ControlFlowDemo.countAfterSkippingMultiplesOfFive())
                    .as("1 到 10 里 5 和 10 被 continue 跳过，剩 8 次")
                    .isEqualTo(8);
        }

        @Test
        @Order(2)
        @DisplayName("break 只能跳出最内层那一层")
        void plainBreakExitsOnlyTheInnermostLoop() {
            assertThat(ControlFlowDemo.plainBreakVisits())
                    .as("每次外层循环进到 j=2 就 break，只跑了 j=0 和 j=1。三轮外层一共 6 次")
                    .isEqualTo(6);
        }

        @Test
        @Order(3)
        @DisplayName("标签能让 break 一次跳出多层")
        void labeledBreakExitsTheOuterLoop() {
            assertThat(ControlFlowDemo.labeledBreakVisits())
                    .as("直接结束整个外层循环，所以只跑了第一轮外层的两次内层")
                    .isEqualTo(2);
        }
    }
}
