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
        section("0. 八种基本类型的取值范围");
        System.out.printf("byte    %22d ~ %d%n", Byte.MIN_VALUE, Byte.MAX_VALUE);
        System.out.printf("short   %22d ~ %d%n", Short.MIN_VALUE, Short.MAX_VALUE);
        System.out.printf("char    %22d ~ %d%n", (int) Character.MIN_VALUE, (int) Character.MAX_VALUE);
        System.out.printf("int     %22d ~ %d%n", Integer.MIN_VALUE, Integer.MAX_VALUE);
        System.out.printf("long    %22d ~ %d%n", Long.MIN_VALUE, Long.MAX_VALUE);
        System.out.printf("float    %s ~ %s%n", -Float.MAX_VALUE, Float.MAX_VALUE);
        System.out.printf("double   %s ~ %s%n", -Double.MAX_VALUE, Double.MAX_VALUE);
        System.out.println("char 的边界是控制字符：MIN 是 U+"
                + String.format("%04X", (int) Character.MIN_VALUE)
                + "，MAX 是 U+" + String.format("%04X", (int) Character.MAX_VALUE));

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

    // ── 0. 取值范围 ──────────────────────────────────────────────────

    /** byte 能装的数字个数：2 的 8 次方。 */
    public static int byteCapacity() {
        return (int) ((long) Byte.MAX_VALUE - Byte.MIN_VALUE + 1);
    }

    /** int 能装的数字个数：2 的 32 次方，约 42.9 亿。 */
    public static long intCapacity() {
        return (long) Integer.MAX_VALUE - Integer.MIN_VALUE + 1;
    }

    /**
     * Double.MIN_VALUE 不是「最小的负数」，而是「最小的正数」。
     * 这是 JDK 里最容易误解的一个常量名。
     */
    public static boolean doubleMinValueIsPositive() {
        return Double.MIN_VALUE > 0;
    }

    /** 想要负无穷，自己加负号。 */
    public static double mostNegativeDouble() {
        return -Double.MAX_VALUE;
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
