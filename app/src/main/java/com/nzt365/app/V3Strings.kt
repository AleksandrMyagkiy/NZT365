package com.nzt365.app

object V3Strings {
    fun t(lang: AppLanguage, key: String): String {
        val ru = mapOf(
            "today" to "Сегодня", "body" to "Тело", "growth" to "Рост", "progress" to "Прогресс", "settings" to "Настройки",
            "hello" to "Привет", "protocol" to "Твой протокол", "readiness" to "Готовность", "workout" to "Тренировка", "nutrition" to "Питание",
            "start" to "Начать", "open" to "Открыть", "habits" to "Привычки", "focus" to "Фокус дня", "books" to "Книги",
            "booksSubtitle" to "12 книг / 12 месяцев", "impact" to "Impact Log", "career" to "Карьера", "money" to "Деньги",
            "week" to "Неделя", "history" to "История", "measurements" to "Замеры", "language" to "Язык", "name" to "Имя",
            "save" to "Сохранить", "edit" to "Изменить", "done" to "Готово", "close" to "Закрыть", "back" to "Назад",
            "sets" to "Подходы", "reps" to "Повторы", "weight" to "Вес", "rir" to "RIR", "rest" to "Отдых",
            "previous" to "Прошлая", "next" to "Рекомендация", "technique" to "Техника", "progression" to "Прогрессия",
            "finish" to "Завершить тренировку", "timer" to "Таймер отдыха", "addFood" to "Добавить продукт", "water" to "Вода",
            "breakfast" to "Завтрак", "lunch" to "Обед", "snack" to "Перекус", "dinner" to "Ужин", "quickAdd" to "Быстро добавить",
            "calories" to "Калории", "protein" to "Белок", "fats" to "Жиры", "carbs" to "Углеводы", "dailyFuel" to "Питание дня",
            "bookProgress" to "Прогресс чтения", "appliedIdea" to "Применённая идея", "note" to "Заметка", "markRead" to "Отметить прочитанной",
            "noData" to "Пока нет данных", "streak" to "Серия", "score" to "Индекс дня", "trend" to "Тренд 7 дней",
            "workouts" to "Тренировки", "completed" to "выполнено", "todayPlan" to "План на сегодня", "recovery" to "Восстановление",
            "personalBest" to "Личный рекорд", "volume" to "Объём", "consistency" to "Стабильность", "checkin" to "Контрольная проверка",
            "allSafe" to "Интерфейс адаптирован под безопасную зону экрана"
        )
        val en = mapOf(
            "today" to "Today", "body" to "Body", "growth" to "Growth", "progress" to "Progress", "settings" to "Settings",
            "hello" to "Hello", "protocol" to "Your protocol", "readiness" to "Readiness", "workout" to "Workout", "nutrition" to "Nutrition",
            "start" to "Start", "open" to "Open", "habits" to "Habits", "focus" to "Daily focus", "books" to "Books",
            "booksSubtitle" to "12 books / 12 months", "impact" to "Impact Log", "career" to "Career", "money" to "Money",
            "week" to "Week", "history" to "History", "measurements" to "Measurements", "language" to "Language", "name" to "Name",
            "save" to "Save", "edit" to "Edit", "done" to "Done", "close" to "Close", "back" to "Back",
            "sets" to "Sets", "reps" to "Reps", "weight" to "Weight", "rir" to "RIR", "rest" to "Rest",
            "previous" to "Previous", "next" to "Next", "technique" to "Technique", "progression" to "Progression",
            "finish" to "Finish workout", "timer" to "Rest timer", "addFood" to "Add food", "water" to "Water",
            "breakfast" to "Breakfast", "lunch" to "Lunch", "snack" to "Snack", "dinner" to "Dinner", "quickAdd" to "Quick add",
            "calories" to "Calories", "protein" to "Protein", "fats" to "Fats", "carbs" to "Carbs", "dailyFuel" to "Daily fuel",
            "bookProgress" to "Reading progress", "appliedIdea" to "Applied idea", "note" to "Note", "markRead" to "Mark as read",
            "noData" to "No data yet", "streak" to "Streak", "score" to "Daily score", "trend" to "7-day trend",
            "workouts" to "Workouts", "completed" to "completed", "todayPlan" to "Today's plan", "recovery" to "Recovery",
            "personalBest" to "Personal best", "volume" to "Volume", "consistency" to "Consistency", "checkin" to "Check-in",
            "allSafe" to "UI respects device safe areas"
        )
        val pl = mapOf(
            "today" to "Dzisiaj", "body" to "Ciało", "growth" to "Rozwój", "progress" to "Postęp", "settings" to "Ustawienia",
            "hello" to "Cześć", "protocol" to "Twój protokół", "readiness" to "Gotowość", "workout" to "Trening", "nutrition" to "Odżywianie",
            "start" to "Start", "open" to "Otwórz", "habits" to "Nawyki", "focus" to "Fokus dnia", "books" to "Książki",
            "booksSubtitle" to "12 książek / 12 miesięcy", "impact" to "Impact Log", "career" to "Kariera", "money" to "Finanse",
            "week" to "Tydzień", "history" to "Historia", "measurements" to "Pomiary", "language" to "Język", "name" to "Imię",
            "save" to "Zapisz", "edit" to "Edytuj", "done" to "Gotowe", "close" to "Zamknij", "back" to "Wstecz",
            "sets" to "Serie", "reps" to "Powtórzenia", "weight" to "Ciężar", "rir" to "RIR", "rest" to "Przerwa",
            "previous" to "Poprzednio", "next" to "Następny krok", "technique" to "Technika", "progression" to "Progresja",
            "finish" to "Zakończ trening", "timer" to "Przerwa", "addFood" to "Dodaj produkt", "water" to "Woda",
            "breakfast" to "Śniadanie", "lunch" to "Obiad", "snack" to "Przekąska", "dinner" to "Kolacja", "quickAdd" to "Szybkie dodawanie",
            "calories" to "Kalorie", "protein" to "Białko", "fats" to "Tłuszcze", "carbs" to "Węglowodany", "dailyFuel" to "Bilans dnia",
            "bookProgress" to "Postęp czytania", "appliedIdea" to "Wdrożona idea", "note" to "Notatka", "markRead" to "Oznacz jako przeczytaną",
            "noData" to "Brak danych", "streak" to "Seria", "score" to "Indeks dnia", "trend" to "Trend 7 dni",
            "workouts" to "Treningi", "completed" to "wykonano", "todayPlan" to "Plan na dziś", "recovery" to "Regeneracja",
            "personalBest" to "Rekord", "volume" to "Objętość", "consistency" to "Regularność", "checkin" to "Kontrola",
            "allSafe" to "Interfejs uwzględnia bezpieczne obszary ekranu"
        )
        val uk = mapOf(
            "today" to "Сьогодні", "body" to "Тіло", "growth" to "Розвиток", "progress" to "Прогрес", "settings" to "Налаштування",
            "hello" to "Привіт", "protocol" to "Твій протокол", "readiness" to "Готовність", "workout" to "Тренування", "nutrition" to "Харчування",
            "start" to "Почати", "open" to "Відкрити", "habits" to "Звички", "focus" to "Фокус дня", "books" to "Книги",
            "booksSubtitle" to "12 книг / 12 місяців", "impact" to "Impact Log", "career" to "Кар’єра", "money" to "Гроші",
            "week" to "Тиждень", "history" to "Історія", "measurements" to "Заміри", "language" to "Мова", "name" to "Ім’я",
            "save" to "Зберегти", "edit" to "Змінити", "done" to "Готово", "close" to "Закрити", "back" to "Назад",
            "sets" to "Підходи", "reps" to "Повтори", "weight" to "Вага", "rir" to "RIR", "rest" to "Відпочинок",
            "previous" to "Минулого разу", "next" to "Рекомендація", "technique" to "Техніка", "progression" to "Прогресія",
            "finish" to "Завершити тренування", "timer" to "Таймер відпочинку", "addFood" to "Додати продукт", "water" to "Вода",
            "breakfast" to "Сніданок", "lunch" to "Обід", "snack" to "Перекус", "dinner" to "Вечеря", "quickAdd" to "Швидко додати",
            "calories" to "Калорії", "protein" to "Білок", "fats" to "Жири", "carbs" to "Вуглеводи", "dailyFuel" to "Раціон дня",
            "bookProgress" to "Прогрес читання", "appliedIdea" to "Застосована ідея", "note" to "Нотатка", "markRead" to "Позначити прочитаною",
            "noData" to "Поки немає даних", "streak" to "Серія", "score" to "Індекс дня", "trend" to "Тренд 7 днів",
            "workouts" to "Тренування", "completed" to "виконано", "todayPlan" to "План на сьогодні", "recovery" to "Відновлення",
            "personalBest" to "Особистий рекорд", "volume" to "Обсяг", "consistency" to "Стабільність", "checkin" to "Контроль",
            "allSafe" to "Інтерфейс враховує безпечні зони екрана"
        )
        val map = when (lang) { AppLanguage.RU -> ru; AppLanguage.EN -> en; AppLanguage.PL -> pl; AppLanguage.UK -> uk }
        return map[key] ?: ru[key] ?: key
    }
}
