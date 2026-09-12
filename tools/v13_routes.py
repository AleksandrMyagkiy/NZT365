from pathlib import Path

path = Path('app/src/main/java/com/nzt365/app/V7Product.kt')
text = path.read_text(encoding='utf-8')
text = text.replace('V10BooksScreen(lang, Modifier.fillMaxSize())', 'V13BooksScreen(lang, Modifier.fillMaxSize())')
text = text.replace('ProNutritionActivity::class.java', 'V13NutritionActivity::class.java')
text = text.replace('V7WorkoutActivity::class.java', 'V13WorkoutActivity::class.java')
text = text.replace(
    'ExercisePhoto(plan.session.exercises.firstOrNull()?.name ?: plan.session.title, Modifier.fillMaxWidth().height(220.dp))',
    'ExercisePhoto(plan.session.title, Modifier.fillMaxWidth().height(195.dp), showLabel = false)'
)
path.write_text(text, encoding='utf-8')
