package dev.javalearning.basics.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 03 课的断言。
 *
 * <p>每一条对应 {@code docs/03-方法.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*methods*'}
 */
class MethodsTest {

    // ── 1. 值传递 ────────────────────────────────────────────────────

    @Test
    @DisplayName("方法里给参数重新赋值，调用方的变量毫无变化——这就是值传递")
    void changingTheParameterDoesNotChangeTheArgument() {
        int x = MethodsDemo.PASS_BY_VALUE_X;
        MethodsDemo.changePrimitive(x);
        assertThat(x).isEqualTo(10);
    }

    @Test
    @DisplayName("方法里改数组内容，调用方看得见——因为拿到的是同一份数据的钥匙")
    void mutatingTheArrayIsVisibleToTheCaller() {
        int[] array = {1, 2, 3};
        MethodsDemo.changeArray(array);
        assertThat(array).containsExactly(99, 2, 3);
    }

    @Test
    @DisplayName("但把参数重新指向别处，调用方还是看不见——钥匙换了，房间没换")
    void reassigningTheParameterIsNotVisibleToTheCaller() {
        int[] array = {1, 2, 3};
        MethodsDemo.reassignArray(array);
        assertThat(array).containsExactly(1, 2, 3);
    }

    // ── 2. 重载 ────────────────────────────────────────────────────

    @Test
    @DisplayName("重载只看参数列表，跟返回值类型无关")
    void overloadResolutionDependsOnlyOnParameters() {
        assertThat(MethodsDemo.overload(1)).isEqualTo("一个 int：1");
        assertThat(MethodsDemo.overload(1, 2)).isEqualTo("两个 int：1,2");
        assertThat(MethodsDemo.overload("ab")).isEqualTo("一个 String：ab");
    }

    @Test
    @DisplayName("重载的「同名不同参」不包括只有返回值不同的情况，那是编译不过的")
    void returnTypeAloneDoesNotMakeAnOverload() {
        // 下面两个方法签名在编译器眼里是同一个，只能留一个。
        // 这类错误没法用运行时断言证明——它连编译都过不去。
        // 真正的证据在 broken/ReturnTypeOnly.java 的真实编译输出里。
        assertThat(MethodsDemo.overload(1)).isEqualTo("一个 int：1");
    }

    // ── 3. 可变参数 ────────────────────────────────────────────────

    @Test
    @DisplayName("可变参数可以一个都不传，此时拿到的是个空数组")
    void varargsCanBeCalledWithNoArguments() {
        assertThat(MethodsDemo.sum()).isZero();
    }

    @Test
    @DisplayName("可变参数传几个都行")
    void varargsAcceptsAnyNumberOfArguments() {
        assertThat(MethodsDemo.sum(1)).isEqualTo(1);
        assertThat(MethodsDemo.sum(1, 2, 3)).isEqualTo(6);
    }

    @Test
    @DisplayName("可变参数本质上就是数组，所以可以直接把数组传进去")
    void varargsIsJustAnArrayUnderneath() {
        int[] already = {4, 5};
        assertThat(MethodsDemo.sum(already)).isEqualTo(9);
    }

    @Test
    @DisplayName("重载解析：一个 int 优先匹配单个 Integer，而不是可变参数那个")
    void boxingPicksSingleArgumentOverVarargs() {
        assertThat(MethodsDemo.pickWithBoxing()).isEqualTo("单个 Integer");
    }

    // ── 4. return ──────────────────────────────────────────────────

    @Test
    @DisplayName("return 不只是给值，也可以提前结束方法")
    void returnCanExitEarly() {
        assertThat(MethodsDemo.grade(91)).isEqualTo("优秀");
        assertThat(MethodsDemo.grade(75)).isEqualTo("及格");
        assertThat(MethodsDemo.grade(-1)).isEqualTo("成绩不合法");
    }

    // ── 5. 交出控制权 ────────────────────────────────────────────────

    @Test
    @DisplayName("传可变对象进去，方法能把调用方的数据改掉——控制权交出去了")
    void passingMutableObjectGivesAwayControl() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c"));
        MethodsDemo.clearList(list);
        assertThat(list).isEmpty();
        assertThat(MethodsDemo.callerListSize()).isZero();
    }

    @Test
    @DisplayName("想防这个，传不可变副本进去：List.copyOf / Arrays.copyOf")
    void defensiveCopyProtectsTheCaller() {
        List<String> original = List.of("a", "b", "c");
        List<String> copy = List.copyOf(original);
        MethodsDemo.clearList(new ArrayList<>(copy));
        assertThat(original).containsExactly("a", "b", "c");
        assertThat(Arrays.toString(new int[] {1})).isEqualTo("[1]");
    }

    // ── 6. 作用域 ────────────────────────────────────────────────────

    @Test
    @DisplayName("局部变量的作用域到花括号结束，出了花括号就不存在")
    void localScopeEndsWithTheBlock() {
        {
            int inner = 1;
            assertThat(inner).isEqualTo(1);
        }
        // 这里再写一个 inner 就编译不过——这正是 scopeDemo() 想说明的事，
        // 反面教材在 broken/OutOfScope.java，里面有真实的编译错误。
        MethodsDemo.scopeDemo();
    }
}
