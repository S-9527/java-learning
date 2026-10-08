package dev.javaquest.meta;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 「活文档」的看门人。
 *
 * <p>这个测试自己不被任何文档覆盖——它住在 {@code dev.javaquest.meta} 包，
 * {@link DocIndex#discover} 会跳过这个包。这是有意为之：
 * 校验机制本身不需要被校验机制校验，否则会无限套娃。
 *
 * <p>跑法：{@code ./gradlew test}。想重新生成索引：{@code ./gradlew generateDocIndex}。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("活文档：文档和测试必须同步")
class DocumentationSyncTest {

    @Test
    @Order(1)
    @DisplayName("文档 ↔ 测试：每个测试都有文档，每篇文档都对应真实的测试")
    void docsAndTestsStayInSync() throws IOException, ClassNotFoundException {
        List<DocIndex.TestCase> tests = DocIndex.discover(DocIndex.testClassesDir());
        List<DocIndex.Doc> docs = DocIndex.readDocs(DocIndex.docsDir());
        List<String> problems = DocIndex.check(tests, docs, DocIndex.docsDir(), Path.of("."));

        assertThat(problems).as(() -> "文档和测试不同步，请逐条修掉：\n" + String.join("\n", problems)).isEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("生成的索引文件是最新的（改了 @DisplayName 之后要跑 generateDocIndex）")
    void generatedIndexIsUpToDate() throws IOException, ClassNotFoundException {
        Path out = DocIndex.docsDir().resolve("GENERATED").resolve("TEST_INDEX.md");

        List<DocIndex.TestCase> tests = DocIndex.discover(DocIndex.testClassesDir());
        String fresh = DocIndex.render(tests);
        String onDisk = Files.isRegularFile(out)
                ? Files.readString(out, StandardCharsets.UTF_8)
                : "<文件不存在>";

        assertThat(onDisk).as(() -> "索引文件过期了。请跑 `./gradlew generateDocIndex` 然后提交它。\n"
                + "期望内容前 400 字符：\n" + fresh.substring(0, Math.min(400, fresh.length()))).isEqualTo(fresh);
    }

    @Test
    @Order(3)
    @DisplayName("示例代码文件都真实存在（文档里引用的路径不会指空）")
    void referencedSourcesExist() throws IOException {
        List<DocIndex.Doc> docs = DocIndex.readDocs(DocIndex.docsDir());
        List<String> problems = new java.util.ArrayList<>();
        for (DocIndex.Doc d : docs) {
            for (String src : d.srcs()) {
                if (!Files.isRegularFile(Path.of(src))) {
                    problems.add(d.name() + " 指向了不存在的文件：" + src);
                }
            }
        }
        assertThat(problems).as(() -> "文档里的代码块标注了不存在的示例文件：\n" + String.join("\n", problems))
                .isEmpty();
    }
}
