package dev.javaquest.meta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * {@code ./gradlew generateDocIndex} 的入口。
 *
 * <p>它做的事只有一件：把测试里写好的断言清单导出成 markdown，供人快速浏览，
 * 也供 {@link DocumentationSyncTest} 判断「文档是不是过期了」。
 *
 * <p>注意这个 main 放在 test 源码集里，而不是 main 源码集：
 * 它要靠 test 的运行时 classpath 才能加载到测试类，
 * Gradle 里对应的是 {@code classpath = sourceSets.test.runtimeClasspath}。
 */
public final class GenerateDocIndex {

    private GenerateDocIndex() {
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Path docs = DocIndex.docsDir();
        Path out = docs.resolve("GENERATED").resolve("TEST_INDEX.md");

        List<DocIndex.TestCase> tests = DocIndex.discover(DocIndex.testClassesDir());
        String text = DocIndex.render(tests);

        Files.createDirectories(out.getParent());
        String before = Files.isRegularFile(out) ? Files.readString(out, StandardCharsets.UTF_8) : null;
        Files.writeString(out, text, StandardCharsets.UTF_8);

        if (before == null) {
            System.out.println("generated " + out + "（" + tests.size() + " 条断言）");
        } else if (before.equals(text)) {
            System.out.println("unchanged " + out);
        } else {
            System.out.println("updated " + out + "（" + tests.size() + " 条断言）——别忘了提交这个文件");
        }
    }
}
