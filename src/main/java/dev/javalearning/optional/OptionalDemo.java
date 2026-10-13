package dev.javalearning.optional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 第 08 课的示例代码：Optional。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/optional/OptionalDemo.java}
 * 每一节对应 {@code docs/08-Optional.md} 里的一节，输出都是真跑出来的。
 *
 * <p>这一课跟前面几课不太一样：它<b>几乎没有编译错误</b>，坑基本都在运行期。
 * 所以这里的演示大量使用 {@code try-catch} 把真实的异常和 message 抓下来打印，
 * 而不是靠 {@code broken/} 里的编译失败。
 */
public class OptionalDemo {

    // ── 给第 5 节用的计数器：证明默认值到底有没有被算出来 ──────────────

    /** {@code orElse} 传进去的那个「默认值」被求值的次数。 */
    private static int orElseCalls = 0;

    /** {@code orElseGet} 传进去的 supplier 被执行的次数。 */
    private static int orElseGetCalls = 0;

    public static void main(String[] args) {
        section("1. 先让代码疼一次：查不到就是 null");
        painfulVersion();

        section("2. Optional 出场：把「可能没有」写在返回类型上");
        optionalVersion();

        section("3. 创建 Optional 的三种方式");
        createThreeWays();

        section("4. isPresent() + get()：改了，但没真改");
        isPresentPlusGet();

        section("5. orElse 和 orElseGet：参数到底什么时候算");
        orElseVersusOrElseGet();

        section("6. orElseThrow：查不到就抛自己的异常");
        orElseThrowDemo();

        section("7. ifPresent 和 ifPresentOrElse：有值才做事");
        ifPresentDemo();

        section("8. map / flatMap / filter：在盒子里做转换");
        mapFlatMapFilter();

        section("9. stream()：0 个或 1 个元素的流");
        streamDemo();

        section("10. 不要用 get()：没值就抛异常");
        getThrows();

        section("11. 不推荐的用法：能编译，但别这么写");
        notRecommended();
    }

    // ── 1 ──────────────────────────────────────────────────────────

    /**
     * 笨办法：仓库返回 null，调用方必须自己判空。
     *
     * <p>先直接调一次，让真实的 NullPointerException 摔在脸上。
     */
    private static void painfulVersion() {
        UserRepository repository = new UserRepository();

        try {
            // 查一个不存在的人，然后直接 .fullName()
            System.out.println("查「赵六」→ " + repository.findUserByName("赵六").fullName());
        } catch (NullPointerException e) {
            System.out.println("NullPointerException: " + e.getMessage());
        }

        // 想不炸，就得自己判空——而且每加一层取值就多一层嵌套
        User user = repository.findUserByName("赵六");
        if (user != null) {
            System.out.println("昵称：" + user.nickname().orElse("（没填）"));
        } else {
            System.out.println("if (user != null) 挡住了：查无此人");
        }

        // 真实项目里的嵌套长这样：想拿「订单 → 客户 → 地址 → 城市」，
        // 中间任何一层是 null 都得判，判完还得往里缩进。
        System.out.println("一层套一层：user != null → nickname != null → 非空串，缩进越来越深");
        System.out.println("毛病不在「忘了判空」，在于返回类型上根本看不出可能没有");
    }

    // ── 2 ──────────────────────────────────────────────────────────

    /** 正解：返回类型直接写 Optional，调用方不点开也知道要处理空。 */
    private static void optionalVersion() {
        UserRepository repository = new UserRepository();

        Optional<User> found = repository.findByName("张三");
        Optional<User> missing = repository.findByName("赵六");

        System.out.println("findByName(\"张三\") → " + found);
        System.out.println("findByName(\"赵六\") → " + missing);
        System.out.println("两者的类型都是 Optional<User>，编译期就看得见「可能没有」");
        System.out.println("有值时：found.orElseThrow().fullName() = " + found.orElseThrow().fullName());
    }

    // ── 3 ──────────────────────────────────────────────────────────

