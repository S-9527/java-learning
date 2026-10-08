# java-quest

用**能跑的测试**当文档的 Java 学习仓库。

不是「写完代码补文档」，而是**文档就是那些断言**——所以它永远不会过期，
因为过期的文档会让测试变红。

```
docs/01-变量与类型.md          ← 讲解（散文，可以随便改）
        ↑ DocIndex 校验
src/test/.../VariableTest.java  ← 唯一真相：可运行、可验证、40 条断言
        ↓ 被测
src/main/.../VariableDemo.java  ← 示例代码，被讲解和被测试的对象
```

## 上手

```bash
./gradlew test                                  # 跑全部断言 + 校验文档没脱节
./gradlew test --tests '*VariableTest'          # 只跑第一章
./gradlew generateDocIndex                      # 测试有变动后重新生成索引，然后提交
```

在 IDE 里打开任一 `*Test.java`，左边的方法列表就是那一章的目录：
方法名是英文（方便跳转和搜索），中文句子放在 `@DisplayName` 里。

## 已写章节

| 章节 | 内容 | 断言数 |
|---|---|---|
| [01 变量与类型](docs/01-变量与类型.md) | 一条主线：变量装的是值还是地址。整数算术、溢出、类型提升、浮点精度 / String 不可变、`==` 与 `equals`、`final`、自动装箱 | 22 |
| [02 流程控制](docs/02-流程控制.md) | 选择/重复/跳转三块主线：短路求值、switch 穷尽匹配、三种循环、break/continue/标签、迭代中删元素 | 22 |
| [03 方法](docs/03-方法.md) | 方法边界的三面：输入（参数是值的副本、可变参数、重载）、输出（return / void）、内部（局部作用域） | 14 |

[测试清单（自动生成）](docs/GENERATED/TEST_INDEX.md) ·
[学习路线](docs/INDEX.md)

## 「活文档」是怎么做到的

每篇 md 顶部声明它对应哪些测试，正文用固定格式点名每一条断言：

```markdown
<!-- covers: VariableTest, VariableExercisesTest -->

`VariableTest#integerDivisionDropsTheRemainder`
```

`DocumentationSyncTest` 每次跑 `test` 都检查六件事，任一不满足就红：

| 检查 | 挡住了什么 |
|---|---|
| 每篇 md 必须有 `<!-- covers: ... -->` | 文档和测试互不认识 |
| 被声明的测试类必须真实存在 | md 里贴了不存在的类名 |
| 每个测试类都要有文档认领 | 写了测试但忘了讲 |
| 每条断言都要在正文被点名 | 漏了一条测试没写进文档 |
| md 里点的每个名号必须存在 | 改方法名后文档变成空指针 |
| `docs/GENERATED/TEST_INDEX.md` 必须是最新 | 索引过期 |

`build.gradle.kts` 里把 `docs/` 声明成了 `test` 任务的输入目录，
所以改了 markdown 之后 `test` 一定会重跑——否则看门人自己就失效了。

## 练习题

每章 3-4 道，答案写成 `@Disabled` 的测试放在 `*ExercisesTest.java`。
删掉方法上的 `@Disabled` 就能跑，答案写在注解的原因里。

## 环境

JDK 25 · Gradle 9.8 · JUnit 6.1.3 · AssertJ 3.27.7。依赖已在本机缓存，可离线构建。

## 后面还想写

集合、字符串、Lambda 与 Stream、并发、泛型、异常、record。
想加哪一章就开 issue，照现有三章的结构写就行——校验会告诉你有没有漏。
