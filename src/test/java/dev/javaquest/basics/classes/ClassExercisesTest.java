package dev.javaquest.basics.classes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 04 章的练习题。
 *
 * <p>用法：把方法上的 {@code @Disabled} 删掉，重新运行，就能看到答案跑起来。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("04 练习题：类与对象")
class ClassExercisesTest {

    /** 静态计数器是跨用例共享的，每个用例开始前先归零。 */
    @BeforeEach
    void resetSharedState() {
        Counter.resetCreated();
    }

    @Test
    @Order(1)
    @Disabled("答案：false。两个对象，内容一样但身份不同，而且 Point 没有重写 equals，"
            + "所以 == 和 equals 都返回 false。要让内容相同的两个点相等，得自己重写 equals 和 hashCode。")
    @DisplayName("练习 1：两个内容相同的 Point，用 equals 比较结果是什么？")
    void exerciseDefaultEqualsIsIdentity() {
        Point a = new Point(1, 2);
        Point b = new Point(1, 2);

        assertThat(a).as("没有重写 equals，默认比的是身份").isNotEqualTo(b);
        assertThat(a == b).isFalse();
    }

    @Test
    @Order(2)
    @Disabled("答案：0、false、null、null。int 默认 0，boolean 默认 false，"
            + "引用类型（String、数组）默认都是 null。")
    @DisplayName("练习 2：没写构造器的类，字段拿到什么默认值？")
    void exerciseDefaultFieldValues() {
        Bare b = new Bare();

        assertThat(b.number).isZero();
        assertThat(b.flag).isFalse();
        assertThat(b.text).isNull();
        assertThat(b.numbers).isNull();
    }

    @Test
    @Order(3)
    @Disabled("答案：1 和 2。created 是 static 的，被所有实例共享，所以编号接着往下排。"
            + "如果它不是 static，每个对象都会从 0 开始，拿到同样的编号 1。")
    @DisplayName("练习 3：连续造两个 Counter，它们的 id 分别是多少？")
    void exerciseStaticCounterIsShared() {
        Counter first = new Counter();
        Counter second = new Counter();

        assertThat(first.getId()).isEqualTo(1);
        assertThat(second.getId()).isEqualTo(2);
        assertThat(Counter.getCreated()).isEqualTo(2);
    }

    @Test
    @Order(4)
    @Disabled("答案：抛 NullPointerException。数组字段的默认值是 null，"
            + "对它取 length 等于在 null 上调方法，会立刻炸。")
    @DisplayName("练习 4：对一个没初始化的数组字段取 length 会怎样？")
    void exerciseNullArrayFieldThrows() {
        Bare b = new Bare();

        assertThatThrownBy(() -> {
            int length = b.numbers.length;   // numbers 是 null，这句会抛
            assertThat(length).as("到不了这里").isZero();
        })
                .as("numbers 默认为 null，访问它的属性就是在 null 上调方法")
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Order(5)
    @Disabled("答案：0。形参和字段同名时，`value = value` 两边都解析成形参，"
            + "等于把参数赋给自己，字段还是初始值。编译器不报错，所以这种错会一路带到运行期。"
            + "写成 `this.value = value` 才拿得到 9。")
    @DisplayName("练习 5：构造器里写 value = value 而不是 this.value = value，字段会拿到值吗？")
    void exerciseSelfAssignmentDoesNotAssignField() {
        WrongConstructor wrong = new WrongConstructor(9);
        RightConstructor right = new RightConstructor(9);

        assertThat(wrong.getValue())
                .as("自赋值，字段还是初始值 0，而不是传进来的 9")
                .isZero();

        assertThat(right.getValue())
                .as("同一个参数，加上 this. 之后就正确赋给字段了")
                .isEqualTo(9);
    }

    /** 故意写错的构造器：形参和字段同名，却没写 this.。 */
    private static final class WrongConstructor {
        private final int value;

        private WrongConstructor(int value) {
            this.value = 0;    // 先给个初值，下面那行如果有效就会被覆盖
            value = value;     // 自赋值，编译器不报错，但这行什么也没做
        }

        int getValue() {
            return value;
        }
    }

    /** 同一个类的正确写法，作为对照。 */
    private static final class RightConstructor {
        private final int value;

        private RightConstructor(int value) {
            this.value = value;   // this. 左边是字段，右边是形参
        }

        int getValue() {
            return value;
        }
    }
}
