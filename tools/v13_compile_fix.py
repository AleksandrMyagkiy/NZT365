from pathlib import Path

# Reader activity helper: V12ReaderActivity intentionally has no companion intent factory.
p = Path('app/src/main/java/com/nzt365/app/V13Books.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('V12ReaderActivity.intent(context,book)', 'android.content.Intent(context, V12ReaderActivity::class.java).putExtra("id", book.id)')
s = s.replace('V12ReaderActivity.intent(context,linked)', 'android.content.Intent(context, V12ReaderActivity::class.java).putExtra("id", linked.id)')
p.write_text(s, encoding='utf-8')

# Replace the dense manager expression with an explicit Compose implementation.
p = Path('app/src/main/java/com/nzt365/app/V13Nutrition.kt')
s = p.read_text(encoding='utf-8')
start = s.index('@Composable private fun V13FoodManager')
end = s.index('@Composable private fun V13EditFood', start)
manager = r'''@Composable
private fun V13FoodManager(
    lang: AppLanguage,
    foods: List<V13Food>,
    onClose: () -> Unit,
    onSave: (List<V13Food>) -> Unit,
    onEdit: (V13Food) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = NztSurface) {
        LazyColumn(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        v13t(lang, "Каталог продуктов", "Food catalog", "Katalog produktów", "Каталог продуктів"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        onEdit(V13Food(UUID.randomUUID().toString(), "", 100.0, 0.0, 0.0, 0.0))
                    }) { Text("+") }
                }
            }
            items(foods, key = { it.id }) { food ->
                Card(
                    Modifier.fillMaxWidth().clickable { onEdit(food) },
                    colors = CardDefaults.cardColors(containerColor = NztSurface2)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(food.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${food.kcal.roundToInt()} kcal · P ${food.p} · F ${food.f} · C ${food.c}",
                                color = NztMuted,
                                fontSize = 11.sp
                            )
                        }
                        Icon(Icons.Default.Edit, null, tint = NztAccent)
                    }
                }
            }
            item {
                Button(onClick = { onSave(foods) }, modifier = Modifier.fillMaxWidth()) {
                    Text(v13t(lang, "Готово", "Done", "Gotowe", "Готово"))
                }
            }
        }
    }
}

'''
s = s[:start] + manager + s[end:]
p.write_text(s, encoding='utf-8')
print('v13 compile fixes applied')
