package dev.javaquest.basics.variables;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 01 章的示例代码。
 *
 * <p>约定：这里每个方法只证明<b>一件事</b>，方法名用英文，
 * 中文解释全部放在测试类的 {@code @DisplayName} 里。
 *
 * <p>本类只装「事实」，不放讲解——讲解在 {@code docs/01-变量与类型.md}。
 * 这样做的好处是：事实可以被测试抓住，讲解可以随时改，两者互不拖累。
 */
public final class VariableDemo {

    private VariableDemo() {
        // 工具类：只装静态方法，禁止 new，也禁止继承。
    }

    // ── 整数 ─────────────────────────────────────────────────────────

    /** 7 除以 2。 */
    public static int divide7By2() {
        return 7 / 2;
    }

    /** 负数除以正数。 */
    public static int divideNegative7By2() {
        return -7 / 2;
    }

    /** 负数取模。 */
    public static int moduloNegative7By2() {
        return -7 % 2;
    }

    /** 给 int 的最大值加一。 */
    public static int addOneToMaxInt() {
        return Integer.MAX_VALUE + 1;
    }

    /** 两个 byte 相加。注意返回类型为什么是 int。 */
    public static int addTwoBytes(byte a, byte b) {
        return a + b;
    }

    // ── 浮点 ─────────────────────────────────────────────────────────

    /** 0.1 加 0.2。 */
    public static double addPointOneAndPointTwo() {
        return 0.1 + 0.2;
    }

    // ── String ───────────────────────────────────────────────────────

    /** 两个长得一样的字面量，是不是同一个对象。 */
    public static boolean twoLiteralsAreTheSameObject() {
        String a = "java";
        String b = "java";
        return a == b;
    }

    /** 手工 new 出来的字符串，和字面量是不是同一个对象。 */
    public static boolean newStringIsTheSameObjectAsLiteral() {
        String a = "java";
        String b = new String("java");
        return a == b;
    }

    /** 拼接之后，左边那个对象有没有被改掉。 */
    public static boolean concatenationChangesTheLeftOperand() {
        String s = "ab";
        String t = s + "c";
        return s == t;
    }

    /** 拼接之后 s 还是不是 "ab"。 */
    public static String leftOperandAfterConcatenation() {
        String s = "ab";
        String t = s + "c";
        return s;
    }

    // ── final ────────────────────────────────────────────────────────

    /** final 引用还能不能往里加东西。返回 true 表示加不进去。 */
    public static boolean finalReferenceBlocksAddingToList() {
        final List<String> list = new ArrayList<>();
        list.add("x");
        return list.isEmpty();
    }

    // ── char ─────────────────────────────────────────────────────────

    /** char 背后的数字。 */
    public static int numericValueOfCharA() {
        return 'A';
    }

    /** 字符能不能直接做算术。 */
    public static char nextCharAfterA() {
        return (char) ('A' + 1);
    }

    // ── 自动装箱 ─────────────────────────────────────────────────────

    /** 127 装箱两次，是不是同一个对象。 */
    public static boolean boxed127IsTheSameObjectAsAnotherBoxed127() {
        Integer a = Integer.valueOf(127);
        Integer b = Integer.valueOf(127);
        return a == b;
    }

    /** 200 装箱两次，是不是同一个对象。 */
    public static boolean boxed200IsTheSameObjectAsAnotherBoxed200() {
        Integer a = Integer.valueOf(200);
        Integer b = Integer.valueOf(200);
        return a == b;
    }
}
