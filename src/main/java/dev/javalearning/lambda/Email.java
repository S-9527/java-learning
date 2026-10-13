package dev.javalearning.lambda;

/**
 * 第 07 课「传统做法」的第一个实现类：发邮件。
 *
 * <p>它存在的唯一理由是：{@code Message} 是个接口，不能直接 new，
 * 想让「发邮件」这件事有个对象，就得先有一个类。
 * Lambda 出场之后，这个类就不需要了。
 */
public class Email implements Message {

    @Override
    public void send(String text) {
        System.out.println("[Email] " + text);
    }
}
