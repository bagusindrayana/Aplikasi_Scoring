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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.stinkmtul.mytarget.CustomKeyboard
import com.stinkmtul.mytarget.R
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.ui.home.MainActivity
import kotlinx.coroutines.runBlocking

data class ShotData(
    val shotNumber: Int,
    val score: Int,
    val scoreType: String
) {
    fun getDisplayValue(): String {
        return when {
            scoreType == "X" -> "X"
            scoreType == "M" -> "M"
            score == 0 -> ""
            else -> score.toString()
        }
    }

    fun getDisplayScore(): Int {
        return when (scoreType) {
            "X" -> 10
            "M" -> 0
            else -> score
        }
    }

    fun getTextColor(): Int {
        return when {
            scoreType == "X" -> Color.parseColor("#D32F2F")
            scoreType == "M" -> Color.parseColor("#FF6D00")
            score >= 9 -> Color.parseColor("#2E7D32")
            score >= 7 -> Color.parseColor("#388E3C")
            score >= 5 -> Color.parseColor("#689F38")
            else -> Color.BLACK
        }
    }

    fun getSortingValue(): Int {
        return when (scoreType) {
            "X" -> 11
            "M" -> 0
            else -> score
        }
    }
}

class DataActivity : AppCompatActivity(), CustomKeyboard.KeyboardListener {

    private var hasUnsavedChanges = false
    private var isNewData = false

    private var trainingIdInt: Int = 0
    private var currentEditText: EditText? = null
    private lateinit var customKeyboard: CustomKeyboard
    private lateinit var textDescription: TextView
    private lateinit var textRambahan: TextView
    private lateinit var textAnakPanah: TextView
    private lateinit var arrowback: ImageView

    private lateinit var dataViewModel: DataViewModel
    private val allScores = mutableMapOf<Triple<Int, Int, Int>, Int>()
    private val allScoreTypes = mutableMapOf<Triple<Int, Int, Int>, String>()
    private val finalScores = mutableMapOf<Int, Int>()
    private val nameMapping = mutableMapOf<Int, String>()

