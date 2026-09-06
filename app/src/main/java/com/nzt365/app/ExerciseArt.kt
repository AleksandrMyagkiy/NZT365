package com.nzt365.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val ArtLime = Color(0xFFE9FF70)
private val ArtWhite = Color(0xFFE8EDF3)
private val ArtMuted = Color(0xFF6D7A88)
private val ArtBg = Color(0xFF0D1722)

enum class ExerciseArtType { PUSH, PULL, SQUAT, LUNGE, HINGE, CORE, PLANK, CURL, TRICEPS, BIKE, RUN, MOBILITY, CALF, GENERIC }

fun artTypeFor(name: String): ExerciseArtType {
    val s = name.lowercase()
    return when {
        listOf("велосип", "bike", "cycling", "rower").any { it in s } -> ExerciseArtType.BIKE
        listOf("бег", "run", "biegan").any { it in s } -> ExerciseArtType.RUN
        listOf("подтяг", "тяга сверху", "pull-up", "pullup", "drąż").any { it in s } -> ExerciseArtType.PULL
        listOf("отжим", "жим", "push", "pike").any { it in s } -> ExerciseArtType.PUSH
        listOf("сплит", "выпад", "lunge", "split").any { it in s } -> ExerciseArtType.LUNGE
        listOf("присед", "squat", "przys").any { it in s } -> ExerciseArtType.SQUAT
        listOf("румын", "good morning", "hinge", "тяга с резинкой к поясу", "тяга резинки одной рукой", "row").any { it in s } -> ExerciseArtType.HINGE
        listOf("планк", "plank").any { it in s } -> ExerciseArtType.PLANK
        listOf("dead bug", "bird-dog", "ролик", "пресс", "core").any { it in s } -> ExerciseArtType.CORE
        listOf("сгибание рук", "curl", "biceps").any { it in s } -> ExerciseArtType.CURL
        listOf("разгибание рук", "triceps").any { it in s } -> ExerciseArtType.TRICEPS
        listOf("икр", "носок", "calf").any { it in s } -> ExerciseArtType.CALF
        listOf("размин", "мобил", "mobility", "stretch").any { it in s } -> ExerciseArtType.MOBILITY
        else -> ExerciseArtType.GENERIC
    }
}

@Composable
fun ExerciseIllustration(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(ArtBg, RoundedCornerShape(24.dp))
            .padding(10.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawGrid()
            when (artTypeFor(name)) {
                ExerciseArtType.PUSH -> drawPush()
                ExerciseArtType.PULL -> drawPull()
                ExerciseArtType.SQUAT -> drawSquat()
                ExerciseArtType.LUNGE -> drawLunge()
                ExerciseArtType.HINGE -> drawHinge()
                ExerciseArtType.CORE -> drawCore()
                ExerciseArtType.PLANK -> drawPlank()
                ExerciseArtType.CURL -> drawCurl()
                ExerciseArtType.TRICEPS -> drawTriceps()
                ExerciseArtType.BIKE -> drawBike()
                ExerciseArtType.RUN -> drawRun()
                ExerciseArtType.MOBILITY -> drawMobility()
                ExerciseArtType.CALF -> drawCalf()
                ExerciseArtType.GENERIC -> drawGeneric()
            }
        }
    }
}

private fun DrawScope.drawGrid() {
    val step = size.width / 8f
    for (i in 1..7) drawLine(ArtMuted.copy(alpha = .08f), Offset(step*i, 0f), Offset(step*i, size.height), 1f)
    val stepY = size.height / 6f
    for (i in 1..5) drawLine(ArtMuted.copy(alpha = .08f), Offset(0f, stepY*i), Offset(size.width, stepY*i), 1f)
}

