package dev.javalearning.lambda;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 第 07 课的示例代码：Lambda 表达式。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/lambda/LambdaDemo.java}
 * 每一节对应 {@code docs/07-Lambda表达式.md} 里的一节，输出都是真跑出来的。
 */
public class LambdaDemo {

    public static void main(String[] args) {
        section("1. 传统做法：发一条消息要四步");
        // ① 定义接口 Message（已经写好了）
        // ② 写实现类 Email / Sms
        // ③ new 出对象
        // ④ 调用 send
        Message email = new Email();
        Message sms = new Sms();
        email.send("你的订单已发货");
        sms.send("验证码 8848");
        System.out.println("① 定义接口 → ② 写实现类 → ③ new 对象 → ④ 调 send");
        System.out.println("Email.java 和 Sms.java 里除了渠道名，没有任何别的信息");

        section("2. Lambda 出场：同一个接口，不用建类");
        Message email2 = text -> System.out.println("[Email] " + text);
        Message sms2 = text -> System.out.println("[SMS] " + text);
        email2.send("你的订单已发货");
        sms2.send("验证码 8848");
        System.out.println("Email 类和 Sms 类一个都没用上，Message 接口也没动");

        section("3. 语法简化阶梯：五步，越写越短");
        // 3.1 完整形式：参数带类型，方法体带大括号
        Message full = (String text) -> {
            System.out.println("[完整] " + text);
        };
        full.send("参数类型和大括号都写全");

        // 3.2 参数类型可省：编译器能从接口的抽象方法推断出来
        Message noType = (text) -> {
            System.out.println("[省类型] " + text);
        };
        noType.send("Message.send(String)，编译器知道是 String");

        // 3.3 单参数可省括号
        Message noParen = text -> {
            System.out.println("[省括号] " + text);
        };
        noParen.send("只有一个参数，括号是多余的");

        // 3.4 单表达式可省大括号和分号
        Message oneLine = text -> System.out.println("[省大括号] " + text);
        oneLine.send("方法体只有一个表达式");

        // 3.5 方法引用：方法体只是「把参数原样转交给另一个方法」时的写法
        Message methodRef = System.out::println;
        methodRef.send("方法引用 System.out::println");

        System.out.println("五种写法都是 Message 的实例，行为等价："
                + (full instanceof Message) + " / " + (oneLine instanceof Message));

        // 上面都是「一个参数、没有返回值」，看不出括号和 return 的规矩。
        // 换成两个参数、有返回值就清楚了。
        Calculator addFull = (int a, int b) -> {
            return a + b;                       // 有大括号，return 不能省
        };
        Calculator addShort = (a, b) -> a + b;  // 单个表达式，return 和大括号一起省
        System.out.println("两个参数时括号不能省：(a, b) -> a + b");
        System.out.println("addFull.calculate(3, 4)  = " + addFull.calculate(3, 4) + "（写了 return）");
        System.out.println("addShort.calculate(3, 4) = " + addShort.calculate(3, 4) + "（表达式就是返回值）");
        System.out.println("类型要么全写要么全省，(int a, b) 这种写一半是不行的");
        System.out.println("而 () -> ... 里的空括号不能省：Supplier 就是这种");

        // 方法引用不止 System.out::println 一种，常见的有三类
        Calculator byStatic = Math::max;                        // 静态方法引用
        Function<String, Integer> byInstance = String::length;  // 实例方法引用
        Supplier<ArrayList<String>> byConstructor = ArrayList::new;  // 构造器引用
        System.out.println();
        System.out.println("方法引用（method reference）常见的三类：");
        System.out.println("  Math::max            → " + byStatic.calculate(3, 4) + "（静态方法）");
        System.out.println("  String::length       → " + byInstance.apply("lambda") + "（实例方法）");
        System.out.println("  ArrayList::new       → " + byConstructor.get().getClass().getSimpleName()
                + "（构造器）");
        System.out.println("它和 lambda 是同一件事的两种写法，不是新东西");

        section("4. 函数式接口：只能有一个抽象方法");
        // Message 只有一个抽象方法，所以 lambda 能挂上去。
        // 多一个抽象方法会怎样？真实编译错误见 broken/TwoAbstractMethods.java
        System.out.println("Message 的抽象方法个数 = " + countAbstractMethods(Message.class));
        System.out.println("Calculator 的抽象方法个数 = " + countAbstractMethods(Calculator.class));
        System.out.println("接口名后面能直接跟 -> 的前提，就是这里等于 1");

        // 但规则有个例外，本课真的撞上了：Comparator 数出来是 2，却能接 lambda
        System.out.println();
        System.out.println("那 Comparator 呢？抽象方法个数 = "
                + countAbstractMethods(java.util.Comparator.class));
        java.util.Comparator<String> byLength = (a, b) -> a.length() - b.length();
        System.out.println("它照样接得住 lambda：compare(\"lambda\", \"abc\") = "
                + byLength.compare("lambda", "abc"));
        System.out.println("多出来的那个是 equals(Object)——Object 早就有了，不算数");
        System.out.println("所以准确的说法是：有且只有一个「必须由实现者补上」的抽象方法");

        // 受检异常能不能抛，也由接口的声明决定
        TextReader failing = () -> {
            throw new java.io.IOException("文件不存在");
        };
        try {
            failing.read();
        } catch (java.io.IOException e) {
            System.out.println();
            System.out.println("TextReader.read() 声明了 throws IOException，所以 lambda 里能抛：" + e.getMessage());
        }
        System.out.println("而 Consumer.accept 没声明异常，往里抛受检异常就编译不过（见 broken/）");

        section("5. @FunctionalInterface：帮你挡住第二个抽象方法");
        System.out.println("Message 上有这个注解：" + Message.class.isAnnotationPresent(
                FunctionalInterface.class));
        System.out.println("Calculator 上故意没加：" + Calculator.class.isAnnotationPresent(
                FunctionalInterface.class));
        System.out.println("两个都能接 lambda——注解是可选的，它只是帮你把关");
        System.out.println("真加了第二个抽象方法，注解会让编译直接失败（见 broken/）");

        section("6. JDK 自带的四大函数式接口");
        // Predicate<T>：boolean test(T) —— 判断
        Predicate<Integer> isEven = n -> n % 2 == 0;
        System.out.println("Predicate<T>    boolean test(T)   判断：test(4) = " + isEven.test(4)
                + "，test(7) = " + isEven.test(7));

        // Function<T,R>：R apply(T) —— 转换
        Function<String, Integer> length = s -> s.length();
        System.out.println("Function<T,R>   R apply(T)        转换：apply(\"lambda\") = "
                + length.apply("lambda"));

        // Consumer<T>：void accept(T) —— 消费，没有返回值
        List<String> consumed = new ArrayList<>();
        Consumer<String> collect = s -> consumed.add(s);
        collect.accept("A");
        collect.accept("B");
        System.out.println("Consumer<T>     void accept(T)    消费：accept 两次后 = " + consumed
                + "（返回 void）");

        // Supplier<T>：T get() —— 生产，没有参数
        Supplier<String> fresh = () -> "刚造出来的字符串";
        System.out.println("Supplier<T>     T get()           生产：get() = " + fresh.get()
                + "（不要参数）");
        System.out.println();
        System.out.println("这四个正好凑齐输入输出的四种组合：");
        System.out.println("  Consumer 有进无出 / Supplier 无进有出 / Predicate 有进、出的是判断结果 / Function 有进、出的是转换结果");
        System.out.println("后面 Stream 的 filter / map / forEach / 短路求值全靠它们");

        section("7. 变量捕获：只能读，不能改");
        String prefix = "[捕获] ";          // 事实上 final：声明后再没被改过
        Message captured = text -> System.out.println(prefix + text);
        captured.send("lambda 读到了外面的 prefix");
        System.out.println("prefix 在 lambda 里被改？不行，真实编译错误见 broken/CaptureAndModify.java");

        // 但实例字段和数组元素不受这个限制
        int[] counter = {0};
        Message counting = text -> counter[0]++;      // 改的是数组元素，不是变量 counter
        counting.send("第一条");
        counting.send("第二条");
        System.out.println("数组元素可以改：counter[0] = " + counter[0]);
        System.out.println("（捕获的是变量 counter 的值的拷贝，那个拷贝一直指向同一个数组）");

        // 为什么非要 effectively final？因为 lambda 可能比那个方法活得久
        Message escaped = makeEscapingLambda("方法已经返回了，这个值还在");
        System.out.println();
        System.out.println("更关键的是：lambda 可能比声明它的那个方法活得久");
        escaped.send("现在才调用");
        System.out.println("makeEscapingLambda 早就返回了，栈上的局部变量也没了");
        System.out.println("但 lambda 还记得那个值——它记的是一份拷贝，不是那个栈位置");

        section("8. 和匿名内部类的区别：this 指向谁");
        Greeter greeter = new Greeter("Albert");
        System.out.println(greeter.greetingByLambda().get());
        System.out.println(greeter.greetingByAnonymousClass().get());
    }

