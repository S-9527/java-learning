package dev.javaquest.meta;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * 「活文档」的核对逻辑。
 *
 * <p>这个仓库的文档分成两层：
 * <ul>
 *   <li><b>测试</b>（src/test）——唯一的真相。能编译、能跑、能失败，所以不会过期。</li>
 *   <li><b>markdown</b>（docs）——散文。解释概念、给类比、讲反例，但没有可执行的部分。</li>
 * </ul>
 *
 * <p>两层之间靠三条约定咬合，任何一条断了这里就会报出来：
 *
 * <ol>
 *   <li>每篇 md 顶部用注释声明它对应哪些测试类：
 *       {@code <!-- covers: VariableTest, VariableExercisesTest -->}</li>
 *   <li>md 正文里用 {@code `Class#method`} 这种固定格式点名每一条断言。</li>
 *   <li>md 代码块第一行可以标 {@code // src: src/main/java/.../Foo.java}，
 *       指向真实存在的示例文件。</li>
 * </ol>
 *
 * <p>本类只负责<b>读</b>：扫描编译出来的测试字节码、解析 markdown、算出差集。
 * 断言在 {@link DocumentationSyncTest} 里。
 */
public final class DocIndex {

    private DocIndex() {
    }

    /** 一条被发现的断言。{@code simple} 对嵌套类写成 {@code Outer.Inner}。 */
    public record TestCase(
            String fqcn, String simple, String method, String displayName, int order, boolean disabled) {

        /** markdown 里引用它的固定格式。 */
        public String ref() {
            return simple + "#" + method;
        }

        public String packageOf() {
            int i = fqcn.lastIndexOf('.');
            return i < 0 ? "" : fqcn.substring(0, i);
        }
    }

    /** 一篇章节 md 的解析结果。 */
    public record Doc(Path path, List<String> covers, Set<String> refs, List<String> srcs) {
        public String name() {
            return path.getFileName().toString();
        }
    }

    // ── 三条例子的正则 ────────────────────────────────────────────────

    /** {@code <!-- covers: A, B -->}，允许跨行但值不能含 '>'。 */
    private static final Pattern COVERS =
            Pattern.compile("<!--[ \\t\\r\\n]*covers:[ \\t]*([^>]*?)[ \\t]*-->");

    /** {@code `Outer.Inner#someMethod`} —— md 里点名断言的格式。 */
    private static final Pattern REF =
            Pattern.compile("`([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*#[A-Za-z_$][A-Za-z0-9_$]*)`");

    /** 代码块里的 {@code // src: src/main/java/.../Foo.java}。 */
    private static final Pattern SRC =
            Pattern.compile("^//\\s*src:[ \\t]*(\\S+\\.java)", Pattern.MULTILINE);

    /** INDEX.md 里的一行：{@code 1. [变量与类型](01-变量与类型.md)}。 */
    private static final Pattern INDEX_ROW =
            Pattern.compile("^\\s*\\d+\\.\\s+\\[[^\\]]*\\]\\((\\d+)-([^)]+)\\.md\\)", Pattern.MULTILINE);

    // ── 默认路径（gradle 的工作目录就是项目根）──────────────────────────

    public static Path testClassesDir() {
        return Path.of("build", "classes", "java", "test");
    }

    public static Path docsDir() {
        return Path.of("docs");
    }

    // ── 扫描测试 ─────────────────────────────────────────────────────

    /**
     * 扫 build/classes/java/test 下的字节码，把所有断言列出来。
     *
     * <p>用反射读注解而不是解析源码：字节码就是编译器真正认可的东西，
     * 源码格式随便改都不会让这份清单失效。
     */
    public static List<TestCase> discover(Path classesDir) throws IOException, ClassNotFoundException {
        List<TestCase> out = new ArrayList<>();
        if (!Files.isDirectory(classesDir)) {
            return out;
        }

        List<Path> classFiles = new ArrayList<>();
        try (Stream<Path> s = Files.walk(classesDir)) {
            s.filter(p -> p.toString().endsWith(".class")).sorted().forEach(classFiles::add);
        }

        ClassLoader cl = DocIndex.class.getClassLoader();
        for (Path file : classFiles) {
            String name = classesDir.relativize(file).toString()
                    .replace(File.separatorChar, '.')
                    .replace('/', '.')
                    .replace("\\", ".");
            if (name.startsWith("module-info") || !name.endsWith(".class")) {
                continue;
            }
            String fqcn = name.substring(0, name.length() - ".class".length());
            if (fqcn.contains("$")) {
                continue;   // 嵌套类由 walk() 递归发现，这里只看顶层类
            }
            if (fqcn.startsWith("dev.javaquest.meta")) {
                continue;   // meta 包是这套校验机制本身，不参与章节文档的覆盖统计
            }
            Class<?> root;
            try {
                root = Class.forName(fqcn, false, cl);
            } catch (Throwable t) {
                continue;
            }
            walk(root, simple(fqcn), out);
        }

        // 排序：先按包，再按类名（让 @Nested 分组各自连成一块），最后按 @Order 和方法名。
        // 有了 @Order 就在类内按作者指定的顺序读；没有就退回方法名，保证结果稳定可复现。
        out.sort(Comparator
                .comparing(TestCase::packageOf)
                .thenComparing(TestCase::simple)
                .thenComparingInt(TestCase::order)
                .thenComparing(TestCase::method));
        return out;
    }

    private static void walk(Class<?> type, String prefix, List<TestCase> out) {
        for (Method m : type.getDeclaredMethods()) {
            if (!isTestMethod(m)) {
                continue;
            }
            DisplayName dn = m.getAnnotation(DisplayName.class);
            String display = dn != null ? dn.value() : m.getName();
            Order order = m.getAnnotation(Order.class);
            int ord = order != null ? order.value() : Order.DEFAULT;
            out.add(new TestCase(type.getName(), prefix, m.getName(), display, ord,
                    m.isAnnotationPresent(Disabled.class)));
        }
        for (Class<?> inner : type.getDeclaredClasses()) {
            if (inner.isAnnotationPresent(Nested.class)) {
                walk(inner, prefix + "." + simple(inner.getName()), out);
            }
        }
    }

    private static boolean isTestMethod(Method m) {
        return m.isAnnotationPresent(Test.class)
                || m.isAnnotationPresent(TestTemplate.class)
                || m.isAnnotationPresent(ParameterizedTest.class)
                || m.isAnnotationPresent(RepeatedTest.class);
    }

    private static String simple(String fqcn) {
        String s = fqcn.replace('$', '.');
        int i = s.lastIndexOf('.');
        return i < 0 ? s : s.substring(i + 1);
    }

    // ── 解析 markdown ────────────────────────────────────────────────

    /** 读 docs/ 下的章节 md，自动跳过 INDEX.md 和 GENERATED/。 */
    public static List<Doc> readDocs(Path docsDir) throws IOException {
        List<Doc> docs = new ArrayList<>();
        if (!Files.isDirectory(docsDir)) {
            return docs;
        }
        try (Stream<Path> s = Files.walk(docsDir)) {
            List<Path> files = s
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> {
                        String rel = docsDir.relativize(p).toString();
                        return !rel.startsWith("GENERATED") && !p.getFileName().toString().equals("INDEX.md");
                    })
                    .sorted()
                    .toList();
            for (Path p : files) {
                String text = Files.readString(p, StandardCharsets.UTF_8);
                docs.add(new Doc(p, splitCovers(text), findRefs(text), findSrcs(text)));
            }
        }
        return docs;
    }

    private static List<String> splitCovers(String text) {
        Matcher m = COVERS.matcher(text);
        if (!m.find()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String part : m.group(1).split("[,\\s]+")) {
            if (!part.isBlank()) {
                names.add(part);
            }
        }
        return names;
    }

    private static Set<String> findRefs(String text) {
        Set<String> refs = new LinkedHashSet<>();
        Matcher m = REF.matcher(text);
        while (m.find()) {
            refs.add(m.group(1));
        }
        return refs;
    }

    private static List<String> findSrcs(String text) {
        List<String> srcs = new ArrayList<>();
        Matcher m = SRC.matcher(text);
        while (m.find()) {
            srcs.add(m.group(1));
        }
        return srcs;
    }

    /** INDEX.md 里列出的章节，顺序敏感。 */
    public static List<String[]> readIndex(Path indexFile) throws IOException {
        List<String[]> rows = new ArrayList<>();
        if (!Files.isRegularFile(indexFile)) {
            return rows;
        }
        Matcher m = INDEX_ROW.matcher(Files.readString(indexFile, StandardCharsets.UTF_8));
        while (m.find()) {
            rows.add(new String[]{m.group(1), m.group(2)});
        }
        return rows;
    }

    // ── 渲染生成物 ───────────────────────────────────────────────────

    /** 生成 docs/GENERATED/TEST_INDEX.md 的内容。按包分组，一眼看出每章有多少条断言。 */
    public static String render(List<TestCase> tests) {
        StringBuilder sb = new StringBuilder();
        long enabled = tests.stream().filter(t -> !t.disabled()).count();
        sb.append("<!--\n");
        sb.append("本文件由 `./gradlew generateDocIndex` 自动生成，请勿手改。\n");
        sb.append("改了测试方法名或 @DisplayName 之后跑一遍该 task，然后提交这个文件。\n");
        sb.append("-->\n\n");
        sb.append("# 测试清单\n\n");
        sb.append(String.format("共 %d 条断言（%d 条启用、%d 条是练习答案），分布在 %d 个包。%n%n",
                tests.size(), enabled, tests.size() - enabled,
                tests.stream().map(TestCase::packageOf).distinct().count()));

        Map<String, List<TestCase>> byPackage = new LinkedHashMap<>();
        for (TestCase t : tests) {
            byPackage.computeIfAbsent(t.packageOf(), k -> new ArrayList<>()).add(t);
        }
        int n = 0;
        for (Map.Entry<String, List<TestCase>> e : byPackage.entrySet()) {
            sb.append("## ").append(e.getKey()).append("\n\n");
            sb.append("| # | 测试类 | 方法 | 断言 | 状态 |\n");
            sb.append("|---|---|---|---|---|\n");
            for (TestCase t : e.getValue()) {
                n++;
                sb.append("| ").append(n)
                        .append(" | `").append(t.simple()).append("`")
                        .append(" | `").append(t.method()).append("`")
                        .append(" | ").append(t.displayName()).append(" |")
                        .append(t.disabled() ? " 练习答案" : "").append(" |\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ── 核对 ─────────────────────────────────────────────────────────

    /**
     * 跑一遍全部核对，返回人类可读的问题清单。空列表 = 文档和测试完全同步。
     */
    public static List<String> check(List<TestCase> tests, List<Doc> docs, Path docsDir, Path root) {
        List<String> problems = new ArrayList<>();

        Map<String, List<TestCase>> bySimple = new LinkedHashMap<>();
        for (TestCase t : tests) {
            bySimple.computeIfAbsent(t.simple(), k -> new ArrayList<>()).add(t);
        }

        // covers 里的一个名字可能覆盖整个测试类，也可能只覆盖它某个 @Nested 分组。
        // 规则：精确匹配；写「Outer」则自动覆盖所有 Outer.Xxx 嵌套分组。
        Set<String> allClasses = bySimple.keySet();

        // 1. 每篇 md 都必须声明覆盖的测试类，而且声明的类要真实存在
        for (Doc d : docs) {
            if (d.covers().isEmpty()) {
                problems.add(d.name() + "：缺少 <!-- covers: ... --> 声明，看不出它对应哪些测试");
            }
            for (String name : d.covers()) {
                boolean anyMatch = allClasses.stream().anyMatch(cls -> covers(name, cls));
                if (!anyMatch) {
                    problems.add(d.name() + "：covers 了不存在的测试类 `" + name + "`");
                }
            }
        }

        // 2. 每个测试类都必须被某篇 md 认领
        Set<String> claimed = new LinkedHashSet<>();
        for (Doc d : docs) {
            for (String name : d.covers()) {
                allClasses.stream().filter(cls -> covers(name, cls)).forEach(claimed::add);
            }
        }
        for (String cls : allClasses) {
            if (!claimed.contains(cls)) {
                problems.add("测试类 `" + cls + "` 没有任何文档认领（在 md 顶部加一行 covers）");
            }
        }

        // 3. 被认领的每条断言，必须在那篇 md 里被点名
        for (Doc d : docs) {
            for (String name : d.covers()) {
                for (String cls : allClasses) {
                    if (!covers(name, cls)) {
                        continue;
                    }
                    for (TestCase t : bySimple.get(cls)) {
                        if (!d.refs().contains(t.ref())) {
                            problems.add(d.name() + "：" + t.ref() + " 没有在正文里被点名（断言：" + t.displayName() + "）");
                        }
                    }
                }
            }
        }

        // 4. md 里点的每个名号必须真实存在，防止改名后文档变成空指针
        for (Doc d : docs) {
            for (String ref : d.refs()) {
                int at = ref.indexOf('#');
                String cls = ref.substring(0, at);
                String method = ref.substring(at + 1);
                List<TestCase> cls_ = bySimple.getOrDefault(cls, List.of());
                boolean hit = cls_.stream().anyMatch(t -> t.method().equals(method));
                if (!hit) {
                    problems.add(d.name() + "：`" + ref + "` 不是一个真实存在的断言（改过方法名？记得回来更新文档）");
                }
            }
        }

        // 5. 代码块标注的示例文件必须真实存在
        for (Doc d : docs) {
            for (String src : d.srcs()) {
                if (!Files.isRegularFile(root.resolve(src))) {
                    problems.add(d.name() + "：代码块标注的示例文件不存在：" + src);
                }
            }
        }

        // 6. INDEX.md 与磁盘上的章节文件互为镜像，编号连续
        try {
            List<String[]> rows = readIndex(docsDir.resolve("INDEX.md"));
            List<String> onDisk = new ArrayList<>();
            for (Doc d : docs) {
                String n = d.name();
                int dash = n.indexOf('-');
                if (dash > 0 && isDigits(n.substring(0, dash))) {
                    onDisk.add(n);
                }
            }
            List<String> listed = rows.stream().map(r -> r[0] + "-" + r[1] + ".md").toList();

            for (String s : listed) {
                if (!onDisk.contains(s)) {
                    problems.add("INDEX.md 列了不存在的章节文件：" + s);
                }
            }
            for (String s : onDisk) {
                if (!listed.contains(s)) {
                    problems.add("INDEX.md 漏了磁盘上已有的章节文件：" + s);
                }
            }
            for (int i = 0; i < rows.size(); i++) {
                String expected = String.format("%02d", i + 1);
                if (!rows.get(i)[0].equals(expected)) {
                    problems.add("INDEX.md 第 " + (i + 1) + " 行的编号应是 " + expected + "，实际是 " + rows.get(i)[0]);
                }
            }
        } catch (IOException e) {
            problems.add("INDEX.md 读不出来：" + e.getMessage());
        }

        return problems;
    }

    /**
     * covers 声明的名字是否覆盖某个具体测试类。
     *
     * <p>「Outer」覆盖 Outer 和它的所有 @Nested 分组；
     * 「Outer.Inner」只覆盖那一个分组。这样 md 里写一个类名就够了，
     * 不必把每个 @Nested 分组都列出来。
     */
    private static boolean covers(String declared, String cls) {
        return cls.equals(declared) || cls.startsWith(declared + ".");
    }

    private static boolean isDigits(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (char c : s.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
