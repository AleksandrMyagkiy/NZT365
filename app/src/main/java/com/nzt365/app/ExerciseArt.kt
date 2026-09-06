package com.nzt365.app

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

private val ArtLime = Color(0xFFE8FF62)
private val ArtWhite = Color(0xFFF1F5F7)
private val ArtMuted = Color(0xFF526270)
private val ArtBg = Color(0xFF0A1620)

enum class ExerciseArtType { PUSH, PULL, SQUAT, LUNGE, HINGE, CORE, PLANK, CURL, TRICEPS, BIKE, RUN, MOBILITY, CALF, GENERIC }

fun artTypeFor(name: String): ExerciseArtType {
    val s = name.lowercase()
    return when {
        listOf("велосип", "bike", "cycling").any { it in s } -> ExerciseArtType.BIKE
        listOf("бег", "run", "biegan").any { it in s } -> ExerciseArtType.RUN
        listOf("подтяг", "pull-up", "pullup", "drąż").any { it in s } -> ExerciseArtType.PULL
        listOf("отжим", "жим", "push", "press").any { it in s } -> ExerciseArtType.PUSH
        listOf("сплит", "выпад", "lunge", "split").any { it in s } -> ExerciseArtType.LUNGE
        listOf("присед", "squat", "przys").any { it in s } -> ExerciseArtType.SQUAT
        listOf("румын", "hinge", "deadlift", "row", "тяга").any { it in s } -> ExerciseArtType.HINGE
        listOf("планк", "plank").any { it in s } -> ExerciseArtType.PLANK
        listOf("dead bug", "bird-dog", "пресс", "core", "raise").any { it in s } -> ExerciseArtType.CORE
        listOf("curl", "biceps", "сгибание").any { it in s } -> ExerciseArtType.CURL
        listOf("triceps", "разгибание").any { it in s } -> ExerciseArtType.TRICEPS
        listOf("calf", "икр", "носок").any { it in s } -> ExerciseArtType.CALF
        listOf("мобил", "mobility", "stretch", "размин").any { it in s } -> ExerciseArtType.MOBILITY
        else -> ExerciseArtType.GENERIC
    }
}

@Composable
fun ExerciseIllustration(name: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "exercise-demo")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), repeatMode = RepeatMode.Reverse),
        label = "phase"
    )

    Box(
        modifier = modifier
            .background(ArtBg, RoundedCornerShape(22.dp))
            .padding(10.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val type = artTypeFor(name)
            drawBackdrop(type)
            when (type) {
                ExerciseArtType.PUSH -> drawPush(phase)
                ExerciseArtType.PULL -> drawPull(phase)
                ExerciseArtType.SQUAT -> drawSquat(phase)
                ExerciseArtType.LUNGE -> drawLunge(phase)
                ExerciseArtType.HINGE -> drawHinge(phase)
                ExerciseArtType.CORE -> drawCore(phase)
                ExerciseArtType.PLANK -> drawPlank(phase)
                ExerciseArtType.CURL -> drawCurl(phase)
                ExerciseArtType.TRICEPS -> drawTriceps(phase)
                ExerciseArtType.BIKE -> drawBike(phase)
                ExerciseArtType.RUN -> drawRun(phase)
                ExerciseArtType.MOBILITY -> drawMobility(phase)
                ExerciseArtType.CALF -> drawCalf(phase)
                ExerciseArtType.GENERIC -> drawGeneric(phase)
            }
        }
    }
}

private fun DrawScope.drawBackdrop(type: ExerciseArtType) {
    drawCircle(ArtLime.copy(alpha = .07f), size.minDimension * .43f, center = Offset(size.width * .72f, size.height * .26f))
    drawLine(ArtMuted.copy(alpha = .28f), Offset(size.width*.12f,size.height*.88f), Offset(size.width*.88f,size.height*.88f), 3f, StrokeCap.Round)
    if (type == ExerciseArtType.PULL) drawLine(ArtMuted, Offset(size.width*.18f,size.height*.16f), Offset(size.width*.82f,size.height*.16f), 6f, StrokeCap.Round)
}

