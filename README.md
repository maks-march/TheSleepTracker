# Сон — трекер сна (Android, Kotlin)

Простое приложение-трекер сна с локальной базой данных и тёмной темой.

## Что умеет

**Дневник**
- Когда лёг спать (дата + время)
- Когда проснулся (дата + время)
- Сколько по ощущениям засыпал (слайдер, 0–120 мин, шаг 5)
- Оценка сна по 10-балльной шкале
- Примечания (свободный текст)
- Редактирование по тапу на карточку, удаление по иконке корзины

Фактическая длительность сна считается как `пробуждение − отход ко сну − время засыпания`.

**Аналитика**
- Переключатель периода: Неделя / Месяц / Год
- Столбчатый график (Vico):
  - неделя — сон по дням за 7 дней;
  - месяц — сон по дням за 30 дней;
  - год — средний сон за ночь по 12 месяцам.
- Сводка за период: средний сон, средняя оценка, среднее время засыпания

## Стек

| Слой | Технология |
|---|---|
| UI | Jetpack Compose + Material 3 (только тёмная тема) |
| Навигация | Navigation Compose (2 вкладки + экран редактора) |
| БД | Room (локальная, `sleep.db`), всё офлайн |
| Графики | Vico 2.1.4 (`compose-m3`) |
| Асинхронность | Coroutines + Flow + StateFlow |

- `minSdk 26`, `compileSdk/targetSdk 35`, Kotlin 2.0.21, AGP 8.7.3, JDK 17.
- Никакого интернета и разрешений — данные только на устройстве.

## Структура

```
app/src/main/java/com/example/sleeptracker/
├── MainActivity.kt
├── data/
│   ├── SleepEntry.kt        # @Entity: bedTime, wakeTime, fallAsleepMinutes, quality, note
│   ├── Converters.kt        # LocalDateTime <-> epoch seconds
│   ├── SleepDao.kt          # observeAll / insert / update / delete
│   ├── SleepDatabase.kt     # Room, singleton
│   └── SleepRepository.kt
├── analytics/
│   └── SleepStats.kt        # Period, ChartPoint, PeriodSummary, buildSummary()
└── ui/
    ├── SleepApp.kt          # NavHost + NavigationBar
    ├── SleepViewModel.kt    # StateFlow: entries, period, summary
    ├── theme/Theme.kt       # тёмная палитра
    └── screens/
        ├── JournalScreen.kt
        ├── EntryEditorScreen.kt
        └── AnalyticsScreen.kt
```

## Как запустить

1. Открыть папку `SleepTracker` в Android Studio (Ladybug или новее) — Gradle подтянет всё сам.
2. Или из терминала:

```bash
cd SleepTracker
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug   # на подключённое устройство/эмулятор
```

`local.properties` содержит путь к SDK этой машины — при переносе проекта замените
`sdk.dir` на свой или удалите файл (Android Studio создаст заново).

## Куда расти

- Экспорт в CSV
- Напоминание «пора спать» через `AlarmManager`
- Цвет столбика в зависимости от оценки сна
- Фильтр по диапазону дат вместо фиксированных периодов
