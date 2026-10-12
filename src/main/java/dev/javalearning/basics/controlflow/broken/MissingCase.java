package dev.javalearning.basics.controlflow.broken;

/**
 * 第 02 课的反面教材：switch 表达式漏写了一个分支。
 *
 * <p>这个类是<b>故意写错的</b>，放在 {@code broken/} 包里，已经被
 * {@code build.gradle.kts} 排除出构建。编译它，看看报什么错——
 * {@code docs/02-运算符与流程控制.md} 里贴的就是这个错误的真实输出。
 */
public class MissingCase {

    enum Direction { NORTH, EAST, SOUTH }

    /** 四个分支里漏掉了 EAST——注意它连返回值都没给全。 */
    public static String describe(Direction d) {
        return switch (d) {
            case NORTH -> "向上";
            case SOUTH -> "向下";
            // case EAST -> "向右";   ← 就是漏了它
        };
    }

    public static void main(String[] args) {
        System.out.println(describe(Direction.EAST));
    }
}