private fun DrawScope.limb(a: Offset, b: Offset, accent: Boolean = false, widthScale: Float = .055f) {
    drawLine(if (accent) ArtLime else ArtWhite, a, b, size.minDimension * widthScale, StrokeCap.Round)
}
private fun DrawScope.head(p: Offset) = drawCircle(ArtWhite, size.minDimension*.085f, p)
private fun DrawScope.joint(p: Offset, accent: Boolean = false) = drawCircle(if (accent) ArtLime else ArtWhite, size.minDimension*.045f, p)
private fun mix(a: Float, b: Float, t: Float) = a + (b-a)*t

private fun DrawScope.drawPush(t: Float) {
    val y = mix(.62f,.72f,t)
    val sh=Offset(size.width*.38f,size.height*y); val hip=Offset(size.width*.61f,size.height*(y+.03f)); val foot=Offset(size.width*.82f,size.height*(y+.09f))
    head(Offset(size.width*.27f,size.height*(y-.03f))); limb(sh,hip,true,.07f); limb(hip,foot,false,.06f)
    val elbow=Offset(size.width*.43f,size.height*mix(.74f,.82f,t)); val hand=Offset(size.width*.28f,size.height*.84f)
    limb(sh,elbow,true); limb(elbow,hand,true); joint(elbow,true); joint(hand)
}

private fun DrawScope.drawPull(t: Float) {
    val bodyY=mix(.58f,.48f,t); val sh=Offset(size.width*.5f,size.height*bodyY); val hip=Offset(size.width*.5f,size.height*(bodyY+.22f))
    head(Offset(size.width*.5f,size.height*(bodyY-.15f))); limb(sh,hip,true,.07f)
    val le=Offset(size.width*.38f,size.height*mix(.36f,.29f,t)); val re=Offset(size.width*.62f,size.height*mix(.36f,.29f,t))
    limb(sh,le,true); limb(le,Offset(size.width*.29f,size.height*.16f),true); limb(sh,re,true); limb(re,Offset(size.width*.71f,size.height*.16f),true)
    limb(hip,Offset(size.width*.42f,size.height*.87f)); limb(hip,Offset(size.width*.58f,size.height*.87f))
}

private fun DrawScope.drawSquat(t: Float) {
    val hipY=mix(.49f,.65f,t); val sh=Offset(size.width*.5f,size.height*mix(.34f,.48f,t)); val hip=Offset(size.width*.5f,size.height*hipY)
    head(Offset(size.width*.5f,size.height*mix(.19f,.33f,t))); limb(sh,hip,true,.07f)
    val lk=Offset(size.width*.36f,size.height*mix(.68f,.75f,t)); val rk=Offset(size.width*.64f,size.height*mix(.68f,.75f,t))
    limb(hip,lk,true); limb(hip,rk,true); limb(lk,Offset(size.width*.31f,size.height*.88f)); limb(rk,Offset(size.width*.69f,size.height*.88f))
    drawLine(ArtLime,Offset(size.width*.27f,size.height*mix(.40f,.54f,t)),Offset(size.width*.73f,size.height*mix(.40f,.54f,t)),5f,StrokeCap.Round)
}

private fun DrawScope.drawLunge(t: Float) {
    val hip=Offset(size.width*.49f,size.height*mix(.52f,.61f,t)); val sh=Offset(size.width*.48f,size.height*mix(.34f,.43f,t))
    head(Offset(size.width*.48f,size.height*mix(.19f,.28f,t))); limb(sh,hip,true,.07f)
    val fk=Offset(size.width*.69f,size.height*mix(.67f,.76f,t)); val bk=Offset(size.width*.34f,size.height*mix(.68f,.77f,t))
    limb(hip,fk,true); limb(fk,Offset(size.width*.74f,size.height*.88f)); limb(hip,bk); limb(bk,Offset(size.width*.22f,size.height*.87f))
    limb(sh,Offset(size.width*.37f,size.height*.54f),true); limb(sh,Offset(size.width*.60f,size.height*.54f),true)
}

