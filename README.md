# NZT 365 — Android v1.0

Нативное Android-приложение для проекта NZT 365.

## Что уже работает
- Daily dashboard и NZT Score.
- Автоматический план дня по дням недели.
- BODY: измерения тела и история.
- Nutrition: калории и белок + пользовательские цели.
- Influence: ежедневные Mirror ×5 и Label ×2.
- Growth / Impact Log: переговоры, книги, карьера, ServiceFlow, дополнительный доход.
- Money snapshots: основной доход, дополнительный доход, капитал, расходы.
- Progress: день из 365, статистика и 30-day checkpoint.
- Локальное хранение данных в телефоне.
- Ежедневное напоминание через WorkManager.

## Сборка
Проект рассчитан на Android Studio / Gradle 8.10.2, JDK 17, Android SDK 35.

### В Android Studio
1. Open -> выбрать папку NZT365_v1.
2. Дождаться Gradle Sync.
3. Build -> Build App Bundles or APKs -> Build APKs.
4. APK будет в app/build/outputs/apk/debug/app-debug.apk.

### Через GitHub Actions
Workflow `.github/workflows/android.yml` автоматически собирает APK и публикует его как artifact `NZT365-debug-apk`.
