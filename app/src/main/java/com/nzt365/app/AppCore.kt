package com.nzt365.app

import android.content.Context
import androidx.compose.runtime.*

enum class AppLanguage(val code: String, val label: String) {
    RU("ru", "Русский"),
    EN("en", "English"),
    PL("pl", "Polski"),
    UK("uk", "Українська");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: RU
    }
}

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("nzt365_profile", Context.MODE_PRIVATE)

    fun isOnboarded(): Boolean = prefs.getBoolean("onboarded", false)
    fun name(): String = prefs.getString("name", "") ?: ""
    fun language(): AppLanguage = AppLanguage.fromCode(prefs.getString("language", "ru"))

    fun save(name: String, language: AppLanguage) {
        prefs.edit()
            .putString("name", name.trim())
            .putString("language", language.code)
            .putBoolean("onboarded", true)
            .apply()
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("language", language.code).apply()
    }

    fun setName(name: String) {
        prefs.edit().putString("name", name.trim()).apply()
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("onboarded", false).apply()
    }
}

class Localizer(val language: AppLanguage) {
    fun t(key: String): String {
        val ru = mapOf(
            "app_name" to "NZT 365",
            "tagline" to "Одна система. Тело, мышление, карьера, капитал.",
            "welcome" to "Добро пожаловать",
            "welcome_back" to "С возвращением",
            "your_name" to "Как тебя зовут?",
            "name_hint" to "Имя",
            "choose_language" to "Выбери язык",
            "continue" to "Продолжить",
            "today" to "Сегодня",
            "body" to "Тело",
            "growth" to "Рост",
            "progress" to "Прогресс",
            "settings" to "Настройки",
            "day" to "День",
            "of" to "из",
            "protocol" to "NZT ПРОТОКОЛ",
            "score" to "Индекс дня",
            "focus_today" to "Фокус сегодня",
            "workout" to "Тренировка",
            "start_workout" to "Начать тренировку",
            "nutrition" to "Питание",
            "open_nutrition" to "Открыть питание",
            "habits" to "Привычки",
            "daily_actions" to "Действия дня",
            "quick_log" to "Быстрая отметка",
            "recovery" to "Восстановление",
            "training_plan" to "План тренировок",
            "exercise_library" to "Библиотека упражнений",
            "replace_workout" to "Заменить тренировку",
            "measurements" to "Измерения тела",
            "weight" to "Вес",
            "waist" to "Талия",
            "chest" to "Грудь",
            "arm" to "Рука",
            "thigh" to "Бедро",
            "save" to "Сохранить",
            "mind" to "Мышление",
            "career" to "Карьера",
            "influence" to "Влияние",
            "money" to "Деньги",
            "books" to "12 книг / 12 месяцев",
            "impact_log" to "Impact Log",
            "add_action" to "Добавить действие",
            "what_done" to "Что сделал / что понял",
            "history" to "История",
            "body_progress" to "Тело",
            "growth_progress" to "Развитие",
            "money_progress" to "Финансы",
            "profile" to "Профиль",
            "language" to "Язык",
            "goals" to "Цели",
            "calories" to "Калории",
            "protein" to "Белок",
            "notifications" to "Напоминания",
            "about" to "О приложении",
            "edit_name" to "Изменить имя",
            "version" to "Версия",
            "offline" to "Данные хранятся локально на устройстве",
            "good_morning" to "Доброе утро",
            "good_day" to "Добрый день",
            "good_evening" to "Добрый вечер",
            "sets" to "подходов",
            "rest" to "отдых",
            "level" to "уровень",
            "load" to "нагрузка",
            "technique" to "Техника",
            "progression" to "Прогрессия",
            "warmup" to "Разминка",
            "complete_set" to "Выполнить подход",
            "done" to "Готово",
            "timer" to "Таймер отдыха",
            "skip" to "Пропустить",
            "finish" to "Завершить тренировку",
            "meal_breakfast" to "Завтрак",
            "meal_lunch" to "Обед",
            "meal_snack" to "Перекус",
            "meal_dinner" to "Ужин",
            "add_food" to "Добавить продукт",
            "grams" to "грамм",
            "fats" to "Жиры",
            "carbs" to "Углеводы",
            "daily_total" to "Итог дня",
            "streak" to "Серия",
            "days" to "дней",
            "week" to "Неделя",
            "phase" to "Фаза",
            "personal_best" to "Личный рекорд",
            "consistency" to "Стабильность",
            "check_in" to "Контрольная проверка",
            "photos" to "Фото прогресса",
            "soon" to "Следующая проверка через 7–14 дней",
            "custom_workout" to "Своя тренировка",
            "equipment" to "Оборудование",
            "home" to "Дом",
            "pullup_bars" to "Турник / брусья",
            "dumbbells" to "Гантели",
            "barbell" to "Штанга",
            "running" to "Бег",
            "cycling" to "Велосипед",
            "mobility" to "Мобильность",
            "no_zero_day" to "Правило: не заканчивать день с нулём"
        )
        val en = mapOf(
            "app_name" to "NZT 365", "tagline" to "One system. Body, mind, career, capital.",
            "welcome" to "Welcome", "welcome_back" to "Welcome back", "your_name" to "What's your name?", "name_hint" to "Name", "choose_language" to "Choose language", "continue" to "Continue",
            "today" to "Today", "body" to "Body", "growth" to "Growth", "progress" to "Progress", "settings" to "Settings", "day" to "Day", "of" to "of",
            "protocol" to "NZT PROTOCOL", "score" to "Daily index", "focus_today" to "Today's focus", "workout" to "Workout", "start_workout" to "Start workout", "nutrition" to "Nutrition", "open_nutrition" to "Open nutrition", "habits" to "Habits", "daily_actions" to "Daily actions", "quick_log" to "Quick log",
            "recovery" to "Recovery", "training_plan" to "Training plan", "exercise_library" to "Exercise library", "replace_workout" to "Replace workout", "measurements" to "Body measurements", "weight" to "Weight", "waist" to "Waist", "chest" to "Chest", "arm" to "Arm", "thigh" to "Thigh", "save" to "Save",
            "mind" to "Mind", "career" to "Career", "influence" to "Influence", "money" to "Money", "books" to "12 books / 12 months", "impact_log" to "Impact Log", "add_action" to "Add action", "what_done" to "What I did / learned", "history" to "History",
            "body_progress" to "Body", "growth_progress" to "Growth", "money_progress" to "Money", "profile" to "Profile", "language" to "Language", "goals" to "Goals", "calories" to "Calories", "protein" to "Protein", "notifications" to "Reminders", "about" to "About", "edit_name" to "Edit name", "version" to "Version", "offline" to "Data is stored locally on this device",
            "good_morning" to "Good morning", "good_day" to "Good afternoon", "good_evening" to "Good evening", "sets" to "sets", "rest" to "rest", "level" to "level", "load" to "load", "technique" to "Technique", "progression" to "Progression", "warmup" to "Warm-up", "complete_set" to "Complete set", "done" to "Done", "timer" to "Rest timer", "skip" to "Skip", "finish" to "Finish workout",
            "meal_breakfast" to "Breakfast", "meal_lunch" to "Lunch", "meal_snack" to "Snack", "meal_dinner" to "Dinner", "add_food" to "Add food", "grams" to "grams", "fats" to "Fats", "carbs" to "Carbs", "daily_total" to "Daily total", "streak" to "Streak", "days" to "days", "week" to "Week", "phase" to "Phase", "personal_best" to "Personal best", "consistency" to "Consistency", "check_in" to "Check-in", "photos" to "Progress photos", "soon" to "Next check-in in 7–14 days", "custom_workout" to "Custom workout", "equipment" to "Equipment", "home" to "Home", "pullup_bars" to "Pull-up / dip bars", "dumbbells" to "Dumbbells", "barbell" to "Barbell", "running" to "Running", "cycling" to "Cycling", "mobility" to "Mobility", "no_zero_day" to "Rule: never finish the day at zero"
        )
        val pl = mapOf(
            "app_name" to "NZT 365", "tagline" to "Jeden system. Ciało, umysł, kariera, kapitał.",
            "welcome" to "Witaj", "welcome_back" to "Witaj ponownie", "your_name" to "Jak masz na imię?", "name_hint" to "Imię", "choose_language" to "Wybierz język", "continue" to "Dalej",
            "today" to "Dzisiaj", "body" to "Ciało", "growth" to "Rozwój", "progress" to "Postęp", "settings" to "Ustawienia", "day" to "Dzień", "of" to "z",
            "protocol" to "PROTOKÓŁ NZT", "score" to "Indeks dnia", "focus_today" to "Dzisiejszy fokus", "workout" to "Trening", "start_workout" to "Rozpocznij trening", "nutrition" to "Odżywianie", "open_nutrition" to "Otwórz odżywianie", "habits" to "Nawyki", "daily_actions" to "Działania dnia", "quick_log" to "Szybki zapis",
            "recovery" to "Regeneracja", "training_plan" to "Plan treningowy", "exercise_library" to "Biblioteka ćwiczeń", "replace_workout" to "Zamień trening", "measurements" to "Pomiary ciała", "weight" to "Waga", "waist" to "Talia", "chest" to "Klatka", "arm" to "Ramię", "thigh" to "Udo", "save" to "Zapisz",
            "mind" to "Umysł", "career" to "Kariera", "influence" to "Wpływ", "money" to "Finanse", "books" to "12 książek / 12 miesięcy", "impact_log" to "Impact Log", "add_action" to "Dodaj działanie", "what_done" to "Co zrobiłem / czego się nauczyłem", "history" to "Historia",
            "body_progress" to "Ciało", "growth_progress" to "Rozwój", "money_progress" to "Finanse", "profile" to "Profil", "language" to "Język", "goals" to "Cele", "calories" to "Kalorie", "protein" to "Białko", "notifications" to "Przypomnienia", "about" to "O aplikacji", "edit_name" to "Edytuj imię", "version" to "Wersja", "offline" to "Dane są przechowywane lokalnie na urządzeniu",
            "good_morning" to "Dzień dobry", "good_day" to "Dzień dobry", "good_evening" to "Dobry wieczór", "sets" to "serii", "rest" to "przerwa", "level" to "poziom", "load" to "obciążenie", "technique" to "Technika", "progression" to "Progresja", "warmup" to "Rozgrzewka", "complete_set" to "Wykonaj serię", "done" to "Gotowe", "timer" to "Timer przerwy", "skip" to "Pomiń", "finish" to "Zakończ trening",
            "meal_breakfast" to "Śniadanie", "meal_lunch" to "Obiad", "meal_snack" to "Przekąska", "meal_dinner" to "Kolacja", "add_food" to "Dodaj produkt", "grams" to "gramów", "fats" to "Tłuszcze", "carbs" to "Węglowodany", "daily_total" to "Podsumowanie dnia", "streak" to "Seria", "days" to "dni", "week" to "Tydzień", "phase" to "Faza", "personal_best" to "Rekord", "consistency" to "Regularność", "check_in" to "Kontrola", "photos" to "Zdjęcia postępu", "soon" to "Następna kontrola za 7–14 dni", "custom_workout" to "Własny trening", "equipment" to "Sprzęt", "home" to "Dom", "pullup_bars" to "Drążek / poręcze", "dumbbells" to "Hantle", "barbell" to "Sztanga", "running" to "Bieganie", "cycling" to "Rower", "mobility" to "Mobilność", "no_zero_day" to "Zasada: nie kończ dnia z zerem"
        )
        val uk = mapOf(
            "app_name" to "NZT 365", "tagline" to "Одна система. Тіло, мислення, кар'єра, капітал.",
            "welcome" to "Вітаємо", "welcome_back" to "З поверненням", "your_name" to "Як тебе звати?", "name_hint" to "Ім'я", "choose_language" to "Обери мову", "continue" to "Продовжити",
            "today" to "Сьогодні", "body" to "Тіло", "growth" to "Розвиток", "progress" to "Прогрес", "settings" to "Налаштування", "day" to "День", "of" to "з",
            "protocol" to "NZT ПРОТОКОЛ", "score" to "Індекс дня", "focus_today" to "Фокус сьогодні", "workout" to "Тренування", "start_workout" to "Почати тренування", "nutrition" to "Харчування", "open_nutrition" to "Відкрити харчування", "habits" to "Звички", "daily_actions" to "Дії дня", "quick_log" to "Швидка відмітка",
            "recovery" to "Відновлення", "training_plan" to "План тренувань", "exercise_library" to "Бібліотека вправ", "replace_workout" to "Замінити тренування", "measurements" to "Заміри тіла", "weight" to "Вага", "waist" to "Талія", "chest" to "Груди", "arm" to "Рука", "thigh" to "Стегно", "save" to "Зберегти",
            "mind" to "Мислення", "career" to "Кар'єра", "influence" to "Вплив", "money" to "Гроші", "books" to "12 книг / 12 місяців", "impact_log" to "Impact Log", "add_action" to "Додати дію", "what_done" to "Що зробив / що зрозумів", "history" to "Історія",
            "body_progress" to "Тіло", "growth_progress" to "Розвиток", "money_progress" to "Фінанси", "profile" to "Профіль", "language" to "Мова", "goals" to "Цілі", "calories" to "Калорії", "protein" to "Білок", "notifications" to "Нагадування", "about" to "Про застосунок", "edit_name" to "Змінити ім'я", "version" to "Версія", "offline" to "Дані зберігаються локально на пристрої",
            "good_morning" to "Доброго ранку", "good_day" to "Добрий день", "good_evening" to "Добрий вечір", "sets" to "підходів", "rest" to "відпочинок", "level" to "рівень", "load" to "навантаження", "technique" to "Техніка", "progression" to "Прогресія", "warmup" to "Розминка", "complete_set" to "Виконати підхід", "done" to "Готово", "timer" to "Таймер відпочинку", "skip" to "Пропустити", "finish" to "Завершити тренування",
            "meal_breakfast" to "Сніданок", "meal_lunch" to "Обід", "meal_snack" to "Перекус", "meal_dinner" to "Вечеря", "add_food" to "Додати продукт", "grams" to "грамів", "fats" to "Жири", "carbs" to "Вуглеводи", "daily_total" to "Підсумок дня", "streak" to "Серія", "days" to "днів", "week" to "Тиждень", "phase" to "Фаза", "personal_best" to "Особистий рекорд", "consistency" to "Стабільність", "check_in" to "Контрольна перевірка", "photos" to "Фото прогресу", "soon" to "Наступна перевірка через 7–14 днів", "custom_workout" to "Своє тренування", "equipment" to "Обладнання", "home" to "Дім", "pullup_bars" to "Турнік / бруси", "dumbbells" to "Гантелі", "barbell" to "Штанга", "running" to "Біг", "cycling" to "Велосипед", "mobility" to "Мобільність", "no_zero_day" to "Правило: не завершуй день з нулем"
        )
        val table = when (language) { AppLanguage.RU -> ru; AppLanguage.EN -> en; AppLanguage.PL -> pl; AppLanguage.UK -> uk }
        return table[key] ?: ru[key] ?: key
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.RU }

@Composable
fun rememberLocalizer(): Localizer = Localizer(LocalAppLanguage.current)
