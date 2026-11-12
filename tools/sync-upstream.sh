#!/usr/bin/env bash
#
# 一条命令吸收上游 Shark 的新机型与后台逻辑，界面代码不动。
#
#   ./tools/sync-upstream.sh              # 同步到 upstream/main 最新
#   ./tools/sync-upstream.sh v2.2.1       # 同步到指定 tag
#
# 做三件事：
#   1. 拉取上游 ref
#   2. 覆盖 jniLibs/arm64-v8a（exploit 与 LKM 二进制）
#   3. 合并机型表（enum + 内核映射 + .so 分支）
# 最后把上游其余 App 层改动列出来，由人决定是否移植。
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REF="${1:-upstream/main}"

cd "$ROOT"

if ! git remote get-url upstream >/dev/null 2>&1; then
  echo "添加 upstream 远端…"
  git remote add upstream https://github.com/Yunnijian/Shark.git
fi

echo "拉取上游…"
git fetch upstream --tags --quiet

echo "同步 native 库…"
git checkout "$REF" -- app/app/src/main/jniLibs/arm64-v8a/
ls -1 app/app/src/main/jniLibs/arm64-v8a/ | sed 's/^/  /'

echo
echo "合并机型表…"
python3 tools/merge_device_profiles.py "$REF"

echo
echo "上游 App 层其余改动（需人工判断是否移植）："
git diff --stat "$REF" -- \
  app/app/src/main/java/com/shark/kty/ \
  app/app/build.gradle.kts \
  app/app/proguard-rules.pro \
  | sed 's/^/  /' || true

echo
echo "下一步："
echo "  1. ./gradlew :app:assembleRelease 验证编译"
echo "  2. 装机确认新机型识别正常"