private fun DrawScope.joint(p: Offset, r: Float = size.minDimension * .035f, color: Color = ArtWhite) = drawCircle(color, r, p)
private fun DrawScope.bone(a: Offset, b: Offset, color: Color = ArtWhite, w: Float = size.minDimension * .035f) = drawLine(color, a, b, w, StrokeCap.Round)
private fun DrawScope.accent(a: Offset, b: Offset, w: Float = size.minDimension * .045f) = drawLine(ArtLime, a, b, w, StrokeCap.Round)
private fun DrawScope.head(p: Offset) = drawCircle(ArtWhite, size.minDimension * .075f, p)

private fun DrawScope.drawPush() {
    val y = size.height*.62f
    val sh=Offset(size.width*.40f,y); val hip=Offset(size.width*.60f,y*.98f); val ankle=Offset(size.width*.80f,y*1.06f)
    head(Offset(size.width*.29f,y*.91f)); bone(Offset(size.width*.34f,y*.94f), sh); accent(sh,hip); bone(hip,ankle)
    val elbow=Offset(size.width*.43f,size.height*.77f); val hand=Offset(size.width*.32f,size.height*.82f)
    bone(sh,elbow,ArtLime); bone(elbow,hand,ArtLime); joint(hand); joint(elbow,4f)
    drawLine(ArtMuted, Offset(size.width*.18f,size.height*.84f), Offset(size.width*.88f,size.height*.84f), 4f, StrokeCap.Round)
}

private fun DrawScope.drawPull() {
    val barY=size.height*.18f; drawLine(ArtMuted,Offset(size.width*.18f,barY),Offset(size.width*.82f,barY),6f,StrokeCap.Round)
    val headP=Offset(size.width*.5f,size.height*.35f); head(headP)
    val shoulder=Offset(size.width*.5f,size.height*.47f); val hip=Offset(size.width*.5f,size.height*.66f)
    accent(shoulder,hip)
    val lh=Offset(size.width*.30f,barY); val rh=Offset(size.width*.70f,barY)
    bone(shoulder,Offset(size.width*.39f,size.height*.34f),ArtLime); bone(Offset(size.width*.39f,size.height*.34f),lh,ArtLime)
    bone(shoulder,Offset(size.width*.61f,size.height*.34f),ArtLime); bone(Offset(size.width*.61f,size.height*.34f),rh,ArtLime)
    bone(hip,Offset(size.width*.42f,size.height*.84f)); bone(hip,Offset(size.width*.58f,size.height*.84f))
}

private fun DrawScope.drawSquat() {
    val headP=Offset(size.width*.5f,size.height*.22f); head(headP)
    val sh=Offset(size.width*.5f,size.height*.37f); val hip=Offset(size.width*.50f,size.height*.58f); accent(sh,hip)
    val lk=Offset(size.width*.36f,size.height*.72f); val rk=Offset(size.width*.64f,size.height*.72f)
    bone(hip,lk,ArtLime); bone(hip,rk,ArtLime); bone(lk,Offset(size.width*.30f,size.height*.88f)); bone(rk,Offset(size.width*.70f,size.height*.88f))
    bone(sh,Offset(size.width*.30f,size.height*.47f)); bone(sh,Offset(size.width*.70f,size.height*.47f))
    drawLine(ArtLime,Offset(size.width*.27f,size.height*.47f),Offset(size.width*.73f,size.height*.47f),5f,StrokeCap.Round)
}

private fun DrawScope.drawLunge() {
    head(Offset(size.width*.48f,size.height*.20f)); val sh=Offset(size.width*.48f,size.height*.36f); val hip=Offset(size.width*.5f,size.height*.57f); accent(sh,hip)
    val fk=Offset(size.width*.68f,size.height*.72f); val bk=Offset(size.width*.35f,size.height*.73f)
    bone(hip,fk,ArtLime); bone(fk,Offset(size.width*.74f,size.height*.88f)); bone(hip,bk); bone(bk,Offset(size.width*.23f,size.height*.86f))
    bone(sh,Offset(size.width*.36f,size.height*.52f)); bone(sh,Offset(size.width*.60f,size.height*.52f))
}

