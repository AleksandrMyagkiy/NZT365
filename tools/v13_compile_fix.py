from pathlib import Path

# Reader activity helper: V12ReaderActivity intentionally has no companion intent factory.
p = Path('app/src/main/java/com/nzt365/app/V13Books.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('V12ReaderActivity.intent(context,book)', 'android.content.Intent(context, V12ReaderActivity::class.java).putExtra("id", book.id)')
s = s.replace('V12ReaderActivity.intent(context,linked)', 'android.content.Intent(context, V12ReaderActivity::class.java).putExtra("id", linked.id)')
p.write_text(s, encoding='utf-8')

# Replace dense nutrition composables with explicit implementations to keep Kotlin parsing stable.
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

start = s.index('@Composable private fun V13EditFood')
end = s.index('private fun v13Meal', start)
editor = r'''@Composable
private fun V13EditFood(
    lang: AppLanguage,
    food: V13Food,
    onClose: () -> Unit,
    onSave: (V13Food) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(food.id) { mutableStateOf(food.name) }
    var kcal by remember(food.id) { mutableStateOf(food.kcal.toString()) }
    var protein by remember(food.id) { mutableStateOf(food.p.toString()) }
    var fat by remember(food.id) { mutableStateOf(food.f.toString()) }
    var carbs by remember(food.id) { mutableStateOf(food.c.toString()) }

    fun number(value: String): Double = value.replace(',', '.').toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(v13t(lang, "Продукт на 100 г", "Food per 100 g", "Produkt / 100 g", "Продукт на 100 г"))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(v13t(lang, "Название", "Name", "Nazwa", "Назва")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text("kcal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protein") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fat") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Carbs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        food.copy(
                            name = name.trim(),
                            kcal = number(kcal),
                            p = number(protein),
                            f = number(fat),
                            c = number(carbs)
                        )
                    )
                }
            ) {
                Text(v13t(lang, "Сохранить", "Save", "Zapisz", "Зберегти"))
            }
        },
        dismissButton = {
            Row {
                if (food.name.isNotBlank()) {
                    TextButton(onClick = { onDelete(food.id) }) {
                        Text(v13t(lang, "Удалить", "Delete", "Usuń", "Видалити"), color = NztDanger)
                    }
                }
                TextButton(onClick = onClose) {
                    Text(v13t(lang, "Отмена", "Cancel", "Anuluj", "Скасувати"))
                }
            }
        },
        containerColor = NztSurface
    )
}

'''
s = s[:start] + editor + s[end:]
p.write_text(s, encoding='utf-8')
print('v13 compile fixes applied')
