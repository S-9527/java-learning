package dev.javalearning.basics.classes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 04 课的断言。
 *
 * <p>每一条对应 {@code docs/04-类与对象.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*classes*'}
 */
class ClassesTest {

    // ── 1. 类与实例 ────────────────────────────────────────────────

    @Test
    @DisplayName("同一份说明书能造出很多个互不相干的对象")
    void oneClassManyInstances() {
        Person albert = new Person("Albert", 30);
        Person neil = new Person("Neil", 25);

        assertThat(albert.name()).isEqualTo("Albert");
        assertThat(neil.name()).isEqualTo("Neil");
        assertThat(albert).isNotSameAs(neil);
    }

    @Test
    @DisplayName("每个对象有自己的一份字段，改一个不影响另一个")
    void eachInstanceHasItsOwnFields() {
        Person albert = new Person("Albert", 30);
        Person neil = new Person("Neil", 25);

        albert.birthday();

        assertThat(albert.age()).isEqualTo(31);
        assertThat(neil.age()).isEqualTo(25);
    }

    @Test
    @DisplayName("字段是 private 的，外面只能通过方法碰")
    void privateFieldsAreOnlyReachableThroughMethods() {
        Person albert = new Person("Albert", 30);
        // albert.name = "x";   // ← 编译不过，真实错误在 broken/ReachIntoPrivate.java
        assertThat(albert.name()).isEqualTo("Albert");
    }

    @Test
    @DisplayName("final 字段造好就不能再改，连自己都不能")
    void finalFieldCannotBeReassigned() {
        Person albert = new Person("Albert", 30);
        albert.birthday();           // age 不是 final，可以改
        assertThat(albert.age()).isEqualTo(31);
        // 但没有 setName——因为 name 是 final 的，压根不给改的机会
        assertThatThrownBy(() -> Person.class.getMethod("setName", String.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    // ── 2. 构造器 ──────────────────────────────────────────────────

    @Test
    @DisplayName("构造器的名字必须和类名一模一样")
    void constructorNameMatchesClassName() {
        Person p = new Person("Neil", 25);
        assertThat(p.name()).isEqualTo("Neil");
        assertThat(p.age()).isEqualTo(25);
    }

    @Test
    @DisplayName("一个类没写构造器，编译器会送一个无参的")
    void defaultConstructorIsProvidedWhenNoneIsWritten() {
        ClassesDemo.NoConstructor nc = new ClassesDemo.NoConstructor();
        assertThat(nc.count).isZero();      // int 字段的默认值
        assertThat(nc.text).isNull();       // 引用类型字段的默认值
    }

    @Test
    @DisplayName("一旦自己写了构造器，编译器就不再送无参的那个")
    void writingAnyConstructorSuppressesTheDefaultOne() {
        // Person 写了 Person(String, int)，所以 new Person() 编译不过。
        // 真实错误在 broken/NoMatchingConstructor.java。
        assertThatThrownBy(() -> Person.class.getDeclaredConstructor())
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    @DisplayName("this 用来区分同名的字段和参数")
    void thisDisambiguatesFieldFromParameter() {
        // Person 的构造器里 this.name = name 就是干这个的
        Person p = new Person("SameName", 1);
        assertThat(p.name()).isEqualTo("SameName");
    }

    // ── 3. static ──────────────────────────────────────────────────

    @Test
    @DisplayName("static 字段属于类，所有对象看到的是同一个值")
    void staticFieldIsSharedByAllInstances() {
        // 注意：instanceCounter 是静态字段，跨测试共享，别断言它的绝对值——
        // 别的测试方法也 new 过 ClassesDemo。这里只断言「三个对象看到同一个值」。
        ClassesDemo a = new ClassesDemo("a");
        ClassesDemo b = new ClassesDemo("b");
        ClassesDemo c = new ClassesDemo("c");

        assertThat(a.instanceCounter)
                .isEqualTo(b.instanceCounter)
                .isEqualTo(c.instanceCounter);
        // 但 name 是实例字段，每个对象自己一份
        assertThat(a.name()).isEqualTo("a");
        assertThat(b.name()).isEqualTo("b");
        assertThat(c.name()).isEqualTo("c");
    }

    @Test
    @DisplayName("静态字段可以通过类名直接访问，不需要先造对象")
    void staticFieldNeedsNoInstance() {
        // 同样是跨测试共享的计数器，所以只比较前后差，不断言绝对值
        int before = ClassesDemo.sharedCounter;
        new ClassesDemo("x");
        new ClassesDemo("y");
        assertThat(ClassesDemo.sharedCounter).isEqualTo(before + 2);
    }

    // ── 4. toString / equals ───────────────────────────────────────

    @Test
    @DisplayName("重写 toString 之后，打印出来的是人能读的内容")
    void overriddenToStringIsReadable() {
        assertThat(new Person("Albert", 30)).hasToString("Person{name='Albert', age=30}");
    }

    @Test
    @DisplayName("不重写 toString 就是类名加 @ 加十六进制，认不出是谁")
    void defaultToStringIsUnreadable() {
        String s = new ClassesDemo.NoToString("Albert", 30).toString();
        assertThat(s).contains("ClassesDemo$NoToString@");
    }

    @Test
    @DisplayName("equals 不重写时比的是身份，内容一样也不算相等")
    void defaultEqualsComparesIdentity() {
        Person albert = new Person("Albert", 30);
        Person albertAgain = new Person("Albert", 30);

        assertThat(albert == albertAgain).isFalse();
        assertThat(albert.equals(albertAgain)).isFalse();
        assertThat(albert.equals(albert)).isTrue();
    }
}
