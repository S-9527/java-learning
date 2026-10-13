package dev.javalearning.lambda;

import java.io.IOException;

/**
 * 第 07 课的函数式接口：读取一段文本，可能失败。
 *
 * <p>它和 {@link Message} 的关键区别是 {@code throws IOException}。
 * 很多人以为「lambda 里不能抛受检异常」，其实不是——<b>能不能抛取决于接口怎么声明的</b>。
 *
 * <ul>
 *   <li>接口方法声明了 {@code throws IOException} → lambda 里可以抛，调用方处理；</li>
 *   <li>接口方法没声明（比如 {@code Consumer.accept}）→ lambda 里抛受检异常就编译不过，
 *       因为 lambda 的方法体受<b>那个接口方法</b>的签名约束。</li>
 * </ul>
 *
 * <p>这正是 JDK 里 {@code Callable<T>}（能抛异常）和 {@code Runnable}（不能）
 * 要分成两个接口的原因。
 */
@FunctionalInterface
public interface TextReader {

    /** 读一段文本，读不到就抛 {@link IOException}。 */
    String read() throws IOException;
}