    /**
     * 返回一个 lambda，而它捕获的是<b>这个方法里的局部变量</b>。
     *
     * <p>方法一返回，栈帧就没了，那个局部变量按理说也不该存在了。
     * 但 lambda 还能正常工作——因为它捕获的是<b>值的一份拷贝</b>，
     * 那份拷贝跟着 lambda 对象一起活在堆上。
     * 这就是「必须 effectively final」的根本原因：拷贝之后如果原变量还能改，
     * 两边就会不一致，而 lambda 永远看不到后来那次修改。
     */
    public static Message makeEscapingLambda(String value) {
        return text -> System.out.println("[逃逸的 lambda] " + value + " ← " + text);
    }

    /**
     * 数一个接口有几个抽象方法——「函数式接口」的判定标准大致就是这个数等于 1。
     *
     * <p>但第 4 节会看到 {@code Comparator} 数出来是 2，却照样是函数式接口：
     * 多出来的那个是 {@code equals(Object)}，而它由 {@code Object} 提供，
     * 不算「必须由实现者补上」。所以这个方法只是个快速近似，不是最终裁判。
     */
    public static long countAbstractMethods(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isAbstract(m.getModifiers()))
                .count();
    }

    /**
     * 把一条消息发到给定的实现上，并把它打印的那一行抓回来。
     *
     * <p>抽出来是为了让测试能直接断言「lambda 和实现类发出的东西一模一样」，
     * 不用去解析整个 stdout。
     */
    public static String sendVia(Message message, String text) {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        java.io.PrintStream original = System.out;
        try {
            System.setOut(new java.io.PrintStream(buffer, true, java.nio.charset.StandardCharsets.UTF_8));
            message.send(text);
        } finally {
            System.setOut(original);
        }
        return buffer.toString(java.nio.charset.StandardCharsets.UTF_8).strip();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
