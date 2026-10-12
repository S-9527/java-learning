package dev.javalearning.basics.controlflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 02 课的断言。
 *
 * <p>每一条对应 {@code docs/02-运算符与流程控制.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*controlflow*'}
 */
class ControlFlowTest {

    // ── 1. 短路 ────────────────────────────────────────────────────

    @Test
    @DisplayName("&& 左边已经 false，右边压根不跑——这就是短路")
    void andShortCircuitsWhenLeftIsFalse() {
        // s 是 null，但右边从没被执行，所以不炸
        assertThat(ControlFlowDemo.shortCircuitAnd()).isFalse();
    }

    @Test
    @DisplayName("右侧表达式被求值的次数是 0，用计数器证明它真被跳过了")
    void rightSideRunsZeroTimes() {
        assertThat(ControlFlowDemo.timesRightSideRan()).isZero();
    }

    @Test
    @DisplayName("把 && 换成 &，两边都求值，于是 NullPointerException")
    void singleAmpersandEvaluatesBothSides() {
        assertThat(ControlFlowDemo.withoutShortCircuit())
                .startsWith("NullPointerException");
    }

    @Test
    @DisplayName("|| 左边已经 true，右边也不跑")
    void orShortCircuitsWhenLeftIsTrue() {
        assertThat(ControlFlowDemo.shortCircuitOr()).isTrue();
    }

    // ── 3. switch 表达式 ───────────────────────────────────────────

    @Test
    @DisplayName("switch 表达式每个分支都要返回值，漏一个编译器就报错")
    void switchExpressionCoversEveryBranch() {
        assertThat(ControlFlowDemo.describe(ControlFlowDemo.Direction.MAP)).isEqualTo("看地图");
        assertThat(ControlFlowDemo.describe(ControlFlowDemo.Direction.SOUTH)).isEqualTo("向下");
    }

    // ── 4. 三种循环 ────────────────────────────────────────────────

    @Test
    @DisplayName("while 先判断后执行，条件一开始就是 false 时一次都不跑")
    void whileMayRunZeroTimes() {
        assertThat(ControlFlowDemo.countWithWhile()).isZero();
    }

    @Test
    @DisplayName("do-while 先执行后判断，同样的条件下至少跑一次")
    void doWhileRunsAtLeastOnce() {
        assertThat(ControlFlowDemo.countWithDoWhile()).isEqualTo(1);
    }

    // ── 5. 迭代中删元素 ─────────────────────────────────────────────

    @Test
    @DisplayName("删中间元素后刚好走完，侥幸没炸——但这是运气，不是安全")
    void removingTheMiddleElementGetsLucky() {
        assertThat(ControlFlowDemo.removeMiddleWithForEach())
                .startsWith("没炸")
                .contains("[a, c]");
    }

    @Test
    @DisplayName("删第一个元素就炸，因为后面还有 next() 要跑")
    void removingTheFirstElementThrows() {
        assertThat(ControlFlowDemo.removeFirstWithForEach())
                .isEqualTo("ConcurrentModificationException");
    }

    @Test
    @DisplayName("每个元素都删也炸：每次删除后面都还有下一次 next()")
    void removingEveryElementThrows() {
        assertThat(ControlFlowDemo.removeEveryElementWithForEach())
                .isEqualTo("ConcurrentModificationException");
    }

    @Test
    @DisplayName("正确做法一：用迭代器自己的 remove")
    void iteratorRemoveIsTheFix() {
        assertThat(ControlFlowDemo.removeWithIterator()).isEqualTo("a,c");
    }

    @Test
    @DisplayName("正确做法二：removeIf，一行搞定")
    void removeIfIsEvenShorter() {
        assertThat(ControlFlowDemo.removeWithRemoveIf()).isEqualTo("a,c");
    }

    // ── 6. break / continue / 标签 ──────────────────────────────────

    @Test
    @DisplayName("不带标签的 break 只跳出当前这一层循环")
    void plainBreakLeavesOnlyInnerLoop() {
        // i 会走 0、1、2，说明外层循环没被打破
        assertThat(ControlFlowDemo.countPlainBreakOuterIterations()).isEqualTo(3);
    }

    @Test
    @DisplayName("带标签的 break 直接跳出外层，外层只走了一轮")
    void labeledBreakLeavesBothLoops() {
        assertThat(ControlFlowDemo.countLabeledBreakOuterIterations()).isEqualTo(1);
    }

    @Test
    @DisplayName("continue 只跳过这一次迭代，循环本身还在继续")
    void continueSkipsOnlyThisRound() {
        assertThat(ControlFlowDemo.continueSum()).isEqualTo(1 + 3 + 5);
    }

    // ── 一个容易被忽略的细节 ────────────────────────────────────────

    @Test
    @DisplayName("switch 里给 case 同时列多个值，要用逗号分隔且不能 fall through")
    void multipleCaseLabels() {
        assertThat(ControlFlowDemo.describeGroup(2)).isEqualTo("小");
        assertThat(ControlFlowDemo.describeGroup(5)).isEqualTo("大");
    }

    @Test
    @DisplayName("switch 表达式必须穷尽所有情况，加 default 或列全枚举")
    void switchExpressionNeedsToBeExhaustive() {
        assertThatThrownBy(() -> ControlFlowDemo.describe(null))
                .isInstanceOf(NullPointerException.class);
    }
}
