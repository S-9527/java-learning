package dev.javalearning.generics.broken;

/**
 * 第 06 课的反面教材：<b>这是故意写错的。</b>
 *
 * <p>泛型数组创建不了。原因要到「类型擦除」那节才说得清：
 * 运行时 {@code T} 已经不存在了，JVM 不知道该给这个数组分配多大、
 * 也不知道该在写入时检查什么类型，所以干脆禁止。
 *
 * <p>为什么放这里：它编译不过，参与构建会让整个项目挂掉，
 * 而这一课讲的正是这个错误本身。{@code build.gradle.kts} 已经整包排除
 * {@code **&#47;broken/**}，在 IDE 里看到它标红是正常的。
 *
 * <p>真实错误见 {@code docs/06-泛型.md}。
 */
public class GenericArrayCreation<T> {

    public T[] makeArray() {
        return new T[10];   // ← 泛型数组创建不了
    }

    public static void main(String[] args) {
        System.out.println(new GenericArrayCreation<String>().makeArray().length);
    }
}
