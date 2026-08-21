package com.example.sleeptracker.settings

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** Язык интерфейса. По умолчанию — системный, запасной вариант English. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    RUSSIAN("ru");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/** Режим оформления. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: DARK
    }
}

/** Снимок пользовательских настроек — на него подписан UI. */
data class SettingsState(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Путь к файлу фонового изображения во внутреннем хранилище, либо null. */
    val backgroundPath: String? = null,
    /** Затемнение фонового фото, 0f..0.9f — чтобы текст оставался читаемым. */
    val backgroundDim: Float = 0.45f,
    val bedtimeReminder: Boolean = false,
    val morningReminder: Boolean = false,
    val onboardingShown: Boolean = false,
) {
    val hasBackgroundImage: Boolean get() = backgroundPath != null
}

/**
 * Единая точка хранения настроек: SharedPreferences + [StateFlow] для Compose.
 * Инициализируется в [com.example.sleeptracker.SleepApplication].
 */
object AppSettings {

    private const val PREFS = "settings"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_BACKGROUND = "background_path"
    private const val KEY_BACKGROUND_DIM = "background_dim"
    private const val KEY_BEDTIME_REMINDER = "bedtime_reminder"
    private const val KEY_MORNING_REMINDER = "morning_reminder"
    private const val KEY_ONBOARDING = "onboarding_shown"

    private const val BACKGROUND_FILE = "background.jpg"

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Читает настройки с диска и применяет язык. Вызывается один раз при старте. */
    fun init(context: Context) {
        val p = prefs(context)
        val savedPath = p.getString(KEY_BACKGROUND, null)
        // файл мог быть удалён — не держим битую ссылку
        val backgroundPath = savedPath?.takeIf { File(it).exists() }

        _state.value = SettingsState(
            language = AppLanguage.fromTag(p.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.tag)),
            themeMode = ThemeMode.fromName(p.getString(KEY_THEME, ThemeMode.DARK.name)),
            backgroundPath = backgroundPath,
            backgroundDim = p.getFloat(KEY_BACKGROUND_DIM, 0.45f),
            bedtimeReminder = p.getBoolean(KEY_BEDTIME_REMINDER, false),
            morningReminder = p.getBoolean(KEY_MORNING_REMINDER, false),
            onboardingShown = p.getBoolean(KEY_ONBOARDING, false),
        )
        applyLocale(_state.value.language)
    }

    // ---- Язык ----

    fun getLanguage(context: Context): AppLanguage = _state.value.language

    /** Сохраняет и сразу применяет язык — Activity пересоздастся автоматически. */
    fun setLanguage(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(KEY_LANGUAGE, language.tag).apply()
        _state.value = _state.value.copy(language = language)
        applyLocale(language)
    }

    private fun applyLocale(language: AppLanguage) {
        val locales =
            if (language == AppLanguage.SYSTEM) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    // ---- Тема ----

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
        _state.value = _state.value.copy(themeMode = mode)
    }

    // ---- Фон ----

    /**
     * Сохраняет уже обрезанный кадр во внутреннее хранилище.
     * @return true, если файл записан
     */
    fun setBackgroundBitmap(context: Context, bitmap: Bitmap): Boolean {
        val app = context.applicationContext
        val target = File(app.filesDir, BACKGROUND_FILE)
        return try {
            target.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            prefs(app).edit().putString(KEY_BACKGROUND, target.absolutePath).apply()
            // сбрасываем путь, чтобы Compose перечитал файл с тем же именем
            _state.value = _state.value.copy(backgroundPath = null)
            _state.value = _state.value.copy(backgroundPath = target.absolutePath)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Копирует выбранное изображение во внутреннее хранилище: так фон переживает
     * перезагрузку и не зависит от прав доступа к чужому URI.
     * @return true, если файл скопирован
     */
    fun setBackgroundImage(context: Context, uri: Uri): Boolean {
        val app = context.applicationContext
        val target = File(app.filesDir, BACKGROUND_FILE)
        return try {
            app.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return false

            prefs(app).edit().putString(KEY_BACKGROUND, target.absolutePath).apply()
            // меняем путь на новый объект, чтобы Compose увидел изменение содержимого файла
            _state.value = _state.value.copy(backgroundPath = null)
            _state.value = _state.value.copy(backgroundPath = target.absolutePath)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearBackgroundImage(context: Context) {
        val app = context.applicationContext
        File(app.filesDir, BACKGROUND_FILE).delete()
        prefs(app).edit().remove(KEY_BACKGROUND).apply()
        _state.value = _state.value.copy(backgroundPath = null)
    }

    fun setBackgroundDim(context: Context, dim: Float) {
        val clamped = dim.coerceIn(0f, 0.9f)
        prefs(context).edit().putFloat(KEY_BACKGROUND_DIM, clamped).apply()
        _state.value = _state.value.copy(backgroundDim = clamped)
    }

    // ---- Напоминания ----

    fun setBedtimeReminder(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BEDTIME_REMINDER, enabled).apply()
        _state.value = _state.value.copy(bedtimeReminder = enabled)
    }

    fun setMorningReminder(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MORNING_REMINDER, enabled).apply()
        _state.value = _state.value.copy(morningReminder = enabled)
    }

    fun markOnboardingShown(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING, true).apply()
        _state.value = _state.value.copy(onboardingShown = true)
    }
}
