package dev.javaquest.basics.controlflow;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * 第 02 章的示例代码。
 *
 * <p>约定：这里每个方法只证明一件事。中文解释在测试类的 {@code @DisplayName} 里。
 */
public final class ControlFlowDemo {

    private ControlFlowDemo() {
    }

    // ── 短路求值 ────────────────────────────────────────────────────

    /** 左边为 false 时，&& 的右边还跑不跑。 */
    public static boolean andSkipsTheRightSide() {
        return false && wouldThrowIfEvaluated();
    }

    /** 左边为 true 时，|| 的右边还跑不跑。 */
    public static boolean orSkipsTheRightSide() {
        return true || wouldThrowIfEvaluated();
    }

    /** 用 || 写，i 只加了一次：左边已经是 true，右边被短路跳过。 */
    public static int orShortCircuitsIncrement() {
        int i = 3;
        boolean unused = (i++ > 0 || i++ > 0);
        return i;
    }

    /** 用 | 写，i 加了两次：非短路版本，右边一定跑。 */
    public static int bitwiseOrAlwaysIncrements() {
        int i = 3;
        boolean unused = (i++ > 0 | i++ > 0);
        return i;
    }

    /** 故意制造一个异常，用来检测短路有没有生效。 */
    private static boolean wouldThrowIfEvaluated() {
        throw new AssertionError("这行不应该被执行到");
    }

    // ── 选择：switch ────────────────────────────────────────────────

    /** 传统 switch：漏写 break 时累加出的结果。 */
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

    /** switch 表达式处理普通 int：必须写 default，因为 int 取值无穷尽。 */
    public static String gradeToLabel(int grade) {
        return switch (grade) {
            case 4 -> "优秀";
            case 3 -> "良好";
            case 2 -> "及格";
            default -> "待改进";
        };
    }

    /**
     * switch 表达式处理枚举：不需要 default，漏掉某个值编译就报错。
     * 返回 DayOfWeek 落在第几组，用来证明每个分支都被覆盖到了。
     */
    public static String dayTypeToLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "工作日";
            case SATURDAY, SUNDAY -> "休息日";
        };
    }

    // ── 选择：三目 ───────────────────────────────────────────────────

    /** 两个分支类型不同，整条表达式的类型会被提升到更宽的那个。 */
    public static long ternaryPromotesToTheWiderType(boolean flag) {
        long result = flag ? 1 : 2L;
        return result;
    }

    /** 结果要变窄时必须显式转换。 */
    public static int ternaryRequiresExplicitNarrowing(boolean flag) {
        return (int) (flag ? 1 : 2L);
    }

    // ── 重复：三种循环 ──────────────────────────────────────────────

    /** for 累加 0 到 n-1。 */
    public static int sumWithFor(int n) {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += i;
        }
        return sum;
    }

    /** while：先判断再执行，条件一开始就假则一次都不进。 */
    public static int whileRunsZeroTimesWhenConditionIsFalse() {
        int count = 0;
        int i = 5;
        while (i < 3) {
            count++;
        }
        return count;
    }

    /** do-while：先执行后判断，所以哪怕条件一开始就假也一定跑一次。 */
    public static int doWhileRunsOnceWhenConditionIsFalse() {
        int count = 0;
        int i = 5;
        do {
            count++;
        } while (i < 3);
        return count;
    }

    /** continue 跳过的次数。1 到 10 里 5 和 10 被跳过，剩 8 次。 */
    public static int countAfterSkippingMultiplesOfFive() {
        int handled = 0;
        for (int i = 1; i <= 10; i++) {
            if (i % 5 == 0) {
                continue;
            }
            handled++;
        }
        return handled;
    }

    // ── 跳转：break / continue / 标签 ────────────────────────────────

    /** break 只跳出最内层那一层。 */
    public static int plainBreakVisits() {
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

    /** 带标签的 break 直接跳出外层。 */
    public static int labeledBreakVisits() {
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

    // ── 重复：增强 for 与集合 ───────────────────────────────────────

    /** 增强 for 里改局部变量，集合不受影响。 */
    public static List<Integer> enhancedForCannotWriteBack() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3));
        for (Integer x : numbers) {
            x = x * 10;   // 改的是局部变量 x
        }
        return numbers;
    }

    /** removeIf 是删除元素的安全做法。 */
    public static List<Integer> removeEvenSafely() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        numbers.removeIf(n -> n % 2 == 0);
        return numbers;
    }

    /**
     * 增强 for 里直接删偶数会抛 ConcurrentModificationException。
     * 这个方法正常返回说明没抛，一旦抛了就说明踩到了。
     */
    public static String removingWhileIterating() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3, 4));
        try {
            for (int x : numbers) {
                if (x % 2 == 0) {
                    numbers.remove(Integer.valueOf(x));
                }
            }
            return "没有抛异常：" + numbers;
        } catch (ConcurrentModificationException e) {
            return "ConcurrentModificationException";
        }
    }

    /**
     * List.remove 有两个重载：remove(int) 按下标删，remove(Object) 按值删。
     * 传 int 会选到按下标那个重载。
     */
    public static List<Integer> removeByIndexVersusByValue() {
        List<Integer> byIndex = new ArrayList<>(List.of(1, 2, 3, 4));
        byIndex.remove(2);                      // 删掉下标 2 的元素，也就是值 3

        List<Integer> byValue = new ArrayList<>(List.of(1, 2, 3, 4));
        byValue.remove(Integer.valueOf(2));     // 删掉值为 2 的元素

        List<Integer> merged = new ArrayList<>();
        merged.addAll(byIndex);
        merged.add(-1);                         // 分隔符，方便肉眼分辨两段结果
        merged.addAll(byValue);
        return merged;
    }

    /** 本章用来演示 switch 穷尽性的枚举。 */
    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }
}
