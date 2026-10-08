# 学习路线

用「能跑的测试」当文档的 Java 学习仓库。每一条结论都写在测试里，跑一次就知道还对不对。

## 怎么用

```bash
./gradlew test                                  # 跑全部断言，顺便校验文档没脱节
./gradlew test --tests '*VariableTest'          # 只跑第一章的测试
./gradlew generateDocIndex                      # 测试清单有变动后，重新生成索引并提交
```

改完 `src/test` 里的 `@DisplayName` 之后记得跑一次 `generateDocIndex`，否则
`DocumentationSyncTest` 会红着提醒你——这是故意的。

## 章节

1. [变量与类型](01-变量与类型.md) —— 一条主线：变量装的是值还是地址。整数算术 / String 不可变、`==` 与 `equals`、`final`、自动装箱
2. [流程控制](02-流程控制.md) —— 选择 / 重复 / 跳转三块主线：短路求值、switch 穷尽匹配、三种循环、break/continue/标签
3. [方法](03-方法.md) —— 方法边界的三面：输入（参数是值的副本、可变参数、重载）、输出（return / void）、内部（局部作用域）
4. [类与对象](04-类与对象.md) —— 类与实例的关系、字段与方法、构造器与 `this`、`static` 成员的归属、可见性与封装

## 目录结构

```
docs/                          讲解（散文，可以随便改）
  01-变量与类型.md
  02-流程控制.md
  03-方法.md
  GENERATED/TEST_INDEX.md       测试清单（自动生成，勿手改）

src/main/java/...               示例代码，被讲解和被测试的对象
src/test/java/...               测试，也就是文档本身
  meta/                         文档与测试的校验机制（不参与章节覆盖统计）
```

## 文档怎么保证不过期

`DocumentationSyncTest` 每次跑 `test` 都会检查这六件事：

| 检查 | 挡住了什么 |
|---|---|
| 每篇 md 用 `<!-- covers: ... -->` 声明对应的测试类 | 文档和测试互不认识 |
| 被声明的测试类必须真实存在 | md 里贴了不存在的类名 |
| 每个测试类都要有文档认领 | 写了测试但忘了讲 |
| 每条断言都要在正文被点名 | 漏了一条测试没写进文档 |
| md 里点的每个名号必须存在 | 改方法名后文档变空指针 |
| `docs/GENERATED/TEST_INDEX.md` 必须是最新 | 索引过期 |

在这个仓库里，「文档过期」不是靠自觉，是靠一条会红的测试。

## 练习题

每章末尾有 2-4 个练习题，答案写成 `@Disabled` 的测试放在
`*ExercisesTest.java` 里。把方法上的 `@Disabled` 删掉就能跑。

## 后面还有

继承与多态、record 与 enum、异常、集合、字符串、Lambda 与 Stream、并发，都还没写。
想加哪一章，直接开 issue 或者照着现有四章的结构写——校验会告诉你有没有漏。
