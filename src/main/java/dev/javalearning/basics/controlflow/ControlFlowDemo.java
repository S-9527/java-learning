package dev.javalearning.basics.controlflow;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

/**
 * 第 02 课的示例代码：运算符与流程控制。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/basics/controlflow/ControlFlowDemo.java}
 * 每一节对应 {@code docs/02-运算符与流程控制.md} 里的一节，输出都是真跑出来的。
 */
public class ControlFlowDemo {

    public static void main(String[] args) {
        section("1. && 的短路：右边的表达式可能根本没跑");
        System.out.println("null != null && s.length() > 0  → " + shortCircuitAnd());
        System.out.println("右侧被跳过的调用次数            → " + timesRightSideRan());
        System.out.println("没有短路会怎样（结果先行）      → " + withoutShortCircuit());

        section("2. || 的短路：找到真的就停下");
        System.out.println("true || 右边 → " + shortCircuitOr());

        section("3. switch 表达式：漏写分支编译器会说话");
        System.out.println("MAP  → " + describe(Direction.MAP));
        System.out.println("SOUTH → " + describe(Direction.SOUTH));

        section("4. 三种循环差在哪一步");
        System.out.println("while  先判断后执行，次数可能为 0");
        System.out.println("do-while 先执行后判断，至少跑 1 次");
        int count = countWithWhile();
        System.out.println("while 循环次数 = " + count + "（条件一开始就是 false）");
        int count2 = countWithDoWhile();
        System.out.println("do-while 循环次数 = " + count2 + "（同样从一开始就是 false）");

        section("5. 增强 for 里删元素：有时炸有时不炸");
        System.out.println("删中间的元素「b」 → " + removeMiddleWithForEach());
        System.out.println("删第一个元素「a」 → " + removeFirstWithForEach());
        System.out.println("每个元素都删      → " + removeEveryElementWithForEach());
        System.out.println("迭代器自己的 remove → " + removeWithIterator());
        System.out.println("removeIf            → " + removeWithRemoveIf());

        section("6. break / continue / 标签");
        labelsDemo();
    }

    // ── 1. 短路 ────────────────────────────────────────────────────

    /** 记录右侧表达式被求值了几次，用来证明它被跳过了。 */
    static int rightSideCount = 0;

    static boolean cheapCheck() {
        rightSideCount++;
        return true;
    }

    /** 左边已经是 false，右边的 cheapCheck() 压根不会跑。 */
    public static boolean shortCircuitAnd() {
        String s = null;
        rightSideCount = 0;
        boolean result = s != null && s.length() > 0;
        return result;
    }

    /** 右侧实际被求值的次数——0 次，这就是「短路」。 */
    public static int timesRightSideRan() {
        shortCircuitAnd();
        return rightSideCount;
    }

    /** 用 & 单与号：两边都求值，于是 NPE。 */
    public static String withoutShortCircuit() {
        String s = null;
        try {
            @SuppressWarnings("unused")
            boolean r = s != null & s.length() > 0;
            return "居然没抛异常";
        } catch (NullPointerException e) {
            return "NullPointerException: " + e.getMessage();
        }
    }

    public static boolean shortCircuitOr() {
        return true || cheapCheck();
    }

    // ── 3. switch 表达式 ────────────────────────────────────────────

    enum Direction { NORTH, EAST, SOUTH, WEST, MAP }

    /** Java 14+ 的 switch 表达式：漏掉一个分支，编译器直接报错。 */
    public static String describe(Direction d) {
        return switch (d) {
            case NORTH -> "向上";
            case EAST -> "向右";
            case SOUTH -> "向下";
            case WEST -> "向左";
            case MAP -> "看地图";
        };
    }

    // ── 4. 三种循环 ────────────────────────────────────────────────

    public static int countWithWhile() {
        int i = 0;
        while (i < 0) {
            i++;
        }
        return i;
    }

    public static int countWithDoWhile() {
        int i = 0;
        do {
            i++;
        } while (i < 0);
        return i;
    }

