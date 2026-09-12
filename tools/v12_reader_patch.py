from pathlib import Path
p=Path('app/src/main/java/com/nzt365/app/V11Reader.kt')
s=p.read_text()
s=s.replace('import androidx.compose.foundation.rememberScrollState\n','').replace('import androidx.compose.foundation.verticalScroll\n','')
s=s.replace('import androidx.compose.ui.platform.LocalContext\n','import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.platform.LocalConfiguration\n')
old='''    val charsPerPage = remember(font, margin, line) {
        (3500f * (18f / font.coerceAtLeast(12)) * (1.55f / line.coerceAtLeast(1.15f)) * (26f / margin.coerceAtLeast(12))).roundToInt().coerceIn(1200, 5200)
    }
'''
new='''    val config = LocalConfiguration.current
    val charsPerPage = remember(font, margin, line, config.screenWidthDp, config.screenHeightDp) {
        val usableWidth = (config.screenWidthDp - margin * 2 - 28).coerceAtLeast(160)
        val usableHeight = (config.screenHeightDp - 210).coerceAtLeast(260)
        val charsPerLine = usableWidth / (font.coerceAtLeast(12) * 0.58f)
        val visibleLines = usableHeight / (font.coerceAtLeast(12) * line.coerceAtLeast(1.15f))
        (charsPerLine * visibleLines * 0.88f).roundToInt().coerceIn(260, 2200)
    }
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
# Keep every character exactly once; only move the break backward to a natural boundary.
start=s.index('private fun smartPages(')
s=s[:start]+'''private fun smartPages(text: String, target: Int): List<String> {
    if (text.isBlank()) return listOf("")
    val clean=text.replace("\\r\\n","\\n").replace(Regex("\\n{4,}"),"\\n\\n")
    val out=mutableListOf<String>(); var pos=0
    while(pos<clean.length){
        var end=(pos+target).coerceAtMost(clean.length)
        if(end<clean.length){
            val floor=(pos+target*0.72f).roundToInt()
            val cuts=listOf(clean.lastIndexOf("\\n\\n",end),clean.lastIndexOf(". ",end),clean.lastIndexOf("! ",end),clean.lastIndexOf("? ",end),clean.lastIndexOf(" ",end))
            cuts.filter{it>=floor}.maxOrNull()?.let{end=it+1}
        }
        out+=clean.substring(pos,end).trimEnd()
        pos=end
        while(pos<clean.length&&clean[pos].isWhitespace())pos++
    }
    return out.ifEmpty{listOf("")}
}
'''
p.write_text(s)
print('reader patched')
