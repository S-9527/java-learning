package dev.javalearning.basics.inheritance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.javalearning.basics.inheritance.broken.BrokenMoney;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 05 课的断言。
 *
 * <p>每一条对应 {@code docs/05-继承与多态.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*inheritance*'}
 */
class InheritanceTest {

    // ── 1. 继承 ────────────────────────────────────────────────────

    @Test
    @DisplayName("子类直接拿到父类的具体方法和字段")
    void subclassInheritsConcreteMembers() {
        Circle circle = new Circle(1);
        // describe() 和 name 都来自父类 Shape，Circle 没写
        assertThat(circle.describe()).isEqualTo("圆 的面积是 3.14");
        assertThat(circle.toString()).startsWith("圆");
    }

    @Test
    @DisplayName("protected 字段子类可以直接读，外面不行")
    void protectedIsVisibleToSubclassOnly() {
        // Circle.toString() 里直接用了父类的 name，这就是 protected 的用武之地
        assertThat(new Circle(1).toString()).isEqualTo("圆(半径 1.0)");
        // 而 InheritanceDemo 里读不到 circle.name —— 那是 broken/ReachIntoPrivate.java 讲的
    }

    @Test
    @DisplayName("子类构造器第一句必须是 super(...)，父类构造器先跑")
    void superCallMustComeFirst() {
        // Circle 的构造器第一句是 super("圆")，所以 name 一定有值
        assertThat(new Circle(2).describe()).contains("圆");
    }

    // ── 2. 重写 ────────────────────────────────────────────────────

    @Test
    @DisplayName("子类重写之后，调用到的是子类的实现")
    void overrideReplacesTheParentImplementation() {
        assertThat(new Circle(1).area()).isEqualTo(Math.PI, org.assertj.core.api.Assertions.offset(1e-9));
        assertThat(new Rectangle(2, 3).area()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("@Override 让编译器帮你抓拼写错误")
    void overrideAnnotationCatchesTypos() {
        // 真实错误在 broken/WrongOverrideName.java
        assertThat(new Circle(1).area()).isGreaterThan(3.1);
    }

    // ── 3. 多态 ────────────────────────────────────────────────────

    @Test
    @DisplayName("同一个调用，不同的子类给出不同的结果")
    void oneCallManyResults() {
        List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3));
        assertThat(shapes.get(0).area()).isNotEqualTo(shapes.get(1).area());
    }

    @Test
    @DisplayName("调用方不需要知道具体类型——totalArea 里只有 Shape")
    void callerIgnoresConcreteTypes() {
        List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3), new Circle(0.5));
        assertThat(InheritanceDemo.totalArea(shapes))
                .isEqualTo(Math.PI + 6.0 + Math.PI * 0.25,
                        org.assertj.core.api.Assertions.offset(1e-9));
    }

    @Test
    @DisplayName("新增子类不用改老代码——这就是多态的价值")
    void addingASubclassDoesNotBreakCallers() {
        // Triangle 是后来加的，totalArea 一行没改就能算它
        List<Shape> shapes = List.of(new Triangle(3, 4, 5));
        assertThat(InheritanceDemo.totalArea(shapes)).isEqualTo(6.0);
    }

    // ── 4. 同名 static 是隐藏 ──────────────────────────────────────

    @Test
    @DisplayName("同名 static 方法是隐藏不是重写：按变量声明的类型绑定")
    void staticMethodsAreHiddenNotOverridden() {
        Circle circle = new Circle(1);
        Shape asShape = circle;

        assertThat(circle.kind()).isEqualTo("circle");   // 变量类型是 Circle
        assertThat(asShape.kind()).isEqualTo("shape");   // 变量类型是 Shape
        // 但 asShape 里装的确实是那个 Circle
        assertThat(asShape).isSameAs(circle);
        // 换成实例方法就按对象走
        assertThat(asShape.area()).isEqualTo(circle.area());
    }

    // ── 5. instanceof / 向下转型 ───────────────────────────────────

    @Test
    @DisplayName("instanceof 看的是实际装的什么对象")
    void instanceofLooksAtTheRealObject() {
        Shape asShape = new Circle(2);
        assertThat(asShape instanceof Circle).isTrue();
        assertThat(asShape instanceof Rectangle).isFalse();
    }

    @Test
    @DisplayName("instanceof 加模式匹配，判断和取值一步完成")
    void patternMatchingCombinesCheckAndCast() {
        Shape asShape = new Circle(2);
        if (asShape instanceof Circle c) {
            assertThat(c.diameter()).isEqualTo(4.0);
        }
        // 而且 && 短路：类型不对时右边压根不跑
        assertThat(List.of(new Circle(1), new Rectangle(5, 1)).stream()
                .filter(s -> s instanceof Circle c && c.diameter() > 3)
                .count()).isZero();
    }

    @Test
    @DisplayName("向下转型要显式写出来")
    void downcastIsExplicit() {
        Shape asShape = new Circle(2);
        Circle back = (Circle) asShape;
        assertThat(back.radius()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("转错的类型要到运行期才抛 ClassCastException")
    void wrongDowncastThrowsAtRuntime() {
        Shape rectangle = new Rectangle(2, 3);
        assertThatThrownBy(() -> {
            @SuppressWarnings("unused")
            Circle wrong = (Circle) rectangle;
        }).isInstanceOf(ClassCastException.class);
    }

    // ── 6. 抽象类 ──────────────────────────────────────────────────

    @Test
    @DisplayName("抽象类不能直接造对象——这条规则由编译器把关")
    void abstractClassCannotBeInstantiated() throws NoSuchMethodException {
        // 正经写法 new Shape("圆") 编译不过，真实错误见 broken/AbstractInstantiation.java。
        // 这里不去用反射绕编译器（那抛什么异常取决于调用方在哪个包，是个坏断言），
        // 只从类本身确认：Shape 被声明成了 abstract。
        Class<?> shape = Shape.class;
        assertThat(java.lang.reflect.Modifier.isAbstract(shape.getModifiers())).isTrue();
        // 而且它确实带了一个没实现的抽象方法
        assertThat(shape.getMethod("area").getModifiers())
                .matches(m -> java.lang.reflect.Modifier.isAbstract(m));
    }

    @Test
    @DisplayName("抽象类照样有构造器和具体方法")
    void abstractClassHasConcreteParts() {
        // describe() 是具体方法，构造器通过 super(...) 被调用
        assertThat(new Circle(1).describe()).contains("面积");
    }

    // ── 7. equals / hashCode 契约 ──────────────────────────────────

    @Test
    @DisplayName("重写 equals 之后，内容相同就算相等")
    void overriddenEqualsComparesContent() {
        Money a = new Money(100);
        Money b = new Money(100);
        assertThat(a).isNotSameAs(b);
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("equals 判相等的两个对象，hashCode 必须一样")
    void hashCodeFollowsEquals() {
        assertThat(new Money(100)).hasSameHashCodeAs(new Money(100));
        assertThat(new Money(100).hashCode()).isEqualTo(new Money(100).hashCode());
    }

    @Test
    @DisplayName("契约成立，HashSet 才能去重")
    void hashSetDeduplicatesWithTheContract() {
        Set<Money> wallet = new HashSet<>();
        wallet.add(new Money(100));
        wallet.add(new Money(100));
        wallet.add(new Money(100));
        assertThat(wallet).hasSize(1);
    }

    @Test
    @DisplayName("只重写 equals 漏 hashCode，HashSet 静默剩 3 个——编译通过、运行不报错")
    void brokenHashCodeContractKeepsDuplicates() {
        Set<BrokenMoney> wallet = new HashSet<>();
        wallet.add(new BrokenMoney(100));
        wallet.add(new BrokenMoney(100));
        wallet.add(new BrokenMoney(100));
        // equals 明明说这三个相等，但 hashCode 各不相同 → 落进不同桶 → equals 没机会被调
        assertThat(wallet).hasSize(3);
    }
}
