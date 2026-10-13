#!/usr/bin/env python3
"""把每一课文档里的主 demo 输出块，和真实运行结果逐字比对。

用法：python3 scripts/verify-doc-output.py

这是这个仓库最硬的一条规矩的执行者：文档里贴的执行结果必须是真的。
每写一课就跑一遍，防止「改了 demo 忘了回去改文档」。

注意：有几课的输出天生不稳定（并行流、竞态演示），它们的主 demo
在文档里已经说明了这一点。如果某一课报不一致，先看它是不是这类。"""
import re, subprocess, pathlib, sys

ROOT = pathlib.Path("/home/anguish/workspace/java-learning")
DOCS = ROOT / "docs"

# 文档 -> 要跑的 java 文件
TARGETS = {
    "00-程序是怎么跑起来的.md": None,   # 没有 main demo
    "01-变量与类型.md": "basics/variables/VariablesDemo.java",
    "02-运算符与流程控制.md": "basics/controlflow/ControlFlowDemo.java",
    "03-方法.md": "basics/methods/MethodsDemo.java",
    "04-类与对象.md": "basics/classes/ClassesDemo.java",
    "05-继承与多态.md": "basics/inheritance/InheritanceDemo.java",
    "06-泛型.md": "generics/GenericsDemo.java",
    "07-Lambda表达式.md": "lambda/LambdaDemo.java",
    "08-Optional.md": "optional/OptionalDemo.java",
    "09-反射.md": "reflection/ReflectionDemo.java",
    "14-JMM与volatile.md": "concurrency/jmm/JmmDemo.java",
    "15-锁.md": "concurrency/locks/LocksDemo.java",
    "16-线程通信.md": "concurrency/coordination/CoordinationDemo.java",
    "17-线程池.md": "concurrency/pool/PoolDemo.java",
    "18-并发集合与ThreadLocal.md": "concurrency/collections/CollectionsDemo.java",
}

ok, fail, skip = 0, 0, 0
for doc_name, rel in sorted(TARGETS.items()):
    doc = DOCS / doc_name
    if not doc.exists():
        print(f"跳过（文档不存在）: {doc_name}"); skip += 1; continue
    if rel is None:
        print(f"跳过（无 main demo）: {doc_name}"); skip += 1; continue
    src = ROOT / "src/main/java/dev/javalearning" / rel
    if not src.exists():
        print(f"!! 源文件不存在: {rel}  (文档 {doc_name})"); fail += 1; continue

    real = subprocess.run(["java", str(src.relative_to(ROOT))],
                          capture_output=True, text=True, cwd=ROOT, timeout=240).stdout.strip()
    text = doc.read_text(encoding="utf-8")
    cmd = f"$ java src/main/java/dev/javalearning/{rel}"
    m = re.search(r"```terminal\n" + re.escape(cmd) + r"\n(.*?)```", text, re.S)
    if not m:
        print(f"?? 文档里找不到该命令的输出块: {doc_name}"); skip += 1; continue
    if m.group(1).strip() == real:
        print(f"OK   {doc_name}"); ok += 1
    else:
        print(f"!!   {doc_name} 输出不一致"); fail += 1
        import difflib
        diff = list(difflib.unified_diff(real.splitlines(), m.group(1).strip().splitlines(),
                                         "真实", "文档", lineterm=""))
        for line in diff[:12]:
            print("      " + line)

print(f"\n一致 {ok} 个，不一致 {fail} 个，跳过 {skip} 个")
sys.exit(1 if fail else 0)