private fun DrawScope.drawHinge() {
    head(Offset(size.width*.36f,size.height*.31f)); val sh=Offset(size.width*.42f,size.height*.42f); val hip=Offset(size.width*.57f,size.height*.56f)
    accent(sh,hip); val k=Offset(size.width*.58f,size.height*.73f); bone(hip,k); bone(k,Offset(size.width*.62f,size.height*.88f))
    bone(sh,Offset(size.width*.34f,size.height*.65f),ArtLime); bone(sh,Offset(size.width*.50f,size.height*.64f),ArtLime)
    drawLine(ArtMuted,Offset(size.width*.24f,size.height*.68f),Offset(size.width*.54f,size.height*.68f),5f,StrokeCap.Round)
}

private fun DrawScope.drawCore() {
    drawLine(ArtMuted,Offset(size.width*.16f,size.height*.78f),Offset(size.width*.84f,size.height*.78f),4f,StrokeCap.Round)
    head(Offset(size.width*.31f,size.height*.56f)); val sh=Offset(size.width*.39f,size.height*.62f); val hip=Offset(size.width*.52f,size.height*.67f); accent(sh,hip)
    bone(sh,Offset(size.width*.52f,size.height*.44f),ArtLime); bone(Offset(size.width*.52f,size.height*.44f),Offset(size.width*.64f,size.height*.33f),ArtLime)
    bone(hip,Offset(size.width*.62f,size.height*.50f)); bone(Offset(size.width*.62f,size.height*.50f),Offset(size.width*.73f,size.height*.39f))
}

private fun DrawScope.drawPlank() {
    drawLine(ArtMuted,Offset(size.width*.12f,size.height*.79f),Offset(size.width*.90f,size.height*.79f),4f,StrokeCap.Round)
    head(Offset(size.width*.27f,size.height*.58f)); val sh=Offset(size.width*.36f,size.height*.61f); val hip=Offset(size.width*.59f,size.height*.65f); val foot=Offset(size.width*.82f,size.height*.72f)
    accent(sh,hip); bone(hip,foot); bone(sh,Offset(size.width*.31f,size.height*.76f),ArtLime); bone(sh,Offset(size.width*.44f,size.height*.76f),ArtLime)
}

private fun DrawScope.drawCurl() {
    head(Offset(size.width*.5f,size.height*.20f)); val sh=Offset(size.width*.5f,size.height*.36f); val hip=Offset(size.width*.5f,size.height*.60f); accent(sh,hip)
    val le=Offset(size.width*.36f,size.height*.49f); val re=Offset(size.width*.64f,size.height*.49f); bone(sh,le); bone(sh,re)
    bone(le,Offset(size.width*.43f,size.height*.38f),ArtLime); bone(re,Offset(size.width*.57f,size.height*.38f),ArtLime)
    bone(hip,Offset(size.width*.42f,size.height*.86f)); bone(hip,Offset(size.width*.58f,size.height*.86f))
    drawCircle(ArtLime,8f,Offset(size.width*.43f,size.height*.38f)); drawCircle(ArtLime,8f,Offset(size.width*.57f,size.height*.38f))
}

private fun DrawScope.drawTriceps() {
    head(Offset(size.width*.5f,size.height*.22f)); val sh=Offset(size.width*.5f,size.height*.38f); val hip=Offset(size.width*.5f,size.height*.61f); accent(sh,hip)
    bone(sh,Offset(size.width*.40f,size.height*.30f),ArtLime); bone(Offset(size.width*.40f,size.height*.30f),Offset(size.width*.46f,size.height*.16f),ArtLime)
    bone(sh,Offset(size.width*.60f,size.height*.30f),ArtLime); bone(Offset(size.width*.60f,size.height*.30f),Offset(size.width*.54f,size.height*.16f),ArtLime)
    bone(hip,Offset(size.width*.43f,size.height*.87f)); bone(hip,Offset(size.width*.57f,size.height*.87f))
}

