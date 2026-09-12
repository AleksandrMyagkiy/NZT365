package com.nzt365.app

import android.content.Context
import java.time.LocalDate
import kotlin.math.roundToInt

data class V12SetState(
    val kg: String = "",
    val reps: String = "",
    val rir: Int = 2,
    val done: Boolean = false
)

class V12WorkoutStore(context: Context) {
    private val p = context.getSharedPreferences("nzt_workout_v7", Context.MODE_PRIVATE)
    private fun key(date: LocalDate, exercise: String, set: Int, field: String) = "${date}_${exercise.hashCode()}_${set}_$field"

    fun load(date: LocalDate, exercise: String, set: Int, defaultReps: Int): V12SetState = V12SetState(
        kg = p.getString(key(date, exercise, set, "kg"), p.getString("prevkg_${exercise.hashCode()}_$set", "")) ?: "",
        reps = p.getString(key(date, exercise, set, "reps"), p.getString("prevreps_${exercise.hashCode()}_$set", defaultReps.toString())) ?: defaultReps.toString(),
        rir = p.getInt(key(date, exercise, set, "rir"), 2),
        done = p.getBoolean(key(date, exercise, set, "done"), false)
    )

    fun save(date: LocalDate, exercise: String, set: Int, state: V12SetState) {
        p.edit().putString(key(date,exercise,set,"kg"),state.kg)
            .putString(key(date,exercise,set,"reps"),state.reps)
            .putInt(key(date,exercise,set,"rir"),state.rir)
            .putBoolean(key(date,exercise,set,"done"),state.done).apply()
    }

    fun previous(exercise:String): String {
        val kg=p.getString("prevkg_${exercise.hashCode()}_0","").orEmpty()
        val reps=p.getString("prevreps_${exercise.hashCode()}_0","").orEmpty()
        return when { kg.isNotBlank() && reps.isNotBlank() -> "$kg kg × $reps"; reps.isNotBlank() -> reps; else -> "—" }
    }
    fun bestE1rm(exercise:String)=p.getFloat("e1rm_${exercise.hashCode()}",0f).toDouble()
    fun bestVolume(exercise:String)=p.getFloat("vol_${exercise.hashCode()}",0f).toDouble()

    fun finishExercise(exercise:String, states:List<V12SetState>) {
        val ed=p.edit()
        states.forEachIndexed { i,s -> if(s.done){ ed.putString("prevkg_${exercise.hashCode()}_$i",s.kg); ed.putString("prevreps_${exercise.hashCode()}_$i",s.reps) } }
        val e1=states.filter{it.done}.maxOfOrNull(::v12E1rm)?:0.0
        val vol=states.filter{it.done}.sumOf(::v12Volume)
        if(e1>bestE1rm(exercise)) ed.putFloat("e1rm_${exercise.hashCode()}",e1.toFloat())
        if(vol>bestVolume(exercise)) ed.putFloat("vol_${exercise.hashCode()}",vol.toFloat())
        ed.apply()
    }

    fun markWorkout(date:LocalDate,code:String){
        p.edit().putBoolean("workout_${date}_$code",true).putInt("workout_count",p.getInt("workout_count",0)+1).apply()
    }
}

fun v12E1rm(s:V12SetState):Double { val kg=s.kg.replace(',','.').toDoubleOrNull()?:0.0; val r=s.reps.toIntOrNull()?:0; return if(kg<=0||r<=0)0.0 else kg*(1+r/30.0) }
fun v12Volume(s:V12SetState):Double=(s.kg.replace(',','.').toDoubleOrNull()?:0.0)*(s.reps.toIntOrNull()?:0)
fun v12DefaultTarget(target:String):Int=Regex("\\d+").find(target)?.value?.toIntOrNull()?:1
fun v12IsCardio(ex:StathamExercise, session:StathamSession):Boolean = session.type in setOf("Велосипед","Бег","Кардио","Восстановление") || (ex.restSeconds==0 && (ex.target.contains("мин",true)||ex.name.contains("Z2",true)||ex.name.contains("интервал",true)||ex.name.contains("замин",true)))
fun v12Text(lang:AppLanguage,ru:String,en:String,pl:String,uk:String)=when(lang){AppLanguage.RU->ru;AppLanguage.EN->en;AppLanguage.PL->pl;AppLanguage.UK->uk}
fun v12Kg(v:Double)=if(v>0)"${v.roundToInt()} kg" else "—"
