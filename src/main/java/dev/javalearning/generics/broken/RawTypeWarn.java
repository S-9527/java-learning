package dev.javalearning.generics.broken;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 06 课的反面教材：<b>原始类型（raw type）只给警告，不给错误。</b>
 *
 * <p><b>它编译得过</b>——这正是它危险的地方。编译器只会说一句
 * 「uses unchecked or unsafe operations」，程序照跑，
 * 直到你把取出来的东西当成错误的类型用，才在运行期炸。
 *
 * <p>为什么放 main 的 broken/ 里：它演示的是<b>编译警告</b>，
 * 而警告的原话要贴进文档，所以得是一个能单独 {@code javac} 的文件。
 * （{@code build.gradle.kts} 已整包排除 {@code **&#47;broken/**}，
 * 所以它不会给日常构建添噪音；真正跑出 ClassCastException 的那份在
 * {@code src/test/.../generics/broken/RawTypeList.java}。）
 *
 * <p>真实编译输出见 {@code docs/06-泛型.md}。
 */
public class RawTypeWarn {

    public static List buildMixedList() {
        List raw = new ArrayList();      // ← 原始类型：丢掉了类型参数
        raw.add("Alice");
        raw.add(42);                     // 编译器只能给警告，拦不住
        return raw;
    }

    public static void main(String[] args) {
        System.out.println(buildMixedList());
    }
}
