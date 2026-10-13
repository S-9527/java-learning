package dev.javalearning.lambda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 07 课的断言。
 *
 * <p>每一条对应 {@code docs/07-Lambda表达式.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*lambda*'}
 */
class LambdaTest {

    // ── 1. 传统做法：接口 + 实现类 ──────────────────────────────────

    @Test
    @DisplayName("传统做法要先有实现类，接口本身不能 new")
    void traditionalApproachNeedsImplementationClasses() {
        Message email = new Email();
        Message sms = new Sms();

        assertThat(email).isInstanceOf(Message.class);
        assertThat(sms).isInstanceOf(Message.class);
        // 两个类除了打印的渠道名不同，没有任何别的信息
        assertThat(LambdaDemo.sendVia(email, "你好")).isEqualTo("[Email] 你好");
        assertThat(LambdaDemo.sendVia(sms, "你好")).isEqualTo("[SMS] 你好");
    }

    // ── 2. Lambda 直接实现接口 ─────────────────────────────────────

    @Test
    @DisplayName("lambda 不用建类，效果和实现类一模一样")
    void lambdaReplacesTheImplementationClass() {
        Message emailByLambda = text -> System.out.println("[Email] " + text);

        // 一个用实现类，一个用 lambda，发出去的东西逐字相同
        assertThat(LambdaDemo.sendVia(emailByLambda, "你好"))
                .isEqualTo(LambdaDemo.sendVia(new Email(), "你好"));
    }

    @Test
    @DisplayName("lambda 本身就是接口的一个实例")
    void lambdaIsAnInstanceOfTheInterface() {
        Message message = text -> System.out.println(text);
        assertThat(message).isInstanceOf(Message.class);
        // 它有类，只是编译器生成的，名字不好看，所以别去断言类名
        assertThat(message.getClass().isSynthetic() || message.getClass().getName().contains("$$"))
                .as("lambda 的运行时类型是编译器合成的，不是 Email")
                .isTrue();
    }

    // ── 3. 语法简化阶梯 ────────────────────────────────────────────

    @Test
    @DisplayName("五种写法行为等价，只是长短不同")
    void allFiveFormsBehaveTheSame() {
        List<String> seen = new ArrayList<>();

        Message full = (String text) -> {
            seen.add("完整:" + text);
        };
        Message noType = (text) -> {
            seen.add("省类型:" + text);
        };
        Message noParen = text -> {
            seen.add("省括号:" + text);
        };
        Message oneLine = text -> seen.add("省大括号:" + text);
        Message methodRef = seen::add;

        full.send("a");
        noType.send("a");
        noParen.send("a");
        oneLine.send("a");
        methodRef.send("a");

        assertThat(seen).containsExactly(
                "完整:a", "省类型:a", "省括号:a", "省大括号:a", "a");
    }

    @Test
    @DisplayName("两个参数时括号不能省，单个表达式时 return 和大括号一起省")
    void parameterAndReturnRules() {
        Calculator withBraces = (int a, int b) -> {
            return a + b;
        };
        Calculator expressionOnly = (a, b) -> a + b;

        assertThat(withBraces.calculate(3, 4)).isEqualTo(7);
        assertThat(expressionOnly.calculate(3, 4)).isEqualTo(7);
    }

    @Test
    @DisplayName("方法引用和 lambda 是同一件事的两种写法")
    void methodReferenceEqualsLambda() {
        Function<String, Integer> byLambda = s -> s.length();
        Function<String, Integer> byReference = String::length;

        assertThat(byReference.apply("lambda")).isEqualTo(byLambda.apply("lambda"));
        assertThat(byReference.apply("lambda")).isEqualTo(6);
    }

    // ── 4. 函数式接口 ──────────────────────────────────────────────

    @Test
    @DisplayName("函数式接口有且只有一个抽象方法")
    void functionalInterfaceHasExactlyOneAbstractMethod() {
        assertThat(LambdaDemo.countAbstractMethods(Message.class)).isEqualTo(1);
        assertThat(LambdaDemo.countAbstractMethods(Calculator.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Comparator 数出来是 2 个抽象方法，却照样接得住 lambda")
    void comparatorIsFunctionalDespiteTwoAbstractMethods() {
        // 多出来的那个是 equals(Object)——Object 已经提供了，不算「必须补上」
        assertThat(LambdaDemo.countAbstractMethods(java.util.Comparator.class)).isEqualTo(2);

        java.util.Comparator<String> byLength = (a, b) -> a.length() - b.length();
        assertThat(byLength.compare("lambda", "abc")).isEqualTo(3);
    }

    @Test
    @DisplayName("接口声明了 throws，lambda 里就能抛受检异常")
    void checkedExceptionDependsOnTheInterfaceSignature() {
        // TextReader.read() 写了 throws IOException，所以这里能抛
        TextReader failing = () -> {
            throw new java.io.IOException("文件不存在");
        };

        assertThatThrownBy(failing::read)
                .isInstanceOf(java.io.IOException.class)
                .hasMessage("文件不存在");

        // 而 Consumer.accept 没声明异常，往里抛受检异常编译不过（见 broken/）
    }

    // ── 5. @FunctionalInterface 注解 ───────────────────────────────

    @Test
    @DisplayName("@FunctionalInterface 是可选的：加了和没加都能接 lambda")
    void annotationIsOptional() {
        assertThat(Message.class.isAnnotationPresent(FunctionalInterface.class)).isTrue();
        assertThat(Calculator.class.isAnnotationPresent(FunctionalInterface.class)).isFalse();

        // 没加注解的那个照样能用
        Calculator minus = (a, b) -> a - b;
        assertThat(minus.calculate(7, 4)).isEqualTo(3);
    }

    // ── 6. 四大函数式接口 ──────────────────────────────────────────

    @Test
    @DisplayName("Predicate：boolean test(T)，用来判断")
    void predicateJudges() {
        Predicate<Integer> isEven = n -> n % 2 == 0;
        assertThat(isEven.test(4)).isTrue();
        assertThat(isEven.test(7)).isFalse();
    }

    @Test
    @DisplayName("Function：R apply(T)，用来转换")
    void functionTransforms() {
        Function<String, Integer> length = String::length;
        assertThat(length.apply("lambda")).isEqualTo(6);
        // 类型也能换：String → String
        Function<String, String> upper = String::toUpperCase;
        assertThat(upper.apply("lambda")).isEqualTo("LAMBDA");
    }

    @Test
    @DisplayName("Consumer：void accept(T)，只进不出")
    void consumerConsumes() {
        List<String> collected = new ArrayList<>();
        Consumer<String> collect = collected::add;

        collect.accept("A");
        collect.accept("B");

        assertThat(collected).containsExactly("A", "B");
    }

    @Test
    @DisplayName("Supplier：T get()，只出不进")
    void supplierProduces() {
        Supplier<String> fresh = () -> "刚造出来的";
        assertThat(fresh.get()).isEqualTo("刚造出来的");
        // 每次 get() 都会重新执行一遍方法体，不是缓存
        int[] calls = {0};
        Supplier<Integer> counter = () -> ++calls[0];
        assertThat(counter.get()).isEqualTo(1);
        assertThat(counter.get()).isEqualTo(2);
    }

    // ── 7. 变量捕获 ────────────────────────────────────────────────

    @Test
    @DisplayName("lambda 能读外层的局部变量，前提是它事实上的 final")
    void capturesEffectivelyFinalLocal() {
        String prefix = "[捕获] ";
        Message message = text -> System.out.println(prefix + text);

        assertThat(LambdaDemo.sendVia(message, "内容")).isEqualTo("[捕获] 内容");
    }

    @Test
    @DisplayName("局部变量本身不能改，但数组元素和字段可以——捕获的是值的拷贝")
    void capturedValueIsACopy() {
        // 变量 counter 自己从没被重新赋值，所以它是 effectively final；
        // 变的是它指向的那个数组里的元素，这不受限制。
        int[] counter = {0};
        Message counting = text -> counter[0]++;

        counting.send("第一条");
        counting.send("第二条");

        assertThat(counter[0]).isEqualTo(2);
    }

    @Test
    @DisplayName("lambda 比声明它的方法活得久——它记的是拷贝，不是栈位置")
    void lambdaOutlivesItsMethod() {
        // 方法返回之后栈帧就没了，但捕获的那个值跟着 lambda 活在堆上
        Message escaped = LambdaDemo.makeEscapingLambda("值还在");

        assertThat(LambdaDemo.sendVia(escaped, "调用")).isEqualTo("[逃逸的 lambda] 值还在 ← 调用");
    }

    // ── 8. lambda 和匿名内部类的 this ──────────────────────────────

    @Test
    @DisplayName("lambda 里的 this 是外层对象，匿名内部类里的是它自己")
    void thisMeansSomethingDifferent() {
        Greeter greeter = new Greeter("Albert");

        assertThat(greeter.lambdaThisIsOuter()).isTrue();
        assertThat(greeter.anonymousThisIsOuter()).isFalse();
        // 但匿名内部类想拿外层也有办法：Outer.this
        assertThat(greeter.anonymousOuterReferenceIsOuter()).isTrue();
    }

    @Test
    @DisplayName("两种写法的输出里，打印出来的 this 不是同一个东西")
    void thisPrintsDifferently() {
        Greeter greeter = new Greeter("Albert");

        assertThat(greeter.greetingByLambda().get()).contains("Greeter(Albert)");
        assertThat(greeter.greetingByAnonymousClass().get()).contains("那个匿名内部类对象");
    }
}
