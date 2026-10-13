package dev.javalearning.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第 09 课的示例代码：反射 Reflection。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/reflection/ReflectionDemo.java}
 * 每一节对应 {@code docs/09-反射.md} 里的一节，输出都是真跑出来的。
 *
 * <p>配套文件：
 * <ul>
 *   <li>{@link User} —— 教材类，各种成员都放齐了；</li>
 *   <li>{@link BaseEntity} —— 用来证明 {@code getFields()} 会带上父类的 public 成员；</li>
 *   <li>{@link HeavyInit} —— 一个从头到尾没被碰过的类，用来演示"谁触发了类初始化"；</li>
 *   <li>{@link ReflectionCost} —— 单独跑，看反射调用到底慢多少。</li>
 * </ul>
 */
public class ReflectionDemo {

    /** 故意不写 {@code @Retention}：默认是 CLASS，注解进了 class 文件但运行时读不到。 */
    @interface ClassOnlyLabel {
    }

    /** 注解演示的载体：一个注解能读到、一个注解读不到、一个压根没贴注解。 */
    static class Labeled {
        @Label("保留到运行时的标签")
        private String runtimeField;
        @ClassOnlyLabel
        private String classField;
        private String plainField;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        section("1. 先看常规访问：直接 new、直接读、直接调");
        User user = new User("小明", 18);
        System.out.println("直接 new 出来的对象：" + user);
        System.out.println("直接读 public 字段 user.name = " + user.name);
        System.out.println("直接调 public 方法 user.hello() = " + user.hello());
        System.out.println("通过类名读 static 方法 User.getCount() = " + User.getCount());
        System.out.println("想直接读 private 字段 user.age —— 编译不过，见 broken/ReadPrivateField.java");

        section("2. 什么时候不够用：类名只在运行时才知道");
        Map<String, String> config = new LinkedHashMap<>();
        config.put("className", "dev.javalearning.reflection.User");
        config.put("methodName", "hello");
        System.out.println("配置文件里读到的是：" + config);
        System.out.println("想造对象：new " + config.get("className") + "()   ← 这行根本编译不过");
        System.out.println("想调方法：user." + config.get("methodName") + "()   ← 同理，方法名也得写死");
        System.out.println("javac 的原话见 broken/DynamicNew.java");
        System.out.println("那有没有办法？有，就是接下来要讲的「反射」");

        section("3. Class 对象：JVM 给每个类建的那份档案");
        System.out.println("User.class 就是 " + User.class);
        System.out.println("它的类型是 " + User.class.getClass().getName() + " —— Class 自己也是个类");
        System.out.println("全限定名 getName() = " + User.class.getName());
        System.out.println("简单名 getSimpleName() = " + User.class.getSimpleName());
        System.out.println("父类 getSuperclass() = " + User.class.getSuperclass().getName());
        System.out.println("是不是接口 isInterface() = " + User.class.isInterface());
        System.out.println("同一个类只有一个 Class 对象：user.getClass() == User.class → "
                + (user.getClass() == User.class));

        section("4. 获取 Class 对象的三种方式，以及一个关键区别");
        System.out.println("① 类名.class（编译期就写死）");
        Class<?> fromLiteral = HeavyInit.class;
        System.out.println("   拿到了 " + fromLiteral.getName() + "，注意上面没有 static 块的输出");
        System.out.println("② 对象.getClass()（手里已经有实例时用）");
        System.out.println("   user.getClass() = " + user.getClass().getName());
        System.out.println("③ Class.forName(\"全限定名\")（运行时才决定加载谁）");
        Class<?> fromForName = Class.forName("dev.javalearning.reflection.HeavyInit");
        System.out.println("   拿到了 " + fromForName.getName() + "，上面那行 static 块输出就是证据");
        System.out.println("三种方式拿到的是同一个 Class 对象吗："
                + (fromLiteral == fromForName) + "（①②③ 指向的都是同一个类）");
        System.out.println("再 forName 一次，已经初始化过了，static 块不会再打：");
        Class.forName("dev.javalearning.reflection.HeavyInit");
        System.out.println("   确实没再打 —— 一个类的初始化一辈子只发生一次");

