package dev.javalearning.optional.broken;

import java.util.Optional;

import dev.javalearning.optional.User;
import dev.javalearning.optional.UserRepository;

/**
 * 第 08 课的反面教材：函数本身就返回 {@code Optional} 时用了 {@code map}。
 *
 * <p><b>这是故意写错的</b>，编译不过，所以住在 {@code broken/} 里。
 *
 * <p>这个错误是写这一课时<b>真踩到的</b>：写 demo 时顺手把
 * {@code .map(User::nickname).map(String::toUpperCase)} 串在一起，
 * 以为 {@code map} 会自动把 {@code Optional<String>} 摊平。
 * 结果 {@code javac} 报「cannot infer type-variable(s) U」。
 *
 * <p>原因：{@code User::nickname} 的返回类型是 {@code Optional<String>}，
 * 所以 {@code map} 把它包成了 {@code Optional<Optional<String>>}，
 * 下一环 {@code String::toUpperCase} 收到的就是 {@code Optional<String>}
 * 而不是 {@code String}，自然对不上。
 *
 * <p>正解是用 {@code flatMap}。真实错误见 {@code docs/08-Optional.md}。
 */
public class NestedMap {

    public static void main(String[] args) {
        UserRepository repository = new UserRepository();

        Optional<String> nickname = repository.findByName("张三")
                .map(User::nickname)          // ← 应该用 flatMap
                .map(String::toUpperCase);
        System.out.println(nickname);
    }
}
