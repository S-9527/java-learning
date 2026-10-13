package dev.javalearning.reflection;

/**
 * 第 09 课的「教材类」：故意把各种成员都放齐，方便演示反射能碰到什么。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/reflection/ReflectionDemo.java}
 * 对应文档 {@code docs/09-反射.md}。
 *
 * <p>为什么 {@code toString()} 被重写了？因为反射的输出里<b>不能出现哈希值</b>
 * （{@code User@1b6d3586} 这种每次都变），否则文档里的执行结果没法逐字复现。
 */
public class User extends BaseEntity {

    /** public 字段：谁都能直接读。 */
    public String name;

    /** private 字段 + 自定义注解：反射的注解演示就打在这里。 */
    @Label("年龄")
    private int age;

    /** static 字段：属于类，不属于对象，反射取值时要传 null 当对象。 */
    private static int count;

    /*
     * static 初始化块：类被「初始化」时执行，而且一辈子只执行一次。
     * 它是本课证明「谁触发了类初始化」的探针。
     */
    static {
        System.out.println("[User 的 static 块执行了 —— User 类被初始化了]");
    }

    /** 无参构造器：转发给下面那个带参的。 */
    public User() {
        this("匿名", 0);
    }

    /** 带参构造器：反射创建实例时会用到它。 */
    public User(String name, int age) {
        this.name = name;
        this.age = age;
        count++;
    }

    /** public 方法：直接调没问题，反射也能调。 */
    public String hello() {
        return "你好，我是 " + name;
    }

    /** private 方法：外面直接调编译不过，反射要用 setAccessible(true) 才碰得到。 */
    private String secret() {
        return "这是 private 方法，外面本来调不到";
    }

    /** 带参数的 private 方法：反射调它得先按类型找到它，再按顺序传参。 */
    private String greet(String prefix, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(prefix).append(" ");
        }
        return sb.toString().trim();
    }

    /** public static 方法：反射调用时对象参数传 null。 */
    public static int getCount() {
        return count;
    }

    /** private static 方法：同样是 private，同样要 setAccessible。 */
    private static String internalCode() {
        return "内部编号-" + count;
    }

    @Override
    public String toString() {
        return "User(name=" + name + ", age=" + age + ")";
    }
}
