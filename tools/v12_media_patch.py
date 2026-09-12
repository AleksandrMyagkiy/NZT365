from pathlib import Path
p=Path('app/src/main/java/com/nzt365/app/V3ExerciseMedia.kt')
s=p.read_text()
s=s.replace('listOf("велосип", "bike", "cycling", "cadence")','listOf("велосип", "bike", "cycling", "cadence", "поездк", "z2", "педал", "отрезок", "интервал", "ускорен", "заминк", "лёгким ходом", "легким ходом")')
block='''        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            color = Color(0xBB071018),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                media.label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = NztAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )
        }
'''
if block in s:s=s.replace(block,'',1)
marker='\n@Composable\nfun ExercisePhoto'
if 'fun exerciseMediaLabel' not in s:s=s.replace(marker,'\nfun exerciseMediaLabel(name: String): String = mediaFor(name).label\n'+marker,1)
p.write_text(s)
p=Path('app/src/main/java/com/nzt365/app/V7Product.kt')
s=p.read_text().replace('ExercisePhoto(plan.session.exercises.firstOrNull()?.name ?: plan.session.title, Modifier.fillMaxWidth().height(220.dp))','ExercisePhoto(plan.session.title, Modifier.fillMaxWidth().height(220.dp))')
p.write_text(s)
p=Path('app/src/main/java/com/nzt365/app/V9PerformanceHub.kt')
s=p.read_text().replace('Text("V9", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = NztAccent, fontWeight = FontWeight.Black)','Text("v${BuildConfig.VERSION_NAME}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = NztAccent, fontWeight = FontWeight.Black)')
p.write_text(s)
print('media patched')
