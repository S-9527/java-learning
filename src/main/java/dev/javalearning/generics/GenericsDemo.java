package dev.javalearning.generics;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 06 课的示例代码：泛型 Generics。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/generics/GenericsDemo.java}
 * 每一节对应 {@code docs/06-泛型.md} 里的一节，输出都是真跑出来的。
 *
 * <p>几个「编译不过」的反面教材没法在这里跑（跑得起来就不叫编译不过了），
 * 所以本节只把结论打出来；{@code javac} 的原话逐字贴在文档里。
 */
public class GenericsDemo {

    public static void main(String[] args) {
        section("1. 先让代码疼一次：每加一种类型就抄一个类");
        new IntegerPrinter(42).print();
        new StringPrinter("hello").print();
        System.out.println("两个类的代码一模一样，只有类型不同");
        System.out.println("再加一个 Double 呢？再加一个 Person 呢？");

        section("2. 泛型类：Box<T>，一份代码装所有类型");
        Box<String> text = new Box<>("hello");
        Box<Integer> number = new Box<>(42);
        System.out.println("text.get()   = " + text.get() + "   ← 直接是 String");
        System.out.println("number.get() = " + number.get() + "   ← 直接是 Integer");
        String asString = text.get();
        int asInt = number.get();
        System.out.println("String asString = text.get() → 不用强转：" + asString);
        System.out.println("int asInt = number.get()     → 不用强转，还自动拆箱了：" + (asInt + 1));
        System.out.println("T 由调用方决定：new Box<String> 和 new Box<Integer> 是同一个类的两种用法");

        section("3. 类型参数不能用基本类型");
        Box<Integer> boxed = new Box<>(42);
        System.out.println("Box<Integer> 可以：" + boxed.get());
        System.out.println("Box<int> 编译不过：javac 说 unexpected type / found: int");
        System.out.println("所以要有装箱类型 Integer、Double、Boolean……");

        section("4. 多个类型参数：Pair<K, V>");
        Pair<String, Integer> age = new Pair<>("Albert", 30);
        Pair<Integer, String> flipped = new Pair<>(1, "one");
        System.out.println(age);
        System.out.println(flipped);
        System.out.println("key() 是 String：" + age.key().toUpperCase());
        System.out.println("value() 是 Integer：" + (age.value() + 1));
        System.out.println("static 工厂也能推类型：" + Pair.of("x", 1.5));

        section("5. 有界泛型：<T extends Number>");
        NumberBox<Integer> intBox = new NumberBox<>(10);
        NumberBox<Double> doubleBox = new NumberBox<>(2.5);
        System.out.println("intBox.doubleValue() = " + intBox.doubleValue());
        System.out.println("doubleBox.get()      = " + doubleBox.get());
        System.out.println("两个不同类型的盒子能相加：" + intBox.plus(doubleBox));
        System.out.println("约束之后 T 当 Number 用，所以 doubleValue() 能调");
        System.out.println("代价：new NumberBox<String>(\"x\") 编译不过，String 不在 Number 范围里");
        System.out.println("类和接口一起约束时，类必须写在前面：<T extends Number & Comparable<T>>");

        section("6. 泛型方法：<T> 写在返回类型之前");
        Printer.print("字符串也能打");
        Printer.print(42);
        Printer.print(3.14);
        System.out.println("一个方法搞定所有类型，方法自己声明了 <T>");
        System.out.println("Printer.half(10)   = " + Printer.half(10));
        System.out.println("Printer.half(2.5)  = " + Printer.half(2.5));
        System.out.println("half 的 T 有界，所以方法体里能调 doubleValue()");

        section("7. 类型安全：List<Object> 的错要到运行期才炸");
        List<Object> loose = new ArrayList<>();
        loose.add("Alice");
        loose.add(42);
        System.out.println("List<Object> 什么都能放，两个 add 都编译通过：" + loose);
        System.out.println("取出来是 Object，要用就得强转：");
        try {
            String wrong = (String) loose.get(1);
            System.out.println("居然转过来了：" + wrong);
        } catch (ClassCastException e) {
            System.out.println("  ClassCastException: " + stableMessage(e));
            System.out.println("  ← 编译期一声不吭，运行到这一行才炸");
        }
        List<String> strict = new ArrayList<>();
        strict.add("Alice");
        System.out.println("List<String> 就不一样：strict.add(42) 编译不过，int 转不成 String");
        System.out.println("取出来直接是 String，强转都省了：" + strict.get(0).toUpperCase());

        section("8. 类型擦除：泛型只活在编译期");
        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();
        System.out.println("strings.getClass() == integers.getClass() → "
                + (strings.getClass() == integers.getClass()));
        System.out.println("new ArrayList<String>().getClass()  = " + new ArrayList<String>().getClass().getName());
        System.out.println("new ArrayList<Integer>().getClass() = " + new ArrayList<Integer>().getClass().getName());
        Box<String> stringBox = new Box<>("a");
        Box<Integer> integerBox = new Box<>(1);
        System.out.println("Box<String> 和 Box<Integer> 的 Class 也是同一个 → "
                + (stringBox.getClass() == integerBox.getClass()));
        System.out.println("Box 里 content 字段运行期的类型        = " + erasedFieldType(Box.class));
        System.out.println("NumberBox 里 content 字段运行期的类型  = " + erasedFieldType(NumberBox.class));
        System.out.println("← 擦成上界：没约束擦成 Object，约束了擦成 Number");

        section("9. 通配符：? / ? extends / ? super");
        List<Integer> ints = List.of(1, 2, 3);
        List<Double> doubles = List.of(1.5, 2.5);
        System.out.println("sum(List<Integer>) = " + sum(ints));
        System.out.println("sum(List<Double>)  = " + sum(doubles));
        System.out.println("sum 的参数是 List<? extends Number>，两种都能收");
        List<Number> numberSink = new ArrayList<>();
        addAll(numberSink, 1, 2, 3);
        System.out.println("addAll 往 List<Number> 里加 → " + numberSink);
        List<Object> objectSink = new ArrayList<>();
        addAll(objectSink, 7);
        System.out.println("addAll 往 List<Object> 里加 → " + objectSink);
        System.out.println("addAll 的参数是 List<? super Integer>，Number 和 Object 都能收");
        System.out.println("但 List<String> 不是 List<Object>：直接赋值编译不过");

        section("10. PECS：Producer Extends, Consumer Super");
        List<? extends Number> producer = ints;
        System.out.println("只读的用 extends：producer.get(0) = " + producer.get(0));
        System.out.println("  producer.add(42) 编译不过：? extends Number 不知道具体是哪个子类");
        List<? super Integer> consumer = new ArrayList<Number>();
        consumer.add(42);
        System.out.println("只写的用 super：consumer.add(42) 之后 = " + consumer);
        System.out.println("  consumer.get(0) 拿到的是 Object，信息已经丢了");
        System.out.println("要读又要写，就别用通配符，老老实实写 List<Integer>");

        section("11. 不推荐的用法");
        System.out.println("① 用原始类型 List（raw type）：编译只给警告，错误留到运行期");
        System.out.println("② 用 List<Object> 代替泛型：等于把编译期检查换成运行期强转");
        System.out.println("③ 用泛型数组：new T[10] 编译不过（generic array creation）");
        System.out.println("④ 让 static 方法引用类上的 T：编译不过，得自己声明一份");
        System.out.println();
        System.out.println("本课的结论：泛型不是语法糖，它把类型检查从运行期搬到了编译期。");
    }

