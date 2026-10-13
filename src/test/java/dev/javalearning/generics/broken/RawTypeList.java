package dev.javalearning.generics.broken;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 06 课的反面教材：<b>原始类型（raw type）的错误留到运行期。</b>
 *
 * <p><b>它住在 src/test 而不是 src/main 的 broken/</b>——这不是编译错误，
 * 而是行为错误：编译得过、程序照跑，直到你把取出来的东西当成错误的类型用，
 * 才在运行期炸。正因为它编译得过，才必须能被测试引用，
 * 才能在断言里把这个静默错误钉住。（编译不过的反面教材才放 main 的 broken/ 里。）
 *
 * <p>写成 {@code List<String>} 的话，{@code raw.add(42)} 那一行编译就不过了。
 * 这就是泛型的价值：把运行期的 {@code ClassCastException} 提前成编译错误。
 *
 * <p>本类用 {@code @SuppressWarnings} 把编译警告压掉了，因为这里要演示的是
 * <b>运行期</b>那一半。编译警告的原话在 {@link RawTypeWarn}（那里没压），
 * 文档里贴的是它的真实输出。
 */
public class RawTypeList {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List buildMixedList() {
        List raw = new ArrayList();      // ← 原始类型：丢掉了类型参数
        raw.add("Alice");
        raw.add(42);                     // 编译器只能给警告，拦不住
        return raw;
    }

    public static void main(String[] args) {
        List<?> raw = buildMixedList();
        System.out.println("原始类型 List 里塞了 String 和 Integer：" + raw);

        try {
            String second = (String) raw.get(1);
            System.out.println("居然转过来了：" + second);
        } catch (ClassCastException e) {
            System.out.println("把第 2 个当成 String 取出来：");
            System.out.println("  ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String");
            System.out.println("← 泛型本来能在编译期拦住它，raw type 把这个能力还回去了");
        }
    }
}
