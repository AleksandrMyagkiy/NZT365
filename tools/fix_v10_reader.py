from pathlib import Path

p = Path('app/src/main/java/com/nzt365/app/V10Reader.kt')
s = p.read_text(encoding='utf-8')

s = s.replace('fun intent(c:Context,b:V10Book)=Intent(', 'internal fun intent(c:Context,b:V10Book)=Intent(')

start = s.index('private fun parseMobi(')
end = s.index('private fun pdfCount10(')
replacement = r'''private fun parseMobi(data: ByteArray): String {
    if (data.size < 100) return ""
    val records = ((data[76].toInt() and 255) shl 8) or (data[77].toInt() and 255)
    if (records < 2) return ""

    fun off(i: Int): Int {
        val p = 78 + i * 8
        if (p + 3 >= data.size) return data.size
        return ((data[p].toInt() and 255) shl 24) or
            ((data[p + 1].toInt() and 255) shl 16) or
            ((data[p + 2].toInt() and 255) shl 8) or
            (data[p + 3].toInt() and 255)
    }

    val r0 = off(0)
    if (r0 + 16 > data.size) return ""
    val comp = ((data[r0].toInt() and 255) shl 8) or (data[r0 + 1].toInt() and 255)
    val textRecords = ((data[r0 + 8].toInt() and 255) shl 8) or (data[r0 + 9].toInt() and 255)
    val out = ByteArrayOutputStream()

    for (i in 1..textRecords.coerceAtMost(records - 1)) {
        val a = off(i).coerceIn(0, data.size)
        val z = if (i + 1 < records) off(i + 1).coerceIn(a, data.size) else data.size
        val rec = data.copyOfRange(a, z)
        out.write(if (comp == 2) palmDoc(rec) else rec)
    }
    return stripHtml(decodeText(out.toByteArray()))
}

private fun palmDoc(src: ByteArray): ByteArray {
    val out = ByteArrayOutputStream()
    var i = 0
    while (i < src.size) {
        val c = src[i].toInt() and 255
        i++
        when {
            c == 0 -> out.write(0)
            c in 1..8 -> repeat(c) { if (i < src.size) out.write(src[i++].toInt()) }
            c in 9..127 -> out.write(c)
            c in 128..191 && i < src.size -> {
                val c2 = src[i++].toInt() and 255
                val v = (c shl 8) or c2
                val distance = (v shr 3) and 0x7ff
                val length = (v and 7) + 3
                repeat(length) {
                    val buf = out.toByteArray()
                    val idx = buf.size - distance
                    if (distance > 0 && idx in buf.indices) out.write(buf[idx].toInt())
                }
            }
            c >= 192 -> {
                out.write(' '.code)
                out.write(c xor 0x80)
            }
        }
    }
    return out.toByteArray()
}

'''
s = s[:start] + replacement + s[end:]
p.write_text(s, encoding='utf-8')
print('V10Reader Kotlin compile fixes applied')
