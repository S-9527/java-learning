package dev.javalearning.generics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import dev.javalearning.generics.broken.RawTypeList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 06 课的断言。
 *
 * <p>每一条对应 {@code docs/06-泛型.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*generics*'}
 *
 * <p>注意：本课最想证明的几件事是<b>编译期</b>的（{@code Box<int>} 不合法、
 * {@code List<String>} 不是 {@code List<Object>}、类和接口的上界顺序），
 * 这些没法写成断言——编译不过的代码根本进不了测试。
 * 它们在 {@code src/main/.../generics/broken/} 里，文档中贴了真实的 javac 原话。
 * 这里只钉住那些「编译得过、能观察」的行为。
 */
class GenericsTest {

    // ── 1. 笨办法：一个类型一个类 ──────────────────────────────────

    @Test
    @DisplayName("没有泛型时，两个 Printer 做的是同一件事，只是类型不同")
    void duplicatedPrintersDoTheSameThing() {
        // 两个类的结构完全一样：一个字段 + 一个构造器 + 一个 print()。
        // 唯一的差别是字段类型。这就是泛型要消灭的重复。
        assertThat(IntegerPrinter.class.getDeclaredFields()[0].getType()).isEqualTo(Integer.class);
        assertThat(StringPrinter.class.getDeclaredFields()[0].getType()).isEqualTo(String.class);
        assertThat(IntegerPrinter.class.getDeclaredMethods().length)
                .isEqualTo(StringPrinter.class.getDeclaredMethods().length);
    }

    // ── 2. 泛型类 ──────────────────────────────────────────────────

    @Test
    @DisplayName("Box<T> 取出来直接就是 T，不用强转")
    void boxReturnsTheDeclaredTypeWithoutCast() {
        Box<String> text = new Box<>("hello");
        String value = text.get();          // 不用写 (String)
        assertThat(value).isEqualTo("hello");

        Box<Integer> number = new Box<>(42);
        int raw = number.get();             // 不用强转，还自动拆箱
        assertThat(raw).isEqualTo(42);
    }

    @Test
    @DisplayName("同一个 Box 类，换个类型实参就是另一种盒子")
    void oneClassManyTypeArguments() {
        Box<String> text = new Box<>("hello");
        Box<Integer> number = new Box<>(42);

        text.set("world");
        number.set(7);
        assertThat(text.get()).isEqualTo("world");
        assertThat(number.get()).isEqualTo(7);
    }

    // ── 3. 类型参数不能用基本类型 ──────────────────────────────────

    @Test
    @DisplayName("基本类型不能当类型参数，要用装箱类型——自动装箱让它看起来像")
    void primitiveNeedsItsWrapper() {
        // Box<int> 编译不过（见 broken/PrimitiveTypeArgument.java）。
        // 但 Box<Integer> 配上自动装箱/拆箱，写起来几乎没差别：
        Box<Integer> box = new Box<>(42);
        int value = box.get();
        assertThat(value).isEqualTo(42);
    }

    // ── 4. 多个类型参数 ────────────────────────────────────────────

    @Test
    @DisplayName("Pair<K, V> 有两个类型参数，各管各的")
    void pairHasTwoTypeParameters() {
        Pair<String, Integer> age = new Pair<>("Albert", 30);
        assertThat(age.key()).isEqualTo("Albert");
        assertThat(age.value()).isEqualTo(30);
        assertThat(age).hasToString("Pair[Albert = 30]");
    }

    @Test
    @DisplayName("调换 K 和 V 就是另一个类型，同一个类")
    void swappingTypeArgumentsMakesAnotherType() {
        Pair<Integer, String> flipped = new Pair<>(1, "one");
        assertThat(flipped.key()).isEqualTo(1);
        assertThat(flipped.value()).isEqualTo("one");
    }

    @Test
    @DisplayName("泛型 static 方法自己声明 <K, V>，编译器从参数推出类型")
    void staticFactoryInfersTypeArguments() {
        // Pair.of 是 static 的，用不了类上的 K/V，所以它自己声明了一份
        Pair<String, Double> pair = Pair.of("x", 1.5);
        assertThat(pair.key()).isEqualTo("x");
        assertThat(pair.value()).isEqualTo(1.5);
    }

    // ── 5. 有界泛型 ────────────────────────────────────────────────

    @Test
    @DisplayName("加了 extends Number 之后，T 就能当 Number 用了")
    void boundedTypeParameterCanCallBoundMethods() {
        // 没有上界时 T 被当成 Object，doubleValue() 调不到（见 broken/UnboundedCall.java）
        assertThat(new NumberBox<>(10).doubleValue()).isEqualTo(10.0);
        assertThat(new NumberBox<>(2.5).get()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("通配符让两个不同类型的 NumberBox 能一起算")
    void wildcardLetsDifferentTypeArgumentsMix() {
        NumberBox<Integer> intBox = new NumberBox<>(10);
        NumberBox<Double> doubleBox = new NumberBox<>(2.5);
        // plus 的参数是 NumberBox<? extends Number>，所以 Integer 和 Double 能相加。
        // 要是写成 NumberBox<T>，这两个盒子就没法放在一起了。
        assertThat(intBox.plus(doubleBox)).isEqualTo(12.5);
    }

    @Test
    @DisplayName("上界写成 extends，哪怕约束的是接口；多个上界时类必须排第一")
    void boundsUseExtendsAndClassComesFirst() {
        // <T extends Number & Comparable<T>> 里的两个 extends/& 都合法。
        // 顺序写反会编译不过（见 broken/WrongBoundOrder.java）。
        assertThat(Printer.half(10)).isEqualTo(5.0);
        // Comparable 是接口，约束它照样写 extends：
        assertThat(maxOf(3, 7)).isEqualTo(7);
    }

    /** 演示「接口上界也写 extends」：Integer 既是 Number 也实现了 Comparable。 */
    static <T extends Number & Comparable<T>> T maxOf(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    // ── 6. 泛型方法 ────────────────────────────────────────────────

    @Test
    @DisplayName("泛型方法 <T> 写在返回类型之前，一个方法吃所有类型")
    void genericMethodHandlesEveryType() {
        // Printer.print 的签名是 public static <T> void print(T content)
        // 它自己声明 T，跟类没关系（Printer 本身不是泛型类）
        assertThat(Printer.half(10)).isEqualTo(5.0);
        assertThat(Printer.half(2.5)).isEqualTo(1.25);
        assertThat(Printer.half(9L)).isEqualTo(4.5);
    }

    // ── 7. 类型安全 ────────────────────────────────────────────────

    @Test
    @DisplayName("List<Object> 什么都能放，错误留到运行期强转时才炸")
    void objectListDefersTheErrorToRuntime() {
        List<Object> loose = new ArrayList<>();
        loose.add("Alice");
        loose.add(42);                       // 编译通过——这正是问题
        assertThat(loose).containsExactly("Alice", 42);

        // 取出来是 Object，要当 String 用必须强转
        assertThat(loose.get(0)).isInstanceOf(String.class);
        assertThatThrownBy(() -> {
            @SuppressWarnings("unused")
            String wrong = (String) loose.get(1);   // 运行期才炸
        }).isInstanceOf(ClassCastException.class)
          .hasMessageContaining("class java.lang.Integer cannot be cast to class java.lang.String");
    }

    @Test
    @DisplayName("List<String> 取出来直接是 String，强转和运行期风险都没了")
    void typedListGivesTypeSafety() {
        List<String> strict = new ArrayList<>();
        strict.add("Alice");
        // strict.add(42) 编译不过——错误被提前到了编译期
        String first = strict.get(0);
        assertThat(first).isEqualTo("Alice");
        assertThat(first.toUpperCase()).isEqualTo("ALICE");
    }

    // ── 8. 类型擦除 ────────────────────────────────────────────────

    @Test
    @DisplayName("擦除证据：List<String> 和 List<Integer> 运行期是同一个类")
    void differentTypeArgumentsShareOneClass() {
        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();

        assertThat(strings.getClass()).isSameAs(integers.getClass());
        assertThat(strings.getClass()).isEqualTo(ArrayList.class);
        // 这行能编译，也说明擦除是真的：类型参数在运行期不存在
        assertThat(strings.getClass() == integers.getClass()).isTrue();
    }

    @Test
    @DisplayName("擦除证据：泛型类的字段运行期被擦成 Object / 上界")
    void fieldsEraseToObjectOrTheirBound() throws NoSuchFieldException {
        Field boxContent = Box.class.getDeclaredField("content");
        assertThat(boxContent.getType()).isEqualTo(Object.class);

        // 有上界的擦成上界，不是 Object
        Field numberBoxContent = NumberBox.class.getDeclaredField("content");
        assertThat(numberBoxContent.getType()).isEqualTo(Number.class);
    }

    @Test
    @DisplayName("擦除证据：不同泛型实参的 Class 对象相同")
    void erasedClassObjectsAreIdentical() {
        assertThat(new Box<String>("a").getClass()).isSameAs(new Box<Integer>(1).getClass());
        assertThat(new ArrayList<String>().getClass()).isSameAs(new ArrayList<Integer>().getClass());
    }

    // ── 9. 通配符 ──────────────────────────────────────────────────

    @Test
    @DisplayName("? extends：只读，List<Integer> 和 List<Double> 都能传进来")
    void extendsWildcardAcceptsSubtypes() {
        // 参数是 List<? extends Number>，写成 List<Number> 的话 List<Integer> 传不进来
        assertThat(GenericsDemo.sum(List.of(1, 2, 3))).isEqualTo(6.0);
        assertThat(GenericsDemo.sum(List.of(1.5, 2.5))).isEqualTo(4.0);
    }

    @Test
    @DisplayName("? super：只写，List<Integer> / Number / Object 都能收")
    void superWildcardAcceptsSupertypes() {
        List<Object> objects = new ArrayList<>();
        GenericsDemo.addAll(objects, 7);
        assertThat(objects).containsExactly(7);

        List<Number> numbers = new ArrayList<>();
        GenericsDemo.addAll(numbers, 1, 2, 3);
        assertThat(numbers).containsExactly(1, 2, 3);

        // 反过来不行：List<? super Integer> 里取出来的东西只能当 Object 用
        Object taken = numbers.get(0);
        assertThat(taken).isEqualTo(1);
    }

    @Test
    @DisplayName("List<String> 和 List<Object> 运行期是同一个类，但编译期互不相容")
    void genericTypesAreInvariantAtCompileTime() {
        // 编译期：List<String> 不能赋给 List<Object>（见 broken/StringListIsNotObjectList.java）
        // 运行期：两者被擦成同一个类，泛型的「不变性」只剩编译期这一道防线
        List<String> strings = new ArrayList<>();
        List<Object> objects = new ArrayList<>();
        assertThat((Class<?>) strings.getClass()).isSameAs(objects.getClass());
    }

    // ── 10. PECS ───────────────────────────────────────────────────

    @Test
    @DisplayName("PECS：生产者用 extends 读，消费者用 super 写")
    void pecsReadWithExtendsWriteWithSuper() {
        List<Integer> source = new ArrayList<>(List.of(1, 2, 3));

        // Producer Extends：只从里面读
        List<? extends Number> producer = source;
        assertThat(producer.get(0)).isEqualTo(1);

        // Consumer Super：只往里面写
        List<Object> sink = new ArrayList<>();
        List<? super Integer> consumer = sink;
        consumer.add(42);
        assertThat(sink).containsExactly(42);
    }

    // ── 11. 不推荐的用法 ───────────────────────────────────────────

    @Test
    @DisplayName("raw type 把编译期检查还回去了：编译只警告，错误留到运行期")
    void rawTypeGivesUpCompileTimeChecking() {
        List<?> raw = RawTypeList.buildMixedList();
        assertThat(raw).hasSize(2);
        assertThat(raw.get(1)).isInstanceOf(Integer.class);

        // 写成 List<String> 的话 raw.add(42) 根本编译不过；
        // 用了 raw type，只能在运行期被 ClassCastException 教育
        assertThatThrownBy(() -> {
            @SuppressWarnings("unused")
            String wrong = (String) raw.get(1);
        }).isInstanceOf(ClassCastException.class);
    }

    @Test
    @DisplayName("通配符列表读出来的元素类型信息会丢，这也是 PECS 的代价")
    void wildcardLosesSomeTypeInformation() {
        List<? super Integer> consumer = new ArrayList<Number>();
        consumer.add(42);
        // get 的返回类型是 Object，不是 Integer——写入端的信息读不回来
        Object value = consumer.get(0);
        assertThat(value).isInstanceOf(Integer.class);
        assertThat(consumer.get(0)).isEqualTo(42);
    }
}
