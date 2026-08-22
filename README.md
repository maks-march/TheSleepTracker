# TheSleepTracker (Android, Kotlin)

Простой офлайн-трекер сна: локальная база, тёмная тема, английский по умолчанию + русская локализация.

## 📥 Скачать

**[TheSleepTracker.apk](apk/TheSleepTracker.apk)** — [прямая ссылка на загрузку](https://github.com/maks-march/TheSleepTracker/raw/main/apk/TheSleepTracker.apk)

Android 8.0+ (minSdk 26). При первой установке разрешите «Установку из неизвестных
источников». APK подписан постоянным ключом `keystore/thesleeptracker.jks`, поэтому
следующие версии ставятся поверх прямо из приложения кнопкой «Обновить».

## Что умеет

**Journal / Дневник**
- Когда лёг спать — дата + время, системный пикер **барабанами** (`timePickerMode=spinner`)
- Когда проснулся — дата + время, тот же пикер
- Сколько по ощущениям засыпал — слайдер 0–120 мин
- Оценка сна по 10-балльной шкале
- Примечания (свободный текст)
- Тап по карточке — редактирование, иконка корзины — удаление
- Кнопка **настроек** — в шапке справа

Фактическая длительность сна = `пробуждение − отход ко сну − время засыпания`.

**Analytics / Аналитика**
- Период: Week / Month / Year
- Столбчатый график (Vico):
  - week — сон по дням за 7 дней;
  - month — сон по дням за 30 дней;
  - year — средний сон за ночь по 12 месяцам.
- Сводка: средний сон, средняя оценка, среднее время засыпания, число ночей, лучшая ночь
- **Поделиться** — иконка в шапке справа: системный шэр-лист с текстовой сводкой
  за период и ссылкой на скачивание APK с GitHub (ссылка добавляется всегда)

**Settings / Настройки**
- **Оформление**: тема Светлая / Тёмная / Как в системе
- **Свой фон картинкой** с **обрезкой**: фото из галереи открывается в кроппере —
  двигаете пальцем, щипком меняете масштаб, в фон уходит ровно видимый кусок.
  Ползунок затемнения (0–90%) держит текст читаемым. Обрезанный кадр хранится
  во внутренней памяти, поэтому переживает перезагрузку
- **Напоминания** (см. ниже)
- **Проверить обновления** — приложение читает `version.json` из репозитория и, если
  там версия новее установленной, предлагает скачать APK и открывает установщик
- **Смена языка** (English / Русский) через `AppCompatDelegate.setApplicationLocales` — применяется на лету, переживает перезапуск
- **Экспорт в Excel (.xlsx)** — файл сразу сохраняется в папку «Загрузки»
  (`MediaStore` на Android 10+, без запроса разрешений). Писалка XLSX самописная,
  без Apache POI — никаких тяжёлых зависимостей
- Ссылка на проект на GitHub (исходники + APK)
- Версия приложения

**Напоминания**

Оба времени считаются **из истории сна**, а не задаются вручную, и пересчитываются после
каждой новой записи (окно — последние 14 ночей; пока записей нет, берутся 23:00 и 08:00).

- **Вечернее** — «пора укладываться» в среднее время отхода ко сну, обычное уведомление.
- **Утреннее** — «запишите прошедшую ночь» в среднее время подъёма, **беззвучное**
  (`IMPORTANCE_LOW` + `setSilent`, без звука и вибрации). Не приходит, если запись
  за сегодня уже есть.
- При **первом запуске** приложение предлагает включить напоминания; на Android 13+
  тут же запрашивается разрешение `POST_NOTIFICATIONS`.
- Будильники восстанавливаются после перезагрузки (`BootReceiver`). Без разрешения на
  точные будильники используется неточный — уведомление всё равно придёт.

Среднее время суток считается по кратчайшей дуге, поэтому отход ко сну в 23:50 и 00:10
даёт 00:00, а не полдень.

## Стек

| Слой | Технология |
|---|---|
| UI | Jetpack Compose + Material 3 (только тёмная тема) |
| Навигация | Navigation Compose (3 вкладки + экран редактора) |
| БД | Room (локальная, `sleep.db`), всё офлайн |
| Графики | Vico 2.1.4 (`compose-m3`) |
| Локали | `AppCompatDelegate` + `res/xml/locales_config.xml` (en, ru) |
| Экспорт | собственный `XlsxWriter` (ZIP + OOXML) + FileProvider |
| Ссылки | `buildConfigField` → `BuildConfig.GITHUB_URL` / `BuildConfig.APK_URL` |
| Напоминания | `AlarmManager` + `BroadcastReceiver` + `NotificationCompat` |
| Обновление | `DownloadManager` + `FileProvider` → системный установщик |
| Асинхронность | Coroutines + Flow + StateFlow |

- `minSdk 26`, `compileSdk/targetSdk 35`, Kotlin 2.0.21, AGP 8.7.3, JDK 17.
- Записи сна никуда не отправляются — БД только на устройстве. Сеть используется
  единственный раз: по кнопке «Скачать свежий APK» приложение обращается к GitHub.

## Структура

```
app/src/main/java/com/example/sleeptracker/
├── MainActivity.kt            # AppCompatActivity + edge-to-edge
├── SleepApplication.kt        # применяет сохранённую локаль на старте
├── data/
│   ├── SleepEntry.kt          # @Entity: bedTime, wakeTime, fallAsleepMinutes, quality, note
│   ├── Converters.kt          # LocalDateTime <-> epoch seconds
│   ├── SleepDao.kt            # observeAll / insert / update / delete
│   ├── SleepDatabase.kt       # Room, singleton
│   └── SleepRepository.kt
├── analytics/
│   └── SleepStats.kt          # Period, ChartPoint, PeriodSummary, buildSummary()
├── export/
│   ├── XlsxWriter.kt          # минимальный генератор .xlsx
│   └── SleepExporter.kt       # выгрузка записей + share Intent
├── reminder/
│   ├── ReminderScheduler.kt   # среднее время сна, планирование будильников
│   ├── ReminderReceiver.kt    # показ уведомления + перепланирование
│   ├── BootReceiver.kt        # восстановление после перезагрузки
│   └── Notifications.kt       # каналы; утренний — беззвучный
├── update/
│   └── ApkDownloader.kt       # загрузка APK с GitHub и установка
├── settings/
│   └── AppSettings.kt         # язык, тема, фон, напоминания (StateFlow)
└── ui/
    ├── SleepApp.kt            # NavHost + NavigationBar (journal / analytics / settings)
    ├── SleepViewModel.kt      # StateFlow: entries, period, summary
    ├── components/
    │   ├── TimeSliderPicker.kt
    │   ├── AppBackground.kt   # отрисовка фонового фото + затемнение
    │   └── OnboardingDialog.kt
    ├── theme/Theme.kt         # тёмная палитра
    └── screens/
        ├── JournalScreen.kt
        ├── EntryEditorScreen.kt
        ├── AnalyticsScreen.kt
        └── SettingsScreen.kt

app/src/main/res/
├── values/strings.xml         # English (default)
├── values-ru/strings.xml      # Русский
└── xml/{locales_config,file_paths}.xml
```

## Как запустить

1. Открыть папку `SleepTracker` в Android Studio (Ladybug или новее) — Gradle подтянет всё сам.
2. Или из терминала:

```bash
cd SleepTracker
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug   # на подключённое устройство/эмулятор

# release — тот APK, что лежит в /apk/
./gradlew assembleRelease
```

Нужен JDK 17 и Android SDK (в Android Studio уже есть). `local.properties` в репозиторий
не входит — Android Studio создаст его сама, либо пропишите свой путь:
`echo "sdk.dir=/путь/к/Android/Sdk" > local.properties`.

Release-сборка подписывается ключом `keystore/thesleeptracker.jks`. Пароли по умолчанию
зашиты в `app/build.gradle.kts`; чтобы задать свои, скопируйте `keystore.properties.example`
в `keystore.properties` (он в `.gitignore`).

## Пример текста «Поделиться»

```
My sleep · Week

Average sleep: 7 h 20 min
Average rating: 7.8/10
Falling asleep: 14 min
Nights tracked: 7
Best night: 8 h 40 min

Tracked with TheSleepTracker
Get the app: https://github.com/maks-march/TheSleepTracker/raw/main/apk/TheSleepTracker.apk
```

## Данные и обновления

База `sleep.db` лежит во внутреннем хранилище приложения, поэтому установка
новой версии **поверх** старой записи не трогает. Room намеренно собран без
`fallbackToDestructiveMigration()` — при смене схемы он не удалит данные молча,
а потребует миграцию (см. `SleepDatabase.MIGRATIONS`).

Дополнительно:
- `backup_rules.xml` / `data_extraction_rules.xml` — база, настройки и фон
  попадают в облачный бэкап Android и в перенос на новое устройство;
- **Настройки → «Резервная копия»** сохраняет все записи в .json в «Загрузки»,
  **«Восстановить из копии»** читает его обратно. Дубликаты (та же пара
  «лёг/проснулся») при импорте пропускаются, так что повторный импорт безопасен.

Копия нужна только там, где обновиться поверх нельзя — например, при смене
ключа подписи.

## Как выпустить обновление

Приложение проверяет обновления по файлу `version.json` в корне ветки `main` —
никакого сервера не нужно. Чтобы выкатить новую версию:

1. Поднимите версию в `app/build.gradle.kts`:
   ```kotlin
   versionCode = 5        // строго больше предыдущего
   versionName = "1.4"
   ```
2. Соберите APK и положите его в репозиторий под тем же именем:
   ```bash
   ./gradlew assembleRelease
   cp app/build/outputs/apk/release/app-release.apk apk/TheSleepTracker.apk
   ```
3. Обновите `version.json` — `versionCode` должен совпасть с тем, что в Gradle:
   ```json
   { "versionCode": 5, "versionName": "1.4", "notes": "Что нового" }
   ```
4. `git add -A && git commit && git push`.

Всё: у пользователей по кнопке «Проверить обновления» появится диалог с вашим
текстом из `notes` и кнопкой «Обновить».

Важно: APK должен быть подписан **тем же ключом**, что и установленная версия,
иначе Android откажется ставить обновление поверх. Сейчас используется отладочный
ключ (`~/.android/debug.keystore`) — для распространения заведите свой keystore
и храните его вне репозитория.

## Куда расти

- Цвет столбика в зависимости от оценки сна
- Фильтр по произвольному диапазону дат
- Импорт из .xlsx / .csv
