package dev.javalearning.optional;

import java.util.Optional;

/**
 * 第 08 课的示例类：一个网站用户。
 *
 * <p>注意 {@code nickname} 这个字段——它是<b>可以为 null</b> 的：不是每个用户
 * 都填了昵称。这一课要处理的就是这类「可能没有」的值。
 *
 * <p>也注意 {@link #nickname()} 的返回类型是 {@code Optional<String>} 而不是
 * {@code String}——这是本课推荐的写法：<b>把「可能没有」写进类型里</b>，
 * 调用方不点开也知道要处理空的情况。
 *
 * <p>反过来，{@code nickname} 字段本身<b>不是</b> {@code Optional<String>}。
 * 为什么字段不推荐用 Optional，见 {@code docs/08-Optional.md} 的「不推荐的用法」。
 */
public class User {

    private final String fullName;
    private final String email;
    private final String nickname;   // 可能为 null：这个用户没填昵称

    public User(String fullName, String email, String nickname) {
        this.fullName = fullName;
        this.email = email;
        this.nickname = nickname;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    /** 推荐：返回类型写 Optional，调用方一眼看到「昵称可能没有」。 */
    public Optional<String> nickname() {
        return Optional.ofNullable(nickname);
    }

    @Override
    public String toString() {
        return fullName + " <" + email + ">";
    }
}
