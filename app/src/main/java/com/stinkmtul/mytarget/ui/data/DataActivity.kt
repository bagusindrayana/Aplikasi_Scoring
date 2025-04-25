package com.stinkmtul.mytarget.ui.data

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.stinkmtul.mytarget.CustomKeyboard
import com.stinkmtul.mytarget.R
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.ui.detail.DetailActivity
import com.stinkmtul.mytarget.ui.home.MainActivity

class DataActivity : AppCompatActivity(), CustomKeyboard.KeyboardListener {

    private var trainingIdInt: Int = 0
    private var currentEditText: EditText? = null
    private lateinit var customKeyboard: CustomKeyboard
    private lateinit var textDescription: TextView
    private lateinit var textRambahan: TextView
    private lateinit var textAnakPanah: TextView

    private lateinit var dataViewModel: DataViewModel
    private val allScores = mutableMapOf<Triple<Int, Int, Int>, Int>()
    private val allScoreTypes = mutableMapOf<Triple<Int, Int, Int>, String>()
    private val finalScores = mutableMapOf<Int, Int>()
    private val nameMapping = mutableMapOf<Int, String>()

    private val allEditTexts = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        textDescription = findViewById(R.id.textdescription)
        textRambahan = findViewById(R.id.textrambahan)
        textAnakPanah = findViewById(R.id.textanakpanah)
        customKeyboard = findViewById(R.id.custom_keyboard)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        customKeyboard = findViewById(R.id.custom_keyboard)
        customKeyboard.visibility = View.GONE
        customKeyboard.setKeyboardListener(this)

        val factory = DataViewModelFactory(application)
        dataViewModel = ViewModelProvider(this, factory)[DataViewModel::class.java]

        val selectedNames = intent.getStringExtra("selected_names")?.split(", ") ?: listOf("No Selection")
        val selectedPersonIdsString = intent.getStringExtra("selected_person_ids")?.split(", ") ?: emptyList<String>()
        val selectedPersonIds = selectedPersonIdsString.mapNotNull { it.toIntOrNull() }

        val session = intent.getStringExtra("session")?.toIntOrNull() ?: 0
        val score = intent.getStringExtra("score")?.toIntOrNull() ?: 0
        val description = intent.getStringExtra("description")?.toString() ?: "No Data"
        val date = intent.getStringExtra("date")?.toString() ?: "No Data"
        val token = intent.getStringExtra("token")?.toString() ?: "No Data"
        val trainingid = intent.getStringExtra("trainingid") ?: "No Data"
        trainingIdInt = trainingid.toIntOrNull() ?: 0

        // Display data in TextViews
        textDescription.text = "$description - $date"
        textRambahan.text = "$session"
        textAnakPanah.text = "$score"

        Log.d("DataActivity", "Selected Names: $selectedNames")
        Log.d("DataActivity", "Session: $session")
        Log.d("DataActivity", "Score: $score")
        Log.d("DataActivity", "Date: $date")
        Log.d("DataActivity", "Description: $description")
        Log.d("DataActivity", "Token: $token")
        Log.d("DataActivity", "Training ID: $trainingid")

        val container: LinearLayout = findViewById(R.id.container)
        container.removeAllViews()

        allEditTexts.clear()

