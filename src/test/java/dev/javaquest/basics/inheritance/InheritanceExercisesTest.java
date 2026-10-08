package dev.javaquest.basics.inheritance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 05 章的练习题。
 *
 * <p>用法：把方法上的 {@code @Disabled} 删掉，重新运行，就能看到答案跑起来。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("05 练习题：继承与多态")
class InheritanceExercisesTest {

    @Test
    @Order(1)
    @Disabled("答案：shape。static 方法在编译期就按变量声明的类型定死了，"
            + "运行期换成什么对象都不影响。同一个对象写成 Circle 才是 circle。")
    @DisplayName("练习 1：把 Circle 对象声明成 Shape，再调 kind()，得到什么？")
    void exerciseStaticMethodResolvesAtCompileTime() {
        Shape asShape = new Circle(1);

        assertThat(asShape.kind())
                .as("变量声明成 Shape，编译期就绑定了 Shape.kind()")
                .isEqualTo("shape");
        assertThat(((Circle) asShape).kind()).as("强转回 Circle 之后才是 circle").isEqualTo("circle");
    }

    @Test
    @Order(2)
    @Disabled("答案：抛 ClassCastException。编译器放你过去，因为它没法知道运行期装的是什么。"
            + "所以强转之前最好先 instanceof 或用模式匹配。")
    @DisplayName("练习 2：把一个 Rectangle 强转成 Circle 会发生什么？")
    void exerciseWrongDowncastThrows() {
        Shape rectangle = new Rectangle(2, 3);

        assertThatThrownBy(() -> {
            Circle wrong = (Circle) rectangle;
            assertThat(wrong.getRadius()).as("到不了这里").isEqualTo(2.0);
        })
                .as("运行期发现实际类型对不上，当场抛异常")
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    @Order(3)
    @Disabled("答案：false。Circle 没有重写 equals，走 Object 的默认实现，比的是身份。"
            + "两个半径都是 1 的圆，面积一样，但它们不是同一个对象。第 04 章讲过这一条。")
    @DisplayName("练习 3：两个 Circle(1) 用 equals 比较，结果是什么？")
    void exerciseEqualityIsStillIdentityByDefault() {
        Circle first = new Circle(1);
        Circle second = new Circle(1);

        assertThat(first.area()).as("面积确实一样").isEqualTo(second.area());
        assertThat(first).as("但 equals 走的是默认的身份比较").isNotEqualTo(second);
    }

    @Test
    @Order(4)
    @Disabled("答案：2 个。equals 说了它们相等，但 hashCode 没跟上，"
            + "HashSet 按 hashCode 分桶，两个对象落进不同的桶，于是各留一份。"
            + "这就是为什么 hashCode 必须和 equals 一起重写。")
    @DisplayName("练习 4：只重写 equals 不重写 hashCode，两个相等的金额放进 HashSet 会剩几个？")
    void exerciseBrokenHashCodeContractKeepsDuplicates() {
        Set<OnlyEqualsIsBroken> set = new HashSet<>();
        set.add(new OnlyEqualsIsBroken(1234));
        set.add(new OnlyEqualsIsBroken(1234));

        assertThat(set)
                .as("BrokenMoney 的 equals 说相等，hashCode 却还是按身份算，契约破了")
                .hasSize(2);
    }

    @Test
    @Order(5)
    @Disabled("答案：圆(1.0) 和 <矩形>。label() 是接口给的 default 方法，"
            + "Circle 嫌它不好看所以重写了，Rectangle 没重写，用的是接口那份。"
            + "同一个方法，两个类表现出不同的行为——这也是多态。")
    @DisplayName("练习 5：Circle 和 Rectangle 的 label() 分别输出什么？")
    void exerciseDefaultMethodCanBeOverridden() {
        Circle circle = new Circle(1);
        Rectangle rectangle = new Rectangle(2, 3);

        assertThat(circle.label()).as("Circle 重写了 label()").isEqualTo("圆(1.0)");
        assertThat(rectangle.label()).as("Rectangle 用的是接口的默认实现").isEqualTo("<矩形>");
    }

    /** 故意只重写 equals，漏掉 hashCode，用来演示契约的重要性。 */
    private static final class OnlyEqualsIsBroken {

        private final int cents;

        OnlyEqualsIsBroken(int cents) {
            this.cents = cents;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof OnlyEqualsIsBroken that && this.cents == that.cents;
        }

        // 故意不重写 hashCode
    }
}
