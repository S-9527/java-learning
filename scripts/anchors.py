#!/usr/bin/env python3
"""
生成 GitHub 风格的 Markdown 锚点，并把 docs/ 里所有「第 NN 课」的暗号引用
换成带锚点的链接。

锚点规则（GitHub / GFM）：
  1. 转小写
  2. 删掉所有既不是 [a-z0-9] 也不是空白的字符（中文标点、·、&、括号……全删）
  3. 剩余空格转成 -
  4. 同名标题重复时，后面几个依次追加 -1、-2

用法：
  ./scripts/anchors.py              # 打印每个标题的锚点
  ./scripts/anchors.py --fix        # 改写 docs/*.md 里的跨课引用
"""
import re
import sys
from pathlib import Path

DOCS = Path(__file__).resolve().parent.parent / "docs"


def slugify(text: str) -> str:
    s = text.strip().lower()
    s = re.sub(r"[^\w\s-]", "", s, flags=re.UNICODE)      # 去掉标点，保留词字符和空白
    s = re.sub(r"\s+", "-", s.strip())
    return s


def collect_anchors() -> dict:
    """返回 {文件名: [(标题, 锚点), ...]}，重复标题按 GitHub 规则追加 -1 -2。"""
    result = {}
    for md in sorted(DOCS.glob("*.md")):
        entries, seen = [], {}
        for line in md.read_text(encoding="utf-8").splitlines():
            m = re.match(r"^#{1,3}\s+(.*)$", line)
            if not m:
                continue
            title, slug = m.group(1).strip(), slugify(m.group(1))
            if slug in seen:
                seen[slug] += 1
                slug = f"{slug}-{seen[slug]}"
            else:
                seen[slug] = 0
            entries.append((title, slug))
        result[md.name] = entries
    return result


def main() -> int:
    anchors = collect_anchors()

    if "--fix" not in sys.argv:
        for name, entries in anchors.items():
            print(f"--- {name}")
            for title, slug in entries:
                print(f"    #{slug}    ← {title}")
        return 0

    # ── 改写引用 ──
    # 匹配「第 NN 课《标题》」「第 NN 课「标题」」「第 NN 课"标题"」和裸「第 NN 课」
    patterns = [
        (re.compile(r"第 (\d\d) 课[《「\"]([^》」\"]+)[》」\"]"), "quoted"),
        (re.compile(r"第 (\d\d) 课"), "bare"),
    ]
    total = 0
    for md in sorted(DOCS.glob("*.md")):
        text = original = md.read_text(encoding="utf-8")
        for rx, kind in patterns:
            def repl(m):
                nonlocal total
                num = m.group(1)
                label = m.group(2).strip() if kind == "quoted" else None
                target_file = next(
                    (f for f in anchors if f.startswith(f"{num}-")), None
                )
                if target_file is None:
                    print(f"  跳过（目标课程不存在）: {md.name}: {m.group(0)}")
                    return m.group(0)
                # 有标题就跳到那一节，否则跳到文件顶部
                slug = ""
                if label:
                    for title, s in anchors[target_file]:
                        if title.startswith(label) or label in title:
                            slug = f"#{s}"
                            break
                total += 1
                return f"[{label or target_file[:-3]}](../{target_file}{slug})"
            text = rx.sub(repl, text)
        if text != original:
            md.write_text(text, encoding="utf-8")
            print(f"  改写 {md.name}")
    print(f"\n共处理 {total} 处引用")
    return 0


if __name__ == "__main__":
    sys.exit(main())
