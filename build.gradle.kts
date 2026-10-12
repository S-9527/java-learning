plugins {
    `java-library`
}

group = "dev.javalearning"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

sourceSets {
    named("main") {
        java {
            // 第 00 课的 Mismatch.java 是刻意写错的反面教材（public 类名和文件名不一致），
            // 参与构建会让整个编译失败。它只被 javac 单独调用来演示编译错误，
            // 所以这里排除掉。看见它报红是正常的——那一课讲的就是这个错误。
            exclude("**/Mismatch.java")
        }
    }
}

dependencies {
    // JUnit 6 + AssertJ。本机 Gradle 缓存里已有这些版本，可以离线构建。
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

// 中文注释、中文文档、中文控制台输出，一律 UTF-8，别依赖系统默认编码。
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()

    // 每一条断言都打印出来。学习仓库里「看见它真的跑过」比构建速度重要。
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
        setExceptionFormat("full")
    }
    jvmArgs("-Dfile.encoding=UTF-8", "-Duser.language=zh", "-Duser.country=CN")
}
