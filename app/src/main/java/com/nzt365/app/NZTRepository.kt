package com.nzt365.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DailyTask(val id:String, val category:String, val title:String, val target:Int=1, val done:Int=0)
data class BodyLog(val date:String, val weight:Double?, val waist:Double?, val chest:Double?, val arm:Double?, val thigh:Double?)
data class GrowthLog(val date:String, val type:String, val note:String)
data class MoneyLog(val date:String, val mainIncome:Double, val sideIncome:Double, val capital:Double, val expenses:Double)

class NZTRepository(context: Context) {
    private val prefs = context.getSharedPreferences("nzt365", Context.MODE_PRIVATE)

    fun startDate(): LocalDate {
        val saved = prefs.getString("startDate", null)
        return if (saved != null) LocalDate.parse(saved) else LocalDate.now().also {
            prefs.edit().putString("startDate", it.toString()).apply()
        }
    }

    fun dayNumber(): Int = (ChronoUnit.DAYS.between(startDate(), LocalDate.now()) + 1).toInt().coerceIn(1,365)

    fun tasksForToday(): List<DailyTask> {
        val key = "tasks_${LocalDate.now()}"
        val raw = prefs.getString(key, null)
        if (raw != null) {
            val tasks = decodeTasks(raw)
            persistScore(LocalDate.now(), completionPercent(tasks))
            return tasks
        }
        val tasks = seedTasks()
        saveTasks(tasks)
        return tasks
    }

    fun saveTasks(tasks: List<DailyTask>) {
        val arr = JSONArray()
        tasks.forEach { t -> arr.put(JSONObject().apply {
            put("id",t.id); put("category",t.category); put("title",t.title); put("target",t.target); put("done",t.done)
        }) }
        val today = LocalDate.now()
        prefs.edit().putString("tasks_$today", arr.toString()).apply()
        persistScore(today, completionPercent(tasks))
    }

    private fun decodeTasks(raw:String):List<DailyTask>{
        val arr=JSONArray(raw); val out=mutableListOf<DailyTask>()
        for(i in 0 until arr.length()){
            val o=arr.getJSONObject(i)
            out += DailyTask(o.getString("id"),o.getString("category"),o.getString("title"),o.optInt("target",1),o.optInt("done",0))
        }
        return out
    }

    private fun seedTasks(): List<DailyTask> {
        val dow = LocalDate.now().dayOfWeek.value
        val body = when(dow){
            1 -> "Силовая: Верх A"
            2 -> "Кардио 40–75 мин"
            3 -> "Ноги + корпус"
            4 -> "Восстановление / мобильность"
            5 -> "Силовая: Верх B"
            6 -> "Функциональная тренировка"
            else -> "Восстановление / прогулка"
        }
        return listOf(
            DailyTask("body","BODY",body),
            DailyTask("protein","NUTRITION","Белок по цели"),
            DailyTask("mirror","INFLUENCE","Mirror",5),
            DailyTask("label","INFLUENCE","Label",2),
            DailyTask("mind","MIND","Чтение / обучение 20 мин"),
            DailyTask("career","CAREER","15 минут карьерного действия"),
            DailyTask("money","MONEY","1 действие для роста дохода")
        )
    }

    private fun persistScore(date: LocalDate, score: Int) {
        prefs.edit().putInt("score_$date", score.coerceIn(0,100)).apply()
    }

    fun scoreForDate(date: LocalDate): Int = prefs.getInt("score_$date", 0)

    fun lastDailyScores(days: Int): List<Pair<LocalDate, Int>> {
        val count = days.coerceIn(1, 30)
        val today = LocalDate.now()
        return (count - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            date to scoreForDate(date)
        }
    }

    fun scoreStreak(): Int {
        var streak = 0
        var date = LocalDate.now()
        while (scoreForDate(date) > 0 && streak < 365) {
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    fun saveBody(log: BodyLog){
        val arr = loadArray("bodyLogs")
        arr.put(JSONObject().apply{
            put("date",log.date); putOpt("weight",log.weight); putOpt("waist",log.waist); putOpt("chest",log.chest); putOpt("arm",log.arm); putOpt("thigh",log.thigh)
        })
        prefs.edit().putString("bodyLogs",arr.toString()).apply()
    }
    fun bodyLogs():List<BodyLog>{
        val arr=loadArray("bodyLogs"); val out=mutableListOf<BodyLog>()
        for(i in 0 until arr.length()){
            val o=arr.getJSONObject(i)
            fun d(k:String)=if(o.has(k)&&!o.isNull(k))o.getDouble(k) else null
            out += BodyLog(o.getString("date"),d("weight"),d("waist"),d("chest"),d("arm"),d("thigh"))
        }
        return out.sortedByDescending{it.date}
    }

    fun addGrowth(type:String,note:String){
        val arr=loadArray("growthLogs")
        arr.put(JSONObject().apply{put("date",LocalDate.now().toString()); put("type",type); put("note",note)})
        prefs.edit().putString("growthLogs",arr.toString()).apply()
    }
    fun growthLogs():List<GrowthLog>{
        val arr=loadArray("growthLogs"); val out=mutableListOf<GrowthLog>()
        for(i in 0 until arr.length()){
            val o=arr.getJSONObject(i); out += GrowthLog(o.getString("date"),o.getString("type"),o.getString("note"))
        }
        return out.reversed()
    }

    fun saveMoney(log: MoneyLog){
        val arr=loadArray("moneyLogs")
        arr.put(JSONObject().apply{put("date",log.date);put("main",log.mainIncome);put("side",log.sideIncome);put("capital",log.capital);put("expenses",log.expenses)})
        prefs.edit().putString("moneyLogs",arr.toString()).apply()
    }
    fun moneyLogs():List<MoneyLog>{
        val arr=loadArray("moneyLogs"); val out=mutableListOf<MoneyLog>()
        for(i in 0 until arr.length()){
            val o=arr.getJSONObject(i); out+=MoneyLog(o.getString("date"),o.getDouble("main"),o.getDouble("side"),o.getDouble("capital"),o.getDouble("expenses"))
        }
        return out.reversed()
    }

    fun setNutrition(cal:Int, protein:Int){prefs.edit().putInt("cal_${LocalDate.now()}",cal).putInt("protein_${LocalDate.now()}",protein).apply()}
    fun nutrition():Pair<Int,Int> = prefs.getInt("cal_${LocalDate.now()}",0) to prefs.getInt("protein_${LocalDate.now()}",0)

    fun setTargets(cal:Int,protein:Int){prefs.edit().putInt("targetCal",cal).putInt("targetProtein",protein).apply()}
    fun targets():Pair<Int,Int> = prefs.getInt("targetCal",2200) to prefs.getInt("targetProtein",140)

    fun completionPercent(tasks:List<DailyTask>):Int{
        val total=tasks.sumOf{it.target}; val done=tasks.sumOf{it.done.coerceAtMost(it.target)}
        return if(total==0)0 else ((done*100.0)/total).toInt()
    }

    private fun loadArray(key:String):JSONArray = try{JSONArray(prefs.getString(key,"[]"))}catch(_:Exception){JSONArray()}
}
