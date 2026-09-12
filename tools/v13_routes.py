from pathlib import Path
path = Path('app/src/main/java/com/nzt365/app/V7Product.kt')
text = path.read_text(encoding='utf-8')
text = text.replace('V10BooksScreen(lang, Modifier.fillMaxSize())', 'V13BooksScreen(lang, Modifier.fillMaxSize())')
text = text.replace('ProNutritionActivity::class.java', 'V13NutritionActivity::class.java')
path.write_text(text, encoding='utf-8')
