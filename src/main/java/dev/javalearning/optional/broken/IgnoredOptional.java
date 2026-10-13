package dev.javalearning.optional.broken;

import dev.javalearning.optional.User;
import dev.javalearning.optional.UserRepository;

/**
 * 第 08 课的反面教材：把 {@code Optional<User>} 当成 {@code User} 用。
 *
 * <p><b>这是故意写错的</b>，编译不过，所以住在 {@code broken/} 里
 * （{@code build.gradle.kts} 已整包排除）。它演示的正是 Optional 最大的好处：
 * 返回类型上写了 Optional，调用方就<b>不可能</b>忘掉空的情况——
 * 编译器会拦住你，而不是等到运行期 NPE。
 *
 * <p>真实错误见 {@code docs/08-Optional.md}。
 */
public class IgnoredOptional {

    public static void main(String[] args) {
        UserRepository repository = new UserRepository();

        User user = repository.findByName("张三");   // ← 返回的是 Optional<User>
        System.out.println(user.fullName());
    }
}