private fun DrawScope.drawHinge(t: Float) {
    val sh=Offset(size.width*mix(.50f,.39f,t),size.height*mix(.35f,.46f,t)); val hip=Offset(size.width*.57f,size.height*.57f)
    head(Offset(sh.x-size.width*.08f,sh.y-size.height*.13f)); limb(sh,hip,true,.07f)
    val k=Offset(size.width*.59f,size.height*.73f); limb(hip,k); limb(k,Offset(size.width*.62f,size.height*.88f))
    val hand=Offset(size.width*mix(.38f,.31f,t),size.height*mix(.57f,.69f,t)); limb(sh,hand,true)
    drawLine(ArtLime,Offset(size.width*.25f,size.height*.72f),Offset(size.width*.55f,size.height*.72f),6f,StrokeCap.Round)
}

private fun DrawScope.drawCore(t: Float) {
    val sh=Offset(size.width*.38f,size.height*.65f); val hip=Offset(size.width*.54f,size.height*.70f)
    head(Offset(size.width*.27f,size.height*.59f)); limb(sh,hip,true,.07f)
    limb(sh,Offset(size.width*mix(.50f,.64f,t),size.height*mix(.48f,.34f,t)),true)
    limb(hip,Offset(size.width*mix(.62f,.76f,t),size.height*mix(.55f,.39f,t)),true)
}

private fun DrawScope.drawPlank(t: Float) {
    val bob=(t-.5f)*.02f; val sh=Offset(size.width*.34f,size.height*(.61f+bob)); val hip=Offset(size.width*.59f,size.height*(.65f+bob)); val foot=Offset(size.width*.82f,size.height*.73f)
    head(Offset(size.width*.24f,size.height*(.57f+bob))); limb(sh,hip,true,.07f); limb(hip,foot); limb(sh,Offset(size.width*.29f,size.height*.79f),true); limb(sh,Offset(size.width*.45f,size.height*.79f),true)
}

private fun DrawScope.drawCurl(t: Float) {
    val sh=Offset(size.width*.5f,size.height*.36f); val hip=Offset(size.width*.5f,size.height*.61f); head(Offset(size.width*.5f,size.height*.20f)); limb(sh,hip,true,.07f)
    val le=Offset(size.width*.35f,size.height*.50f); val re=Offset(size.width*.65f,size.height*.50f); limb(sh,le); limb(sh,re)
    val hY=mix(.68f,.37f,t); limb(le,Offset(size.width*.41f,size.height*hY),true); limb(re,Offset(size.width*.59f,size.height*hY),true)
    drawCircle(ArtLime,8f,Offset(size.width*.41f,size.height*hY)); drawCircle(ArtLime,8f,Offset(size.width*.59f,size.height*hY)); limb(hip,Offset(size.width*.43f,size.height*.87f)); limb(hip,Offset(size.width*.57f,size.height*.87f))
}

private fun DrawScope.drawTriceps(t: Float) {
    val sh=Offset(size.width*.5f,size.height*.38f); val hip=Offset(size.width*.5f,size.height*.62f); head(Offset(size.width*.5f,size.height*.22f)); limb(sh,hip,true,.07f)
    val y=mix(.36f,.18f,t); limb(sh,Offset(size.width*.39f,size.height*.31f),true); limb(Offset(size.width*.39f,size.height*.31f),Offset(size.width*.45f,size.height*y),true); limb(sh,Offset(size.width*.61f,size.height*.31f),true); limb(Offset(size.width*.61f,size.height*.31f),Offset(size.width*.55f,size.height*y),true)
    limb(hip,Offset(size.width*.43f,size.height*.88f)); limb(hip,Offset(size.width*.57f,size.height*.88f))
}

