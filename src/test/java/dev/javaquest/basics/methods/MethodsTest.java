package dev.javaquest.basics.methods;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 03 章：方法。
 *
 * <p>这个类本身就是文档。方法名是英文（方便 IDE 跳转和搜索），
 * 中文句子放在 {@code @DisplayName} 里。
 */
@DisplayName("03 方法")
class MethodsTest {

    @Nested
    @DisplayName("参数怎么传")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Parameters {

        @Test
        @Order(1)
        @DisplayName("基本类型是值传递：方法内改参数，调用方看不到")
        void primitivesArePassedByValue() {
            int x = 1;
            int inside = MethodsDemo.reassignParameter(x);

            assertThat(inside).as("方法内部的 x 确实变成了 100").isEqualTo(100);
            assertThat(x).as("但调用方的 x 还是 1。Java 只有值传递，没有引用传递").isEqualTo(1);
        }

        @Test
        @Order(2)
        @DisplayName("对象是值传递地址：两个变量指向同一个对象，改一个另一个就变")
        void objectsShareTheSameTarget() {
            int[] data = {1, 2, 3};
            int inside = MethodsDemo.mutateSharedObject(data);

            assertThat(inside).as("方法内部读到 100").isEqualTo(100);
            assertThat(data)
                    .as("调用方的数组第一个元素也变成了 100。传递的是引用地址的副本，指向的对象是同一个")
                    .containsExactly(100, 2, 3);
        }

        @Test
        @Order(3)
        @DisplayName("在方法内把参数重新指向别处，调用方看不到")
        void reassigningTheParameterDoesNotLeakOut() {
            int[] data = {1, 2, 3};
            int inside = MethodsDemo.reassignParameterToNewArray(data);

            assertThat(inside).as("方法内部指向了新数组，看到 100").isEqualTo(100);
            assertThat(data)
                    .as("调用方完全不受影响，因为新数组只存在于方法栈上，返回时就消失了")
                    .containsExactly(1, 2, 3);
        }

        @Test
        @Order(4)
        @DisplayName("传可变对象等于交出控制权，方法能改你的数据")
        void passingAMutableObjectHandsOverControl() {
            MethodsDemo.Field box = new MethodsDemo.Field(1);

            int before = MethodsDemo.changeAndReport(box);

            assertThat(before).as("方法报告了改动前的值").isEqualTo(1);
            assertThat(box.value).as("但字段确实被改了。这是 Java 里最常见的副作用来源").isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("返回值")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ReturnValues {

        @Test
        @Order(1)
        @DisplayName("有返回值的方法，return 直接给出结果")
        void returningMethodsGiveYouTheValue() {
            assertThat(MethodsDemo.add(2, 3)).isEqualTo(5);
        }

        @Test
        @Order(2)
        @DisplayName("void 方法里的 return 只是提前结束，不带值")
        void voidReturnOnlyExitsEarly() {
            assertThat(captureStdout(() -> MethodsDemo.printAndReturnEarly(5)))
                    .as("limit 给 5，但 i == 2 时就 return 了，所以只打印两行，「循环正常走完」不会输出")
                    .containsExactly("i = 0", "i = 1");
        }

        /** 跑一段代码把它打印的东西收进 List，用来断言 void 方法的行为。 */
        private static java.util.List<String> captureStdout(Runnable action) {
            java.io.PrintStream original = System.out;
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            System.setOut(new java.io.PrintStream(buffer, true, java.nio.charset.StandardCharsets.UTF_8));
            try {
                action.run();
            } finally {
                System.setOut(original);
            }
            return buffer.toString(java.nio.charset.StandardCharsets.UTF_8)
                    .lines().toList();
        }
    }

    @Nested
    @DisplayName("重载")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Overloading {

        @Test
        @Order(1)
        @DisplayName("同名方法可以有多个版本，靠参数个数或类型区分")
        void overloadsArePickedByTheArgumentList() {
            assertThat(MethodsDemo.describe(42)).isEqualTo(42);
            assertThat(MethodsDemo.describe(42, "岁")).isEqualTo("42岁");
            assertThat(MethodsDemo.describe(1, 2, 3)).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("可变参数")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Varargs {

        @Test
        @Order(1)
        @DisplayName("int... 就是 int[]，调用点自动打包")
        void varargsIsJustAnArray() {
            assertThat(MethodsDemo.sumOfAll(1, 2, 3)).isEqualTo(6);
            assertThat(MethodsDemo.sumOfAll()).as("一个都不传，得到空数组，sum 是 0").isZero();
        }

        @Test
        @Order(2)
        @DisplayName("传数组和传散数完全等价")
        void passingAnArrayIsIdenticalToPassingScalars() {
            assertThat(MethodsDemo.sumOfArrayPassedAsVarargs(new int[]{1, 2, 3})).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("作用域")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Scope {

        @Test
        @Order(1)
        @DisplayName("局部变量的作用域到所属花括号结束为止")
        void localScopeEndsAtTheClosingBrace() {
            assertThat(MethodsDemo.scopeEndsAtTheClosingBrace())
                    .as("if 块里声明的 inner 只在块内可见，出了块就消失了")
                    .isEqualTo(10);
        }
    }
}