    private val allEditTexts = mutableListOf<EditText>()
    private val existingShots = mutableMapOf<Int, MutableList<Shot>>()
    private var allDataLoaded = false
    private var isUpdatingUI = false
    private val shotsPerSession = mutableMapOf<Pair<Int, Int>, MutableList<ShotData>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        initializeViews()
        setupViewModel()
        loadIntentData()
        setupUI()
        loadExistingShots()
    }

    private fun initializeViews() {
        textDescription = findViewById(R.id.textdescription)
        textRambahan = findViewById(R.id.textrambahan)
        textAnakPanah = findViewById(R.id.textanakpanah)
        customKeyboard = findViewById(R.id.custom_keyboard)
        arrowback = findViewById(R.id.btn_back)

        arrowback.setOnClickListener {
            onBackPressed()
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        customKeyboard.visibility = View.GONE
        customKeyboard.setKeyboardListener(this)
    }

    private fun setupViewModel() {
        val factory = DataViewModelFactory(application)
        dataViewModel = ViewModelProvider(this, factory)[DataViewModel::class.java]
    }

    private fun loadIntentData() {
        val selectedNames = intent.getStringExtra("selected_names")?.split(", ") ?: listOf("No Selection")
        val selectedPersonIdsString = intent.getStringExtra("selected_person_ids")?.split(", ") ?: emptyList<String>()
        val selectedPersonIds = selectedPersonIdsString.mapNotNull { it.toIntOrNull() }

        val session = intent.getStringExtra("session")?.toIntOrNull() ?: 0
        val score = intent.getStringExtra("score")?.toIntOrNull() ?: 0
        val description = intent.getStringExtra("description")?.toString()?.split(" ")
            ?.joinToString(" ") { it.capitalize() } ?: "No Data"
        val date = intent.getStringExtra("date")?.toString() ?: "No Data"
        val token = intent.getStringExtra("token")?.toString() ?: "No Data"
        val trainingid = intent.getStringExtra("trainingid") ?: "No Data"
        trainingIdInt = trainingid.toIntOrNull() ?: 0

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
    }

    private fun setupUI() {
        val container: LinearLayout = findViewById(R.id.container)
        container.removeAllViews()
        allEditTexts.clear()
    }

    private fun loadExistingShots() {
        val selectedNames = intent.getStringExtra("selected_names")?.split(", ") ?: listOf("No Selection")
        val selectedPersonIdsString = intent.getStringExtra("selected_person_ids")?.split(", ") ?: emptyList<String>()
        val selectedPersonIds = selectedPersonIdsString.mapNotNull { it.toIntOrNull() }
        val session = intent.getStringExtra("session")?.toIntOrNull() ?: 0
        val score = intent.getStringExtra("score")?.toIntOrNull() ?: 0

        selectedPersonIds.forEach { personId ->
            dataViewModel.getShotsForPerson(personId, trainingIdInt).observe(this) { shotsList ->
                existingShots[personId] = shotsList.toMutableList()
                Log.d("DataActivity", "Loaded ${shotsList.size} shots for person $personId")

                if (existingShots.size == selectedPersonIds.size) {
                    allDataLoaded = true

                    var totalShots = 0
                    existingShots.forEach { (_, shots) -> totalShots += shots.size }
                    isNewData = totalShots == 0

                    if (isNewData) {
                        hasUnsavedChanges = true
                    }

                    createTablesWithData(selectedNames, selectedPersonIds, session, score)
                }
            }
        }
    }


    private fun createTablesWithData(
        selectedNames: List<String>,
        selectedPersonIds: List<Int>,
        session: Int,
        shots: Int
    ) {
        val container: LinearLayout = findViewById(R.id.container)
        container.removeAllViews()

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
                val formattedName = name
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                text = "#$formattedName"
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 10, 0, 10)
            }

            personContainer.addView(titleTextView)
            personContainer.addView(scrollView)
            container.addView(personContainer)

            createStyledTable(tableLayout, session, shots, selectedNames, selectedPersonIds, index, trainingIdInt.toString(), personId)
        }
    }

    private fun createStyledTable(
        tableLayout: TableLayout,
        rows: Int,
        shots: Int,
        selectedNames: List<String>,
        selectedPersonIds: List<Int>,
        personIndex: Int,
        trainingid: String,
        personId: Int
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

        for (i in 1..rows) {
            val sessionKey = Pair(personId, i)
            if (!shotsPerSession.containsKey(sessionKey)) {
                shotsPerSession[sessionKey] = mutableListOf()
            }
        }

        val personShots = existingShots[personId] ?: mutableListOf()
        personShots.forEach { shot ->
            if (shot.session!! <= rows && shot.shot_number!! <= shots) {
                val scoreType = shot.scoretype ?: "0"
                val shotData = ShotData(
                    shotNumber = shot.shot_number!!,
                    score = shot.score!!,
                    scoreType = scoreType
                )

                val sessionKey = Pair(personId, shot.session!!)
                shotsPerSession.getOrPut(sessionKey) { mutableListOf() }.add(shotData)

                allScores[Triple(personId, shot.session, shot.shot_number) as Triple<Int, Int, Int>] = shot.score!!
                allScoreTypes[Triple(personId, shot.session, shot.shot_number) as Triple<Int, Int, Int>] = scoreType
            }
        }

        shotsPerSession.forEach { (sessionKey, shotsList) ->
            shotsList.sortByDescending { it.getSortingValue() }
        }

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

            val sessionTextView = TextView(this).apply {
                text = i.toString()
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(20, 16, 20, 16)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.TRANSPARENT)
                    cornerRadius = 4f
                }
            }
            tableRow.addView(sessionTextView)

            val sessionKey = Pair(personId, i)
            val sessionShots = shotsPerSession[sessionKey] ?: mutableListOf()

            val rowEditTexts = mutableListOf<EditText>()

            for (j in 1..shots) {
                val editText = EditText(this).apply {
                    inputType = InputType.TYPE_CLASS_TEXT
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    imeOptions = EditorInfo.IME_ACTION_NEXT

                    val displayValue = if (j <= sessionShots.size) {
                        val shotData = sessionShots[j-1]
                        setTextColor(shotData.getTextColor())
                        // Ubah tampilan skor 0 menjadi string kosong
                        if (shotData.scoreType != "M" && shotData.score == 0) {
                            ""
                        } else {
                            shotData.getDisplayValue()
                        }
                    } else {
                        ""
                    }
                    setText(displayValue)

                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(Color.WHITE)
                        setStroke(1, Color.LTGRAY)
                        cornerRadius = 4f
                    }

                    layoutParams = TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(4, 2, 0, 2)
                    }
                    setPadding(20, 16, 20, 16)

                    showSoftInputOnFocus = false

                    setOnFocusChangeListener { _, hasFocus ->
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

                    tag = j
                }

                rowEditTexts.add(editText)
                allEditTexts.add(editText)
                tableRow.addView(editText)
            }

            for (j in 0 until shots) {
                val editText = rowEditTexts[j]
                editText.addTextChangedListener(createSortedTextWatcher(i, personId, shots, rowEditTexts, totalTextViews, endTextViews))
            }

            val totalTextView = TextView(this).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(20, 16, 20, 16)

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#E8F5E9"))
                    setStroke(1, Color.parseColor("#81C784"))
                    cornerRadius = 4f
                }

                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(4, 2, 0, 2)
                }
                minimumWidth = 80

                val sessionKey = Pair(personId, i)
                val rowTotal = shotsPerSession[sessionKey]?.sumOf { it.getDisplayScore() } ?: 0
                if (rowTotal > 0) {
                    text = rowTotal.toString()
                    allScores[Triple(personId, i, shots + 1)] = rowTotal
                    allScoreTypes[Triple(personId, i, shots + 1)] = "0"
                }
            }
            tableRow.addView(totalTextView)
            totalTextViews.add(totalTextView)

            val endTextView = TextView(this).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(20, 16, 20, 16)

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#E3F2FD"))
                    setStroke(1, Color.parseColor("#64B5F6"))
                    cornerRadius = 4f
                }

                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(4, 2, 0, 2)
                }
                minimumWidth = 80

                val cumulativeTotal = totalTextViews.mapIndexed { index, textView ->
                    if (index <= i-1) textView.text.toString().toIntOrNull() ?: 0 else 0
                }.sum()
                if (cumulativeTotal > 0) {
                    text = cumulativeTotal.toString()
                    allScores[Triple(personId, i, shots + 2)] = cumulativeTotal
                    allScoreTypes[Triple(personId, i, shots + 2)] = "0"
                    finalScores[personId] = cumulativeTotal
                }
            }
            tableRow.addView(endTextView)
            endTextViews.add(endTextView)

            tableLayout.addView(tableRow)
        }

        setupSaveButton(selectedPersonIds, trainingid)
    }

    private fun createSortedTextWatcher(
        session: Int,
        personId: Int,
        totalShots: Int,
        rowEditTexts: List<EditText>,
        totalTextViews: List<TextView>,
        endTextViews: List<TextView>
    ): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdatingUI) return

                s?.let {
                    val input = it.toString().uppercase()
                    var scoreValue = -1
                    var scoreType = ""

                    when {
                        input == "X" -> {
                            scoreValue = 10
                            scoreType = "X"
                        }
                        input == "M" -> {
                            scoreValue = 0
                            scoreType = "M"
                        }
                        input.isNotEmpty() && input.all { it.isDigit() } -> {
                            scoreValue = input.toInt()
                            scoreType = "0"
                        }
                    }

                    if (scoreValue != -1) {
                        hasUnsavedChanges = true
                        val sessionKey = Pair(personId, session)
                        val sessionShots = shotsPerSession.getOrPut(sessionKey) { mutableListOf() }

                        val visualPos = currentEditText?.tag as? Int ?: 1

                        var updatedExisting = false
                        for (i in sessionShots.indices) {
                            if (sessionShots[i].shotNumber == visualPos) {
                                sessionShots[i] = ShotData(visualPos, scoreValue, scoreType)
                                updatedExisting = true
                                break
                            }
                        }

                        if (!updatedExisting) {
                            sessionShots.add(ShotData(visualPos, scoreValue, scoreType))
                        }

                        sessionShots.sortByDescending { it.getSortingValue() }

                        updateRowDisplay(rowEditTexts, sessionShots)

                        updateSortedTotals(session, personId, totalShots, totalTextViews, endTextViews)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingUI) return

                if (!s.isNullOrEmpty()) {
                    val input = s.toString()
                    if (input.equals("x", ignoreCase = true) && input != "X") {
                        isUpdatingUI = true
                        s.replace(0, s.length, "X")
                        isUpdatingUI = false
                    } else if (input.equals("m", ignoreCase = true) && input != "M") {
                        isUpdatingUI = true
                        s.replace(0, s.length, "M")
                        isUpdatingUI = false
                    }
                }
            }
        }
    }

    private fun updateRowDisplay(rowEditTexts: List<EditText>, sessionShots: List<ShotData>) {
        isUpdatingUI = true

        try {
            for (i in rowEditTexts.indices) {
                val editText = rowEditTexts[i]
                if (i < sessionShots.size) {
                    val shotData = sessionShots[i]
                    val displayValue = if (shotData.scoreType != "M" && shotData.score == 0) {
                        ""
                    } else {
                        shotData.getDisplayValue()
                    }
                    editText.setText(displayValue)
                    editText.setTextColor(shotData.getTextColor())

                    editText.tag = shotData.shotNumber
                } else {
                    editText.setText("")
                    editText.setTextColor(Color.BLACK)
                    editText.tag = i + 1
                }
            }
        } finally {
            isUpdatingUI = false
        }
    }

    private fun updateSortedTotals(
        session: Int,
        personId: Int,
        totalShots: Int,
        totalTextViews: List<TextView>,
        endTextViews: List<TextView>
    ) {
        val rowIndex = session - 1
        val sessionKey = Pair(personId, session)

        val rowTotal = shotsPerSession[sessionKey]?.sumOf { it.getDisplayScore() } ?: 0

        totalTextViews[rowIndex].text = rowTotal.toString()
        allScores[Triple(personId, session, totalShots + 1)] = rowTotal
        allScoreTypes[Triple(personId, session, totalShots + 1)] = "0"

        var runningTotal = 0
        for (i in 0 until totalTextViews.size) {
            val currentTotal = totalTextViews[i].text.toString().toIntOrNull() ?: 0
            runningTotal += currentTotal

            endTextViews[i].text = runningTotal.toString()
            allScores[Triple(personId, i + 1, totalShots + 2)] = runningTotal
            allScoreTypes[Triple(personId, i + 1, totalShots + 2)] = "0"

            if (i == totalTextViews.size - 1) {
                finalScores[personId] = runningTotal
            }
        }
    }

    private fun setupSaveButton(selectedPersonIds: List<Int>, trainingid: String) {
        val saveButton: MaterialButton = findViewById(R.id.btn_save)
        saveButton.setOnClickListener {
            var countSaved = 0

            val session = intent.getStringExtra("session")?.toIntOrNull() ?: 0
            val shots = intent.getStringExtra("score")?.toIntOrNull() ?: 0

            selectedPersonIds.forEach { personId ->
                for (i in 1..session) {
                    val sessionKey = Pair(personId, i)
                    val sessionShots = shotsPerSession.getOrPut(sessionKey) { mutableListOf() }

                    val existingShotNumbers = sessionShots.map { it.shotNumber }.toSet()

                    for (j in 1..shots) {
                        if (j !in existingShotNumbers) {
                            sessionShots.add(ShotData(j, 0, "0"))
                        }
                    }

                    sessionShots.sortByDescending { it.getSortingValue() }

                    val totalScore = allScores[Triple(personId, i, shots + 1)] ?: 0
                    val totalScoreType = allScoreTypes[Triple(personId, i, shots + 1)] ?: "0"
                    val existingTotalShot = existingShots[personId]?.find {
                        it.session == i && it.shot_number == shots + 1
                    }

                    if (existingTotalShot != null) {
                        val updatedTotalShot = Shot(
                            shot_id = existingTotalShot.shot_id,
                            session = i,
                            shot_number = shots + 1,
                            score = totalScore,
                            person_id = personId,
                            training_id = trainingIdInt,
                            scoretype = totalScoreType
                        )
                        dataViewModel.update(updatedTotalShot)
                        countSaved++
                    } else {
                        val newTotalShot = Shot(
                            session = i,
                            shot_number = shots + 1,
                            score = totalScore,
                            person_id = personId,
                            training_id = trainingIdInt,
                            scoretype = totalScoreType
                        )
                        dataViewModel.insert(newTotalShot)
                        countSaved++
                    }

                    val endScore = allScores[Triple(personId, i, shots + 2)] ?: 0
                    val endScoreType = allScoreTypes[Triple(personId, i, shots + 2)] ?: "0"
                    val existingEndShot = existingShots[personId]?.find {
                        it.session == i && it.shot_number == shots + 2
                    }

                    if (existingEndShot != null) {
                        val updatedEndShot = Shot(
                            shot_id = existingEndShot.shot_id,
                            session = i,
                            shot_number = shots + 2,
                            score = endScore,
                            person_id = personId,
                            training_id = trainingIdInt,
                            scoretype = endScoreType
                        )
                        dataViewModel.update(updatedEndShot)
                        countSaved++
                    } else {
                        val newEndShot = Shot(
                            session = i,
                            shot_number = shots + 2,
                            score = endScore,
                            person_id = personId,
                            training_id = trainingIdInt,
                            scoretype = endScoreType
                        )
                        dataViewModel.insert(newEndShot)
                        countSaved++
                    }
                }
            }

            shotsPerSession.forEach { (sessionKey, sortedShots) ->
                val (personId, session) = sessionKey

                sortedShots.forEachIndexed { index, shotData ->
                    val sortedShotNumber = index + 1
                    val existingShot = existingShots[personId]?.find {
                        it.session == session && it.shot_number == shotData.shotNumber
                    }

                    if (existingShot != null) {
                        val updatedShot = Shot(
                            shot_id = existingShot.shot_id,
                            session = session,
                            shot_number = sortedShotNumber,
                            score = shotData.score,
                            person_id = personId,
                            training_id = trainingIdInt,
                            scoretype = shotData.scoreType
                        )
                        dataViewModel.update(updatedShot)
                        countSaved++
                    } else {
                        val newShot = Shot(
                            session = session,
                            shot_number = sortedShotNumber,
                            score = shotData.score,
                            person_id = personId,
                            training_id = trainingIdInt,
                            scoretype = shotData.scoreType
                        )
                        dataViewModel.insert(newShot)
                        countSaved++
                    }
                }
            }
            hasUnsavedChanges = false
            isNewData = false

            Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
            showRankings()
            val intent = Intent(this@DataActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun showRankings() {
        val xCounts = mutableMapOf<Int, Int>()

        shotsPerSession.forEach { (sessionKey, shotsList) ->
            val personId = sessionKey.first

            shotsList.forEach { shotData ->
                if (shotData.scoreType == "X") {
                    xCounts[personId] = (xCounts[personId] ?: 0) + 1
                    Log.d("XCount", "Added X for Person ID: $personId, Shot: ${shotData.shotNumber}, Total X: ${xCounts[personId]}")
                }
            }
        }

        xCounts.forEach { (personId, count) ->
            val name = nameMapping[personId] ?: "Unknown"
            Log.d("XCountSummary", "Person: $name (ID: $personId), X Count: $count")
        }

        val playerRankings = finalScores.map { (personId, score) ->
            Triple(personId, score, xCounts[personId] ?: 0)
        }.toMutableList()

        playerRankings.sortWith { a, b ->
            val (aPersonId, aScore, aXCount) = a
            val (bPersonId, bScore, bXCount) = b

            when {
                aScore != bScore -> bScore.compareTo(aScore)
                aXCount != bXCount -> bXCount.compareTo(aXCount)
                else -> aPersonId.compareTo(bPersonId)
            }
        }

        playerRankings.forEachIndexed { index, (personId, score, xCount) ->
            val name = nameMapping[personId] ?: "Unknown"
            Log.d("Ranking", "Rank ${index+1}: Person: $name (ID: $personId), Score: $score, X Count: $xCount")
        }

        val trainingId = trainingIdInt

        runBlocking {
            val existingEntries = dataViewModel.getAllLeaderboardSync(trainingId)
            val existingEntriesMap = existingEntries.associateBy { it.person_id }

            playerRankings.forEachIndexed { index, (personId, score, xCount) ->
                val name = nameMapping[personId] ?: "Peserta tidak dikenal"
                val rank = index + 1

                val existingEntry = existingEntriesMap[personId]

                if (existingEntry != null) {
                    val updatedEntry = Leaderboard(
                        leaderboard_id = existingEntry.leaderboard_id,
                        person_id = personId,
                        score = score,
                        the_champion = rank,
                        training_id = trainingId
                    )
                    dataViewModel.update(updatedEntry)
                    Log.d("Leaderboard", "Updated: Rank $rank, Person: $name (ID: $personId), Score: $score, X count: $xCount")
                } else {
                    val newEntry = Leaderboard(
                        person_id = personId,
                        score = score,
                        the_champion = rank,
                        training_id = trainingId
                    )
                    dataViewModel.insertLeaderboard(newEntry)
                    Log.d("Leaderboard", "Inserted: Rank $rank, Person: $name (ID: $personId), Score: $score, X count: $xCount")
                }
            }
        }
    }

    override fun onKeyPressed(value: String) {
        currentEditText?.let { editText ->
            if (value == "ENTER") {
                if (editText.text.isNotEmpty()) {
                    editText.setBackgroundColor(Color.WHITE)
                    focusNextEditText(editText)
                } else {
                    editText.setBackgroundColor(Color.rgb(255, 240, 240))
                }
            } else {
                editText.setText(value)
                editText.setBackgroundColor(Color.WHITE)
                editText.setSelection(editText.text.length)

                if (editText.text.isNotEmpty()) {
                    focusNextEditText(editText)
                }
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (customKeyboard.visibility == View.VISIBLE) {
            customKeyboard.visibility = View.GONE
            return
        }

        if (hasUnsavedChanges || isNewData) {
            val message = if (isNewData) {
                "Data tabel belum disimpan. Silahkan tekan tombol SIMPAN untuk menyimpan data."
            } else {
                "Silahkan tekan tombol SIMPAN untuk menyimpan perubahan Anda"
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage(message)
                .setPositiveButton("Ok", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}