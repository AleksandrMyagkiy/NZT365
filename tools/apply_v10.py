from pathlib import Path
import re

root = Path('.')

p = root/'app/src/main/java/com/nzt365/app/V7Product.kt'
s = p.read_text(encoding='utf-8')
s = s.replace('V7Tab.GROWTH -> V5BooksScreen(lang, Modifier.fillMaxSize())', 'V7Tab.GROWTH -> V10BooksScreen(lang, Modifier.fillMaxSize())')

pattern = re.compile(r'@Composable\nprivate fun V7Settings\([\s\S]*?\n}\n\nprivate fun v7Text', re.M)
new_settings = r'''@Composable
private fun V7Settings(
    profile: ProfileStore,
    lang: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    onProfileChanged: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.name()) }
    var showReset by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(v7Text(lang, "Настройки", "Settings", "Ustawienia", "Налаштування"), fontSize = 30.sp, fontWeight = FontWeight.Black) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(name, { name = it.take(28) }, modifier = Modifier.fillMaxWidth(), label = { Text(v7Text(lang, "Имя", "Name", "Imię", "Ім'я")) })
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { profile.setName(name); onProfileChanged() }, modifier = Modifier.fillMaxWidth()) { Text(v7Text(lang, "Сохранить", "Save", "Zapisz", "Зберегти")) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(v7Text(lang, "Язык", "Language", "Język", "Мова"), fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    AppLanguage.entries.forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { onLanguage(item) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = item == lang, onClick = { onLanguage(item) })
                            Text(item.label)
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24171A)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestartAlt, null, tint = Color(0xFFFF7777))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text(v7Text(lang, "Начать NZT 365 заново", "Restart NZT 365", "Zacznij NZT 365 od nowa", "Почати NZT 365 заново"), fontWeight = FontWeight.Black)
                            Text(v7Text(lang, "Обнулит тренировки, питание, книги, замеры и все результаты.", "Clears workouts, nutrition, books, measurements and all progress.", "Usuwa treningi, odżywianie, książki, pomiary i cały postęp.", "Обнулить тренування, харчування, книги, заміри та весь прогрес."), color = NztMuted, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DeleteForever, null)
                        Spacer(Modifier.width(7.dp))
                        Text(v7Text(lang, "Обнулить всё", "Reset everything", "Wyzeruj wszystko", "Обнулити все"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Surface(color = NztSurface2, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("NZT 365 v${BuildConfig.VERSION_NAME}", color = NztAccent, fontWeight = FontWeight.Black)
                    Text(v7Text(lang, "Версия определяется автоматически. Данные хранятся локально на устройстве.", "Version is detected automatically. Data is stored locally on this device.", "Wersja jest wykrywana automatycznie. Dane są przechowywane lokalnie.", "Версія визначається автоматично. Дані зберігаються локально."), color = NztMuted, fontSize = 11.sp)
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(v7Text(lang, "Точно начать сначала?", "Start from zero?", "Na pewno zacząć od zera?", "Точно почати спочатку?"), fontWeight = FontWeight.Black) },
            text = { Text(v7Text(lang, "Будут удалены все результаты, история тренировок, замеры, питание, заметки и загруженные книги. Это действие нельзя отменить.", "All progress, workout history, measurements, nutrition, notes and imported books will be deleted. This cannot be undone.", "Cały postęp, historia treningów, pomiary, odżywianie, notatki i zaimportowane książki zostaną usunięte. Tej operacji nie można cofnąć.", "Увесь прогрес, історія тренувань, заміри, харчування, нотатки та завантажені книги будуть видалені. Дію не можна скасувати.")) },
            confirmButton = { Button(onClick = { showReset = false; V10Reset.everything(context) }) { Text(v7Text(lang, "УДАЛИТЬ И НАЧАТЬ", "DELETE & RESTART", "USUŃ I ZACZNIJ", "ВИДАЛИТИ Й ПОЧАТИ")) } },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(v7Text(lang, "Отмена", "Cancel", "Anuluj", "Скасувати")) } },
            containerColor = NztSurface
        )
    }
}

private fun v7Text'''

s2, n = pattern.subn(new_settings, s, count=1)
if n != 1:
    raise SystemExit(f'Could not replace V7Settings, matches={n}')
p.write_text(s2, encoding='utf-8')

p = root/'app/src/main/java/com/nzt365/app/V9PerformanceHub.kt'
s = p.read_text(encoding='utf-8')
old = '''            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(48.dp)
                    .height(92.dp)
                    .clickable { showHub = true },
                color = NztAccent,
                contentColor = Color.Black,
                shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Insights, null, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.height(5.dp))
                    Text("LAB", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }'''
new = '''            FloatingActionButton(
                onClick = { showHub = true },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 14.dp, bottom = 92.dp).size(50.dp),
                containerColor = NztAccent,
                contentColor = Color.Black,
                shape = RoundedCornerShape(17.dp)
            ) { Icon(Icons.Default.Insights, "Performance Lab", modifier = Modifier.size(24.dp)) }'''
if old not in s:
    raise SystemExit('LAB block not found')
s = s.replace(old, new)
p.write_text(s, encoding='utf-8')

print('NZT v10 source integration applied')
