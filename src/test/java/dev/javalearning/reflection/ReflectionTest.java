package dev.javalearning.reflection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 09 课的断言。
 *
 * <p>每一条对应 {@code docs/09-反射.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*reflection*'}
 *
 * <p>这一课测试的特别之处：测的就是反射本身，所以断言里全是
 * {@code getDeclaredField} / {@code invoke} 这些东西——有点"用反射测反射"的味道。
 */
class ReflectionTest {

    // ── 1. 常规访问 ────────────────────────────────────────────────

    @Test
    @DisplayName("常规访问：直接 new、直接读字段、直接调方法")
    void plainAccessWorks() {
        User user = new User("小明", 18);
        assertThat(user.name).isEqualTo("小明");
        assertThat(user.hello()).isEqualTo("你好，我是 小明");
        assertThat(user.toString()).isEqualTo("User(name=小明, age=18)");
    }

    @Test
    @DisplayName("toString 重写过，打印出来没有哈希值，文档才可能逐字复现")
    void toStringHasNoHashCode() {
        assertThat(new User("小明", 18).toString()).doesNotContain("@");
    }

    // ── 3. Class 对象 ──────────────────────────────────────────────

    @Test
    @DisplayName("每个类只有一个 Class 对象，三种拿法指向同一个")
    void oneClassObjectPerClass() throws ClassNotFoundException {
        User user = new User("小明", 18);
        Class<?> byLiteral = User.class;
        Class<?> byGetClass = user.getClass();
        Class<?> byForName = Class.forName("dev.javalearning.reflection.User");

        assertThat(byGetClass).isSameAs(byLiteral);
        assertThat(byForName).isSameAs(byLiteral);
    }

    @Test
    @DisplayName("Class 对象是类的档案：名字、父类、是不是接口都能问出来")
    void classObjectDescribesTheClass() {
        assertThat(User.class.getName()).isEqualTo("dev.javalearning.reflection.User");
        assertThat(User.class.getSimpleName()).isEqualTo("User");
        assertThat(User.class.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(User.class.isInterface()).isFalse();
        assertThat(Label.class.isInterface()).isTrue();   // @interface 本质就是接口
    }

    // ── 4. 三种获取方式的关键区别：类初始化 ────────────────────────

    @Test
    @DisplayName("类名.class 不触发初始化，Class.forName 才触发——这是三种方式最关键的区别")
    void classLiteralDoesNotInitializeButForNameDoes() throws ClassNotFoundException {
        // InitProbe 只被这一条测试碰过，所以探针一开始一定是 false。
        // 探针标志位放在 InitProbeLog 里：读 InitProbe 自己的 static 字段
        // 就会触发初始化，那等于自己把要测的东西弄脏了。
        assertThat(InitProbeLog.probeInitialized).isFalse();

        assertThat(InitProbe.class).isNotNull();                 // 只是拿到 Class 对象
        assertThat(InitProbeLog.probeInitialized).isFalse();     // ← 没初始化，证据在这

        Class<?> loaded = Class.forName("dev.javalearning.reflection.InitProbe");
        assertThat(loaded).isSameAs(InitProbe.class);            // 同一个 Class 对象
        assertThat(InitProbeLog.probeInitialized).isTrue();      // ← forName 触发了初始化
    }

    @Test
    @DisplayName("对象.getClass() 也是拿档案，不额外触发什么")
    void getClassReturnsTheSameArchive() {
        // 故意用 User 而不是 InitProbe：InitProbe 只许被上一条测试碰。
        // JUnit 不保证测试方法的执行顺序，如果这里也去 new InitProbe()，
        // 上一条的 `probeInitialized == false` 就可能先被别人弄脏。
        User user = new User("小明", 18);
        assertThat(user.getClass()).isSameAs(User.class);
    }

    // ── 5. 查看成员：declared 系列 vs 不带 declared ────────────────

    @Test
    @DisplayName("getDeclaredFields 拿本类所有字段（含 private），不含父类")
    void declaredFieldsAreAllOfThisClassOnly() {
        Set<String> names = Arrays.stream(User.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder("name", "age", "count");
        // 父类那个 public id 不在里面——declared 只看本类
        assertThat(names).doesNotContain("id");
    }

    @Test
    @DisplayName("getFields 只拿 public，但会把父类的也带上")
    void publicFieldsIncludeInheritedOnes() {
        List<Field> fields = List.of(User.class.getFields());
        Set<String> names = fields.stream().map(Field::getName).collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder("name", "id");
        // 关键区别：id 是父类 BaseEntity 声明的，getFields 带上了
        assertThat(fields.stream()
                .filter(f -> f.getName().equals("id"))
                .findFirst().orElseThrow()
                .getDeclaringClass()).isEqualTo(BaseEntity.class);
        // 而 private 的 age / count 一个都不在
        assertThat(names).doesNotContain("age", "count");
    }

    @Test
    @DisplayName("getDeclaredMethods 能看到 private 方法，getMethods 看不到")
    void declaredMethodsSeePrivateOnes() {
        Set<String> declared = Arrays.stream(User.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        Set<String> publics = Arrays.stream(User.class.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(declared).contains("secret", "greet", "internalCode", "hello");
        assertThat(publics).contains("hello");
        assertThat(publics).doesNotContain("secret", "greet", "internalCode");
    }

    @Test
    @DisplayName("getMethods 会带上父类和 Object 的 public 方法")
    void publicMethodsIncludeInheritedAndObjectOnes() {
        Set<String> publics = Arrays.stream(User.class.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(publics).contains("whoAmI");          // 父类 BaseEntity 的
        assertThat(publics).contains("equals", "hashCode", "wait");   // Object 的
        // declared 那侧没有它们
        Set<String> declared = Arrays.stream(User.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertThat(declared).doesNotContain("whoAmI", "wait");
    }

    @Test
    @DisplayName("getDeclaredConstructors 看得到 private 构造器，getConstructors 看不到")
    void constructorsAlsoHaveTheDeclaredSplit() {
        assertThat(User.class.getDeclaredConstructors()).hasSize(2);
        assertThat(User.class.getConstructors()).hasSize(2);

        // HeavyInit 的构造器是 private 的，只有 declared 那侧看得到
        assertThat(HeavyInit.class.getDeclaredConstructors()).hasSize(1);
        assertThat(HeavyInit.class.getConstructors()).isEmpty();
    }

    // ── 6. 操作成员 ────────────────────────────────────────────────

    @Test
    @DisplayName("public 字段直接读写，改完对象自己也变了")
    void publicFieldCanBeReadAndWritten() throws ReflectiveOperationException {
        User user = new User("小明", 18);
        Field name = User.class.getField("name");

        assertThat(name.get(user)).isEqualTo("小明");
        name.set(user, "小红");
        assertThat(name.get(user)).isEqualTo("小红");
        assertThat(user.name).isEqualTo("小红");   // 不是副本，改的就是那个对象
    }

    @Test
    @DisplayName("private 字段直接读会抛 IllegalAccessException——这不是摆设")
    void privateFieldThrowsIllegalAccess() throws ReflectiveOperationException {
        User user = new User("小明", 18);
        Field age = User.class.getDeclaredField("age");

        assertThatThrownBy(() -> age.get(user))
                .isInstanceOf(IllegalAccessException.class)
                .hasMessageContaining("private");
    }

    @Test
    @DisplayName("setAccessible(true) 之后 private 字段照样读写")
    void setAccessibleUnlocksPrivateField() throws ReflectiveOperationException {
        User user = new User("小明", 18);
        Field age = User.class.getDeclaredField("age");
        age.setAccessible(true);

        assertThat(age.get(user)).isEqualTo(18);
        age.set(user, 20);
        assertThat(age.get(user)).isEqualTo(20);
        assertThat(user.toString()).isEqualTo("User(name=小明, age=20)");
    }

    @Test
    @DisplayName("static 字段传 null 当对象参数也能读，结果和传个对象一模一样")
    void staticFieldReadsWithNullTarget() throws ReflectiveOperationException {
        Field count = User.class.getDeclaredField("count");
        count.setAccessible(true);
        User someone = new User("路人", 0);

        // static 字段属于类，不属于对象，所以对象参数传 null 和传谁都一样。
        //
        // 这里踩过一个坑：原来写成
        //     assertThat(count.get(null)).isEqualTo(count.get(new User("路人", 0)));
        // 结果 expected 9 but was 10 —— 因为参数是从左往右求值的，
        // 左边先读到 9，然后 new User(...) 把 count 变成 10，右边读到 10。
        // 断言里的表达式也会改变被断言的状态，所以先把对象造好再比。
        assertThat(count.get(null)).isEqualTo(count.get(someone));
        assertThat(count.get(someone)).isEqualTo(count.get(null));
    }

    @Test
    @DisplayName("public 方法用 invoke 调用，结果和直接调一样")
    void publicMethodInvokeMatchesDirectCall() throws ReflectiveOperationException {
        User user = new User("小明", 18);
        Method hello = User.class.getMethod("hello");
        assertThat(hello.invoke(user)).isEqualTo(user.hello());
    }

    @Test
    @DisplayName("private 方法 invoke 会抛 IllegalAccessException，setAccessible 之后能调")
    void privateMethodNeedsSetAccessible() throws ReflectiveOperationException {
        User user = new User("小明", 18);
        Method secret = User.class.getDeclaredMethod("secret");

        assertThatThrownBy(() -> secret.invoke(user))
                .isInstanceOf(IllegalAccessException.class);

        secret.setAccessible(true);
        assertThat(secret.invoke(user)).isEqualTo("这是 private 方法，外面本来调不到");
    }

    @Test
    @DisplayName("带参数的 private 方法：先按类型找到它，再按顺序传参")
    void privateMethodWithArguments() throws ReflectiveOperationException {
        User user = new User("小明", 18);
        Method greet = User.class.getDeclaredMethod("greet", String.class, int.class);
        greet.setAccessible(true);

        assertThat(greet.invoke(user, "嗨", 3)).isEqualTo("嗨 嗨 嗨");
        assertThat(greet.invoke(user, "哈", 1)).isEqualTo("哈");
        // 参数类型不对会抛 IllegalArgumentException，不是编译错误
        assertThatThrownBy(() -> greet.invoke(user, 123, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("static 方法的对象参数传 null")
    void staticMethodInvokesWithNull() throws ReflectiveOperationException {
        Method getCount = User.class.getMethod("getCount");
        assertThat(getCount.invoke(null)).isEqualTo(User.getCount());

        Method internalCode = User.class.getDeclaredMethod("internalCode");
        internalCode.setAccessible(true);
        assertThat((String) internalCode.invoke(null)).startsWith("内部编号-");
    }

    // ── 7. 创建实例 ────────────────────────────────────────────────

    @Test
    @DisplayName("constructor.newInstance(args) 造对象，等价于直接 new")
    void newInstanceCreatesObjects() throws ReflectiveOperationException {
        Constructor<?> noArg = User.class.getDeclaredConstructor();
        assertThat(noArg.newInstance().toString()).isEqualTo("User(name=匿名, age=0)");

        Constructor<?> withArgs = User.class.getDeclaredConstructor(String.class, int.class);
        assertThat(withArgs.newInstance("小刚", 20).toString()).isEqualTo("User(name=小刚, age=20)");
    }

    @Test
    @DisplayName("private 构造器要 setAccessible(true) 才造得出来")
    void privateConstructorNeedsSetAccessible() throws ReflectiveOperationException {
        Constructor<?> hidden = HeavyInit.class.getDeclaredConstructor();
        assertThatThrownBy(hidden::newInstance)
                .isInstanceOf(IllegalAccessException.class);

        hidden.setAccessible(true);
        assertThat(hidden.newInstance()).isInstanceOf(HeavyInit.class);
    }

    // ── 8. 注解 ────────────────────────────────────────────────────

    @Test
    @DisplayName("贴了 RUNTIME 注解的字段，运行时读得到")
    void runtimeAnnotationIsReadable() throws ReflectiveOperationException {
        Field age = User.class.getDeclaredField("age");
        Label label = age.getAnnotation(Label.class);

        assertThat(label).isNotNull();
        assertThat(label.value()).isEqualTo("年龄");
    }

    @Test
    @DisplayName("没贴注解的字段返回 null——所以拿注解前必须先判空")
    void missingAnnotationReturnsNull() throws ReflectiveOperationException {
        Field name = User.class.getDeclaredField("name");
        assertThat(name.getAnnotation(Label.class)).isNull();
        assertThat(name.getAnnotations()).isEmpty();
    }

    @Test
    @DisplayName("没写 @Retention(RUNTIME) 的注解，编译过了但运行时读不到，静默返回 null")
    void classRetentionAnnotationIsInvisibleAtRuntime() throws ReflectiveOperationException {
        // ReflectionDemo.ClassOnlyLabel 故意没写 @Retention，默认是 CLASS
        Field classField = ReflectionDemo.Labeled.class.getDeclaredField("classField");
        assertThat(classField.getAnnotation(ReflectionDemo.ClassOnlyLabel.class)).isNull();
        // 对比：同一个类里那个写了 RUNTIME 的字段就读得到
        Field runtimeField = ReflectionDemo.Labeled.class.getDeclaredField("runtimeField");
        assertThat(runtimeField.getAnnotation(Label.class)).isNotNull();
    }

    @Test
    @DisplayName("注解自己也能被反射检查：@Retention 的值是 RUNTIME")
    void retentionMetaAnnotationIsItselfVisible() {
        assertThat(Label.class.getAnnotation(java.lang.annotation.Retention.class).value())
                .isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
        // 没有 @Retention 的注解，getAnnotation(Retention.class) 就是 null
        assertThat(ReflectionDemo.ClassOnlyLabel.class
                .getAnnotation(java.lang.annotation.Retention.class)).isNull();
    }

    @Test
    @DisplayName("@Target(FIELD) 限定注解只能贴字段——元注解同样能读出来")
    void targetMetaAnnotationIsReadable() {
        java.lang.annotation.Target target = Label.class.getAnnotation(java.lang.annotation.Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(java.lang.annotation.ElementType.FIELD);
    }

    // ── 9. 代价与风险 ──────────────────────────────────────────────

    @Test
    @DisplayName("字段名拼错：编译期一声不吭，运行时才抛 NoSuchFieldException")
    void typoInFieldNameBlowsUpAtRuntime() {
        assertThatThrownBy(() -> User.class.getDeclaredField("nmae"))
                .isInstanceOf(NoSuchFieldException.class)
                .hasMessageContaining("nmae");
    }

    @Test
    @DisplayName("方法名拼错：运行时才抛 NoSuchMethodException")
    void typoInMethodNameBlowsUpAtRuntime() {
        assertThatThrownBy(() -> User.class.getDeclaredMethod("helo"))
                .isInstanceOf(NoSuchMethodException.class)
                .hasMessageContaining("helo");
    }

    @Test
    @DisplayName("方法签名对不上（参数类型不对）也抛 NoSuchMethodException")
    void wrongParameterTypesAlsoThrow() {
        // greet 的第二个参数是 int，写成 String 就找不到
        assertThatThrownBy(() -> User.class.getDeclaredMethod("greet", String.class, String.class))
                .isInstanceOf(NoSuchMethodException.class);
        // 但参数个数对、类型对就找得到
        assertThat(User.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("greet") && m.getParameterCount() == 2);
    }

    @Test
    @DisplayName("反射能改 private 字段，说明它确实破坏了封装")
    void reflectionBreaksEncapsulation() throws ReflectiveOperationException {
        User user = new User("小明", 18);
        Field age = User.class.getDeclaredField("age");
        age.setAccessible(true);
        age.set(user, -999);   // 这个值没有任何校验能拦住
        assertThat(user.toString()).isEqualTo("User(name=小明, age=-999)");
    }

    @Test
    @DisplayName("setAccessible 只是关掉检查，没把 private 变成 public")
    void setAccessibleDoesNotChangeModifiers() throws ReflectiveOperationException {
        Field age = User.class.getDeclaredField("age");
        assertThat(Modifier.isPrivate(age.getModifiers())).isTrue();
        age.setAccessible(true);
        // 修饰符一点没变，变的只是"调用时查不查"
        assertThat(Modifier.isPrivate(age.getModifiers())).isTrue();
        assertThat(age.canAccess(new User("小明", 18))).isTrue();
    }

    // ── 10. 框架为什么离不开它 ─────────────────────────────────────

    @Test
    @DisplayName("框架的典型动作：拿到 Class，扫一遍方法，找出带某个注解的")
    void frameworkStyleScanWorks() {
        // 这里模拟 JUnit 找 @Test 方法、Spring 找 @Autowired 字段的做法
        List<String> labeled = Arrays.stream(User.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Label.class))
                .map(Field::getName)
                .toList();
        assertThat(labeled).containsExactly("age");

        // 再模拟一次"按注解取出它的值"
        String label = Arrays.stream(User.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Label.class))
                .map(f -> f.getAnnotation(Label.class).value())
                .findFirst()
                .orElseThrow();
        assertThat(label).isEqualTo("年龄");
    }

    @Test
    @DisplayName("反射能调到的成员，正好就是文档里列出的那些")
    void reflectionSeesExactlyWhatTheDocClaims() {
        assertThat(User.class.getDeclaredFields()).hasSize(3);
        assertThat(User.class.getFields()).hasSize(2);
        assertThat(User.class.getDeclaredConstructors()).hasSize(2);
        // getMethods 的数量取决于 JDK，别写死数字；只断言"含父类和 Object 的"
        assertThat(User.class.getMethods().length).isGreaterThan(User.class.getDeclaredMethods().length);
    }

    @Test
    @DisplayName("注解类型本身也是接口，能当 Class 用")
    void annotationTypeIsAnInterface() {
        Class<? extends Annotation> type = Label.class;
        assertThat(type.isInterface()).isTrue();
        assertThat(type.isAnnotation()).isTrue();
        assertThat(type.getSimpleName()).isEqualTo("Label");
    }
}
