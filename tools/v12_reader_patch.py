from pathlib import Path
import re
p=Path('app/src/main/java/com/nzt365/app/V11Reader.kt')
s=p.read_text()
s=s.replace('import androidx.compose.foundation.rememberScrollState\n','').replace('import androidx.compose.foundation.verticalScroll\n','')
s=s.replace('import androidx.compose.ui.platform.LocalContext\n','import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.platform.LocalDensity\n')
s=s.replace('import androidx.compose.ui.text.font.FontFamily\n','import androidx.compose.ui.text.AnnotatedString\nimport androidx.compose.ui.text.TextMeasurer\nimport androidx.compose.ui.text.TextStyle\nimport androidx.compose.ui.text.font.FontFamily\nimport androidx.compose.ui.text.rememberTextMeasurer\n')
s=s.replace('import androidx.compose.ui.unit.dp\n','import androidx.compose.ui.unit.Constraints\nimport androidx.compose.ui.unit.dp\n')
old='''    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(initial.path) {
        text = loadV10Text(File(initial.path), initial.type)
        loading = false
    }

    val charsPerPage = remember(font, margin, line) {
        (3500f * (18f / font.coerceAtLeast(12)) * (1.55f / line.coerceAtLeast(1.15f)) * (26f / margin.coerceAtLeast(12))).roundToInt().coerceIn(1200, 5200)
    }
    val pages = remember(text, charsPerPage) { smartPages(text, charsPerPage) }
    var page by remember(pages.size) { mutableIntStateOf(initial.position.coerceIn(0, (pages.size - 1).coerceAtLeast(0))) }
    val palette = theme.palette()
'''
new='''    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    val textMeasurer = rememberTextMeasurer()
    var pages by remember { mutableStateOf(listOf("")) }
    var page by remember { mutableIntStateOf(initial.position.coerceAtLeast(0)) }

    LaunchedEffect(initial.path) {
        text = loadV10Text(File(initial.path), initial.type)
        loading = false
    }

    val palette = theme.palette()
'''
assert old in s
s=s.replace(old,new,1)
old='''.verticalScroll(rememberScrollState())
                                .padding(horizontal = margin.dp, vertical = 20.dp),
                            color = palette.text,
                            fontSize = font.sp,
                            lineHeight = (font * line).sp,
                            fontFamily = FontFamily.Serif,
                            textAlign = if (justify) TextAlign.Justify else TextAlign.Start
'''
new='''.padding(horizontal = margin.dp, vertical = 20.dp),
                            color = palette.text,
                            fontSize = font.sp,
                            lineHeight = (font * line).sp,
                            fontFamily = FontFamily.Serif,
                            textAlign = if (justify) TextAlign.Justify else TextAlign.Start,
                            overflow = TextOverflow.Clip
'''
assert old in s
s=s.replace(old,new,1)
# Replace character pagination with a conservative rendered-height-safe pager.
start=s.index('private fun smartPages(')
s=s[:start]+'''private fun smartPages(text: String, target: Int): List<String> {
    if (text.isBlank()) return listOf("")
    val clean=text.replace("\\r\\n","\\n").replace(Regex("\\n{4,}"),"\\n\\n")
    // V12 deliberately uses smaller non-overflowing pages. The page itself no longer scrolls,
    // so no hidden text can be skipped between page N and page N+1.
    val safe=(target*0.58f).roundToInt().coerceIn(700,2600)
    val out=mutableListOf<String>(); var pos=0
    while(pos<clean.length){
        var end=(pos+safe).coerceAtMost(clean.length)
        if(end<clean.length){
            val floor=(pos+safe*0.72f).roundToInt()
            val cuts=listOf(clean.lastIndexOf("\\n\\n",end),clean.lastIndexOf(". ",end),clean.lastIndexOf("! ",end),clean.lastIndexOf("? ",end),clean.lastIndexOf(" ",end))
            cuts.filter{it>=floor}.maxOrNull()?.let{end=it+1}
        }
        out+=clean.substring(pos,end).trim()
        pos=end
        while(pos<clean.length&&clean[pos].isWhitespace())pos++
    }
    return out.ifEmpty{listOf("")}
}
'''
p.write_text(s)
print('reader patched')
