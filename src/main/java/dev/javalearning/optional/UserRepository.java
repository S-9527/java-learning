package dev.javalearning.optional;

import java.util.List;
import java.util.Optional;

/**
 * 第 08 课的「数据库」替身。
 *
 * <p>它故意同时提供两套查询方法，用来对照这一课的笨办法和正解：
 *
 * <ul>
 *   <li>{@link #findUserByName(String)} —— 笨办法：查不到返回 {@code null}</li>
 *   <li>{@link #findByName(String)}、{@link #findByEmail(String)} —— 正解：返回
 *       {@code Optional}</li>
 * </ul>
 *
 * <p>{@code findByName} 内部就是调 {@code findUserByName} 再包一层
 * {@code Optional.ofNullable}——真实项目里从老代码迁移过来，走的就是这条路。
 */
public class UserRepository {

    private static final List<User> USERS = List.of(
            new User("张三", "zhangsan@example.com", "小三"),
            new User("李四", "lisi@example.com", null),          // 李四没填昵称
            new User("王五", "wangwu@example.com", "五哥"),
            new User("钱七", "qianqi@other.com", "七七"));        // 外站邮箱，filter 会用到

    /**
     * 笨办法：查不到返回 {@code null}。
     *
     * <p>返回类型上看不出「可能查不到」，调用方忘了判空就等着 NPE。
     */
    public User findUserByName(String name) {
        for (User user : USERS) {
            if (user.fullName().equals(name)) {
                return user;
            }
        }
        return null;
    }

    /**
     * 正解：返回 {@code Optional}。
     *
     * <p>查不到就是 {@code Optional.empty()}，这件事写在返回类型上了，
     * 调用方想装作看不见都不行——编译器会拦（见 broken/ 里的两个例子）。
     */
    public Optional<User> findByName(String name) {
        return Optional.ofNullable(findUserByName(name));
    }

    /** 按邮箱查，也是返回 Optional。 */
    public Optional<User> findByEmail(String email) {
        for (User user : USERS) {
            if (user.email().equals(email)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
}
