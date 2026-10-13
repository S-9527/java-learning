#!/usr/bin/env python3
"""
校验 docs/ 下所有 markdown 的相对链接。

只检查「文件是否存在」，不解析锚点——我们不锚到具体小节，
所以任何渲染器（GitHub、VS Code、typora、纯文本）看到的行为都一致。

校验这些形式的链接：
  ./NN-xxx.md          → docs/ 之间的互相引用
  ../src/...           → 指向仓库里的代码文件
  ../build.gradle.kts  → 指向仓库根的文件

指向未来课程的链接只警告、不算失败：课一写出来，链接自动生效，零维护。
判断依据是 docs/README.md 路线图里规划了哪些课。

用法：./scripts/check-links.py
退出码：0 = 没有坏链；1 = 有坏链
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
ROADMAP = DOCS / "README.md"

# 匹配 [text](target) 里的 target，忽略 http(s)、mailto、纯锚点 #
LINK = re.compile(r"\]\((?!https?:|mailto:|#)([^)\s]+)\)")


def planned_lessons() -> set:
    """路线图里规划了哪些课——这些课的链接即使还没写也允许存在。

    路线图里出现的形式有三种，都要认：
      | 05 | 继承与多态 | ...      ← 章节表格
      │   └── inheritance/  第 05 课：...   ← 目录树
      ｜ [05](05-xxx.md) ✅ | ...  ← 已完成的章节
    """
    if not ROADMAP.exists():
        return set()
    text = ROADMAP.read_text(encoding="utf-8")
    planned = set(re.findall(r"\|\s*(\d\d)\s*\|", text))
    planned |= set(re.findall(r"第\s*(\d\d)\s*课", text))
    planned |= set(re.findall(r"\((\d\d)-[\w]", text))
    return planned


def main() -> int:
    planned = planned_lessons()
    bad, future, checked = [], [], 0

    # 除了 docs/ 下的课，根目录的 README.md 也要查——它是仓库入口，
    # 里面全是指向 docs/ 和 src/ 的链接，坏了影响最大。
    files_to_check = sorted(DOCS.glob("*.md"))
    root_readme = ROOT / "README.md"
    if root_readme.exists():
        files_to_check.append(root_readme)

    for md in files_to_check:
        text = md.read_text(encoding="utf-8")
        # 用相对仓库根的路径显示，方便定位
        label = md.name if md.parent == DOCS else md.relative_to(ROOT).as_posix()
        for i, line in enumerate(text.splitlines(), 1):
            for m in LINK.finditer(line):
                target = m.group(1).split("#", 1)[0]
                if not target:
                    continue
                resolved = (md.parent / target).resolve()
                checked += 1
                if resolved.exists():
                    continue
                # 目标不存在：看它是不是路线图里规划好的未来课
                num = re.match(r"\.*/?(\d\d)-", target)
                if num and num.group(1) in planned:
                    future.append(f"{label}:{i}  ->  {target}")
                else:
                    bad.append(f"{label}:{i}  ->  {target}")

    for f in future:
        print("未来课程的链接（写出来自动生效）:", f)
    for b in bad:
        print("坏链:", b)

    # 顺带查一件容易漏的事：路线图的 ✅ 和 docs/ 里实际有哪些文件是否对得上。
    # 新课写完了忘了打勾，或者打了勾却没写文档，这里都能抓到。
    actual = sorted(
        p.name[:2] for p in DOCS.glob("*.md") if re.match(r"\d\d-", p.name)
    )
    roadmap_text = ROADMAP.read_text(encoding="utf-8") if ROADMAP.exists() else ""
    checked_marks = sorted(set(re.findall(r"^\| \[(\d\d)\]", roadmap_text, re.M)))
    unchecked = [n for n in actual if n not in checked_marks]
    dangling = [n for n in checked_marks if n not in actual]
    for n in unchecked:
        print(f"路线图漏打勾: {n} 有文档但 README 里没标 ✅")
    for n in dangling:
        print(f"路线图虚标: {n} 标了 ✅ 但 docs/ 里没有对应文档")

    # 再查一件：文档头部声称的断言条数，和测试文件里真实的 @Test 数量是否对得上。
    # 这类不一致很隐蔽——改了测试忘了回去改文档头部，读者会以为漏了几条。
    count_mismatch = []
    for md in sorted(DOCS.glob("[0-9][0-9]-*.md")):
        text = md.read_text(encoding="utf-8")
        m = re.search(r"断言：\[`(\w+Test)\.java`\][^）]*（(\d+) 条）", text)
        if not m:
            continue
        test_cls, claimed = m.group(1), int(m.group(2))
        # 在 src/test 下找这个测试类
        matches = list((ROOT / "src/test").rglob(f"{test_cls}.java"))
        if not matches:
            count_mismatch.append(f"{md.name}: 文档声称 {claimed} 条，但找不到 {test_cls}.java")
            continue
        real = len(re.findall(r"^\s*@Test\b", matches[0].read_text(encoding="utf-8"), re.M))
        if real != claimed:
            count_mismatch.append(f"{md.name}: 文档写 {claimed} 条，实际 {real} 条")
    for c in count_mismatch:
        print("断言条数不符:", c)

    print(
        f"\n检查了 {checked} 个链接：坏链 {len(bad)} 个，"
        f"指向未来课程 {len(future)} 个；"
        f"路线图漏打勾 {len(unchecked)} 个，虚标 {len(dangling)} 个；"
        f"断言条数不符 {len(count_mismatch)} 个"
    )
    return 1 if bad or unchecked or dangling or count_mismatch else 0


if __name__ == "__main__":
    sys.exit(main())
