package dev.javalearning.lambda;

/**
 * 第 07 课的起点：一个「能发消息」的接口。
 *
 * <p>它只有一个抽象方法 {@link #send(String)}，这种接口后面会被正式命名为
 * <b>函数式接口</b>（functional interface）——Lambda 只能往这种接口上挂。
 *
 * <p>它身上还带着 {@link FunctionalInterface} 注解，作用见 {@code LambdaDemo} 第 5 节。
 */
@FunctionalInterface
public interface Message {

    /** 把一条消息发出去。发给谁、怎么发，由实现类决定。 */
    void send(String text);
}
