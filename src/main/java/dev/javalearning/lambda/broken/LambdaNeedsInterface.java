package dev.javalearning.lambda.broken;

/**
 * 第 07 课的<b>反面教材</b>：想把 lambda 赋给一个抽象类。
 *
 * <p><b>这是故意写错的，编译一定失败。</b> 放在 {@code broken/} 包里是因为
 * {@code build.gradle.kts} 把整个 broken 子包排除出了构建。
 *
 * <p>要证明的是第 4 节那句话的另一半：lambda 的落地目标
 * （target type）<b>必须是接口</b>，而且必须是函数式接口。
 * 抽象类哪怕只有一个抽象方法也不行。
 *
 * <p>写这个例子时我原以为编译器会说 {@code interface expected here}，
 * 实际报的是 {@code incompatible types: Task is not a functional interface}
 * （见文档里贴的真实输出）。两种说法都对，但真话只有一种——
 * 这类「以为会报 A、结果报了 B」的地方，正是本仓库最看重的教学素材。
 */
public class LambdaNeedsInterface {

    /** 抽象类，只有一个抽象方法，但它是 class 不是 interface。 */
    abstract static class Task {
        abstract void run();
    }

    public static void main(String[] args) {
        // ← 编译错误：interface expected here
        Task task = () -> System.out.println("试试能不能编译");
        task.run();
    }
}
