from pathlib import Path

# Route the product shell directly to the newest modules and polish the home card.
p = Path('app/src/main/java/com/nzt365/app/V7Product.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('V10BooksScreen(lang, Modifier.fillMaxSize())', 'V13BooksScreen(lang, Modifier.fillMaxSize())')
s = s.replace('V7WorkoutActivity::class.java', 'V13WorkoutActivity::class.java')
s = s.replace('ProNutritionActivity::class.java', 'V13NutritionActivity::class.java')
s = s.replace('Text("TODAY\'S SESSION", color = NztAccent, fontWeight = FontWeight.Black, fontSize = 10.sp)', 'Text(v7Text(lang, "СЕГОДНЯШНЯЯ ТРЕНИРОВКА", "TODAY\'S SESSION", "DZISIEJSZY TRENING", "СЬОГОДНІШНЄ ТРЕНУВАННЯ"), color = NztAccent, fontWeight = FontWeight.Black, fontSize = 10.sp)')
s = s.replace('Text("${plan.session.minutes} min • ${plan.session.exercises.size} exercises", color = Color.White.copy(alpha = .78f))', 'Text("${plan.session.minutes} ${v7Text(lang, "мин", "min", "min", "хв")} • ${plan.session.exercises.size} ${v7Text(lang, "упражнения", "exercises", "ćwiczenia", "вправи")}", color = Color.White.copy(alpha = .78f))')
p.write_text(s, encoding='utf-8')

# Make workout visuals contextual to the whole session instead of guessing from the exercise title alone.
p = Path('app/src/main/java/com/nzt365/app/V13Workout.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('val visual=v13Visual(ex.name,cardio)', 'val visual=v13Visual(ex.name,cardio,session.type)')
old = '''private fun v13Visual(name:String,cardio:Boolean):V13Visual{val s=name.lowercase();return when{cardio&&("велосип" in s||"z2" in s||"поезд" in s)->V13Visual("CYCLING",Icons.Default.PedalBike,Color(0xFF74E6C4));cardio->V13Visual("CARDIO",Icons.Default.DirectionsRun,Color(0xFF74E6C4));listOf("подтяг","тяга","row","pull").any{it in s}->V13Visual("PULL",Icons.Default.FitnessCenter,Color(0xFF78BFFF));listOf("отжим","жим","push","брусь").any{it in s}->V13Visual("PUSH",Icons.Default.FitnessCenter,Color(0xFFFF8BA7));listOf("присед","выпад","ног","румын","мост","squat").any{it in s}->V13Visual("LOWER",Icons.Default.DirectionsWalk,Color(0xFFFFC857));listOf("планк","press","core","dead bug","ролик").any{it in s}->V13Visual("CORE",Icons.Default.SelfImprovement,Color(0xFFC4A7FF));else->V13Visual("STRENGTH",Icons.Default.FitnessCenter,NztAccent)}}'''
new = '''private fun v13Visual(name:String,cardio:Boolean,sessionType:String):V13Visual{
    val s=name.lowercase();val type=sessionType.lowercase()
    return when{
        "велосип" in type || listOf("велосип","z2","поезд","педал","каденс").any{it in s} -> V13Visual("CYCLING",Icons.Default.PedalBike,Color(0xFF74E6C4))
        "восстанов" in type || listOf("размин","замин","мобиль","растяж","дыхание").any{it in s} -> V13Visual("RECOVERY",Icons.Default.SelfImprovement,Color(0xFF74E6C4))
        cardio -> V13Visual("CARDIO",Icons.Default.DirectionsRun,Color(0xFF74E6C4))
        listOf("подтяг","тяга","row","pull").any{it in s} -> V13Visual("PULL",Icons.Default.FitnessCenter,Color(0xFF78BFFF))
        listOf("отжим","жим","push","брусь").any{it in s} -> V13Visual("PUSH",Icons.Default.FitnessCenter,Color(0xFFFF8BA7))
        listOf("присед","выпад","ног","румын","мост","squat").any{it in s} -> V13Visual("LOWER",Icons.Default.DirectionsWalk,Color(0xFFFFC857))
        listOf("планк","press","core","dead bug","ролик","bird-dog","hollow").any{it in s} -> V13Visual("CORE",Icons.Default.SelfImprovement,Color(0xFFC4A7FF))
        else -> V13Visual("STRENGTH",Icons.Default.FitnessCenter,NztAccent)
    }
}'''
if old in s:
    s = s.replace(old, new)
p.write_text(s, encoding='utf-8')

# Keep the helper API scoped to the module because V10Book is internal.
p = Path('app/src/main/java/com/nzt365/app/V12Reader.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('fun intent(context: Context, book: V10Book)', 'internal fun intent(context: Context, book: V10Book)')
p.write_text(s, encoding='utf-8')

print('v14 product polish applied')
