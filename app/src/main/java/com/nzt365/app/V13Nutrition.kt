package com.nzt365.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

private data class V13Food(val id:String,val name:String,val kcal:Double,val p:Double,val f:Double,val c:Double)
private data class V13MealItem(val id:String,val meal:String,val foodId:String,val grams:Double)
private val v13Meals = listOf("BREAKFAST","LUNCH","SNACK","DINNER")

private class V13FoodStore(context: Context) {
    private val p=context.getSharedPreferences("nzt_food_v13",Context.MODE_PRIVATE)
    private val defaults=listOf(
        V13Food("eggs","Яйца",143.0,13.0,9.5,.7), V13Food("oats","Овсянка",370.0,13.0,7.0,60.0),
        V13Food("chicken","Куриная грудка",165.0,31.0,3.6,0.0), V13Food("rice","Рис готовый",130.0,2.7,.3,28.0),
        V13Food("skyr","Skyr",63.0,11.0,.2,4.0), V13Food("banana","Банан",89.0,1.1,.3,23.0),
        V13Food("cottage","Творог 4%",121.0,17.0,4.0,3.0), V13Food("salmon","Лосось",208.0,20.0,13.0,0.0),
        V13Food("potato","Картофель",87.0,1.9,.1,20.0), V13Food("olive","Оливковое масло",884.0,0.0,100.0,0.0)
    )
    fun foods():List<V13Food>{
        val raw=p.getString("foods",null) ?: return defaults
        return runCatching { val a=JSONArray(raw); (0 until a.length()).map{ i->val o=a.getJSONObject(i);V13Food(o.getString("id"),o.getString("name"),o.getDouble("kcal"),o.getDouble("p"),o.getDouble("f"),o.getDouble("c"))}}.getOrDefault(defaults)
    }
    fun saveFoods(list:List<V13Food>){ val a=JSONArray();list.forEach{f->a.put(JSONObject().apply{put("id",f.id);put("name",f.name);put("kcal",f.kcal);put("p",f.p);put("f",f.f);put("c",f.c)})};p.edit().putString("foods",a.toString()).apply() }
    fun items(date:LocalDate):List<V13MealItem>{ val raw=p.getString("items_$date","[]")?:"[]";return runCatching{val a=JSONArray(raw);(0 until a.length()).map{i->val o=a.getJSONObject(i);V13MealItem(o.getString("id"),o.getString("meal"),o.getString("foodId"),o.getDouble("grams"))}}.getOrDefault(emptyList()) }
    fun saveItems(date:LocalDate,list:List<V13MealItem>){val a=JSONArray();list.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("meal",x.meal);put("foodId",x.foodId);put("grams",x.grams)})};p.edit().putString("items_$date",a.toString()).apply()}
    fun water(date:LocalDate)=p.getInt("water_$date",0)
    fun setWater(date:LocalDate,v:Int)=p.edit().putInt("water_$date",v.coerceAtLeast(0)).apply()
}

class V13NutritionActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val date=intent.getStringExtra("date")?.let(LocalDate::parse)?:LocalDate.now();val profile=ProfileStore(this);setContent{CompositionLocalProvider(LocalAppLanguage provides profile.language()){NZTProTheme{V13NutritionScreen(date){finish()}}}}}
}

