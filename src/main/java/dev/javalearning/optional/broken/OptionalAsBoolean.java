package dev.javalearning.optional.broken;

import java.util.Optional;

/**
 * 第 08 课的反面教材：把 {@code Optional} 直接当布尔用。
 *
 * <p><b>这是故意写错的</b>，编译不过，所以住在 {@code broken/} 里。
 * 从别的语言（Python 的 truthy、JS 的 `if (obj)`）过来的人容易这么写：
 * 以为「有值」就能直接进 if。Java 的类型系统不认，
 * {@code Optional} 不是 {@code boolean}，必须显式调 {@code isPresent()}。
 *
 * <p>真实错误见 {@code docs/08-Optional.md}。
 */
public class OptionalAsBoolean {

    public static void main(String[] args) {
        Optional<String> name = Optional.of("张三");

        if (name) {                              // ← Optional 不是 boolean
            System.out.println(name.get());
        }
    }
}
