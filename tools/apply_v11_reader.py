from pathlib import Path

p = Path('app/src/main/java/com/nzt365/app/V7Workout.kt')
s = p.read_text(encoding='utf-8')

old = '''    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(26.dp)) {
        Column {
            Box {
                ExercisePhoto(exercise.name, Modifier.fillMaxWidth().height(210.dp))
                Surface(
                    Modifier.align(Alignment.TopEnd).padding(12.dp),
                    color = if (isPr) NztAccent else Color(0xCC10202B),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (isPr) "NEW PR" else "${states.count { it.done }}/$setCount",
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = if (isPr) Color.Black else NztText,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text("${index + 1}. ${exercise.name}", fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("${exercise.target} • ${exercise.restSeconds}s rest", color = NztMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
'''

new = '''    Card(colors = CardDefaults.cardColors(containerColor = NztSurface), shape = RoundedCornerShape(24.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(18.dp), modifier = Modifier.size(92.dp)) {
                    ExerciseThumbnail(exercise.name, Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("УПРАЖНЕНИЕ ${index + 1}", color = NztAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(exercise.name, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    Text("${exercise.target} • отдых ${exercise.restSeconds} с", color = NztMuted, fontSize = 11.sp)
                }
                Surface(
                    color = if (isPr) NztAccent else NztSurface2,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (isPr) "PR" else "${states.count { it.done }}/$setCount",
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = if (isPr) Color.Black else NztText,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
            HorizontalDivider(color = NztLine.copy(alpha = .65f))
            Column(Modifier.padding(14.dp)) {
'''

if old not in s:
    raise SystemExit('Workout card block not found')
s = s.replace(old, new, 1)

s = s.replace('Text("Use clean form. Log every set. Keep 1–3 RIR on compounds and progress only after completing the prescribed work.", color = NztMuted, fontSize = 12.sp, lineHeight = 17.sp)',
              'Text("Чистая техника, каждый подход в журнале и 1–3 RIR в базовых упражнениях. Прогрессируй только после качественного выполнения плана.", color = NztMuted, fontSize = 12.sp, lineHeight = 17.sp)')
s = s.replace('Text("REST")', 'Text("ОТДЫХ")')
s = s.replace('Text("FINISH", fontWeight = FontWeight.Black)', 'Text("ЗАВЕРШИТЬ", fontWeight = FontWeight.Black)')
s = s.replace('Text("REST TIMER", color = NztMuted, fontWeight = FontWeight.Bold)', 'Text("ТАЙМЕР ОТДЫХА", color = NztMuted, fontWeight = FontWeight.Bold)')
s = s.replace('Button(onClick = { showRest = false }, modifier = Modifier.weight(1f)) { Text("SKIP") }', 'Button(onClick = { showRest = false }, modifier = Modifier.weight(1f)) { Text("ПРОПУСТИТЬ") }')
s = s.replace('title = { Text("WORKOUT COMPLETE", fontWeight = FontWeight.Black) }', 'title = { Text("ТРЕНИРОВКА ЗАВЕРШЕНА", fontWeight = FontWeight.Black) }')
s = s.replace('Text("Volume: ${volume.roundToInt()} kg")', 'Text("Объём: ${volume.roundToInt()} kg")')
s = s.replace('Text("Best estimated 1RM: ${e1rm.roundToInt()} kg")', 'Text("Лучший расчётный 1RM: ${e1rm.roundToInt()} kg")')
s = s.replace('Text("Previous set values and personal records will be updated.", color = NztMuted)', 'Text("Результаты подходов и личные рекорды будут сохранены.", color = NztMuted)')
s = s.replace('}) { Text("DONE") }', '}) { Text("ГОТОВО") }')
s = s.replace('dismissButton = { TextButton(onClick = { showFinish = false }) { Text("BACK") } }', 'dismissButton = { TextButton(onClick = { showFinish = false }) { Text("НАЗАД") } }')
s = s.replace('if (done.isEmpty()) return "NEXT: complete the prescribed work with clean technique"', 'if (done.isEmpty()) return "ДАЛЬШЕ: выполни назначенный объём с чистой техникой"')
s = s.replace('"NEXT: increase load 2–5% or choose a harder variation"', '"ДАЛЬШЕ: увеличь нагрузку на 2–5% или выбери более сложный вариант"')
s = s.replace('"NEXT: keep load and add reps before increasing weight"', '"ДАЛЬШЕ: сохрани вес и сначала добавь повторения"')
s = s.replace('"NEXT: keep or reduce load; protect technique and recovery"', '"ДАЛЬШЕ: сохрани или снизь нагрузку, приоритет — техника и восстановление"')
s = s.replace('"NEXT: complete all work sets before progressing"', '"ДАЛЬШЕ: выполни все рабочие подходы перед прогрессией"')

p.write_text(s, encoding='utf-8')
