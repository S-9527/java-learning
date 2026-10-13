package dev.javalearning.lambda;

/**
 * 第 07 课用来演示「两个参数 + 有返回值」的函数式接口。
 *
 * <p>{@link Message} 只有一个参数、没有返回值，看不出括号和 {@code return} 的规矩。
 * 多一个参数、多一个返回值，才能说清两件事：
 *
 * <ul>
 *   <li>参数多于一个时，括号<b>不能</b>省；</li>
 *   <li>方法体是单个表达式时，可以省掉大括号和 {@code return}，
 *       表达式的值就是返回值。</li>
 * </ul>
 *
 * <p>注意：<b>这里故意没有加 {@code @FunctionalInterface} 注解</b>。
 * 它照样能接 Lambda——那个注解是可选的，只是帮你把关，不是编译器认不认的门票。
 * 对比 {@link Message}（加了注解）。
 */
public interface Calculator {

    /** 算两个整数，返回结果。 */
    int calculate(int a, int b);
}
