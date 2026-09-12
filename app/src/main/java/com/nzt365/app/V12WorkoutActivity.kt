package com.nzt365.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

class V12WorkoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val date=intent.getStringExtra("date")?.let(LocalDate::parse)?:LocalDate.now()
        val profile=ProfileStore(this)
        setContent { CompositionLocalProvider(LocalAppLanguage provides profile.language()) { NZTProTheme { V12WorkoutScreen(date){finish()} } } }
    }
}

@Composable
private fun V12WorkoutScreen(date:LocalDate,onClose:()->Unit){
    val context=LocalContext.current
    val lang=LocalAppLanguage.current
    val store=remember{V12WorkoutStore(context)}
    val plan=remember(date){StathamEngine.forDate(date)}
    var tick by remember{mutableIntStateOf(0)}
    var elapsed by remember{mutableIntStateOf(0)}
    var finish by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){while(true){delay(1000);elapsed++}}
    val all=plan.session.exercises.flatMap{ex->(0 until ex.sets.coerceAtLeast(1)).map{i->store.load(date,ex.name,i,v12DefaultTarget(ex.target))}}
    val done=all.count{it.done}; val total=all.size
    val volume=all.filter{it.done}.sumOf(::v12Volume)
    val best=all.filter{it.done}.maxOfOrNull(::v12E1rm)?:0.0
    val progress=if(total==0)0f else done.toFloat()/total

    Scaffold(containerColor=NztBg,contentWindowInsets=WindowInsets.safeDrawing,
        topBar={Surface(color=NztBg){Column{Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onClose){Icon(Icons.Default.Close,null)};Column(Modifier.weight(1f)){Text("NZT PERFORMANCE",color=NztAccent,fontSize=10.sp,fontWeight=FontWeight.Black);Text(plan.session.title,fontSize=20.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)};Surface(color=NztSurface2,shape=RoundedCornerShape(14.dp)){Text(v12Time(elapsed),Modifier.padding(12.dp,8.dp),color=NztAccent,fontWeight=FontWeight.Black)}};LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(4.dp),color=NztAccent,trackColor=NztLine)}}},
        bottomBar={Surface(color=Color(0xFF08121A)){Button(onClick={finish=true},modifier=Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp).height(56.dp),shape=RoundedCornerShape(18.dp)){Icon(Icons.Default.Flag,null);Spacer(Modifier.width(8.dp));Text(v12Text(lang,"ЗАВЕРШИТЬ","FINISH","ZAKOŃCZ","ЗАВЕРШИТИ"),fontWeight=FontWeight.Black)}}}
    ){pad->LazyColumn(Modifier.padding(pad).fillMaxSize(),contentPadding=PaddingValues(14.dp,12.dp,14.dp,24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{V12Summary(done,total,volume,best,lang)}
        item{V12Coach(lang)}
        itemsIndexed(plan.session.exercises){index,ex->V12ExerciseCard(date,index,ex,plan.session,store,tick){tick++}}
    }}

    if(finish) AlertDialog(onDismissRequest={finish=false},containerColor=NztSurface,title={Text(v12Text(lang,"ТРЕНИРОВКА ЗАВЕРШЕНА","WORKOUT COMPLETE","TRENING ZAKOŃCZONY","ТРЕНУВАННЯ ЗАВЕРШЕНО"),fontWeight=FontWeight.Black)},text={Text("$done / $total • ${(progress*100).roundToInt()}%\n${v12Text(lang,"Объём","Volume","Objętość","Обсяг")}: ${volume.roundToInt()} kg")},confirmButton={Button(onClick={plan.session.exercises.forEach{ex->store.finishExercise(ex.name,(0 until ex.sets.coerceAtLeast(1)).map{i->store.load(date,ex.name,i,v12DefaultTarget(ex.target))})};store.markWorkout(date,plan.session.code);onClose()}){Text(v12Text(lang,"ГОТОВО","DONE","GOTOWE","ГОТОВО"))}},dismissButton={TextButton(onClick={finish=false}){Text(v12Text(lang,"НАЗАД","BACK","WSTECZ","НАЗАД"))}})
}

private fun v12Time(sec:Int)=String.format("%02d:%02d",sec/60,sec%60)