        //satu tabel satu orang
        selectedNames.forEachIndexed { index, name ->
            val personId = selectedPersonIds.getOrElse(index) { 0 }

            nameMapping[personId] = name

            val personContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 90)
                }
            }

            val scrollView = HorizontalScrollView(this)
            val tableLayout = TableLayout(this)
            scrollView.addView(tableLayout)

            val titleTextView = TextView(this).apply {
                text = "Skor - $name"
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 10, 0, 10)
            }

            personContainer.addView(titleTextView)
            personContainer.addView(scrollView)

            container.addView(personContainer)

            Log.d("DataActivity", "Creating table for: $name (Person ID: $personId)")

            val totalSessions = session
            createStyledTable(tableLayout, totalSessions, score, selectedNames, selectedPersonIds, index, trainingid)
        }

        val saveButton: MaterialButton = findViewById(R.id.btn_save)
        saveButton.setOnClickListener {
            if (validateAllFieldsAreFilled()) {
                var countSaved = 0
                allScores.forEach { (key, score) ->
                    val (personId, session, shotNumber) = key
                    val scoreType = allScoreTypes[key] ?: ""

                    val shotData = Shot(
                        session = session,
                        shot_number = shotNumber,
                        score = score,
                        person_id = personId,
                        training_id = trainingIdInt,
                        scoretype = scoreType
                    )

                    dataViewModel.insert(shotData)
                    countSaved++
                }

                Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                Log.d("DatabaseSave", "Saved $countSaved data entries to database")

                showRankings()

                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("training_id", trainingid)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Semua kolom skor harus diisi!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateAllFieldsAreFilled(): Boolean {
        var allFilled = true
        for (editText in allEditTexts) {
            if (editText.text.toString().trim().isEmpty()) {
                editText.setBackgroundColor(Color.parseColor("#A04747"))
                allFilled = false
            } else {
                editText.setBackgroundColor(Color.WHITE)
            }
        }
        return allFilled
    }

    override fun onKeyPressed(value: String) {
        currentEditText?.let { editText ->
            if (value == "ENTER") {
                editText.setBackgroundColor(Color.WHITE)

                focusNextEditText(editText)
            } else {
                editText.setText(value)
                editText.setBackgroundColor(Color.WHITE)
                editText.setSelection(editText.text.length)

                focusNextEditText(editText)
            }
        }
    }

    private fun focusNextEditText(currentEditText: EditText) {
        val currentRow = currentEditText.parent as? TableRow
        val tableLayout = currentRow?.parent as? TableLayout

        if (currentRow != null && tableLayout != null) {
            val rowIndex = tableLayout.indexOfChild(currentRow)
            val columnIndex = currentRow.indexOfChild(currentEditText)

            var nextView: View? = null
            for (i in columnIndex + 1 until currentRow.childCount) {
                val child = currentRow.getChildAt(i)
                if (child is EditText) {
                    nextView = child
                    break
                }
            }

            if (nextView != null && nextView is EditText) {
                nextView.requestFocus()
                return
            }

            for (i in rowIndex + 1 until tableLayout.childCount) {
                val nextRow = tableLayout.getChildAt(i) as? TableRow
                if (nextRow != null) {
                    for (j in 0 until nextRow.childCount) {
                        val child = nextRow.getChildAt(j)
                        if (child is EditText) {
                            child.requestFocus()
                            return
                        }
                    }
                }
            }
        }
    }

    override fun onBackspace() {
        currentEditText?.let {
            val text = it.text
            if (text.isNotEmpty()) {
                it.setText("")
            }
        }
    }

    //menampilkan peringkat
    private fun showRankings() {
        val xCounts = mutableMapOf<Int, Int>()

        allScoreTypes.forEach { (key, scoreType) ->
            if (scoreType == "X") {
                val personId = key.first
                xCounts[personId] = (xCounts[personId] ?: 0) + 1
            }
        }

        xCounts.forEach { (personId, count) ->
            Log.d("Leaderboard", "Person ID: $personId has $count X scores")
        }

        val sortedScores = finalScores.entries.sortedWith(
            compareByDescending<Map.Entry<Int, Int>> { it.value }
                .thenByDescending { xCounts[it.key] ?: 0 }
        )

        Log.d("Leaderboard", "Sorted Scores: $sortedScores")

        val rankingText = StringBuilder("Peringkat Akhir:\n\n")
        val trainingid = intent.getStringExtra("trainingid") ?: "No Data"
        val trainingIdInt = trainingid.toIntOrNull() ?: 0

        sortedScores.forEachIndexed { index, entry ->
            val personId = entry.key
            val score = entry.value
            val name = nameMapping[personId] ?: "Peserta tidak dikenal"
            val xCount = xCounts[personId] ?: 0

            rankingText.append("${index + 1}. $name: $score poin (X: $xCount)\n")

            val championRank = index + 1

            val leaderboardEntry = Leaderboard(
                person_id = personId,
                score = score,
                the_champion = championRank,
                training_id = trainingIdInt
            )

            dataViewModel.insertLeaderboard(leaderboardEntry)
            Log.d("Leaderboard", "Saved: Rank ${index + 1}, Person: $name (ID: $personId), Score: $score, X count: $xCount, Champion: $championRank")
        }
    }

    private fun createStyledTable(
        tableLayout: TableLayout,
        rows: Int,
        shots: Int,
        selectedNames: List<String>,
        selectedPersonIds: List<Int>,
        personIndex: Int,
        trainingid: String
    ) {
        tableLayout.removeAllViews()
        val totalColumns = shots + 3

        val headerRow = TableRow(this)
        val headerTitles = listOf("Rambahan") + (1..shots).map { "Shot $it" } + listOf("Total", "End")

        val headerBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(this@DataActivity, R.color.grey))
            cornerRadius = 8f
        }

        headerTitles.forEachIndexed { index, title ->
            val textView = TextView(this).apply {
                text = title
                gravity = Gravity.CENTER
                setPadding(20, 16, 20, 16)
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                background = headerBackground
                if (index > 0) {
                    val params = TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(4, 0, 0, 0)
                    layoutParams = params
                }
            }
            headerRow.addView(textView)
        }
        tableLayout.addView(headerRow)

        val scores = Array(rows) { IntArray(shots) { -1 } }
        val totalTextViews = mutableListOf<TextView>()
        val endTextViews = mutableListOf<TextView>()

        val evenRowColor = Color.parseColor("#3C3D37")
        val oddRowColor = Color.parseColor("#3C3D37")

        for (i in 1..rows) {
            val tableRow = TableRow(this).apply {
                if (i % 2 == 0) {
                    setBackgroundColor(evenRowColor)
                } else {
                    setBackgroundColor(oddRowColor)
                }
            }
            val editTexts = mutableListOf<EditText>()

            val cellBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.TRANSPARENT)
                cornerRadius = 4f
            }

            for (j in 0 until totalColumns) {
                when (j) {
                    0 -> {
                        val sessionTextView = TextView(this).apply {
                            text = i.toString()
                            gravity = Gravity.CENTER
                            setTextColor(Color.WHITE)
                            typeface = Typeface.DEFAULT_BOLD
                            setPadding(20, 16, 20, 16)
                            background = cellBackground
                        }
                        tableRow.addView(sessionTextView)
                    }
                    totalColumns - 2 -> {
                        val totalTextView = TextView(this).apply {
                            text = ""
                            gravity = Gravity.CENTER
                            setTextColor(Color.BLACK)
                            typeface = Typeface.DEFAULT_BOLD
                            setPadding(20, 16, 20, 16)

                            val totalCellBackground = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                setColor(Color.parseColor("#E8F5E9"))
                                setStroke(1, Color.parseColor("#81C784"))
                                cornerRadius = 4f
                            }
                            background = totalCellBackground

                            val params = TableRow.LayoutParams(
                                TableRow.LayoutParams.MATCH_PARENT,
                                TableRow.LayoutParams.MATCH_PARENT
                            )
                            params.setMargins(4, 2, 0, 2)
                            layoutParams = params
                            minimumWidth = 80
                        }
                        tableRow.addView(totalTextView)
                        totalTextViews.add(totalTextView)
                    }
                    totalColumns - 1 -> {
                        val endTextView = TextView(this).apply {
                            text = ""
                            gravity = Gravity.CENTER
                            setTextColor(Color.BLACK)
                            typeface = Typeface.DEFAULT_BOLD
                            setPadding(20, 16, 20, 16)

                            val endCellBackground = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                setColor(Color.parseColor("#E3F2FD"))
                                setStroke(1, Color.parseColor("#64B5F6"))
                                cornerRadius = 4f
                            }
                            background = endCellBackground

                            val params = TableRow.LayoutParams(
                                TableRow.LayoutParams.MATCH_PARENT,
                                TableRow.LayoutParams.MATCH_PARENT
                            )
                            params.setMargins(4, 2, 0, 2)
                            layoutParams = params
                            minimumWidth = 80
                        }
                        tableRow.addView(endTextView)
                        endTextViews.add(endTextView)
                    }
                    else -> {
                        val editText = EditText(this).apply {
                            inputType = InputType.TYPE_CLASS_TEXT
                            filters = arrayOf(InputFilter.LengthFilter(2))
                            gravity = Gravity.CENTER
                            setTextColor(Color.BLACK)
                            imeOptions = EditorInfo.IME_ACTION_NEXT

                            val shotCellBackground = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                setColor(Color.WHITE)
                                setStroke(1, Color.LTGRAY)
                                cornerRadius = 4f
                            }
                            background = shotCellBackground

                            val params = TableRow.LayoutParams(
                                TableRow.LayoutParams.WRAP_CONTENT,
                                TableRow.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(4, 2, 0, 2)
                            layoutParams = params
                            setPadding(20, 16, 20, 16)

                            showSoftInputOnFocus = false

                            setOnFocusChangeListener { v, hasFocus ->
                                if (hasFocus) {
                                    currentEditText = this
                                    customKeyboard.visibility = View.VISIBLE
                                }
                            }

                            setOnClickListener {
                                currentEditText = this
                                customKeyboard.visibility = View.VISIBLE
                                requestFocus()
                            }
                        }

                        allEditTexts.add(editText)

                        editText.setText("")

                        val shotNumber = j
                        val personId = selectedPersonIds.getOrElse(personIndex) { 0 }

                        editText.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                val shotCellBackground = GradientDrawable().apply {
                                    shape = GradientDrawable.RECTANGLE
                                    setColor(Color.WHITE)
                                    setStroke(1, Color.LTGRAY)
                                    cornerRadius = 4f
                                }
                                editText.background = shotCellBackground

                                val input = s.toString().uppercase()
                                var scoreValue = -1
                                var scoreType = ""

                                when {
                                    input == "X" -> {
                                        scoreValue = 10
                                        scoreType = "X"
                                        editText.setTextColor(Color.parseColor("#D32F2F"))
                                        editText.setTypeface(null, Typeface.BOLD)
                                    }
                                    input == "M" -> {
                                        scoreValue = 0
                                        scoreType = "M"
                                        editText.setTextColor(Color.parseColor("#FF6D00"))
                                        editText.setTypeface(null, Typeface.BOLD)
                                    }
                                    input.isNotEmpty() && input.all { it.isDigit() } -> {
                                        scoreValue = input.toInt()
                                        scoreType = "0"

                                        when {
                                            scoreValue >= 9 -> editText.setTextColor(Color.parseColor("#2E7D32"))
                                            scoreValue >= 7 -> editText.setTextColor(Color.parseColor("#388E3C"))
                                            scoreValue >= 5 -> editText.setTextColor(Color.parseColor("#689F38"))
                                            else -> editText.setTextColor(Color.BLACK)
                                        }
                                        editText.setTypeface(null, Typeface.NORMAL)
                                    }
                                    else -> {
                                        editText.setTextColor(Color.BLACK)
                                        editText.setTypeface(null, Typeface.NORMAL)
                                    }
                                }

                                if (scoreValue != -1) {
                                    val rowIndex = i - 1
                                    val colIndex = j - 1
                                    if (colIndex > 0) {
                                        val currentRow = editText.parent as? TableRow

                                        val currentCompareValue = when (scoreType) {
                                            "X" -> 11
                                            "M" -> -1
                                            else -> scoreValue
                                        }

                                        var targetPosition = 0
                                        for (prevColIndex in 0 until colIndex) {
                                            val prevShotValue = scores[rowIndex][prevColIndex]
                                            val prevScoreType = allScoreTypes[Triple(personId, i, prevColIndex + 1)] ?: "0"
                                            val prevCompareValue = when {
                                                prevShotValue == 10 && prevScoreType == "X" -> 11
                                                prevShotValue == 0 && prevScoreType == "M" -> -1
                                                else -> prevShotValue
                                            }

                                            if (currentCompareValue <= prevCompareValue || prevShotValue == -1) {
                                                targetPosition = prevColIndex + 1
                                            }
                                        }

                                        if (targetPosition < colIndex && scores[rowIndex][targetPosition] != -1) {
                                            val currentText = input
                                            val currentScore = scoreValue
                                            val currentType = scoreType

                                            val editTexts = ArrayList<Pair<EditText, TextWatcher>>()
                                            for (col in targetPosition until colIndex + 1) {
                                                val cellEditText = currentRow?.getChildAt(col + 1) as? EditText
                                                if (cellEditText != null) {
                                                    val watcher = cellEditText.tag as? TextWatcher
                                                    if (watcher != null) {
                                                        cellEditText.removeTextChangedListener(watcher)
                                                        editTexts.add(Pair(cellEditText, watcher))
                                                    }
                                                }
                                            }

                                            for (col in colIndex downTo targetPosition + 1) {
                                                scores[rowIndex][col] = scores[rowIndex][col - 1]

                                                val cellEditText = currentRow?.getChildAt(col + 1) as? EditText
                                                if (cellEditText != null) {
                                                    val prevText = (currentRow.getChildAt(col) as? EditText)?.text?.toString() ?: ""
                                                    cellEditText.setText(prevText)

                                                    when {
                                                        prevText == "X" -> {
                                                            cellEditText.setTextColor(Color.parseColor("#D32F2F"))
                                                            cellEditText.setTypeface(null, Typeface.BOLD)
                                                        }
                                                        prevText == "M" -> {
                                                            cellEditText.setTextColor(Color.parseColor("#FF6D00"))
                                                            cellEditText.setTypeface(null, Typeface.BOLD)
                                                        }
                                                        prevText.isNotEmpty() && prevText.all { it.isDigit() } -> {
                                                            val value = prevText.toInt()
                                                            when {
                                                                value >= 9 -> cellEditText.setTextColor(Color.parseColor("#2E7D32"))
                                                                value >= 7 -> cellEditText.setTextColor(Color.parseColor("#388E3C"))
                                                                value >= 5 -> cellEditText.setTextColor(Color.parseColor("#689F38"))
                                                                else -> cellEditText.setTextColor(Color.BLACK)
                                                            }
                                                            cellEditText.setTypeface(null, Typeface.NORMAL)
                                                        }
                                                        else -> {
                                                            cellEditText.setTextColor(Color.BLACK)
                                                            cellEditText.setTypeface(null, Typeface.NORMAL)
                                                        }
                                                    }

                                                    val shotNum = col + 1
                                                    val prevShotNum = col
                                                    if (scores[rowIndex][col] != -1) {
                                                        allScores[Triple(personId, i, shotNum)] = scores[rowIndex][col]
                                                        allScoreTypes[Triple(personId, i, shotNum)] = allScoreTypes[Triple(personId, i, prevShotNum)] ?: "0"
                                                    } else {
                                                        allScores.remove(Triple(personId, i, shotNum))
                                                        allScoreTypes.remove(Triple(personId, i, shotNum))
                                                    }
                                                }
                                            }

                                            scores[rowIndex][targetPosition] = currentScore
                                            val targetEditText = currentRow?.getChildAt(targetPosition + 1) as? EditText
                                            if (targetEditText != null) {
                                                targetEditText.setText(currentText)

                                                when {
                                                    currentText == "X" -> {
                                                        targetEditText.setTextColor(Color.parseColor("#D32F2F"))
                                                        targetEditText.setTypeface(null, Typeface.BOLD)
                                                    }
                                                    currentText == "M" -> {
                                                        targetEditText.setTextColor(Color.parseColor("#FF6D00"))
                                                        targetEditText.setTypeface(null, Typeface.BOLD)
                                                    }
                                                    currentText.isNotEmpty() && currentText.all { it.isDigit() } -> {
                                                        val value = currentText.toInt()
                                                        when {
                                                            value >= 9 -> targetEditText.setTextColor(Color.parseColor("#2E7D32"))
                                                            value >= 7 -> targetEditText.setTextColor(Color.parseColor("#388E3C"))
                                                            value >= 5 -> targetEditText.setTextColor(Color.parseColor("#689F38"))
                                                            else -> targetEditText.setTextColor(Color.BLACK)
                                                        }
                                                        targetEditText.setTypeface(null, Typeface.NORMAL)
                                                    }
                                                }

                                                allScores[Triple(personId, i, targetPosition + 1)] = currentScore
                                                allScoreTypes[Triple(personId, i, targetPosition + 1)] = currentType
                                            }

                                            for (pair in editTexts) {
                                                pair.first.addTextChangedListener(pair.second)
                                            }

                                            val total = scores[rowIndex].filter { it != -1 }.sum()
                                            totalTextViews[rowIndex].text = if (total == 0 && !scores[rowIndex].any { it == 0 }) "" else total.toString()

                                            val cumulativeTotal = totalTextViews.mapIndexed { index, textView ->
                                                if (index <= rowIndex) textView.text.toString().toIntOrNull() ?: 0 else 0
                                            }.sum()
                                            endTextViews[rowIndex].text = if (cumulativeTotal == 0) "" else cumulativeTotal.toString()

                                            if (cumulativeTotal > 0) {
                                                finalScores[personId] = cumulativeTotal
                                            }

                                            if (total > 0) {
                                                val totalColumnIndex = shots + 1
                                                allScores[Triple(personId, i, totalColumnIndex)] = total
                                                allScoreTypes[Triple(personId, i, totalColumnIndex)] = "0"
                                            } else {
                                                val totalColumnIndex = shots + 1
                                                allScores.remove(Triple(personId, i, totalColumnIndex))
                                                allScoreTypes.remove(Triple(personId, i, totalColumnIndex))
                                            }

                                            if (cumulativeTotal > 0) {
                                                val endColumnIndex = shots + 2
                                                allScores[Triple(personId, i, endColumnIndex)] = cumulativeTotal
                                                allScoreTypes[Triple(personId, i, endColumnIndex)] = "0"
                                            } else {
                                                val endColumnIndex = shots + 2
                                                allScores.remove(Triple(personId, i, endColumnIndex))
                                                allScoreTypes.remove(Triple(personId, i, endColumnIndex))
                                            }

                                            return
                                        }
                                    }

                                    scores[i - 1][j - 1] = scoreValue
                                }

                                val rowIndex = i - 1
                                val totalTextView = totalTextViews[rowIndex]
                                val endTextView = endTextViews[rowIndex]

                                val total = scores[i - 1].filter { it != -1 }.sum()
                                totalTextView.text = if (total == 0 && !scores[i - 1].any { it == 0 }) "" else total.toString()

                                val cumulativeTotal = totalTextViews.mapIndexed { index, textView ->
                                    if (index <= rowIndex) textView.text.toString().toIntOrNull() ?: 0 else 0
                                }.sum()
                                endTextView.text = if (cumulativeTotal == 0) "" else cumulativeTotal.toString()

                                if (cumulativeTotal > 0) {
                                    finalScores[personId] = cumulativeTotal
                                }

                                val name = selectedNames.getOrElse(personIndex) { "Unknown" }
                                Log.d("TableIndex", "Session: $i, Shot: ${shotNumber}, Score: $scoreValue, ScoreType: $scoreType, Total: ${totalTextView.text}, End: ${endTextView.text}, Name: $name, Person ID: $personId, Training ID: $trainingid")

                                if (scoreValue != -1) {
                                    allScores[Triple(personId, i, shotNumber)] = scoreValue
                                    allScoreTypes[Triple(personId, i, shotNumber)] = scoreType
                                } else {
                                    allScores.remove(Triple(personId, i, shotNumber))
                                    allScoreTypes.remove(Triple(personId, i, shotNumber))
                                }

                                if (total > 0) {
                                    val totalColumnIndex = shots + 1
                                    allScores[Triple(personId, i, totalColumnIndex)] = total
                                    allScoreTypes[Triple(personId, i, totalColumnIndex)] = "0"
                                    Log.d("DatabaseTotal", "Menyimpan Total untuk Session: $i, Person: $personId, Index: $totalColumnIndex, Value: $total")
                                } else {
                                    val totalColumnIndex = shots + 1
                                    allScores.remove(Triple(personId, i, totalColumnIndex))
                                    allScoreTypes.remove(Triple(personId, i, totalColumnIndex))
                                }

                                if (cumulativeTotal > 0) {
                                    val endColumnIndex = shots + 2
                                    allScores[Triple(personId, i, endColumnIndex)] = cumulativeTotal
                                    allScoreTypes[Triple(personId, i, endColumnIndex)] = "0"
                                    Log.d("DatabaseEnd", "Menyimpan End untuk Session: $i, Person: $personId, Index: $endColumnIndex, Value: $cumulativeTotal")
                                } else {
                                    val endColumnIndex = shots + 2
                                    allScores.remove(Triple(personId, i, endColumnIndex))
                                    allScoreTypes.remove(Triple(personId, i, endColumnIndex))
                                }
                            }

                            override fun afterTextChanged(s: Editable?) {
                                if (!s.isNullOrEmpty()) {
                                    val input = s.toString()
                                    if (input.equals("x", ignoreCase = true) && input != "X") {
                                        s.replace(0, s.length, "X")
                                    } else if (input.equals("m", ignoreCase = true) && input != "M") {
                                        s.replace(0, s.length, "M")
                                    }
                                }
                            }
                        })

                        editTexts.add(editText)
                        tableRow.addView(editText)
                    }
                }
            }
            tableLayout.addView(tableRow)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (customKeyboard.visibility == View.VISIBLE) {
            customKeyboard.visibility = View.GONE
            return
        }

        AlertDialog.Builder(this).apply {
            setTitle("Konfirmasi Pembatalan")
            setMessage("Apakah Anda yakin ingin membatalkan kegiatan ini?")
            setPositiveButton("OK") { _, _ ->
                dataViewModel.deleteTrainingById(trainingIdInt)
                Toast.makeText(this@DataActivity, "Data dihapus", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@DataActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }
}