    // ── 5. 迭代中删元素 ─────────────────────────────────────────────

    /**
     * 反直觉的一个：删的是中间的「b」，循环反倒没炸。
     * 原因不是「删中间安全」，而是删完之后刚好没有下一次 next() 了——
     * for-each 发现列表已经走完，自然结束，modCount 的变化根本没机会被检查到。
     * 这是运气，不是安全。删「a」同样会炸，见 removeFirstWithForEach。
     */
    public static String removeMiddleWithForEach() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c"));
        try {
            for (String s : list) {
                if (s.equals("b")) {
                    list.remove(s);
                }
            }
            return "没炸，结果 = " + list + "（删完刚好走完，纯属侥幸）";
        } catch (ConcurrentModificationException e) {
            return "ConcurrentModificationException";
        }
    }

    /** 同样是删一个元素，换成删第一个「a」就炸了——因为后面还有 next() 要跑。 */
    public static String removeFirstWithForEach() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c"));
        try {
            for (String s : list) {
                if (s.equals("a")) {
                    list.remove(s);
                }
            }
            return "没炸，结果 = " + list;
        } catch (ConcurrentModificationException e) {
            return "ConcurrentModificationException";
        }
    }

    /**
     * 每个元素都删：每次删除之后都还有下一次 next()，
     * 于是 modCount 的变化立刻被发现，抛 ConcurrentModificationException。
     */
    public static String removeEveryElementWithForEach() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c"));
        try {
            for (String s : list) {
                list.remove(s);
            }
            return "没炸，结果 = " + list;
        } catch (ConcurrentModificationException e) {
            return "ConcurrentModificationException";
        }
    }

    /** 正确做法：用迭代器自己的 remove。 */
    public static String removeWithIterator() {
        List<String> list = new ArrayList<>(
                List.of("a", "b", "c"));
        for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
            if (it.next().equals("b")) {
                it.remove();
            }
        }
        return String.join(",", list);
    }

    /** 或者用 removeIf，一行搞定。 */
    public static String removeWithRemoveIf() {
        List<String> list = new ArrayList<>(
                List.of("a", "b", "c"));
        list.removeIf(s -> s.equals("b"));
        return String.join(",", list);
    }

    // ── 6. 标签 ────────────────────────────────────────────────────

    /** 不带标签：外层循环跑了几轮（3 轮说明外层没被打破）。 */
    public static int countPlainBreakOuterIterations() {
        int rounds = 0;
        for (int i = 0; i < 3; i++) {
            rounds++;
            for (int j = 0; j < 3; j++) {
                if (j == 1) {
                    break;
                }
            }
        }
        return rounds;
    }

    /** 带标签：外层循环跑了几轮（1 轮说明两层一起跳出来了）。 */
    public static int countLabeledBreakOuterIterations() {
        int rounds = 0;
        outer:
        for (int i = 0; i < 3; i++) {
            rounds++;
            for (int j = 0; j < 3; j++) {
                if (j == 1) {
                    break outer;
                }
            }
        }
        return rounds;
    }

    /** continue 跳过这一次，剩下的奇数和照加。 */
    public static int continueSum() {
        int sum = 0;
        for (int i = 1; i <= 5; i++) {
            if (i % 2 == 0) {
                continue;
            }
            sum += i;
        }
        return sum;
    }

    /** 一个分支里列多个值，用逗号分隔。 */
    public static String describeGroup(int n) {
        return switch (n) {
            case 1, 2, 3 -> "小";
            case 4, 5, 6 -> "大";
            default -> "未知";
        };
    }

    public static void labelsDemo() {
        System.out.println("break 只能跳出一层：");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 1) {
                    break;
                }
                System.out.println("  i=" + i + " j=" + j);
            }
        }

        System.out.println("带标签的 break 直接跳出外层：");
        outer:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 1) {
                    break outer;
                }
                System.out.println("  i=" + i + " j=" + j);
            }
        }
        System.out.println("（标签演示结束）");
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
