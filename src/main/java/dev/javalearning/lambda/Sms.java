package dev.javalearning.lambda;

/**
 * 第 07 课「传统做法」的第二个实现类：发短信。
 *
 * <p>和 {@link Email} 一样，除了打印的渠道名不同，没有任何额外信息。
 * 两个类长这样，就该警觉了：**这种类只是在替一个方法占位。**
 */
public class Sms implements Message {

    @Override
    public void send(String text) {
        System.out.println("[SMS] " + text);
    }
}