@Composable private fun V13NutritionScreen(date:LocalDate,onClose:()->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current;val store=remember{V13FoodStore(context)};val lang=LocalAppLanguage.current
    var foods by remember{mutableStateOf(store.foods())};var items by remember{mutableStateOf(store.items(date))};var water by remember{mutableIntStateOf(store.water(date))}
    var selectedMeal by remember{mutableStateOf("BREAKFAST")};var editFood by remember{mutableStateOf<V13Food?>(null)};var addFood by remember{mutableStateOf<V13Food?>(null)};var grams by remember{mutableStateOf("100")};var manage by remember{mutableStateOf(false)}
    val totals=items.mapNotNull{x->foods.firstOrNull{it.id==x.foodId}?.let{f->listOf(f.kcal*x.grams/100,f.p*x.grams/100,f.f*x.grams/100,f.c*x.grams/100)}}
    val kcal=totals.sumOf{it[0]};val protein=totals.sumOf{it[1]};val fat=totals.sumOf{it[2]};val carbs=totals.sumOf{it[3]}
    val targets=StathamEngine.targets(StathamEngine.forDate(date))
    Scaffold(containerColor=NztBg,contentWindowInsets=WindowInsets.safeDrawing,topBar={Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onClose){Icon(Icons.Default.ArrowBack,null)};Column(Modifier.weight(1f)){Text(v13t(lang,"Питание","Nutrition","Odżywianie","Харчування"),fontSize=25.sp,fontWeight=FontWeight.Black);Text(date.toString(),color=NztMuted,fontSize=11.sp)};IconButton(onClick={manage=true}){Icon(Icons.Default.Tune,null,tint=NztAccent)}}}){pad->
        LazyColumn(Modifier.padding(pad).fillMaxSize(),contentPadding=PaddingValues(16.dp,8.dp,16.dp,28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            item{Card(colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(18.dp)){Text("DAILY FUEL",color=NztMuted,fontSize=10.sp,fontWeight=FontWeight.Black);Row(verticalAlignment=Alignment.Bottom){Text("${kcal.roundToInt()} / ${targets.calories} kcal",color=NztAccent,fontSize=28.sp,fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));Text("P ${protein.roundToInt()}/${targets.protein}",fontWeight=FontWeight.Black)};LinearProgressIndicator(progress={ (kcal/targets.calories).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(top=12.dp),color=NztAccent,trackColor=NztLine);Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){V13Macro("PROTEIN","${protein.roundToInt()}g",Modifier.weight(1f));V13Macro("FATS","${fat.roundToInt()}g",Modifier.weight(1f));V13Macro("CARBS","${carbs.roundToInt()}g",Modifier.weight(1f))}}}}
            item{Card(colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.WaterDrop,null,tint=NztAccent2);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(v13t(lang,"Вода","Water","Woda","Вода"),color=NztMuted,fontSize=10.sp,fontWeight=FontWeight.Bold);Text("$water ml",fontSize=22.sp,fontWeight=FontWeight.Black)};OutlinedButton(onClick={water=(water-250).coerceAtLeast(0);store.setWater(date,water)}){Text("−250")};Spacer(Modifier.width(6.dp));Button(onClick={water+=250;store.setWater(date,water)}){Text("+250")}}}}
            item{Text(v13t(lang,"Куда добавить","Add to meal","Dodaj do posiłku","Куди додати"),color=NztMuted,fontWeight=FontWeight.Bold,fontSize=11.sp);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){v13Meals.forEach{m->FilterChip(selected=selectedMeal==m,onClick={selectedMeal=m},label={Text(v13Meal(lang,m))})}}}
            item{Text(v13t(lang,"Быстро добавить","Quick add","Szybkie dodawanie","Швидко додати"),color=NztMuted,fontWeight=FontWeight.Bold,fontSize=11.sp);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){foods.take(8).forEach{f->AssistChip(onClick={addFood=f;grams="100"},label={Text(f.name)})};AssistChip(onClick={manage=true},label={Text("+ ${v13t(lang,"продукт","food","produkt","продукт")}")})}}
            v13Meals.forEach{meal->item{V13MealCard(lang,meal,items.filter{it.meal==meal},foods,onAdd={selectedMeal=meal;addFood=foods.firstOrNull();grams="100"},onDelete={id->items=items.filterNot{it.id==id};store.saveItems(date,items)})}}
        }
    }
    if(addFood!=null){val f=addFood!!;AlertDialog(onDismissRequest={addFood=null},title={Text("${v13Meal(lang,selectedMeal)} · ${f.name}")},text={Column{Text(v13t(lang,"Количество, г","Amount, g","Ilość, g","Кількість, г"));OutlinedTextField(grams,{grams=it.filter{c->c.isDigit()||c=='.'||c==','}},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true);Text("${(f.kcal*(grams.replace(',','.').toDoubleOrNull()?:0.0)/100).roundToInt()} kcal",color=NztAccent,modifier=Modifier.padding(top=8.dp))}},confirmButton={Button(onClick={val g=grams.replace(',','.').toDoubleOrNull()?:100.0;items=items+V13MealItem(UUID.randomUUID().toString(),selectedMeal,f.id,g);store.saveItems(date,items);addFood=null}){Text(v13t(lang,"Добавить","Add","Dodaj","Додати"))}},dismissButton={TextButton(onClick={addFood=null}){Text(v13t(lang,"Отмена","Cancel","Anuluj","Скасувати"))}},containerColor=NztSurface)}
    if(manage){V13FoodManager(lang,foods,onClose={manage=false},onSave={foods=it;store.saveFoods(it);manage=false},onEdit={editFood=it})}
    if(editFood!=null){V13EditFood(lang,editFood!!,onClose={editFood=null},onSave={f->foods=(foods.filterNot{it.id==f.id}+f).sortedBy{it.name};store.saveFoods(foods);editFood=null},onDelete={id->foods=foods.filterNot{it.id==id};store.saveFoods(foods);editFood=null})}
}

