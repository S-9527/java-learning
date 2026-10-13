package dev.javalearning.reflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自己写的注解，用来演示「字段上贴的标签，运行时能不能读出来」。
 *
 * <p>两个元注解一个都不能少，少一个反射就读不到：
 * <ul>
 *   <li>{@code @Retention(RUNTIME)} —— 不写默认是 {@code CLASS}，
 *       注解在编译后就被丢掉，运行时 {@code getAnnotation} 只会返回 {@code null}；</li>
 *   <li>{@code @Target(FIELD)} —— 限定它只能贴在字段上，贴错地方编译器会拦。</li>
 * </ul>
 *
 * <p>少了 {@code @Retention(RUNTIME)} 会怎样？{@code ReflectionDemo} 第 8 节拿
 * {@code @ClassOnlyLabel} 当场演示了一遍：编译通过、运行不报错，
 * 但 {@code getAnnotation} 就是返回 {@code null}——这种静默失效比报错难查得多。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Label {

    /** 标签文字。写了默认值，所以 {@code @Label} 和 {@code @Label("年龄")} 都合法。 */
    String value() default "";
}
