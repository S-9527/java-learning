package dev.javaquest.basics.classes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 04 章：类与对象。
 *
 * <p>这个类本身就是文档。方法名是英文，中文句子放在 {@code @DisplayName} 里。
 *
 * <p>分组对应文档的一到六节。
 */
@DisplayName("04 类与对象")
class ClassTest {

    @Nested
    @DisplayName("一、类型与实例")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TypeAndInstance {

        @Test
        @Order(1)
        @DisplayName("同一个类可以造出很多个对象，它们互相独立")
        void oneClassManyInstances() {
            Point a = new Point(1, 2);
            Point b = new Point(1, 2);

            assertThat(a.getX()).isEqualTo(1);
            assertThat(b.getX()).isEqualTo(1);
            assertThat(a)
                    .as("a 和 b 内容一样，但它们是两个对象。改其中一个的来源不会影响另一个")
                    .isNotSameAs(b);
        }

        @Test
        @Order(2)
        @DisplayName("没有重写 equals 时，比较的是身份而不是内容")
        void identityEqualityByDefault() {
            Point a = new Point(3, 4);
            Point b = new Point(3, 4);

            assertThat(a == b).as("两个不同对象，地址不同").isFalse();
            assertThat(a)
                    .as("也没重写 equals，所以 equals 也走默认的身份比较。内容相同不代表它们相等")
                    .isNotEqualTo(b);
        }

        @Test
        @Order(3)
        @DisplayName("toString 的默认格式是类名加哈希值")
        void toStringHasADefaultFormat() {
            Point p = new Point(1, 2);

            assertThat(p.toString())
                    .as("没重写 toString 时，输出的是 全类名@哈希值 的十六进制。调试时记得重写它")
                    .startsWith("dev.javaquest.basics.classes.Point@");
        }
    }

    @Nested
    @DisplayName("二、状态与行为")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class StateAndBehaviour {

        @Test
        @Order(1)
        @DisplayName("字段存状态，方法读出来")
        void fieldsHoldState() {
            Point p = new Point(5, 8);

            assertThat(p.getX()).as("构造器把 5 存进了 x").isEqualTo(5);
            assertThat(p.getY()).as("构造器把 8 存进了 y").isEqualTo(8);
        }

        @Test
        @Order(2)
        @DisplayName("方法可以同时用到自己的字段和别人的字段")
        void methodUsesBothObjects() {
            Point origin = new Point(0, 0);
            Point target = new Point(3, 4);

            int squared = origin.distanceSquaredTo(target);

            assertThat(squared)
                    .as("3 的平方加 4 的平方等于 25。这就是经典的 3-4-5 直角三角形")
                    .isEqualTo(25);
            assertThat(target.distanceSquaredTo(origin))
                    .as("交换两个对象算出来的结果一样")
                    .isEqualTo(25);
        }

        @Test
        @Order(3)
        @DisplayName("字段是 final 时，方法返回一个改过的新对象")
        void immutableFieldsMeanNewObjects() {
            Point p = new Point(1, 2);
            Point moved = p.withX(10);

            assertThat(p.getX()).as("原对象的 x 没变").isEqualTo(1);
            assertThat(moved.getX()).as("新对象的 x 是 10").isEqualTo(10);
            assertThat(moved.getY()).as("没提到的字段沿用原来的值").isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("三、构造器")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Constructing {

        @Test
        @Order(1)
        @DisplayName("构造器里的 x = x 是在给参数赋值，不是给字段赋值")
        void thisIsRequiredToAssignFields() {
            // Point 的构造器写的是 this.x = x。如果写成 x = x，
            // 两边都解析成形参，等于把参数赋给自己，字段的值不会变。
            // 这里验证的是「写了 this 之后字段确实拿到了值」。
            Point p = new Point(7, 7);

            assertThat(p.getX())
                    .as("构造器里用的是 this.x = x，所以字段真的拿到了 7。写成 x = x 的话这里会是 0")
                    .isEqualTo(7);
        }

        @Test
        @Order(2)
        @DisplayName("一个类没写构造器时，字段是默认值而不是报错")
        void missingConstructorMeansDefaultValues() {
            Bare b = new Bare();

            assertThat(b.number).as("int 的默认值是 0").isZero();
            assertThat(b.flag).as("boolean 的默认值是 false").isFalse();
            assertThat(b.text).as("引用类型的默认值是 null").isNull();
            assertThat(b.numbers).as("数组也是引用，默认 null，调用它的方法会抛 NPE").isNull();
        }
    }

    @Nested
    @DisplayName("四、this")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ThisKeyword {

        @Test
        @Order(1)
        @DisplayName("方法里的 this 指向调用它的那个对象")
        void thisRefersToTheReceiver() {
            Point p = new Point(3, 4);

            // distanceSquaredTo 内部用 this.x 读本对象、other.x 读另一个对象。
            // 两个对象调用同一个方法，this 各自指向调用方。
            int fromP = p.distanceSquaredTo(new Point(0, 0));

            assertThat(fromP).as("p 作为调用方，读到的是 p 自己的 x=3、y=4").isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("五、static")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class StaticMembers {

        @Test
        @Order(1)
        @DisplayName("static 字段被所有实例共享，实例字段各归各")
        void staticIsSharedAndInstanceIsNot() {
            Counter.resetCreated();

            Counter first = new Counter();
            Counter second = new Counter();

            assertThat(first.getId()).as("第一个对象分到编号 1").isEqualTo(1);
            assertThat(second.getId()).as("第二个对象分到编号 2").isEqualTo(2);
            assertThat(Counter.getCreated())
                    .as("created 是 static 的，造了几个对象它就记几个。两个对象共享同一个计数")
                    .isEqualTo(2);
        }

        @Test
        @Order(2)
        @DisplayName("实例字段不共享：各自 increment 各自涨")
        void instanceFieldsAreIndependent() {
            Counter.resetCreated();

            Counter first = new Counter();
            Counter second = new Counter();

            first.increment();
            first.increment();
            second.increment();

            assertThat(first.getValue()).as("first 加了两次").isEqualTo(2);
            assertThat(second.getValue()).as("second 只加了一次，和 first 互不影响").isEqualTo(1);
        }

        @Test
        @Order(3)
        @DisplayName("static 方法通过类名调用，不需要先造对象")
        void staticMethodsNeedNoInstance() {
            Counter.resetCreated();

            assertThat(Counter.getCreated())
                    .as("还没造过任何对象，静态计数器就是 0。这条 static 方法完全没用到 this")
                    .isZero();
        }
    }

    @Nested
    @DisplayName("六、可见性与封装")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Visibility {

        @Test
        @Order(1)
        @DisplayName("private 字段只能通过方法访问")
        void privateFieldsAreReachedThroughMethods() {
            Point p = new Point(2, 3);

            assertThat(p.getX())
                    .as("x 和 y 都是 private，外部拿不到它们本身，只能通过 getX()、getY() 读")
                    .isEqualTo(2);
        }

        @Test
        @Order(2)
        @DisplayName("字段用 final 时，对象的对外表现也就固定了")
        void finalFieldsLockDownTheObjectsShape() {
            Point p = new Point(1, 2);

            Point[] points = {p};

            assertThat(points[0].getY())
                    .as("数组里放的是同一个对象的引用，取出来的还是它。没有办法从外部改它的坐标")
                    .isEqualTo(2);
        }
    }
}
