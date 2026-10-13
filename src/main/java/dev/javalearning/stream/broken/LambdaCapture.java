package dev.javalearning.stream.broken;

import java.util.List;

/**
 * 这是<b>故意写错</b>的反面教材：想在 lambda 里用外部局部变量累加。
 *
 * <p>它<b>编译不过</b>，所以住在 {@code broken/} 包里——{@code build.gradle.kts}
 * 已经把整个 {@code **&#47;broken/**} 排除出构建，在 IDE 里看到它标红是正常的。
 *
 * <p>真实报错（完整输出连同行号和插入符见 docs/10-Stream.md，那里是逐字复制的）：
 * <pre>
 * error: local variables referenced from a lambda expression must be final or effectively final
 * </pre>
 *
 * <p>讲 {@code forEach} 的副作用时用它。正确做法是让流自己算：
 * {@code people().stream().mapToInt(Person::age).sum()}。
 */
public class LambdaCapture {

    public static void main(String[] args) {
        int sum = 0;
        List.of(1, 2, 3).stream().forEach(n -> sum += n);   // ← 编译不过
        System.out.println(sum);
    }
}
