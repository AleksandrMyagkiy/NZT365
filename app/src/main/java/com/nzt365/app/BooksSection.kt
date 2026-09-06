package com.nzt365.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

data class NZTBook(
    val number: Int,
    val author: String,
    val title: String,
    val purpose: String
)

private val books = listOf(
    NZTBook(
        1,
        "Chris Voss",
        "Never Split the Difference",
        "Переговоры и влияние"
    ),
    NZTBook(
        2,
        "James Clear",
        "Atomic Habits",
        "Системы и привычки"
    ),
    NZTBook(
        3,
        "Morgan Housel",
        "The Psychology of Money",
        "Деньги и мышление"
    ),
    NZTBook(
        4,
        "Robert Cialdini",
        "Influence",
        "Психология убеждения"
    ),
    NZTBook(
        5,
        "Cal Newport",
        "Deep Work",
        "Концентрация и продуктивность"
    ),
    NZTBook(
        6,
        "Peter Drucker",
        "The Effective Executive",
        "Рост до руководителя"
    ),
    NZTBook(
        7,
        "Jim Collins",
        "Good to Great",
        "Системное управление"
    ),
    NZTBook(
        8,
        "Ray Dalio",
        "Principles",
        "Система принятия решений"
    ),
    NZTBook(
        9,
        "Daniel Kahneman",
        "Thinking, Fast and Slow",
        "Мышление и когнитивные ошибки"
    ),
    NZTBook(
        10,
        "Robert Greene",
        "The 48 Laws of Power",
        "Влияние, статус и власть"
    ),
    NZTBook(
        11,
        "Nassim Nicholas Taleb",
        "Antifragile",
        "Устойчивость и неопределённость"
    ),
    NZTBook(
        12,
        "Benjamin Graham",
        "The Intelligent Investor",
        "Капитал и инвестиционная дисциплина"
    )
)

@Composable
fun BooksSection() {

    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "nzt_books",
            Context.MODE_PRIVATE
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "12 КНИГ / 12 МЕСЯЦЕВ",
            color = Color(0xFF9BA3AF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Text(
            text = "Цель: не просто прочитать, а внедрить хотя бы одну идею из каждой книги.",
            color = Color(0xFF9BA3AF),
            fontSize = 13.sp
        )

        books.forEach { book ->

            BookCard(
                book = book,
                initialProgress = prefs.getInt(
                    "book_${book.number}",
                    0
                ),
                onProgressChanged = { progress ->

                    prefs.edit()
                        .putInt(
                            "book_${book.number}",
                            progress
                        )
                        .apply()
                }
            )
        }
    }
}

@Composable
private fun BookCard(
    book: NZTBook,
    initialProgress: Int,
    onProgressChanged: (Int) -> Unit
) {

    var progress by remember {
        mutableIntStateOf(initialProgress)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF15181D)
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "MONTH ${book.number}",
                    color = Color(0xFFE9FF70),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        if (progress >= 100)
                            "✓ ПРОЧИТАНО"
                        else
                            "$progress%",
                    color =
                        if (progress >= 100)
                            Color(0xFFE9FF70)
                        else
                            Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = book.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = book.author,
                color = Color(0xFF9BA3AF),
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = book.purpose,
                color = Color(0xFFE9FF70),
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            LinearProgressIndicator(
                progress = {
                    progress / 100f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = Color(0xFFE9FF70),
                trackColor = Color(0xFF2A2F36)
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    onClick = {

                        progress =
                            (progress - 10)
                                .coerceAtLeast(0)

                        onProgressChanged(progress)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("-10%")
                }

                Button(
                    onClick = {

                        progress =
                            (progress + 10)
                                .coerceAtMost(100)

                        onProgressChanged(progress)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+10%")
                }
            }

            if (progress >= 100) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Следующий шаг: выбери одну идею из книги и примени её на практике.",
                    color = Color(0xFF9BA3AF),
                    fontSize = 12.sp
                )
            }
        }
    }
}