        section("5. 查看成员：declared 系列和不带 declared 的区别");
        System.out.println("getDeclaredFields() 共 " + User.class.getDeclaredFields().length
                + " 个（本类所有字段，含 private，不含父类）：");
        for (Field f : sortedFields(User.class.getDeclaredFields())) {
            System.out.println(fieldLine(f));
        }
        System.out.println("getFields() 共 " + User.class.getFields().length
                + " 个（只有 public，但含父类）：");
        for (Field f : sortedFields(User.class.getFields())) {
            System.out.println(fieldLine(f));
        }
        System.out.println("getDeclaredMethods() 共 " + User.class.getDeclaredMethods().length + " 个（本类的，含 private）：");
        for (Method m : sortedMethods(User.class.getDeclaredMethods())) {
            System.out.println(methodLine(m));
        }
        System.out.println("getMethods() 共 " + User.class.getMethods().length
                + " 个（public，含父类，连 Object 的也算）：");
        for (Method m : sortedMethods(User.class.getMethods())) {
            System.out.println(methodLine(m));
        }
        System.out.println("getDeclaredConstructors() 共 " + User.class.getDeclaredConstructors().length + " 个：");
        for (Constructor<?> c : User.class.getDeclaredConstructors()) {
            System.out.println("  " + Modifier.toString(c.getModifiers()) + " " + c.getName() + paramsOf(c));
        }
        System.out.println("getConstructors() 共 " + User.class.getConstructors().length + " 个（只有 public 的）");

        section("6. 操作成员：读、改、调用");
        Field name = User.class.getField("name");
        System.out.println("读 public 字段 field.get(user) = " + name.get(user));
        name.set(user, "小红");
        System.out.println("改完再读 = " + name.get(user) + "，对象自己也变了：" + user);
        Field age = User.class.getDeclaredField("age");
        try {
            System.out.println("读 private 字段 = " + age.get(user));
        } catch (IllegalAccessException e) {
            System.out.println("读 private 字段 → IllegalAccessException: " + e.getMessage());
        }
        age.setAccessible(true);
        System.out.println("setAccessible(true) 之后 = " + age.get(user));
        age.set(user, 20);
        System.out.println("连 private 字段都能改：" + user);
        Field count = User.class.getDeclaredField("count");
        count.setAccessible(true);
        System.out.println("static 字段传 null 当对象也能读：count = " + count.get(null));
        Method hello = User.class.getMethod("hello");
        System.out.println("调 public 方法 method.invoke(user) = " + hello.invoke(user));
        Method secret = User.class.getDeclaredMethod("secret");
        try {
            System.out.println("调 private 方法 = " + secret.invoke(user));
        } catch (IllegalAccessException e) {
            System.out.println("调 private 方法 → IllegalAccessException: " + e.getMessage());
        }
        secret.setAccessible(true);
        System.out.println("setAccessible(true) 之后 = " + secret.invoke(user));
        Method greet = User.class.getDeclaredMethod("greet", String.class, int.class);
        greet.setAccessible(true);
        System.out.println("带参数的 private 方法 greet(\"嗨\", 3) = [" + greet.invoke(user, "嗨", 3) + "]");
        Method getCount = User.class.getMethod("getCount");
        System.out.println("static 方法对象参数传 null：getCount() = " + getCount.invoke(null));
        Method internalCode = User.class.getDeclaredMethod("internalCode");
        internalCode.setAccessible(true);
        System.out.println("private static 方法同样传 null：internalCode() = " + internalCode.invoke(null));

        section("7. 创建实例：constructor.newInstance(args)");
        Constructor<?> noArg = User.class.getDeclaredConstructor();
        System.out.println("无参构造器 newInstance() = " + noArg.newInstance());
        Constructor<?> withArgs = User.class.getDeclaredConstructor(String.class, int.class);
        System.out.println("带参构造器 newInstance(\"小刚\", 20) = " + withArgs.newInstance("小刚", 20));
        System.out.println("Class.newInstance() 从 Java 9 起就废弃了，统一走构造器");
        Constructor<?> hidden = HeavyInit.class.getDeclaredConstructor();
        System.out.println("HeavyInit 的 getConstructors() = " + HeavyInit.class.getConstructors().length
                + " 个，getDeclaredConstructors() = " + HeavyInit.class.getDeclaredConstructors().length + " 个");
        try {
            System.out.println("private 构造器直接 newInstance() = " + hidden.newInstance());
        } catch (IllegalAccessException e) {
            System.out.println("private 构造器直接 newInstance() → IllegalAccessException: " + e.getMessage());
        }
        hidden.setAccessible(true);
        System.out.println("setAccessible(true) 之后造出来了，类型是 "
                + hidden.newInstance().getClass().getSimpleName());

