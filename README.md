# java-learning

从零开始学 Java 的公开笔记。`docs/` 下每一课都按「**先让代码疼一次，再给解法**」的顺序写，
代码块后面紧跟真实的执行结果——读起来应该像看视频，而不是读 API 文档。

## 现在学到哪

见 [docs/README.md](docs/README.md) 的路线图（共 19 课，从「程序是怎么跑起来的」到「并发集合」）。

## 怎么跑代码

```bash
# 单文件直接跑，不需要构建（JDK 11+）
java src/main/java/dev/javalearning/intro/Hello.java
java src/main/java/dev/javalearning/basics/variables/VariablesDemo.java

# 先编译再跑
javac -encoding UTF-8 -d out src/main/java/dev/javalearning/intro/Hello.java
java -cp out dev.javalearning.intro.Hello

# 跑某一课的断言
./gradlew test --tests '*variables*'
```

## 约定

- 代码**按主题分包**（`basics/variables`、`optional`、`concurrency/pool`…），不按课时；
  学习顺序看 [`docs/README.md`](docs/README.md)
- 测试包与被测类**严格同包**，不另起炉灶
- 一课一个主题包，要验证的东西写成 `src/test/` 下的断言
- 方法名用英文，`@DisplayName` 用中文
- `docs/` 讲完一课才建一个文件，不留空占位
- 故意写错的反面教材放该主题包的 `broken/` 子包里，已被构建排除，标红是正常的
- 代码块后面的「执行结果」都是真跑出来的，不是编的
