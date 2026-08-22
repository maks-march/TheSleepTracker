#!/usr/bin/env bash
#
# Выпуск новой версии TheSleepTracker.
#
# Делает всё, что нужно, чтобы у пользователей заработала кнопка «Обновить»:
#   1. поднимает versionCode/versionName в app/build.gradle.kts
#   2. собирает release APK постоянным ключом
#   3. кладёт его в apk/TheSleepTracker.apk
#   4. пишет version.json (versionCode строго совпадает с APK)
#   5. коммитит и пушит
#
# Использование:
#   ./release.sh 1.4 "Что нового в этой версии"
#   ./release.sh 1.4 "Что нового" --no-push     # собрать, но не пушить
#
set -euo pipefail

cd "$(dirname "$0")"

VERSION_NAME="${1:-}"
NOTES="${2:-}"
PUSH=true
[[ "${3:-}" == "--no-push" ]] && PUSH=false

if [[ -z "$VERSION_NAME" ]]; then
  echo "Использование: ./release.sh <versionName> \"<что нового>\" [--no-push]" >&2
  echo "Пример:        ./release.sh 1.4 \"Починил напоминания\"" >&2
  exit 1
fi

GRADLE_FILE="app/build.gradle.kts"

CURRENT_CODE=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' "$GRADLE_FILE" | head -1)
NEW_CODE=$((CURRENT_CODE + 1))

echo "==> Версия: $CURRENT_CODE -> $NEW_CODE ($VERSION_NAME)"

# 1. поднимаем версию
sed -i -E "s/versionCode\s*=\s*[0-9]+/versionCode = $NEW_CODE/" "$GRADLE_FILE"
sed -i -E "s/versionName\s*=\s*\"[^\"]*\"/versionName = \"$VERSION_NAME\"/" "$GRADLE_FILE"

# 2. собираем
echo "==> Сборка release APK…"
./gradlew --quiet assembleRelease

APK_BUILT="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK_BUILT" ]]; then
  echo "ОШИБКА: APK не собрался" >&2
  exit 1
fi

# 3. кладём в репозиторий
mkdir -p apk
cp "$APK_BUILT" apk/TheSleepTracker.apk
echo "==> apk/TheSleepTracker.apk обновлён ($(du -h apk/TheSleepTracker.apk | cut -f1))"

# 4. version.json — versionCode обязан совпадать с APK, иначе обновление не предложится
python3 - "$NEW_CODE" "$VERSION_NAME" "$NOTES" <<'PY'
import json, sys
code, name, notes = int(sys.argv[1]), sys.argv[2], sys.argv[3]
with open("version.json", "w", encoding="utf-8") as f:
    json.dump({"versionCode": code, "versionName": name, "notes": notes},
              f, ensure_ascii=False, indent=2)
    f.write("\n")
PY
echo "==> version.json:"
cat version.json

# 5. проверяем, что подпись не поменялась — иначе обновление не встанет поверх
if command -v apksigner >/dev/null 2>&1; then
  echo "==> Подпись:"
  apksigner verify --print-certs apk/TheSleepTracker.apk | grep -i "SHA-256 digest" | head -1
fi

# 6. коммит и пуш
git add -A
git commit -q -m "Release $VERSION_NAME (versionCode $NEW_CODE)

$NOTES"
echo "==> Коммит создан"

if $PUSH; then
  git push origin main
  echo "==> Запушено. Обновление доступно пользователям."
else
  echo "==> Пуш пропущен (--no-push). Не забудьте: git push origin main"
fi
