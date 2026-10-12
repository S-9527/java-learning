package dev.javalearning.basics.variables;

import java.math.BigDecimal;

/**
 * 第 01 课的示例代码：一个变量里到底装着什么。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/basics/variables/VariablesDemo.java}
 * 每一节对应 {@code docs/01-变量与类型.md} 里的一节，输出都是真跑出来的。
 */
public class VariablesDemo {

    public static void main(String[] args) {
        section("1. 整数：7 / 2 等于几");
        System.out.println("7 / 2    = " + divide7By2());
        System.out.println("7 / 2.0  = " + divide7By2Point0());
        System.out.println("-7 / 2   = " + divideNegative7By2());

        section("2. 溢出：给它最大值加一");
        System.out.println("Integer.MAX_VALUE = " + Integer.MAX_VALUE);
        System.out.println("MAX_VALUE + 1     = " + addOneToMaxInt() + "   ← 不报错，静默回绕");

        section("3. 浮点：0.1 + 0.2 等于 0.3 吗");
        System.out.println("0.1 + 0.2            = " + addPointOneAndPointTwo());
        System.out.println("0.1 + 0.2 == 0.3     = " + pointOnePlusPointTwoEqualsZeroPointThree());
        System.out.println("BigDecimal 的正确做法 = " + addPointOneAndPointTwoExactly());

        section("4. String：== 有时灵有时不灵");
        System.out.println("两个字面量 a == b          → " + twoLiteralsAreTheSameObject());
        System.out.println("字面量和 new 出来的 a == c → " + literalEqualsNewString());
        System.out.println("a.equals(c)                → " + literalEqualsNewStringByContent());

        section("5. 装地址的变量：数组");
        int[] original = {1, 2, 3};
        int[] alias = original;
        alias[0] = 99;
        System.out.println("改了 alias[0]，original[0] = " + original[0] + "   ← 跟着变了");

        section("6. 装箱：== 在 Integer 上又是另一套");
        System.out.println("127 == 127 → " + smallIntegersAreCached());
        System.out.println("128 == 128 → " + bigIntegersAreNotCached());
        System.out.println("128 用 equals → " + bigIntegersAreEqualByContent());
    }

    // ── 1. 整数 ────────────────────────────────────────────────────

    public static int divide7By2() {
        return 7 / 2;
    }

    public static double divide7By2Point0() {
        return 7 / 2.0;
    }

    public static int divideNegative7By2() {
        return -7 / 2;
    }

    // ── 2. 溢出 ────────────────────────────────────────────────────

    public static int addOneToMaxInt() {
        return Integer.MAX_VALUE + 1;
    }

    // ── 3. 浮点 ────────────────────────────────────────────────────

    public static double addPointOneAndPointTwo() {
        return 0.1 + 0.2;
    }

    public static boolean pointOnePlusPointTwoEqualsZeroPointThree() {
        return 0.1 + 0.2 == 0.3;
    }

    public static String addPointOneAndPointTwoExactly() {
        return new BigDecimal("0.1").add(new BigDecimal("0.2")).toString();
    }

    // ── 4. String ──────────────────────────────────────────────────

    public static boolean twoLiteralsAreTheSameObject() {
        String a = "java";
        String b = "java";
        return a == b;
    }

    public static boolean literalEqualsNewString() {
        String a = "java";
        String c = new String("java");
        return a == c;
    }

    public static boolean literalEqualsNewStringByContent() {
        String a = "java";
        String c = new String("java");
        return a.equals(c);
    }

    // ── 6. 装箱 ────────────────────────────────────────────────────

    public static boolean smallIntegersAreCached() {
        Integer i1 = 127;
        Integer i2 = 127;
        return i1 == i2;
    }

    public static boolean bigIntegersAreNotCached() {
        Integer i1 = 128;
        Integer i2 = 128;
        return i1 == i2;
    }

    public static boolean bigIntegersAreEqualByContent() {
        Integer i1 = 128;
        Integer i2 = 128;
        return i1.equals(i2);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