    /** 三种创建方式，以及前两种什么时候炸。 */
    private static void createThreeWays() {
        // ① empty()：明确表示「这里没有值」
        Optional<String> empty = Optional.empty();
        System.out.println("Optional.empty()          → " + empty);

        // ② of(value)：确定有值。传 null 立刻炸——这是真的错误，不是用法错误
        Optional<String> of = Optional.of("张三");
        System.out.println("Optional.of(\"张三\")       → " + of);
        try {
            Optional.of(null);
        } catch (NullPointerException e) {
            // 注意：这里的 message 是 null，不是一句话
            System.out.println("Optional.of(null)         → 抛 NullPointerException，message = " + e.getMessage());
        }

        // ③ ofNullable(value)：允许 null，最常用
        System.out.println("Optional.ofNullable(null) → " + Optional.ofNullable(null));
        System.out.println("Optional.ofNullable(\"张三\") → " + Optional.ofNullable("张三"));
        System.out.println();
        System.out.println("of(null) 的异常 message 是 null——它自己也没告诉你「不该传 null」");
        System.out.println("所以拿不准的时候用 ofNullable，它永远不会因为你传 null 而炸");
    }

    // ── 4 ──────────────────────────────────────────────────────────

    /**
     * 用 isPresent() + get() 改造笨办法。
     *
     * <p>能跑，但和 {@code if (user != null)} 没有本质区别——还是命令式，
     * 还是「先问有没有，再取出来」。
     */
    private static void isPresentPlusGet() {
        UserRepository repository = new UserRepository();

        Optional<User> found = repository.findByName("张三");
        Optional<User> missing = repository.findByName("赵六");

        if (found.isPresent()) {
            System.out.println("found.isPresent()   → true，get() 拿到 " + found.get().fullName());
        }
        System.out.println("missing.isPresent() → " + missing.isPresent());
        System.out.println("missing.isEmpty()   → " + missing.isEmpty());
        System.out.println();
        System.out.println("写法和 if (user != null) 一模一样，只是把 null 换成了 isPresent()");
        System.out.println("这仍然是命令式编程——Optional 的正确用法是链式调用，见下面几节");
    }

    // ── 5 ──────────────────────────────────────────────────────────

    /**
     * 这一课最值钱的实验：{@code orElse} 的参数<b>总会</b>先算出来，
     * {@code orElseGet} 的 supplier <b>只在为空时</b>才执行。
     */
    private static void orElseVersusOrElseGet() {
        orElseCalls = 0;
        orElseGetCalls = 0;

        System.out.println("── 情况 A：有值，用 orElse ──");
        String a = Optional.of("小三").orElse(computeDefaultForOrElse());
        System.out.println("结果 = " + a + "，默认值被算了 " + orElseCalls + " 次");

        System.out.println("── 情况 B：有值，用 orElseGet ──");
        String b = Optional.of("小三").orElseGet(() -> computeDefaultForOrElseGet());
        System.out.println("结果 = " + b + "，supplier 被执行了 " + orElseGetCalls + " 次");

        System.out.println("── 情况 C：没值，这次默认值真的用上了 ──");
        orElseCalls = 0;
        orElseGetCalls = 0;
        String c = Optional.<String>empty().orElse(computeDefaultForOrElse());
        String d = Optional.<String>empty().orElseGet(() -> computeDefaultForOrElseGet());
        System.out.println("orElse    → " + c + "，被算了 " + orElseCalls + " 次");
        System.out.println("orElseGet → " + d + "，被执行了 " + orElseGetCalls + " 次");

        System.out.println();
        System.out.println("结论：有值的时候 orElse 也算了默认值（1 次），orElseGet 一次都没算（0 次）");
        System.out.println("如果默认值只是一句话，无所谓；如果它要查数据库、读文件，orElse 就白干了一次");
    }

    /** 假装这个默认值很贵——这里用一个计数器代替耗时操作。 */
    private static String computeDefaultForOrElse() {
        orElseCalls++;
        return "默认昵称";
    }

    /** 同上，但这次是给 orElseGet 用的。 */
    private static String computeDefaultForOrElseGet() {
        orElseGetCalls++;
        return "默认昵称";
    }

