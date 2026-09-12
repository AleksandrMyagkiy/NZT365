package com.nzt365.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.math.roundToInt

enum class V12ReaderTheme { PAPER, WHITE, SEPIA, NIGHT, OLED }
private data class V12Colors(val outer:Color,val page:Color,val text:Color,val muted:Color)
private fun V12ReaderTheme.colors()=when(this){
    V12ReaderTheme.PAPER->V12Colors(Color(0xFFE9E3D7),Color(0xFFFFFCF4),Color(0xFF28231F),Color(0xFF766F67))
    V12ReaderTheme.WHITE->V12Colors(Color(0xFFE9EDF0),Color.White,Color(0xFF161616),Color(0xFF666666))
    V12ReaderTheme.SEPIA->V12Colors(Color(0xFFD9C7A7),Color(0xFFF5E4C1),Color(0xFF33281D),Color(0xFF75624D))
    V12ReaderTheme.NIGHT->V12Colors(Color(0xFF071018),Color(0xFF111B22),Color(0xFFE9EDF0),Color(0xFF89949B))
    V12ReaderTheme.OLED->V12Colors(Color.Black,Color.Black,Color(0xFFECECEC),Color(0xFF888888))
}

class V12ReaderActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val lib=V10Library(this); val id=intent.getStringExtra("id")?:return finish(); val b=lib.books().firstOrNull{it.id==id}?:return finish()
        setContent{V12Reader(b,lib){finish()}}
    }
}

@Composable
private fun V12Reader(initial:V10Book,lib:V10Library,onClose:()->Unit){
    val context=LocalContext.current
    val prefs=remember{context.getSharedPreferences("nzt_reader_v12",android.content.Context.MODE_PRIVATE)}
    var theme by remember{mutableStateOf(runCatching{V12ReaderTheme.valueOf(prefs.getString("theme","PAPER")!!)}.getOrDefault(V12ReaderTheme.PAPER))}
    var font by remember{mutableIntStateOf(prefs.getInt("font",20))}
    var line by remember{mutableFloatStateOf(prefs.getFloat("line",1.55f))}
    var margin by remember{mutableIntStateOf(prefs.getInt("margin",22))}
    var text by remember{mutableStateOf("")}; var ready by remember{mutableStateOf(false)}
    var controls by remember{mutableStateOf(true)}; var settings by remember{mutableStateOf(false)}
    val scroll=rememberScrollState(); val c=theme.colors()

    LaunchedEffect(initial.path){ text=loadV10Text(File(initial.path),initial.type); ready=true }
    LaunchedEffect(ready){ if(ready){ kotlinx.coroutines.delay(120); scroll.scrollTo(initial.position.coerceAtLeast(0).coerceAtMost(scroll.maxValue)) } }
    LaunchedEffect(scroll,ready){
        if(ready) snapshotFlow{Pair(scroll.value,scroll.maxValue)}.collect{(pos,max)->
            val pct=if(max<=0)0 else (pos*100f/max).roundToInt().coerceIn(0,100)
            lib.update(initial.copy(position=pos,progress=pct))
        }
    }
    DisposableEffect(theme,font,line,margin){onDispose{prefs.edit().putString("theme",theme.name).putInt("font",font).putFloat("line",line).putInt("margin",margin).apply()}}

    val pct=if(scroll.maxValue<=0)0 else (scroll.value*100f/scroll.maxValue).roundToInt().coerceIn(0,100)
    Scaffold(containerColor=c.outer,contentWindowInsets=WindowInsets.safeDrawing,
        topBar={if(controls)Surface(color=c.outer){Row(Modifier.fillMaxWidth().padding(horizontal=6.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onClose){Icon(Icons.Default.ArrowBack,null,tint=c.text)};Column(Modifier.weight(1f)){Text(initial.title,color=c.text,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("$pct%",color=c.muted,fontSize=10.sp)};IconButton(onClick={settings=true}){Icon(Icons.Default.Tune,null,tint=c.text)}}}},
        bottomBar={if(controls)Surface(color=c.outer){Column(Modifier.navigationBarsPadding().padding(horizontal=16.dp,vertical=8.dp)){LinearProgressIndicator(progress={pct/100f},modifier=Modifier.fillMaxWidth().height(5.dp),color=NztAccent,trackColor=c.muted.copy(alpha=.25f));Spacer(Modifier.height(5.dp));Text("$pct%  •  ${if(scroll.maxValue>0)"≈ ${((100-pct)*1.8).roundToInt()} мин" else ""}",color=c.muted,fontSize=10.sp)}}}
    ){pad->
        Box(Modifier.padding(pad).fillMaxSize().background(c.outer)){
            if(!ready) CircularProgressIndicator(Modifier.align(Alignment.Center),color=NztAccent)
            else Surface(Modifier.fillMaxSize().padding(horizontal=8.dp,vertical=4.dp),color=c.page,shape=RoundedCornerShape(if(theme==V12ReaderTheme.OLED)0.dp else 6.dp)){
                Box{
                    Text(text,Modifier.fillMaxWidth().verticalScroll(scroll).padding(horizontal=margin.dp,vertical=24.dp),color=c.text,fontSize=font.sp,lineHeight=(font*line).sp,fontFamily=FontFamily.Serif,textAlign=TextAlign.Justify)
                    TextButton(onClick={controls=!controls},modifier=Modifier.align(Alignment.TopEnd).padding(2.dp)){Text(if(controls)"◫" else "☰",color=c.muted.copy(alpha=.25f))}
                }
            }
        }
    }

    if(settings)ModalBottomSheet(onDismissRequest={settings=false},containerColor=NztSurface){Column(Modifier.fillMaxWidth().padding(18.dp).navigationBarsPadding()){
        Text("NZT READER · ВИД ЧТЕНИЯ",color=NztAccent,fontWeight=FontWeight.Black,fontSize=11.sp);Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){V12ReaderTheme.entries.forEach{t->FilterChip(selected=theme==t,onClick={theme=t},label={Text(when(t){V12ReaderTheme.PAPER->"Книга";V12ReaderTheme.WHITE->"Белый";V12ReaderTheme.SEPIA->"Сепия";V12ReaderTheme.NIGHT->"Ночь";V12ReaderTheme.OLED->"OLED"},fontSize=9.sp)})}}
        Text("Шрифт $font",fontWeight=FontWeight.Bold);Slider(font.toFloat(),{font=it.roundToInt()},valueRange=14f..32f)
        Text("Интервал ${"%.1f".format(line)}",fontWeight=FontWeight.Bold);Slider(line,{line=it},valueRange=1.2f..2.0f)
        Text("Поля $margin",fontWeight=FontWeight.Bold);Slider(margin.toFloat(),{margin=it.roundToInt()},valueRange=12f..42f)
        Text("Непрерывный книжный поток: строки больше не теряются между экранами.",color=NztMuted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))
    }}
}
