package dev.javaquest.basics.methods;

/**
 * 第 03 章的示例代码。
 *
 * <p>每个方法只证明一件事。中文解释在测试类的 {@code @DisplayName} 里。
 */
public final class MethodsDemo {

    private MethodsDemo() {
    }

    // ── 参数传递 ─────────────────────────────────────────────────────

    /**
     * 传 int 进方法，在里面改它。返回方法内部看到的值。
     * 调用方那个变量不受影响 —— Java 只有值传递，没有引用传递。
     */
    public static int reassignParameter(int x) {
        x = 100;
        return x;
    }

    /**
     * 传可变对象进方法，在里面改它的内容。返回方法内部看到的内容。
     * 调用方的对象也被改了 —— 因为两个变量指向同一个对象。
     */
    public static int mutateSharedObject(int[] data) {
        data[0] = 100;
        return data[0];
    }

    /** 在方法内部把参数重新指向别处，看得见吗。 */
    public static int reassignParameterToNewArray(int[] data) {
        data = new int[]{100};
        return data[0];
    }

    // ── 返回值 ───────────────────────────────────────────────────────

    /** 有返回值：直接 return。 */
    public static int add(int a, int b) {
        return a + b;
    }

    /** 无返回值：void 方法里的 return 只是「提前结束」。 */
    public static void printAndReturnEarly(int limit) {
        for (int i = 0; i < limit; i++) {
            if (i == 2) {
                return;    // 不是「返回 2」，是「什么都别返回，直接走人」
            }
            System.out.println("i = " + i);
        }
        System.out.println("循环正常走完");
    }

    // ── 重载 ─────────────────────────────────────────────────────────

    /** 重载 1：两个 int。 */
    public static int describe(int value) {
        return value;
    }

    /** 重载 2：int 和 String。 */
    public static String describe(int value, String unit) {
        return value + unit;
    }

    /** 重载 3：三个 int。 */
    public static int describe(int a, int b, int c) {
        return a + b + c;
    }

    // ── 可变参数 ─────────────────────────────────────────────────────

    /**
     * 可变参数本质上就是一个数组。
     * 编译器在调用点把散着的参数打包成数组传进来。
     */
    public static int sumOfAll(int... numbers) {
        int sum = 0;
        for (int n : numbers) {
            sum += n;
        }
        return sum;
    }

    /** 可变参数也可以直接传一个数组进去，两者完全等价。 */
    public static int sumOfArrayPassedAsVarargs(int[] numbers) {
        return sumOfAll(numbers);
    }

    // ── 作用域 ───────────────────────────────────────────────────────

    /**
     * 局部变量的作用域只到所属的那对花括号结束。
     * 这里的 inner 出了 if 的块就看不见了。
     */
    public static int scopeEndsAtTheClosingBrace() {
        int result;
        if (true) {
            int inner = 10;
            result = inner;
        }
        // int inner = 20;   // 这里已经看不见 inner 了，作用域到上面那个 } 就结束
        return result;
    }

    // ── 副作用 ───────────────────────────────────────────────────────

    /**
     * 方法默认允许有副作用。
     * 这个方法把自己变成别的东西，返回「变成之前的状态」。
     */
    public static int changeAndReport(Field box) {
        int before = box.value;
        box.value = 100;
        return before;
    }

    /** 一个极简的可变数据载体，用来演示「传对象等于交出控制权」。 */
    public static final class Field {
        public int value;

        public Field(int value) {
            this.value = value;
        }
    }
}
