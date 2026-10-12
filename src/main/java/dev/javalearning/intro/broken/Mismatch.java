package dev.javalearning.intro.broken;
/**
 * 第 00 课的「反面教材」。
 *
 * <p>文件名是 {@code Mismatch.java}，但里面声明的是 {@code public class Typo}。
 * 编译它，看看会发生什么——这一课的文档里贴的就是这个错误的真实输出。
 *
 * <p>注意：这个类是故意写错的，别去「修好」它。
 */
public class Typo {

    public static void main(String[] args) {
        System.out.println("这个文件编译不过");
    }
}
