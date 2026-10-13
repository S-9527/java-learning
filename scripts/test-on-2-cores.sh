#!/usr/bin/env bash
# 用「2 核」跑全部测试，模拟 GitHub Actions runner 的环境。
#
# 为什么需要这个：GitHub 的 ubuntu-latest runner 是 2 核。
# 而 ForkJoinPool.commonPool 的并行度 = 核数 - 1，所以 CI 上并行流
# 实际上是**串行**跑（并行度 1）——依赖"多线程竞争"的测试在那里
# 根本触发不了。
#
# 我踩过一次：本地 16 核必现的并行流测试，在 CI 上必不现，
# 结果「本地全绿、CI 红」。所以推送前先跑一遍这个脚本。
#
# 用法：./scripts/test-on-2-cores.sh
set -e

cd "$(dirname "$0")/.."

INIT_SCRIPT="$(mktemp /tmp/twocore-XXXXXX.gradle.kts)"
cat > "$INIT_SCRIPT" <<'KTS'
allprojects {
    tasks.withType<Test>().configureEach {
        jvmArgs("-XX:ActiveProcessorCount=2")
    }
}
KTS

echo "==> 用 2 核跑全部测试（模拟 CI 的 ubuntu-latest runner）"
./gradlew test --rerun-tasks --offline --console=plain -I "$INIT_SCRIPT" "$@"
STATUS=$?
rm -f "$INIT_SCRIPT"
exit $STATUS
