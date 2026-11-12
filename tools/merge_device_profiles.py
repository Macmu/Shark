#!/usr/bin/env python3
"""
把上游 Shark 的机型支持合并进 Snake，界面代码一行不动。

只处理三层纯数据：
  1. enum class ExploitKind 的条目（vivo 6.6.127 这类新内核）
  2. PROFILE_BY_RELEASE 的 内核 release -> exploit 映射
  3. runFlow 里 ExploitKind -> 哪个 .so 的对应关系

用法：
  python3 tools/merge_device_profiles.py v2.2.1
  python3 tools/merge_device_profiles.py upstream/main
"""

import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
UPSTREAM_KT = "app/app/src/main/java/com/shark/kty/MainActivity.kt"
LOCAL_KT = ROOT / "app/app/src/main/java/com/android/snake/MainActivity.kt"


def parse_enum(text: str):
    m = re.search(
        r"enum class ExploitKind\(val fullLabel: String\) \{(.*?)\n    \}", text, re.S
    )
    if not m:
        return []
    return re.findall(r'^\s*(CVE\d+)\("([^"]+)"\)', m.group(1), re.M)


def parse_profile(text: str):
    m = re.search(
        r"PROFILE_BY_RELEASE: Map<String, ExploitKind> = mapOf\((.*?)\n        \)",
        text,
        re.S,
    )
    if not m:
        return []
    return re.findall(r'"([^"]+)" to ExploitKind\.(\w+)', m.group(1), re.M)


def parse_cmds(text: str):
    return dict(
        re.findall(
            r'ExploitKind\.(\w+) -> arrayOf\("\$nativeDir/(lib\w+\.so)", "run"\)', text
        )
    )


def main() -> int:
    ref = sys.argv[1] if len(sys.argv) > 1 else "upstream/main"
    try:
        src = subprocess.check_output(
            ["git", "-C", str(ROOT), "show", f"{ref}:{UPSTREAM_KT}"],
            text=True,
            stderr=subprocess.PIPE,
        )
    except subprocess.CalledProcessError as exc:
        print(f"取不到 {ref} 的 MainActivity.kt：{exc.stderr.decode()[:200]}")
        return 1

    dst = LOCAL_KT.read_text()

    up_enum, loc_enum = parse_enum(src), parse_enum(dst)
    up_map, loc_map = parse_profile(src), parse_profile(dst)
    up_cmd, loc_cmd = parse_cmds(src), parse_cmds(dst)

    loc_enum_names = {name for name, _ in loc_enum}
    loc_map_keys = {key for key, _ in loc_map}

    added_enum = [e for e in up_enum if e[0] not in loc_enum_names]
    added_map = [m for m in up_map if m[0] not in loc_map_keys]
    added_cmd = [
        (kind, so)
        for kind, so in up_cmd.items()
        if kind not in loc_cmd and kind in {e[0] for e in up_enum}
    ]

    if not (added_enum or added_map or added_cmd):
        print("机型支持已是最新，无需合并。")
        return 0

    # 1) enum 条目插到 UNSUPPORTED 之前
    if added_enum:
        block = "".join(f'        {name}("{label}"),\n' for name, label in added_enum)
        dst = dst.replace(
            '        UNSUPPORTED("当前机型不支持")',
            block + '        UNSUPPORTED("当前机型不支持")',
            1,
        )

    # 2) 映射条目插到 mapOf 的最后一条之后
    if added_map:
        block = "".join(f'            "{key}" to ExploitKind.{kind},\n' for key, kind in added_map)
        last = list(re.finditer(r'^            "[^"]+" to ExploitKind\.\w+,\n', dst, re.M))[-1]
        dst = dst[: last.end()] + block + dst[last.end():]

    # 3) .so 分支插到 UNSUPPORTED -> null 之前
    if added_cmd:
        block = "".join(
            f'                    ExploitKind.{kind} -> arrayOf("$nativeDir/{so}", "run")\n'
            for kind, so in added_cmd
        )
        dst = dst.replace(
            "                    ExploitKind.UNSUPPORTED -> null",
            block + "                    ExploitKind.UNSUPPORTED -> null",
            1,
        )

    LOCAL_KT.write_text(dst)

    print("已合并：")
    for name, label in added_enum:
        print(f"  + 机型类型 {name}  ({label})")
    for key, kind in added_map:
        print(f"  + 内核映射 {key} -> {kind}")
    for kind, so in added_cmd:
        print(f"  + 启动分支 {kind} -> {so}")
    print("\n还要人工确认两件事：")
    print("  1. jniLibs 里对应的 .so 是否已同步（sync-upstream.sh 会做）")
    print("  2. supportsSusfs / isGateKernel 之类的能力判断是否要跟着改")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
