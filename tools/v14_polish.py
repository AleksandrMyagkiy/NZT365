from pathlib import Path

# Reader: horizontal theme selector import used by the new paged reader.
p = Path('app/src/main/java/com/nzt365/app/V12Reader.kt')
s = p.read_text(encoding='utf-8')
if 'import androidx.compose.foundation.horizontalScroll' not in s:
    s = s.replace('import androidx.compose.foundation.clickable\n', 'import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.horizontalScroll\n')
p.write_text(s, encoding='utf-8')

# Workout: make cardio metrics relevant and make every cycling segment visually consistent.
p = Path('app/src/main/java/com/nzt365/app/V13Workout.kt')
s = p.read_text(encoding='utf-8')

old_metrics = '''            item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){V13Metric(v12Text(lang,"ПОДХОДЫ","SETS","SERIE","ПІДХОДИ"),"$done/$total",Modifier.weight(1f));V13Metric(v12Text(lang,"ОБЪЁМ","VOLUME","OBJĘTOŚĆ","ОБСЯГ"),if(vol>0)"${vol.roundToInt()} kg" else "—",Modifier.weight(1f));V13Metric("e1RM",v12Kg(e1),Modifier.weight(1f))}}
'''
new_metrics = '''            item{
                if(v13SessionIsCardio(plan.session)){
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        V13Metric(v12Text(lang,"ЭТАПЫ","STAGES","ETAPY","ЕТАПИ"),"$done/$total",Modifier.weight(1f))
                        V13Metric(v12Text(lang,"ВРЕМЯ","TIME","CZAS","ЧАС"),v13time(elapsed),Modifier.weight(1f))
                        V13Metric(v12Text(lang,"ПЛАН","PLAN","PLAN","ПЛАН"),"${plan.session.minutes} min",Modifier.weight(1f))
                    }
                }else{
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        V13Metric(v12Text(lang,"ПОДХОДЫ","SETS","SERIE","ПІДХОДИ"),"$done/$total",Modifier.weight(1f))
                        V13Metric(v12Text(lang,"ОБЪЁМ","VOLUME","OBJĘTOŚĆ","ОБСЯГ"),if(vol>0)"${vol.roundToInt()} kg" else "—",Modifier.weight(1f))
                        V13Metric("e1RM",v12Kg(e1),Modifier.weight(1f))
                    }
                }
            }
'''
if old_metrics not in s:
    raise SystemExit('v14: workout metrics anchor not found')
s = s.replace(old_metrics, new_metrics, 1)

old_visual_call = '    val visual=v13Visual(ex.name,cardio)\n'
if old_visual_call not in s:
    raise SystemExit('v14: visual call anchor not found')
s = s.replace(old_visual_call, '    val visual=v13Visual(ex.name,session,cardio)\n', 1)

start = s.index('private data class V13Visual')
end = s.index('@Composable private fun V13SetRow', start)
visual_block = '''private data class V13Visual(val label:String,val icon:ImageVector,val color:Color)

private fun v13IsBikeSession(session:StathamSession):Boolean{
    val code=session.code.uppercase()
    val title=session.title.lowercase()
    val type=session.type.lowercase()
    return code.startsWith("BIKE") || "велосип" in title || "велосип" in type || "cycling" in title || "cycling" in type || "rower" in title || "rower" in type
}

private fun v13SessionIsCardio(session:StathamSession):Boolean{
    val type=session.type.lowercase()
    return v13IsBikeSession(session) || "кардио" in type || "cardio" in type || "біг" in type || "бег" in type || "run" in type
}

private fun v13Visual(name:String,session:StathamSession,cardio:Boolean):V13Visual{
    val s=name.lowercase()
    val bike=v13IsBikeSession(session)
    return when{
        bike -> V13Visual("CYCLING",Icons.Default.PedalBike,Color(0xFF74E6C4))
        cardio && listOf("ходь","walk","прогул").any{it in s} -> V13Visual("WALK",Icons.Default.DirectionsWalk,Color(0xFF7DD3FC))
        cardio -> V13Visual("CARDIO",Icons.Default.DirectionsRun,Color(0xFF74E6C4))
        listOf("размин","замин","warm","cooldown","mobility","мобиль","растяж","дыхание").any{it in s} -> V13Visual("MOBILITY",Icons.Default.SelfImprovement,Color(0xFF7FE3C3))
        listOf("подтяг","тяга","row","pull").any{it in s}->V13Visual("PULL",Icons.Default.FitnessCenter,Color(0xFF78BFFF))
        listOf("отжим","жим","push","брусь","dip").any{it in s}->V13Visual("PUSH",Icons.Default.FitnessCenter,Color(0xFFFF8BA7))
        listOf("присед","выпад","ног","румын","мост","squat","lunge").any{it in s}->V13Visual("LOWER",Icons.Default.FitnessCenter,Color(0xFFFFC857))
        listOf("планк","пресс","core","dead bug","ролик","hollow").any{it in s}->V13Visual("CORE",Icons.Default.SelfImprovement,Color(0xFFC4A7FF))
        else->V13Visual("STRENGTH",Icons.Default.FitnessCenter,NztAccent)
    }
}

'''
s = s[:start] + visual_block + s[end:]
p.write_text(s, encoding='utf-8')
print('NZT v14 polish applied')
