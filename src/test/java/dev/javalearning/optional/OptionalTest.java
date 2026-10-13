package dev.javalearning.optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 第 08 课的断言。
 *
 * <p>每一条对应 {@code docs/08-Optional.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*optional*'}
 *
 * <p>这一课的重点是<b>运行期</b>行为，所以断言里抓异常的地方特别多。
 */
class OptionalTest {

    private final UserRepository repository = new UserRepository();

    // ── 1. 先让代码疼一次 ──────────────────────────────────────────

    @Test
    @DisplayName("笨办法：查不到返回 null，直接取值就是 NullPointerException")
    void nullReturnBlowsUpAtTheCallSite() {
        // 返回类型是 User，从签名上完全看不出「可能查不到」
        User user = repository.findUserByName("赵六");
        assertThat(user).isNull();
        // 运行期才炸——这就是这一课要解决的问题
        assertThatThrownBy(() -> user.fullName())
                .isInstanceOf(NullPointerException.class);
    }

    // ── 2. Optional 把「可能没有」写进类型 ─────────────────────────

    @Test
    @DisplayName("返回类型写 Optional，查得到和查不到都是同一个类型")
    void optionalPutsMaybeIntoTheType() {
        assertThat(repository.findByName("张三")).isPresent();
        assertThat(repository.findByName("赵六")).isEmpty();
        // 关键：两者的编译期类型完全一样，调用方不点开也知道要处理空
        Optional<User> found = repository.findByName("张三");
        Optional<User> missing = repository.findByName("赵六");
        assertThat(found.orElseThrow().fullName()).isEqualTo("张三");
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("按邮箱查也是 Optional，找不到就是 empty")
    void findByEmailReturnsOptional() {
        assertThat(repository.findByEmail("wangwu@example.com"))
                .map(User::fullName)
                .contains("王五");
        assertThat(repository.findByEmail("nobody@example.com")).isEmpty();
    }

    // ── 3. 三种创建方式 ────────────────────────────────────────────

    @Nested
    @DisplayName("创建 Optional 的三种方式")
    class Creation {

        @Test
        @DisplayName("empty() 明确表示没有值")
        void emptyMeansNoValue() {
            assertThat(Optional.empty()).isEmpty();
            assertThat(Optional.empty().isPresent()).isFalse();
        }

        @Test
        @DisplayName("of(value) 传 null 会立刻抛 NullPointerException")
        void ofRejectsNullImmediately() {
            assertThat(Optional.of("张三")).contains("张三");
            // 这是「真的出错了」，不是「用法不对」——你不该在这里传 null
            assertThatThrownBy(() -> Optional.of(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("ofNullable(value) 允许 null，最常用")
        void ofNullableAcceptsNull() {
            assertThat(Optional.ofNullable(null)).isEmpty();
            assertThat(Optional.ofNullable("张三")).contains("张三");
            // 仓库的正解就是拿它包住老的 null 返回值
            assertThat(Optional.ofNullable(repository.findUserByName("赵六"))).isEmpty();
        }
    }

    // ── 4. isPresent + get 只是换了名字 ────────────────────────────

    @Test
    @DisplayName("isPresent() / isEmpty()：能跑，但和判 null 没有本质区别")
    void isPresentPlusGetIsStillImperative() {
        Optional<User> found = repository.findByName("张三");
        Optional<User> missing = repository.findByName("赵六");

        assertThat(found.isPresent()).isTrue();
        assertThat(missing.isPresent()).isFalse();
        assertThat(missing.isEmpty()).isTrue();

        // 这就是「if (user != null) { user.getFullName() }」的翻版，
        // 命令式、要写 if、还容易忘了判就 get()。正确姿势见下面几节。
        if (found.isPresent()) {
            assertThat(found.get().fullName()).isEqualTo("张三");
        }
    }

    // ── 5. orElse vs orElseGet：这一课最值钱的实验 ─────────────────

    @Nested
    @DisplayName("orElse 和 orElseGet 的执行时机")
    class OrElseVersusOrElseGet {

        @Test
        @DisplayName("有值时 orElse 的默认值照样被算了一次——参数是提前求值的")
        void orElseAlwaysEvaluatesItsArgument() {
            AtomicInteger calls = new AtomicInteger();
            String result = Optional.of("小三").orElse(expensiveDefault(calls));
            assertThat(result).isEqualTo("小三");
            // 值明明在，默认值还是算了
            assertThat(calls).hasValue(1);
        }

        @Test
        @DisplayName("有值时 orElseGet 的 supplier 一次都不执行")
        void orElseGetSkipsItsSupplierWhenValueIsPresent() {
            AtomicInteger calls = new AtomicInteger();
            String result = Optional.of("小三").orElseGet(() -> expensiveDefault(calls));
            assertThat(result).isEqualTo("小三");
            // 这就是 orElseGet 存在的理由
            assertThat(calls).hasValue(0);
        }

        @Test
        @DisplayName("没值时两者都会用上默认值，此时没差别")
        void bothAreUsedWhenEmpty() {
            AtomicInteger forOrElse = new AtomicInteger();
            AtomicInteger forOrElseGet = new AtomicInteger();

            assertThat(Optional.<String>empty().orElse(expensiveDefault(forOrElse)))
                    .isEqualTo("默认昵称");
            assertThat(Optional.<String>empty().orElseGet(() -> expensiveDefault(forOrElseGet)))
                    .isEqualTo("默认昵称");

            assertThat(forOrElse).hasValue(1);
            assertThat(forOrElseGet).hasValue(1);
        }

        /** 假装这个默认值很贵：查库、读文件之类。 */
        private String expensiveDefault(AtomicInteger counter) {
            counter.incrementAndGet();
            return "默认昵称";
        }
    }

    // ── 6. orElseThrow ─────────────────────────────────────────────

    @Test
    @DisplayName("orElseThrow() 没值抛 NoSuchElementException")
    void orElseThrowWithoutArgumentThrowsNoSuchElement() {
        assertThat(repository.findByName("张三").orElseThrow().fullName()).isEqualTo("张三");
        assertThatThrownBy(() -> repository.findByName("赵六").orElseThrow())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("No value present");
    }

    @Test
    @DisplayName("orElseThrow(supplier) 抛你自己的异常，message 自己写")
    void orElseThrowWithSupplierThrowsYourOwnException() {
        assertThatThrownBy(() -> repository.findByName("赵六")
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：赵六")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户不存在：赵六");
    }

    // ── 7. ifPresent / ifPresentOrElse ─────────────────────────────

    @Test
    @DisplayName("ifPresent 只在有值时执行，没值什么都不做")
    void ifPresentOnlyRunsWhenValueExists() {
        List<String> hits = new ArrayList<>();
        repository.findByName("张三").ifPresent(u -> hits.add(u.fullName()));
        repository.findByName("赵六").ifPresent(u -> hits.add("这行不该出现"));
        assertThat(hits).containsExactly("张三");
    }

    @Test
    @DisplayName("ifPresentOrElse 两个分支都能给，正好补上 ifPresent 的空缺")
    void ifPresentOrElseCoversBothBranches() {
        assertThat(labelOf("张三")).isEqualTo("用户：张三");
        assertThat(labelOf("赵六")).isEqualTo("查无此人");
    }

    private String labelOf(String name) {
        return repository.findByName(name)
                .map(u -> "用户：" + u.fullName())
                .orElse("查无此人");
    }

    // ── 8. map / flatMap / filter ──────────────────────────────────

    @Nested
    @DisplayName("map / flatMap / filter")
    class Transforming {

        @Test
        @DisplayName("map 把普通值包成 Optional，返回 null 也不会 NPE")
        void mapWrapsPlainValues() {
            assertThat(repository.findByName("张三").map(User::fullName))
                    .contains("张三");
            // 映射函数返回 null → 结果是 empty，而不是 NPE
            assertThat(repository.findByName("李四").map(User::nickname))
                    .contains(Optional.empty());
        }

        @Test
        @DisplayName("map 的函数本身返回 Optional 时，会包成两层")
        void mapNestsWhenTheFunctionReturnsOptional() {
            // User::nickname 返回 Optional<String>，map 就包成 Optional<Optional<String>>
            Optional<Optional<String>> nested = repository.findByName("张三").map(User::nickname);
            assertThat(nested).contains(Optional.of("小三"));
            // 两层很别扭，取值还得再 get 一次
            assertThat(nested.orElseThrow().orElseThrow()).isEqualTo("小三");
        }

        @Test
        @DisplayName("flatMap 不会多包一层——函数返回 Optional 时就该用它")
        void flatMapDoesNotNest() {
            Optional<String> flat = repository.findByName("张三").flatMap(User::nickname);
            assertThat(flat).contains("小三");
            // 李四没昵称，flatMap 之后还是 empty，不会变成 Optional[Optional.empty]
            assertThat(repository.findByName("李四").flatMap(User::nickname)).isEmpty();
        }

        @Test
        @DisplayName("map 的函数返回 null 会变成 empty，flatMap 的会直接抛 NPE")
        void mapToleratesNullButFlatMapDoesNot() {
            // map：null 被悄悄转成 empty
            assertThat(repository.findByName("张三").map(u -> (String) null)).isEmpty();
            // flatMap：拿到的必须是 Optional，返回 null 就 NPE
            assertThatThrownBy(() -> repository.findByName("张三").flatMap(u -> null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("filter 不满足就变 empty，同样不抛异常")
        void filterTurnsNonMatchingIntoEmpty() {
            assertThat(repository.findByName("张三").filter(u -> u.email().endsWith("@example.com")))
                    .isPresent();
            // 钱七这个人存在，但邮箱不在站内，filter 之后照样是 empty
            assertThat(repository.findByName("钱七").filter(u -> u.email().endsWith("@example.com")))
                    .isEmpty();
        }

        @Test
        @DisplayName("链式调用：filter + flatMap + map 串起来，全程没有 if")
        void chainingReplacesIfStatements() {
            assertThat(nicknameInUpperCase("张三")).contains("小三");
            assertThat(nicknameInUpperCase("李四")).isEmpty();   // filter 挡掉了：没昵称
            assertThat(nicknameInUpperCase("赵六")).isEmpty();   // 人都没有
        }

        private Optional<String> nicknameInUpperCase(String name) {
            return repository.findByName(name)
                    .filter(u -> u.email().endsWith("@example.com"))   // 只留站内用户
                    .flatMap(User::nickname)
                    .map(String::toUpperCase);
        }
    }

    // ── 9. stream() ────────────────────────────────────────────────

    @Test
    @DisplayName("stream() 把 Optional 变成 0 个或 1 个元素的流")
    void streamYieldsZeroOrOneElement() {
        assertThat(Optional.empty().stream().count()).isZero();
        assertThat(Optional.of("张三").stream().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("用 stream() 过滤一批查询结果，查不到的自动消失")
    void streamSkipsTheMissingOnes() {
        List<String> emails = new ArrayList<>();
        for (String name : List.of("张三", "赵六", "王五")) {
            repository.findByName(name).stream().map(User::email).forEach(emails::add);
        }
        // 3 个名字只出来 2 个邮箱，赵六那一路是空流，forEach 什么都不做
        assertThat(emails).containsExactly("zhangsan@example.com", "wangwu@example.com");
    }

    // ── 10. get() 是危险动作 ───────────────────────────────────────

    @Test
    @DisplayName("没值就 get()，抛的是 NoSuchElementException 而不是 NPE")
    void getWithoutCheckingThrowsNoSuchElement() {
        assertThatThrownBy(() -> Optional.empty().get())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("No value present");
    }

    @Test
    @DisplayName("orElseThrow() 和 get() 抛的是同一个异常")
    void getAndOrElseThrowThrowTheSameThing() {
        assertThatThrownBy(() -> Optional.empty().get())
                .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> Optional.empty().orElseThrow())
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── 11. 不推荐的用法：编译器不拦，只能自己守 ───────────────────

    @Test
    @DisplayName("Optional 当字段 / 当参数都能编译能跑——所以只能靠自己记住别这么写")
    void badStylesStillCompile() {
        OptionalDemo.BadStyle bad = new OptionalDemo.BadStyle(Optional.of("小三"));
        // 跑得好好的，编译器一句怨言都没有
        assertThat(bad.nickname()).contains("小三");
        assertThat(bad.greet(Optional.of("你好"))).isEqualTo("你好，小三");
        // 参数传 null 才会炸，而且炸在运行时
        assertThatThrownBy(() -> bad.greet(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("用 Optional 包集合是双重否定：空集合本身已经表达了「没有」")
    void wrappingACollectionIsRedundant() {
        Optional<List<String>> wrapped = Optional.of(List.of());
        // 外面这层 Optional 永远是 present，判断它毫无意义
        assertThat(wrapped).isPresent();
        assertThat(wrapped.orElseThrow()).isEmpty();
        // 直接用空集合就够了
        assertThat(List.<String>of()).isEmpty();
    }

    @Test
    @DisplayName("User.nickname() 用 Optional 当返回类型，字段本身仍然是 String")
    void optionalAsReturnTypeButNotAsField() throws NoSuchFieldException, NoSuchMethodException {
        // 返回类型是 Optional<String>
        assertThat(User.class.getMethod("nickname").getReturnType()).isEqualTo(Optional.class);
        // 但字段类型是 String——字段不该用 Optional
        assertThat(User.class.getDeclaredField("nickname").getType()).isEqualTo(String.class);
    }
}
