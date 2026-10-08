package dev.javaquest.basics.controlflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 第 02 章的示例代码。
 *
 * <p>每个方法只证明一件事。中文解释在测试类的 {@code @DisplayName} 里。
 */
public final class ControlFlowDemo {

    private ControlFlowDemo() {
    }

    // ── if / 短路 ────────────────────────────────────────────────────

    /** 条件为 false 时，&& 后面的代码还跑不跑。返回 true 表示跑了。 */
    public static boolean andShortCircuits() {
        return false && wouldThrowIfEvaluated();
    }

    /** 条件为 true 时，|| 后面的代码还跑不跑。返回 true 表示跑了。 */
    public static boolean orShortCircuits() {
        return true || wouldThrowIfEvaluated();
    }

    /** 故意制造一个异常，用来检测短路有没有生效。 */
    private static boolean wouldThrowIfEvaluated() {
        throw new AssertionError("这行不应该被执行到");
    }

    // ── switch ───────────────────────────────────────────────────────

    /** 传统 switch：忘记写 break 会怎样。返回累加结果。 */
    public static int classicSwitchFallsThrough() {
        int grade = 4;
        int sum = 0;
        switch (grade) {
            case 4:
                sum += 4;
                // 故意不写 break
            case 3:
                sum += 3;
                break;
            default:
                sum += 0;
        }
        return sum;
    }

    /** 新式 switch 表达式：漏了分支编译器直接报错，不会静默走错。 */
    public static String gradeToLabel(int grade) {
        return switch (grade) {
            case 4 -> "优秀";
            case 3 -> "良好";
            case 2 -> "及格";
            default -> "待改进";
        };
    }

    /** switch 表达式必须是穷尽的，这里故意让编译器看到所有分支都被覆盖。 */
    public static int classifySign(int n) {
        return switch (Integer.signum(n)) {
            case -1 -> -1;
            case 0 -> 0;
            default -> 1;
        };
    }

    // ── for ──────────────────────────────────────────────────────────

    /** 普通 for 循环累加 0..n-1。 */
    public static int sumWithFor(int n) {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += i;
        }
        return sum;
    }

    /** 循环变量能不能在循环外看见。这里故意让它越界，返回循环实际跑了多少次。 */
    public static int howManyTimesTheLoopRan(int n) {
        int count = 0;
        for (int i = 0; i < n; i++) {
            count++;
        }
        return count;
    }

    /** 增强 for 遍历出来的元素能不能改回去。返回原列表。 */
    public static List<Integer> enhancedForCannotReplaceElements() {
        List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3));
        for (Integer x : numbers) {
            x = x * 10;   // 只是改了局部变量 x，列表里的还是原来的
        }
        return numbers;
    }

    /** 边遍历边删除会发生什么。这里用受检的方式安全地删掉所有偶数。 */
    public static List<Integer> removeEvenSafely() {
        List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        numbers.removeIf(n -> n % 2 == 0);
        return numbers;
    }

    /** break 和 continue 的区别：这个函数返回被 break 掉的次数和继续的次数之和。 */
    public static int countBreaksAndContinues(int n) {
        int handled = 0;
        for (int i = 1; i <= n; i++) {
            if (i % 5 == 0) {
                continue;   // 跳过这一次，继续下一次
            }
            handled++;
        }
        return handled;
    }

    /** 三层嵌套的循环，break 到底能出去几层。返回被访问的 (i,j) 组合个数。 */
    public static int breakOnlyExitsTheInnermostLoop() {
        int visited = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 2) {
                    break;
                }
                visited++;
            }
        }
        return visited;
    }

    /** 带标签的 break 能直接跳出外层。 */
    public static int labeledBreakExitsTheOuterLoop() {
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
        return visited;
    }

    // ── while ────────────────────────────────────────────────────────

    /** while 和 do-while 的区别：条件一开始就为 false 时，循环体跑几次。 */
    public static int whileRunsHowManyTimesIfConditionIsFalse() {
        int count = 0;
        int i = 5;
        while (i < 3) {
            count++;
        }
        return count;
    }

    public static int doWhileRunsHowManyTimesIfConditionIsFalse() {
        int count = 0;
        int i = 5;
        do {
            count++;
        } while (i < 3);
        return count;
    }

    // ── 逻辑运算符 ───────────────────────────────────────────────────

    /** 空值判断 + 短路，安全地访问可能为 null 的对象。 */
    public static int lengthOfMaybeNull(String s) {
        if (s != null) {
            return s.length();
        }
        return -1;
    }

    /** 三目运算符的两个分支类型不一致时，编译器会自动提升到更宽的类型。 */
    public static long ternaryPicksTheWiderType(boolean flag) {
        long result = flag ? 1 : 2L;
        return result;
    }

    /** 反向验证：上面的表达式类型是 long，硬塞进 int 就编译不过。这里显式转换。 */
    public static int ternaryRequiresExplicitNarrowing(boolean flag) {
        return (int) (flag ? 1 : 2L);
    }
}