        section("8. 注解：字段上贴的标签，运行时读得出来吗");
        Field annotated = User.class.getDeclaredField("age");
        System.out.println("age 上的 @Label = " + annotated.getAnnotation(Label.class).value());
        Field notAnnotated = User.class.getDeclaredField("name");
        System.out.println("name 上的 @Label = " + notAnnotated.getAnnotation(Label.class) + "（没贴就是 null）");
        Field runtimeField = Labeled.class.getDeclaredField("runtimeField");
        System.out.println("@Label 有 @Retention(RUNTIME)，读到了 = " + runtimeField.getAnnotation(Label.class).value());
        Field classField = Labeled.class.getDeclaredField("classField");
        System.out.println("@ClassOnlyLabel 没写 @Retention，默认是 CLASS，读到 = "
                + classField.getAnnotation(ClassOnlyLabel.class));
        Field plainField = Labeled.class.getDeclaredField("plainField");
        System.out.println("没贴注解的字段读到 = " + plainField.getAnnotation(Label.class));

        section("9. 反射的代价与风险");
        try {
            User.class.getDeclaredField("nmae");
        } catch (NoSuchFieldException e) {
            System.out.println("字段名拼错 getDeclaredField(\"nmae\") → NoSuchFieldException: " + e.getMessage());
        }
        try {
            User.class.getDeclaredMethod("helo");
        } catch (NoSuchMethodException e) {
            System.out.println("方法名拼错 getDeclaredMethod(\"helo\") → NoSuchMethodException: " + e.getMessage());
        }
        System.out.println("注意：这两行编译期一声不吭，写完代码看不出来，跑起来才炸");
        // 耗时每次运行都不一样，贴进这里会让本节输出不稳定，
        // 所以真实数字单独跑 ReflectionCost 看，见 docs/09-反射.md。
        System.out.println("反射调用比直接调用慢 —— 真实耗时见 ReflectionCost.java，数字每轮都不同");
        System.out.println("setAccessible(true) 让 private 形同虚设 —— 上面已经演示过了");
        System.out.println("Spring 的依赖注入、JUnit 找 @Test 方法，都靠反射，因为那些类写框架时还不存在");

        section("10. 什么时候别用反射");
        System.out.println("能用直接调用就别用反射：编译期能查出的错，别留到运行期");
        System.out.println("能写 user.hello() 就别写 method.invoke(user)");
        System.out.println("setAccessible(true) 别滥用：它只是关掉了检查，不是把 private 变成 public");
        System.out.println("反射该出场的地方：框架、容器、序列化、测试工具");
    }

    /** 排序只是为了输出稳定好看，反射返回的顺序本来就不保证。 */
    private static Field[] sortedFields(Field[] fields) {
        Arrays.sort(fields, Comparator.comparing((Field f) -> f.getName()));
        return fields;
    }

    private static Method[] sortedMethods(Method[] methods) {
        Arrays.sort(methods, Comparator.comparing((Method m) -> m.getName())
                .thenComparingInt(Method::getParameterCount));
        return methods;
    }

    private static String fieldLine(Field f) {
        return "  " + Modifier.toString(f.getModifiers()) + " " + f.getType().getSimpleName()
                + " " + f.getName() + "   ← 声明它的类是 " + f.getDeclaringClass().getSimpleName();
    }

    private static String methodLine(Method m) {
        return "  " + Modifier.toString(m.getModifiers()) + " " + m.getReturnType().getSimpleName()
                + " " + m.getName() + paramsOf(m.getParameterTypes())
                + "   ← 声明它的类是 " + m.getDeclaringClass().getSimpleName();
    }

    private static String paramsOf(Constructor<?> c) {
        return paramsOf(c.getParameterTypes());
    }

    private static String paramsOf(Class<?>[] types) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(types[i].getSimpleName());
        }
        return sb.append(")").toString();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