@Composable private fun V13Macro(label:String,value:String,modifier:Modifier){Surface(modifier,color=NztSurface2,shape=RoundedCornerShape(14.dp)){Column(Modifier.padding(11.dp)){Text(label,color=NztMuted,fontSize=9.sp,fontWeight=FontWeight.Bold);Text(value,fontWeight=FontWeight.Black,fontSize=16.sp)}}}
@Composable private fun V13MealCard(lang:AppLanguage,meal:String,items:List<V13MealItem>,foods:List<V13Food>,onAdd:()->Unit,onDelete:(String)->Unit){val kcal=items.sumOf{x->foods.firstOrNull{it.id==x.foodId}?.let{it.kcal*x.grams/100}?:0.0};Card(colors=CardDefaults.cardColors(containerColor=NztSurface),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(v13Meal(lang,meal),fontSize=21.sp,fontWeight=FontWeight.Black);Text("${kcal.roundToInt()} kcal",color=NztMuted)};FilledIconButton(onClick=onAdd){Icon(Icons.Default.Add,null)}};items.forEach{x->val f=foods.firstOrNull{it.id==x.foodId};if(f!=null)Row(Modifier.fillMaxWidth().padding(top=9.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(f.name,fontWeight=FontWeight.Bold);Text("${x.grams.roundToInt()} g · ${(f.kcal*x.grams/100).roundToInt()} kcal",color=NztMuted,fontSize=11.sp)};IconButton(onClick={onDelete(x.id)}){Icon(Icons.Default.DeleteOutline,null,tint=NztMuted)}}};if(items.isEmpty())Text(v13t(lang,"Пока пусто","No foods yet","Brak produktów","Поки порожньо"),color=NztMuted,modifier=Modifier.padding(vertical=10.dp));OutlinedButton(onClick=onAdd,modifier=Modifier.fillMaxWidth()){Text("+ ${v13t(lang,"Добавить продукт","Add food","Dodaj produkt","Додати продукт")}")}}}}

@Composable private fun V13FoodManager(lang:AppLanguage,foods:List<V13Food>,onClose:()->Unit,onSave:(List<V13Food>)->Unit,onEdit:(V13Food)->Unit){var list by remember(foods){mutableStateOf(foods)};ModalBottomSheet(onDismissRequest=onClose,containerColor=NztSurface){LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Row(verticalAlignment=Alignment.CenterVertically){Text(v13t(lang,"Каталог продуктов","Food catalog","Katalog produktów","Каталог продуктів"),fontSize=22.sp,fontWeight=FontWeight.Black,modifier=Modifier.weight(1f));Button(onClick={onEdit(V13Food(UUID.randomUUID().toString(),"",100.0,0.0,0.0,0.0))}){Text("+")}}};items(list,key={it.id}){f->Card(Modifier.fillMaxWidth().clickable{onEdit(f)},colors=CardDefaults.cardColors(containerColor=NztSurface2)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(f.name,fontWeight=FontWeight.Bold);Text("${f.kcal.roundToInt()} kcal · P ${f.p} · F ${f.f} · C ${f.c}",color=NztMuted,fontSize=11.sp)};Icon(Icons.Default.Edit,null,tint=NztAccent)}}};item{Button(onClick={onSave(list)},modifier=Modifier.fillMaxWidth()){Text(v13t(lang,"Готово","Done","Gotowe","Готово"))}}}}}

@Composable private fun V13EditFood(lang:AppLanguage,food:V13Food,onClose:()->Unit,onSave:(V13Food)->Unit,onDelete:(String)->Unit){var name by remember{mutableStateOf(food.name)};var kcal by remember{mutableStateOf(food.kcal.toString())};var p by remember{mutableStateOf(food.p.toString())};var f by remember{mutableStateOf(food.f.toString())};var c by remember{mutableStateOf(food.c.toString())};AlertDialog(onDismissRequest=onClose,title={Text(v13t(lang,"Продукт на 100 г","Food per 100 g","Produkt / 100 g","Продукт на 100 г"))},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(name,{name=it},label={Text(v13t(lang,"Название","Name","Nazwa","Назва"))});listOf("kcal" to kcal,"P" to p,"F" to f,"C" to c).forEach{(lab,v)->OutlinedTextField(v,{nv->when(lab){"kcal"->kcal=nv;"P"->p=nv;"F"->f=nv;else->c=nv}},label={Text(lab)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)}}},confirmButton={Button(enabled=name.isNotBlank(),onClick={fun d(s:String)=s.replace(',','.').toDoubleOrNull()?:0.0;onSave(food.copy(name=name.trim(),kcal=d(kcal),p=d(p),f=d(f),c=d(c)))}){Text(v13t(lang,"Сохранить","Save","Zapisz","Зберегти"))}},dismissButton={Row{if(food.name.isNotBlank())TextButton(onClick={onDelete(food.id)}){Text(v13t(lang,"Удалить","Delete","Usuń","Видалити"),color=NztDanger)};TextButton(onClick=onClose){Text(v13t(lang,"Отмена","Cancel","Anuluj","Скасувати"))}}},containerColor=NztSurface)}

private fun v13Meal(l:AppLanguage,m:String)=when(m){"BREAKFAST"->v13t(l,"Завтрак","Breakfast","Śniadanie","Сніданок");"LUNCH"->v13t(l,"Обед","Lunch","Obiad","Обід");"SNACK"->v13t(l,"Перекус","Snack","Przekąska","Перекус");else->v13t(l,"Ужин","Dinner","Kolacja","Вечеря")}
private fun v13t(l:AppLanguage,ru:String,en:String,pl:String,uk:String)=when(l){AppLanguage.RU->ru;AppLanguage.EN->en;AppLanguage.PL->pl;AppLanguage.UK->uk}
