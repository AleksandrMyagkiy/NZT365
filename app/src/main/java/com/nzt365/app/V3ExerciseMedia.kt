package com.nzt365.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ExerciseMedia(val label:String,val icon:ImageVector,val accent:Color)

private fun mediaFor(name:String):ExerciseMedia{
    val s=name.lowercase()
    return when{
        listOf("велосип","поездк","z2","педал","отрезок","интервал","ускорен","заминк","лёгким ходом","легким ходом","bike","cycling","cadence").any{it in s}->ExerciseMedia("CYCLING",Icons.Default.PedalBike,Color(0xFF79E6C5))
        listOf("ходь","прогул","walk").any{it in s}->ExerciseMedia("WALK / RECOVERY",Icons.Default.DirectionsWalk,Color(0xFF7DD3FC))
        listOf("бег","run","strides").any{it in s}->ExerciseMedia("RUNNING",Icons.Default.DirectionsRun,Color(0xFFFFB86B))
        listOf("подтяг","pull-up","pullup","chin-up","тяга","row").any{it in s}->ExerciseMedia("PULL",Icons.Default.FitnessCenter,Color(0xFF81C7FF))
        listOf("отжим","push-up","брусь","dip","жим").any{it in s}->ExerciseMedia("PUSH",Icons.Default.FitnessCenter,Color(0xFFFF8BA7))
        listOf("присед","выпад","squat","lunge","ягод","румын","deadlift","носок","ног").any{it in s}->ExerciseMedia("LOWER BODY",Icons.Default.FitnessCenter,Color(0xFFFFC857))
        listOf("планк","пресс","dead bug","bird-dog","ролик","core","hollow").any{it in s}->ExerciseMedia("CORE",Icons.Default.SelfImprovement,Color(0xFFC4A7FF))
        listOf("размин","warm","mobility","мобиль","stretch","дыхание","растяж").any{it in s}->ExerciseMedia("MOBILITY",Icons.Default.SelfImprovement,Color(0xFF7FE3C3))
        else->ExerciseMedia("STRENGTH",Icons.Default.FitnessCenter,NztAccent)
    }
}

fun exerciseMediaLabel(name:String):String=mediaFor(name).label

@Composable
fun ExercisePhoto(name:String,modifier:Modifier=Modifier){ ExerciseVisual(mediaFor(name),modifier,RoundedCornerShape(22.dp),true) }

@Composable
fun ExerciseThumbnail(name:String,modifier:Modifier=Modifier){ ExerciseVisual(mediaFor(name),modifier,RoundedCornerShape(18.dp),false) }

@Composable
private fun ExerciseVisual(media:ExerciseMedia,modifier:Modifier,shape:RoundedCornerShape,large:Boolean){
    Box(
        modifier.clip(shape).background(
            Brush.linearGradient(listOf(Color(0xFF102735),Color(0xFF09141D),media.accent.copy(alpha=.16f)))
        ),
        contentAlignment=Alignment.Center
    ){
        Surface(color=Color.White.copy(alpha=.05f),shape=RoundedCornerShape(if(large)30.dp else 20.dp)){
            Box(Modifier.size(if(large)126.dp else 70.dp),contentAlignment=Alignment.Center){
                Icon(media.icon,null,tint=media.accent,modifier=Modifier.size(if(large)70.dp else 40.dp))
            }
        }
        if(large){
            Text(media.label,modifier=Modifier.align(Alignment.BottomStart).padding(16.dp),color=media.accent,fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=1.1.sp)
        }
    }
}
