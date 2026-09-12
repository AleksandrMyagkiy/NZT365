from pathlib import Path

p = Path('app/src/main/java/com/nzt365/app/V10Reader.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('context.startActivity(V10ReaderActivity.intent(context,book))', 'context.startActivity(if(book.type in setOf("PDF","CBZ","CBR","DJVU")) V10ReaderActivity.intent(context,book) else V11ReaderActivity.intent(context,book))')
s = s.replace('private fun loadV10Text(file:File,type:String):String', 'internal fun loadV10Text(file:File,type:String):String')
p.write_text(s, encoding='utf-8')
