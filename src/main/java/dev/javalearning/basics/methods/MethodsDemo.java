package dev.javalearning.basics.methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 第 03 课的示例代码：方法。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/basics/methods/MethodsDemo.java}
 * 每一节对应 {@code docs/03-方法.md} 里的一节，输出都是真跑出来的。
 */
public class MethodsDemo {

    static final int PASS_BY_VALUE_X = 10;

    public static void main(String[] args) {
        section("1. 值传递：方法里改了参数，外面毫无变化");
        System.out.println("改之前 x = " + PASS_BY_VALUE_X);
        changePrimitive(PASS_BY_VALUE_X);
        System.out.println("改之后 x = " + PASS_BY_VALUE_X + "   ← 一点没动");

        section("2. 同一个对象：数组就能改到，因为拿到的是同一份数据的钥匙");
        int[] array = {1, 2, 3};
        System.out.println("改之前 array = " + Arrays.toString(array));
        changeArray(array);
        System.out.println("改之后 array = " + Arrays.toString(array) + "   ← 真的改了");

        section("3. 但把参数重新指向别处，外面还是看不见");
        int[] array2 = {1, 2, 3};
        System.out.println("改之前 array2 = " + Arrays.toString(array2));
        reassignArray(array2);
        System.out.println("改之后 array2 = " + Arrays.toString(array2) + "   ← 没变");

        section("4. 重载：同名不同参，编译器按参数列表挑");
        System.out.println("overload(1)      → " + overload(1));
        System.out.println("overload(1, 2)   → " + overload(1, 2));
        System.out.println("overload(\"ab\")   → " + overload("ab"));

        section("5. 可变参数：个数不定，本质上是个数组");
        System.out.println("sum()           → " + sum());
        System.out.println("sum(1)          → " + sum(1));
        System.out.println("sum(1, 2, 3)    → " + sum(1, 2, 3));
        int[] already = {4, 5};
        System.out.println("sum(already)    → " + sum(already) + "   ← 数组可以直接传");
        System.out.println("pick(1) 选中了   → " + pickWithBoxing() + "（不是可变参数那个）");

        section("6. return 在 String 方法里：提前溜走，不带值");
        System.out.println("grade(91) → " + grade(91));
        System.out.println("grade(75) → " + grade(75));
        System.out.println("grade(-1) → " + grade(-1));

        section("7. 传可变对象 = 交出控制权");
        System.out.println("方法把列表 clear 之后，调用方看到的 size = " + callerListSize() + "   ← 被掏空了");

        section("8. 局部变量的作用域到花括号结束");
        scopeDemo();
    }

    // ── 1. 值传递 ────────────────────────────────────────────────────

    public static void changePrimitive(int x) {
        x = 999;   // 改的是形参那份拷贝，调用方那个变量一点没动
    }

    // ── 2. 数组能改到 ────────────────────────────────────────────────

    public static void changeArray(int[] a) {
        a[0] = 99;   // 顺着钥匙打开的是外面那个房间
    }

    // ── 3. 重新指向不行 ──────────────────────────────────────────────

    public static void reassignArray(int[] a) {
        a = new int[] {7, 7, 7};   // 换了把新钥匙，外面那把没变
    }

    // ── 4. 重载 ────────────────────────────────────────────────────

    public static String overload(int x) {
        return "一个 int：" + x;
    }

    public static String overload(int x, int y) {
        return "两个 int：" + x + "," + y;
    }

    public static String overload(String s) {
        return "一个 String：" + s;
    }

    // ── 5. 可变参数 ──────────────────────────────────────────────────

    public static int sum(int... numbers) {
        int total = 0;
        for (int n : numbers) {
            total += n;
        }
        return total;
    }

    public static String pick(int... xs) {
        return "可变参数 int...";
    }

    public static String pick(Integer x) {
        return "单个 Integer";
    }

    /** 1 是 int，但选中的是 {@code pick(Integer)} 而不是 {@code pick(int...)}。 */
    public static String pickWithBoxing() {
        return pick(1);
    }

    // ── 6. return ────────────────────────────────────────────────────

    /** 有返回值的方法里，return 负责「给值并结束」。 */
    public static String grade(int score) {
        if (score < 0) {
            return "成绩不合法";   // 提前溜走，不带值
        }
        if (score >= 90) {
            return "优秀";
        }
        return "及格";
    }

    // ── 7. 传可变对象 = 交出控制权 ────────────────────────────────────

    /** 演示「传可变对象等于交出控制权」：方法把调用方的列表清空了。 */
    public static void clearList(List<String> list) {
        list.clear();   // 调用方看得见
    }

    public static int callerListSize() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c"));
        clearList(list);
        return list.size();
    }

    // ── 8. 作用域 ────────────────────────────────────────────────────

    public static Void scopeDemo() {
        {
            int inner = 1;
            System.out.println("内层花括号里 inner = " + inner);
        }
        // System.out.println(inner);   // 编译不过：离开了花括号就没了
        for (int i = 0; i < 1; i++) {
            System.out.println("for 里声明的 i = " + i);
        }
        // System.out.println(i);       // 同样编译不过
        System.out.println("（离开花括号，inner 和 i 都不存在了）");
        return null;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
