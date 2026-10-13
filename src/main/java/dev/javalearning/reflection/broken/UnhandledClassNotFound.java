package dev.javalearning.reflection.broken;

/**
 * 第 09 课的反面教材：{@code Class.forName} 抛的是受检异常，不处理编译不过。
 *
 * <p><b>这是故意写错的</b>。{@code forName} 的签名是
 * {@code Class<?> forName(String) throws ClassNotFoundException}——
 * 受检异常 checked exception，编译器要求你当场处理或者往外抛。
 *
 * <p>注意对比：{@code User.class} 和 {@code user.getClass()} 都不抛异常，
 * 只有运行时才去查名字的 {@code forName} 会。
 *
 * <p>它编译不过，所以放在 {@code broken/} 里。文档里贴真实的 {@code javac} 错误。
 */
public class UnhandledClassNotFound {

    public static void main(String[] args) {
        // 没有 try-catch，也没有 throws ClassNotFoundException
        Class<?> clazz = Class.forName("dev.javalearning.reflection.User");
        System.out.println(clazz.getName());
    }
}