private fun DrawScope.drawBike(t: Float) {
    val r=size.minDimension*.16f; val c1=Offset(size.width*.30f,size.height*.71f); val c2=Offset(size.width*.70f,size.height*.71f)
    drawCircle(ArtWhite,r,c1,style=androidx.compose.ui.graphics.drawscope.Stroke(width=5f)); drawCircle(ArtWhite,r,c2,style=androidx.compose.ui.graphics.drawscope.Stroke(width=5f))
    val crank=Offset(size.width*.50f,size.height*.68f); limb(c1,crank,true,.04f); limb(crank,c2,true,.04f); limb(crank,Offset(size.width*.43f,size.height*.52f),true,.04f); limb(Offset(size.width*.43f,size.height*.52f),c1,true,.04f)
    head(Offset(size.width*.53f,size.height*.29f)); val sh=Offset(size.width*.51f,size.height*.42f); limb(sh,Offset(size.width*.43f,size.height*.52f),true)
    val pedalAngle=if(t<.5f)-1f else 1f; limb(crank,Offset(size.width*(.50f+.08f*pedalAngle),size.height*.78f),true)
    limb(sh,Offset(size.width*.70f,size.height*.49f));
}

private fun DrawScope.drawRun(t: Float) {
    val sh=Offset(size.width*.50f,size.height*.37f); val hip=Offset(size.width*.49f,size.height*.57f); head(Offset(size.width*.53f,size.height*.21f)); limb(sh,hip,true,.07f)
    val swing=(t-.5f)*.14f
    limb(sh,Offset(size.width*(.35f+swing),size.height*.47f),true); limb(sh,Offset(size.width*(.65f-swing),size.height*.48f))
    val k1=Offset(size.width*(.63f-swing),size.height*.70f); val k2=Offset(size.width*(.37f+swing),size.height*.70f); limb(hip,k1,true); limb(k1,Offset(size.width*(.77f-swing),size.height*.84f),true); limb(hip,k2); limb(k2,Offset(size.width*(.24f+swing),size.height*.85f))
}

private fun DrawScope.drawMobility(t: Float) {
    val sh=Offset(size.width*.5f,size.height*.36f); val hip=Offset(size.width*.5f,size.height*.59f); head(Offset(size.width*.5f,size.height*.20f)); limb(sh,hip,true,.07f)
    val spread=mix(.12f,.26f,t); limb(sh,Offset(size.width*(.5f-spread),size.height*.26f),true); limb(sh,Offset(size.width*(.5f+spread),size.height*.26f),true); limb(hip,Offset(size.width*.38f,size.height*.87f)); limb(hip,Offset(size.width*.62f,size.height*.87f))
}

private fun DrawScope.drawCalf(t: Float) {
    val lift=mix(0f,-.05f,t); val sh=Offset(size.width*.5f,size.height*(.35f+lift)); val hip=Offset(size.width*.5f,size.height*(.58f+lift)); head(Offset(size.width*.5f,size.height*(.19f+lift))); limb(sh,hip,true,.07f)
    val lk=Offset(size.width*.43f,size.height*(.73f+lift)); val rk=Offset(size.width*.57f,size.height*(.73f+lift)); limb(hip,lk); limb(hip,rk); limb(lk,Offset(size.width*.40f,size.height*(.88f+lift)),true); limb(rk,Offset(size.width*.60f,size.height*(.88f+lift)),true)
}

private fun DrawScope.drawGeneric(t: Float) {
    val sh=Offset(size.width*.5f,size.height*.37f); val hip=Offset(size.width*.5f,size.height*.61f); head(Offset(size.width*.5f,size.height*.20f)); limb(sh,hip,true,.07f)
    val spread=mix(.12f,.23f,t); limb(sh,Offset(size.width*(.5f-spread),size.height*.52f),true); limb(sh,Offset(size.width*(.5f+spread),size.height*.52f),true); limb(hip,Offset(size.width*.40f,size.height*.87f)); limb(hip,Offset(size.width*.60f,size.height*.87f))
}
