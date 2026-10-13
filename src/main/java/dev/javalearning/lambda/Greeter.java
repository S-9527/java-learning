package dev.javalearning.lambda;

import java.util.function.Supplier;

/**
 * 第 07 课用来演示「lambda 和匿名内部类里的 {@code this} 不是一回事」。
 *
 * <p>同一个 {@link Supplier} 任务，一个用 lambda 写、一个用匿名内部类写。
 * 两者都能跑，但打印出来的 {@code this} 指向完全不同的对象：
 *
 * <ul>
 *   <li>lambda 里的 {@code this} —— <b>外层</b>的 {@code Greeter} 对象；</li>
 *   <li>匿名内部类里的 {@code this} —— <b>那个匿名对象自己</b>，
 *       想拿外层得写 {@code Greeter.this}。</li>
 * </ul>
 *
 * <p>为了输出稳定，这里的 {@link #toString()} 被重写了——默认实现会打印哈希值，
 * 每次运行都不一样，那种输出不能往文档里贴。
 */
public class Greeter {

    private final String name;

    public Greeter(String name) {
        this.name = name;
    }

    /**
     * 用 lambda 写。{@code this} 在这里指的是外层这个 {@code Greeter} 对象，
     * 所以 {@code this == outer} 是 {@code true}。
     */
    public Supplier<String> greetingByLambda() {
        Greeter outer = this;
        return () -> "lambda 里的 this = " + this
                + "，和外层是同一个对象：" + (this == outer);
    }

    /**
     * 用匿名内部类写。{@code this} 在这里指的是这个匿名对象自己，
     * 想拿外层必须写 {@code Greeter.this}。
     *
     * <p>注意那个 {@code (Object)} 强转：直接写 {@code this == outer} 编译器会报
     * {@code incomparable types}——它认为「匿名 Supplier」和「Greeter」是两种
     * 八竿子打不着的类型，压根不该比。这本身就是 {@code this} 换了含义的证据。
     * 想比只能先把两边都当成 {@code Object}。
     */
    public Supplier<String> greetingByAnonymousClass() {
        Greeter outer = this;
        return new Supplier<>() {
            @Override
            public String get() {
                return "匿名内部类里的 this = " + this
                        + "，和外层是同一个对象：" + ((Object) this == outer)
                        + "（Greeter.this 才是外层：" + (Greeter.this == outer) + "）";
            }

            @Override
            public String toString() {
                // 不重写的话这里是 Greeter$1@<哈希>，哈希每次运行都变，贴进文档就不稳定了
                return "那个匿名内部类对象";
            }
        };
    }

    @Override
    public String toString() {
        return "Greeter(" + name + ")";
    }

    // ── 下面三个只回答 true / false，方便测试直接断言，不用去解析打印出来的字符串 ──

    /** lambda 里的 {@code this} 是不是外层这个对象？——是。 */
    public boolean lambdaThisIsOuter() {
        Greeter outer = this;
        Supplier<Boolean> check = () -> this == outer;
        return check.get();
    }

    /** 匿名内部类里的 {@code this} 是不是外层这个对象？——不是，是它自己。 */
    public boolean anonymousThisIsOuter() {
        Greeter outer = this;
        Supplier<Boolean> check = new Supplier<>() {
            @Override
            public Boolean get() {
                return (Object) this == outer;
            }
        };
        return check.get();
    }

    /** 匿名内部类里写 {@code Greeter.this} 拿到的才是外层对象。 */
    public boolean anonymousOuterReferenceIsOuter() {
        Greeter outer = this;
        Supplier<Boolean> check = new Supplier<>() {
            @Override
            public Boolean get() {
                return Greeter.this == outer;
            }
        };
        return check.get();
    }
}
