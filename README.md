# java-learning

从零开始学 Java 的公开笔记。`docs/` 下每一课都按「**先让代码疼一次，再给解法**」的顺序写，
代码块后面紧跟真实的执行结果——读起来应该像看视频，而不是读 API 文档。

## 现在学到哪

见 [docs/README.md](docs/README.md) 的路线图（共 19 课，从「程序是怎么跑起来的」到「并发集合」）。

## 怎么跑代码

```bash
# 单文件直接跑，不需要构建（JDK 11+）
java src/main/java/dev/javalearning/lesson00/Hello.java

# 先编译再跑
javac -encoding UTF-8 -d out src/main/java/dev/javalearning/lesson00/Hello.java
java -cp out dev.javalearning.lesson00.Hello

# 跑某一课的全部断言
./gradlew test --tests '*lesson00*'
```

## 约定

- 一课一个包：`src/main/java/dev/javalearning/lessonNN/`，要验证的东西写成 `src/test/` 下的断言
- 方法名用英文，`@DisplayName` 用中文
- `docs/` 讲完一课才建一个文件，不留空占位
- 代码块后面的「执行结果」都是真跑出来的，不是编的
