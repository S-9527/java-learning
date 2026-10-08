package dev.javaquest.basics.inheritance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 05 章：继承与多态。
 *
 * <p>这个类本身就是文档。方法名是英文，中文句子放在 {@code @DisplayName} 里。
 *
 * <p>分组对应文档的一到六节。
 */
@DisplayName("05 继承与多态")
class InheritanceTest {

    @Nested
    @DisplayName("一、继承")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Extending {

        @Test
        @Order(1)
        @DisplayName("子类对象能直接用父类的具体方法")
        void inheritsParentMethods() {
            Circle circle = new Circle(2);

            assertThat(circle.describe())
                    .as("describe() 是父类的具体方法，子类一个字都没重写。父类填好名字，"
                            + "子类填好 area()，两边拼起来就是这句话")
                    .isEqualTo("圆 的面积是 12.57");
        }

        @Test
        @Order(2)
        @DisplayName("super(...) 的参数决定了父类的字段")
        void superCallFillsParentFields() {
            Circle circle = new Circle(1);
            Rectangle rectangle = new Rectangle(2, 3);

            assertThat(circle.name()).as("Circle 的构造器调 super(\"圆\")").isEqualTo("圆");
            assertThat(rectangle.name()).as("Rectangle 的构造器调 super(\"矩形\")").isEqualTo("矩形");
        }

        @Test
        @Order(3)
        @DisplayName("protected 字段子类能直接读")
        void protectedFieldIsVisibleToSubclasses() {
            Circle circle = new Circle(1);

            assertThat(circle)
                    .as("Shape.name 是 protected，Circle.toString() 里能直接写 name。"
                            + "换成 private 就得调 getter，换成不写修饰符就只有同包能用")
                    .isInstanceOf(Shape.class);
            assertThat(circle.toString())
                    .as("输出的是子类自己写的格式，用到了继承来的 name 字段")
                    .isEqualTo("圆(半径 1.0)");
        }
    }

    @Nested
    @DisplayName("二、重写")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Overriding {

        @Test
        @Order(1)
        @DisplayName("子类重写之后，调用到的是子类的实现")
        void subclassImplementationWins() {
            Circle circle = new Circle(1);
            Rectangle rectangle = new Rectangle(2, 3);

            assertThat(circle.area()).as("Circle 的算法是 πr²").isCloseTo(Math.PI, within());
            assertThat(rectangle.area()).as("Rectangle 的算法是宽乘高").isEqualTo(6.0);
        }

        @Test
        @Order(2)
        @DisplayName("同名 static 方法是隐藏，不是重写")
        void staticMethodsAreHiddenNotOverridden() {
            Circle circle = new Circle(1);
            Shape asShape = circle;      // 同一个对象，换个类型看它

            assertThat(Circle.kind()).as("Circle 自己的静态方法").isEqualTo("circle");
            assertThat(Shape.kind()).as("Shape 自己的静态方法").isEqualTo("shape");
            assertThat(circle.kind())
                    .as("写成 Circle 时，编译期就定死了调 Circle.kind()。运行期换成别的对象也没用")
                    .isEqualTo("circle");
            assertThat(asShape.kind())
                    .as("同一个对象，写成 Shape，调的却是 Shape.kind()。static 方法不参加多态")
                    .isEqualTo("shape");
        }
    }

    @Nested
    @DisplayName("三、多态")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Polymorphism {

        @Test
        @Order(1)
        @DisplayName("同一个调用，不同的子类给出不同的结果")
        void oneCallManyResults() {
            List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3));