    // ── 6 ──────────────────────────────────────────────────────────

    /** 查不到时抛自己的异常，而不是让调用方去猜。 */
    private static void orElseThrowDemo() {
        UserRepository repository = new UserRepository();

        User found = repository.findByName("张三").orElseThrow();
        System.out.println("有值时 orElseThrow() 直接拿到：" + found.fullName());

        // 无参版：抛的是 NoSuchElementException
        try {
            repository.findByName("赵六").orElseThrow();
        } catch (NoSuchElementException e) {
            System.out.println("无参版没值 → " + e.getClass().getName() + ": " + e.getMessage());
        }

        // 带 supplier 版：抛你自己的异常，message 自己写
        try {
            repository.findByName("赵六")
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在：赵六"));
        } catch (IllegalArgumentException e) {
            System.out.println("带 supplier → " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    // ── 7 ──────────────────────────────────────────────────────────

    /** 有值才做事：{@code ifPresent} 和 {@code ifPresentOrElse}。 */
    private static void ifPresentDemo() {
        UserRepository repository = new UserRepository();

        repository.findByName("张三").ifPresent(u -> System.out.println("ifPresent 命中：" + u));
        repository.findByName("赵六").ifPresent(u -> System.out.println("这行不该打印"));

        repository.findByName("张三").ifPresentOrElse(
                u -> System.out.println("ifPresentOrElse 命中：" + u.fullName()),
                () -> System.out.println("这行不该打印"));
        repository.findByName("赵六").ifPresentOrElse(
                u -> System.out.println("这行不该打印"),
                () -> System.out.println("ifPresentOrElse 落空：查无赵六"));

        System.out.println("注意 ifPresent 是「有值才做」，它不返回新值；");
        System.out.println("想在两个分支里都产出结果，用 ifPresentOrElse 或者下面的 map");
    }

    // ── 8 ──────────────────────────────────────────────────────────

    /** map 会自动再包一层，flatMap 不会——这是最容易搞混的地方。 */
    private static void mapFlatMapFilter() {
        UserRepository repository = new UserRepository();

        // map：String → int，自动包成 Optional<Integer>
        Optional<Integer> length = repository.findByName("张三")
                .map(User::fullName)
                .map(String::length);
        System.out.println("map 取名字长度：" + length);

        // map 返回 null → 变成 empty，不会 NPE
        Optional<String> mapped = repository.findByName("李四")
                .flatMap(User::nickname)
                .map(String::toUpperCase);
        System.out.println("李四没昵称，flatMap + map 链走完：" + mapped);

        // 关键对比：函数本身返回 Optional 时，map 会包成两层
        Optional<Optional<String>> nested = repository.findByName("张三").map(User::nickname);
        Optional<String> flattened = repository.findByName("张三").flatMap(User::nickname);
        System.out.println("map(User::nickname)     → " + nested + "   ← 两层 Optional");
        System.out.println("flatMap(User::nickname) → " + flattened + "   ← 一层，我们要的就是这个");
        // 空值也照样套两层——这是 map 的通病，跟值在不在无关
        System.out.println("李四 map(User::nickname) → " + repository.findByName("李四").map(User::nickname)
                + "   ← 空值也套两层");

        // map 的函数返回 null 时，结果变成 empty，而不是 NPE
        Optional<String> fromNull = repository.findByName("张三").map(u -> (String) null);
        System.out.println("map(u -> (String) null) → " + fromNull + "   ← 返回 null 也只是 empty，不炸");

        // 但 flatMap 没这个保护：函数返回 null 会直接 NPE
        try {
            repository.findByName("张三").flatMap(u -> null);
        } catch (NullPointerException e) {
            System.out.println("flatMap(u -> null) → 抛 NullPointerException（map 不会）");
        }

        // filter：不满足就变 empty，同样不抛异常
        System.out.println("张三 filter(邮箱是 @example.com)："
                + repository.findByName("张三").filter(u -> u.email().endsWith("@example.com")));
        System.out.println("钱七 filter(邮箱是 @example.com)："
                + repository.findByName("钱七").filter(u -> u.email().endsWith("@example.com"))
                + "   ← 人找到了，但不满足条件，一样是 empty");
        System.out.println();
        System.out.println("口诀：转换后还是普通值 → map；转换后本身就是 Optional → flatMap");
    }

    // ── 9 ──────────────────────────────────────────────────────────

    /** stream() 把 Optional 变成一个 0 或 1 个元素的流。 */
    private static void streamDemo() {
        UserRepository repository = new UserRepository();

        System.out.println("Optional.empty().stream().count()  = " + Optional.empty().stream().count());
        System.out.println("Optional.of(\"张三\").stream().count() = " + Optional.of("张三").stream().count());

        // 实用场景：一批名字，查得到的收进列表，查不到的直接跳过
        List<String> emails = new ArrayList<>();
        for (String name : List.of("张三", "赵六", "王五")) {
            repository.findByName(name).stream().map(User::email).forEach(emails::add);
        }
        System.out.println("3 个名字查出来 " + emails.size() + " 个邮箱：" + emails);
        System.out.println("赵六查不到，它的流是空的，forEach 什么都不做——不用写一个 if");
    }

    // ── 10 ─────────────────────────────────────────────────────────

    /** get() 没判断就用，抛的是 NoSuchElementException，不是 NPE。 */
    private static void getThrows() {
        Optional<String> empty = Optional.empty();

        try {
            empty.get();
        } catch (NoSuchElementException e) {
            System.out.println("Optional.empty().get() → " + e.getClass().getName() + ": " + e.getMessage());
        }

        System.out.println("异常类型是 NoSuchElementException，不是 NullPointerException");
        System.out.println("get() 不是不能用，但它把「可能没有」又变回了运行期才会发现的错误——");
        System.out.println("那和直接返回 null 有什么区别？只在已经确认有值的地方用。");
    }

    // ── 11 ─────────────────────────────────────────────────────────

    /** 这些都能编译、也能跑，但都是社区公认的坏味道。 */
    private static void notRecommended() {
        // ① 当字段 ② 当方法参数 ③ 当构造器参数：三个都编译得过
        BadStyle bad = new BadStyle(Optional.of("小三"));
        System.out.println("① Optional 当字段：编译通过、运行正常 → " + bad.nickname());
        System.out.println("② Optional 当方法参数：编译通过、运行正常 → " + bad.greet(Optional.of("你好")));
        System.out.println("③ Optional 当构造器参数：编译通过、运行正常");

        // ④ 包装集合：集合本身就能表达「空」
        Optional<List<String>> wrapped = Optional.of(List.of());
        System.out.println("④ Optional 包集合：Optional.of(List.of()) = " + wrapped);
        System.out.println("   一个空列表已经是「没有元素」了，外面再套一层 Optional 是双重否定");

        // ⑤ isPresent() + get()：第 4 节已经证明它等于白用
        System.out.println("⑤ isPresent() + get()：等于把 null 换了个名字，见第 4 节");

        System.out.println();
        System.out.println("这些编译器都不拦——所以只能靠你自己记住：");
        System.out.println("Optional 的主要用途是「方法的返回类型」，就这一条。");
    }

    /**
     * 故意的反面写法：Optional 当字段、当构造器参数、当方法参数。
     *
     * <p>它<b>编译得过、也跑得动</b>，所以不能放进 {@code broken/}
     * （那里放的是编译不过的）。把它留在 demo 里跑给你看，是要说明一件事：
     * 编译器管不了坏味道，这些规则只能靠自己守。
     */
    static class BadStyle {

        private final Optional<String> nickname;   // ✗ 不推荐：字段

        BadStyle(Optional<String> nickname) {      // ✗ 不推荐：构造器参数
            this.nickname = nickname;
        }

        Optional<String> nickname() {
            return nickname;
        }

        String greet(Optional<String> prefix) {    // ✗ 不推荐：方法参数
            return prefix.orElse("") + "，" + nickname.orElse("（没填）");
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
