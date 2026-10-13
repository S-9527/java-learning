package dev.javalearning.lambda.broken;

/**
 * 第 07 课的<b>反面教材</b>：给函数式接口加了第二个抽象方法。
 *
 * <p><b>这是故意写错的，编译一定失败。</b> 放在 {@code broken/} 包里是因为
 * {@code build.gradle.kts} 把整个 broken 子包排除出了构建，
 * 它不会拖垮正常编译；文档里贴的是 {@code javac} 的真实报错。
 *
 * <p>两个错叠在一起，正好说明两件事：
 * <ol>
 *   <li>{@code @FunctionalInterface} 注解会当场抓住「不止一个抽象方法」；</li>
 *   <li>没有这个注解也躲不过去——Lambda 挂上去的时候编译器还是会发现
 *       「这不是个函数式接口」。</li>
 * </ol>
 */
@FunctionalInterface
public interface TwoAbstractMethods {

    void send(String text);

    // ← 第二个抽象方法。Lambda 没法回答「你要实现的是 send 还是 receive」
    void receive(String text);

    class Demo {
        public static void main(String[] args) {
            // 编译器不知道这个 lambda 想实现哪一个抽象方法
            TwoAbstractMethods m = text -> System.out.println(text);
            m.send("试试能不能编译");
        }
    }
}