            assertThat(shapes.get(0).area()).as("第一个是圆").isCloseTo(Math.PI, within());
            assertThat(shapes.get(1).area()).as("第二个是矩形").isEqualTo(6.0);
            assertThat(shapes)
                    .as("变量类型都是 Shape，实际装的是不同子类，所以 area() 的行为由对象自己决定")
                    .hasSize(2);
        }

        @Test
        @Order(2)
        @DisplayName("调用方不需要知道具体类型")
        void callerIgnoresConcreteTypes() {
            List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3), new Circle(2));

            double total = ShapeDemo.totalArea(shapes);

            assertThat(total)
                    .as("ShapeDemo.totalArea 的参数是 List<? extends Shape>，从头到尾没提过 Circle "
                            + "或 Rectangle。加新子类时它一行都不用改")
                    .isCloseTo(Math.PI * 5 + 6, within());
        }

        @Test
        @Order(3)
        @DisplayName("父类的方法模板可以被不同子类填出不同文字")
        void templateMethodWithDifferentFillings() {
            List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3));

            assertThat(ShapeDemo.describeAll(shapes))
                    .as("describe() 只写在父类一处，两句输出都由它拼出来")
                    .isEqualTo("圆 的面积是 3.14；矩形 的面积是 6.0");
        }
    }

    @Nested
    @DisplayName("四、类型检查")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TypeChecks {

        @Test
        @Order(1)
        @DisplayName("向上转型是隐式的，写子类给父类不用转换")
        void upcastIsImplicit() {
            Shape asShape = new Circle(1);

            assertThat(asShape.area())
                    .as("变量声明成 Shape，调的却还是 Circle.area()。this 记的还是那个 Circle 对象")
                    .isCloseTo(Math.PI, within());
        }

        @Test
        @Order(2)
        @DisplayName("instanceof 看的是实际装的什么对象")
        void instanceofLooksAtTheRealObject() {
            Shape circle = new Circle(1);
            Shape rectangle = new Rectangle(2, 3);

            assertThat(circle).as("声明成 Shape，实际是 Circle").isInstanceOf(Circle.class);
            assertThat(circle).as("Circle 往上数，每一层祖先都成立").isInstanceOf(Shape.class);
            assertThat(circle).as("Object 是所有人的祖先").isInstanceOf(Object.class);
            assertThat(rectangle).isNotInstanceOf(Circle.class);
        }

        @Test
        @Order(3)
        @DisplayName("instanceof 加上模式匹配，判断和取值一步做完")
        void patternMatchingCombinesCheckAndCast() {
            List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3), new Rectangle(3, 4));

            double totalWidth = 0;
            for (Shape shape : shapes) {
                if (shape instanceof Rectangle r && r.getWidth() > 2) {
                    totalWidth += r.getWidth();
                }
            }

            assertThat(totalWidth)
                    .as("旧写法要先 if (x instanceof Rectangle) 再强转一次。写成 instanceof Rectangle r "
                            + "之后 r 已经在手上了，而且如果它不是 Rectangle，后面的代码根本不会进")
                    .isEqualTo(3.0);
        }

        @Test
        @Order(4)
        @DisplayName("向下转型要显式写出来")
        void downcastIsExplicit() {
            Shape asShape = new Circle(2);

            Circle backToCircle = (Circle) asShape;

            assertThat(backToCircle.getRadius())
                    .as("强转不是转换对象，是告诉编译器「你按 Circle 来理解它」。对象本身没变")
                    .isEqualTo(2.0);
        }

        @Test
        @Order(5)
        @DisplayName("转错的类型在运行期抛 ClassCastException")
        void wrongDowncastThrows() {
            Shape rectangle = new Rectangle(2, 3);

            assertThatThrownBy(() -> {
                Circle wrong = (Circle) rectangle;
                assertThat(wrong.getRadius()).as("到不了这里").isEqualTo(2.0);
            })
                    .as("编译器允许你强转任何父类引用，它没法确定运行期装的是什么。"
                            + "所以这一步要到运行期才检查")
                    .isInstanceOf(ClassCastException.class);
        }
    }

    @Nested
    @DisplayName("五、抽象类与接口")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AbstractAndInterface {

        @Test
        @Order(1)
        @DisplayName("抽象类不能直接造对象")
        void abstractClassCannotBeInstantiated() {
            assertThat(Modifier.isAbstract(Shape.class.getModifiers()))
                    .as("new Shape(...) 编译就过不了——没有面积可言的图形不存在。"
                            + "所以编译器替你挡住了一整类写错的对象")
                    .isTrue();
        }

        @Test
        @Order(2)
        @DisplayName("抽象类照样有构造器和具体方法")
        void abstractClassHasConcreteParts() {
            Circle circle = new Circle(1);

            assertThat(circle.describe())
                    .as("抽象指的是「造不出实例」，不是「什么都不许有」。构造器、具体方法、字段都照常")
                    .isEqualTo("圆 的面积是 3.14");
            assertThat(Shape.round(3.14159)).as("static 具体方法也能用").isEqualTo(3.14);
        }

        @Test
        @Order(3)
        @DisplayName("接口里的方法隐式是 public abstract")
        void interfaceMethodsArePublicAndAbstract() throws NoSuchMethodException {
            Method name = Describable.class.getMethod("name");

            assertThat(Modifier.isPublic(name.getModifiers()))
                    .as("接口方法不写修饰符，但编译器补上 public——不然没法被类外的代码调用")
                    .isTrue();
            assertThat(Modifier.isAbstract(name.getModifiers()))
                    .as("同理补上 abstract。声明里写不写修饰符都一样")
                    .isTrue();
            assertThat(Describable.class.getFields())
                    .as("接口不能有实例字段，所以这里取不到任何字段")
                    .isEmpty();
        }

        @Test
        @Order(4)
        @DisplayName("default 方法不重写也能用")
        void defaultMethodIsInherited() {
            Rectangle rectangle = new Rectangle(2, 3);

            assertThat(rectangle.label())
                    .as("Rectangle 没有重写 label()，用的是接口给的那份默认实现")
                    .isEqualTo("<矩形>");
        }

        @Test
        @Order(5)
        @DisplayName("重写 default 方法是可选的")
        void overridingADefaultMethodIsOptional() {
            Rectangle untouched = new Rectangle(2, 3);
            Circle overridden = new Circle(2);

            assertThat(untouched.label()).isEqualTo("<矩形>");
            assertThat(overridden.label())
                    .as("Circle 重写了 label()，输出的是它自己那套。两个类实现同一个接口，行为可以不同")
                    .isEqualTo("圆(2.0)");
        }
    }

    @Nested
    @DisplayName("六、重写 Object 的方法")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ObjectMethods {

        @Test
        @Order(1)
        @DisplayName("重写 equals 之后，内容相同就相等")
        void overriddenEqualsComparesContent() {
            Money a = new Money(1234);
            Money b = new Money(1234);
            Money c = new Money(1235);

            assertThat(a).as("金额一样就算相等，哪怕不是同一个对象").isEqualTo(b);
            assertThat(a).as("差一分就不相等").isNotEqualTo(c);
            assertThat(a).as("和别的类型比直接说不相等，不用逐个排除").isNotEqualTo("12.34");
        }

        @Test
        @Order(2)
        @DisplayName("equals 相等的对象，hashCode 必须一样")
        void hashCodeFollowsEquals() {
            Money a = new Money(1234);
            Money b = new Money(1234);

            assertThat(a.hashCode())
                    .as("这条契约必须成立：哈希容器先比 hashCode 再比 equals，"
                            + "两个数对不上，去重就失效了")
                    .isEqualTo(b.hashCode());
        }

        @Test
        @Order(3)
        @DisplayName("契约成立，HashSet 才能去重")
        void hashSetDeduplicatesWithTheContract() {
            Set<Money> set = new HashSet<>();
            set.add(new Money(1234));
            set.add(new Money(1234));

            assertThat(set)
                    .as("两个不同的 Money 对象被合成一个。漏掉 hashCode 时这里会是 2——"
                            + "这就是为什么两对方法必须一起写")
                    .hasSize(1);
        }

        @Test
        @Order(4)
        @DisplayName("重写 toString 让输出可读")
        void overriddenToStringIsReadable() {
            Money money = new Money(1234);

            assertThat(money.toString())
                    .as("不重写的话是 全类名@哈希值，第 04 章已经见到了")
                    .isEqualTo("12元34分");
        }
    }

    /** 面积都是无理数，比较时留一点余量。 */
    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.api.Assertions.within(0.0001);
    }
}