    /**
     * 读：只从集合里取东西，所以用 {@code ? extends Number}。
     *
     * <p>写成 {@code List<Number>} 的话 {@code List<Integer>} 传不进来；
     * 写成 {@code List<?>} 的话又拿不到 {@code doubleValue()}。
     */
    public static double sum(List<? extends Number> numbers) {
        double total = 0;
        for (Number number : numbers) {
            total += number.doubleValue();
        }
        return total;
    }

    /**
     * 写：只往集合里放东西，所以用 {@code ? super Integer}。
     *
     * <p>这样 {@code List<Integer>}、{@code List<Number>}、{@code List<Object>}
     * 三种实参都能收——往里面塞 Integer 对它们来说都是安全的。
     */
    public static void addAll(List<? super Integer> target, Integer... values) {
        for (Integer value : values) {
            target.add(value);
        }
    }

    /** 反射看一下擦除之后字段到底变成了什么类型——这是擦除最硬的证据。 */
    static String erasedFieldType(Class<?> type) {
        try {
            return String.valueOf(type.getDeclaredField("content").getType());
        } catch (NoSuchFieldException e) {
            return "（找不到 content 字段）";
        }
    }

    /**
     * 只取异常 message 里稳定的前半段。
     *
     * <p>{@code ClassCastException} 的完整 message 末尾还会带 module / loader 信息，
     * 那些在不同机器上可能不一样，打印出来文档就对不上了。
     */
    static String stableMessage(RuntimeException e) {
        String message = e.getMessage();
        int cut = message.indexOf(" (");
        return cut > 0 ? message.substring(0, cut) : message;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