private fun DrawScope.drawBike() {
    val r=size.minDimension*.16f; val c1=Offset(size.width*.30f,size.height*.68f); val c2=Offset(size.width*.70f,size.height*.68f)
    drawCircle(ArtWhite,r,c1,style=Stroke(width=5f)); drawCircle(ArtWhite,r,c2,style=Stroke(width=5f))
    val crank=Offset(size.width*.50f,size.height*.65f); accent(c1,crank); accent(crank,c2); accent(crank,Offset(size.width*.43f,size.height*.49f)); accent(Offset(size.width*.43f,size.height*.49f),c1)
    head(Offset(size.width*.53f,size.height*.29f)); val sh=Offset(size.width*.51f,size.height*.40f); bone(sh,Offset(size.width*.43f,size.height*.49f),ArtLime); bone(sh,Offset(size.width*.65f,size.height*.47f)); bone(Offset(size.width*.65f,size.height*.47f),Offset(size.width*.72f,size.height*.48f))
}

private fun DrawScope.drawRun() {
    head(Offset(size.width*.53f,size.height*.21f)); val sh=Offset(size.width*.50f,size.height*.36f); val hip=Offset(size.width*.49f,size.height*.56f); accent(sh,hip)
    bone(sh,Offset(size.width*.35f,size.height*.45f),ArtLime); bone(Offset(size.width*.35f,size.height*.45f),Offset(size.width*.27f,size.height*.35f),ArtLime)
    bone(sh,Offset(size.width*.65f,size.height*.45f)); bone(Offset(size.width*.65f,size.height*.45f),Offset(size.width*.73f,size.height*.56f))
    val k1=Offset(size.width*.65f,size.height*.69f); val k2=Offset(size.width*.37f,size.height*.70f); bone(hip,k1,ArtLime); bone(k1,Offset(size.width*.78f,size.height*.83f),ArtLime); bone(hip,k2); bone(k2,Offset(size.width*.24f,size.height*.84f))
}

private fun DrawScope.drawMobility() {
    head(Offset(size.width*.5f,size.height*.18f)); val sh=Offset(size.width*.5f,size.height*.34f); val hip=Offset(size.width*.5f,size.height*.57f); accent(sh,hip)
    bone(sh,Offset(size.width*.28f,size.height*.24f),ArtLime); bone(sh,Offset(size.width*.72f,size.height*.24f),ArtLime)
    bone(hip,Offset(size.width*.37f,size.height*.85f)); bone(hip,Offset(size.width*.63f,size.height*.85f))
    drawArc(ArtLime.copy(alpha=.55f),210f,120f,false,Offset(size.width*.17f,size.height*.08f),Size(size.width*.66f,size.height*.42f),style=Stroke(4f))
}

private fun DrawScope.drawCalf() {
    head(Offset(size.width*.5f,size.height*.18f)); val sh=Offset(size.width*.5f,size.height*.34f); val hip=Offset(size.width*.5f,size.height*.57f); accent(sh,hip)
    val lk=Offset(size.width*.43f,size.height*.72f); val rk=Offset(size.width*.57f,size.height*.72f); bone(hip,lk); bone(hip,rk); bone(lk,Offset(size.width*.40f,size.height*.87f),ArtLime); bone(rk,Offset(size.width*.60f,size.height*.87f),ArtLime)
    drawLine(ArtMuted,Offset(size.width*.31f,size.height*.91f),Offset(size.width*.69f,size.height*.91f),5f,StrokeCap.Round)
}

private fun DrawScope.drawGeneric() {
    head(Offset(size.width*.5f,size.height*.20f)); val sh=Offset(size.width*.5f,size.height*.36f); val hip=Offset(size.width*.5f,size.height*.60f); accent(sh,hip)
    bone(sh,Offset(size.width*.31f,size.height*.51f),ArtLime); bone(sh,Offset(size.width*.69f,size.height*.51f),ArtLime); bone(hip,Offset(size.width*.41f,size.height*.87f)); bone(hip,Offset(size.width*.59f,size.height*.87f))
}
