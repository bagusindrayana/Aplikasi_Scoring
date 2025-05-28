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
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AbsListView
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.stinkmtul.mytarget.CustomKeyboard
import com.stinkmtul.mytarget.R
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.person.Person
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.ui.data.DataActivity
import com.stinkmtul.mytarget.ui.home.MainActivity
import kotlinx.coroutines.runBlocking
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min

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

data class PersonShotItem(val person: Person, val shots: List<Shot>)


class PersonShotItemAdapter(
    private val itemList: List<PersonShotItem>,
    private val sessions: Int,
    private val shots: Int,
    private val trainingid: Int,
    private val createSortedTextWatcher: (
        session: Int,
        personId: Int,
        totalShots: Int,
        rowEditTexts: List<EditText>,
        totalTextViews: List<TextView>,
        endTextViews: List<TextView>
    ) -> TextWatcher,
    private val showKeyboard: (EditText) -> Unit
) :
    RecyclerView.Adapter<PersonShotItemAdapter.TableViewHolder>() {

    class TableViewHolder(val rowLayout: LinearLayout) : RecyclerView.ViewHolder(rowLayout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val linearLayout = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Tambahin TextView ke dalam LinearLayout
            addView(TextView(parent.context).apply {
                textSize = 18f
                setTextColor(Color.BLACK)
            })
        }

        return TableViewHolder(linearLayout)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        val item = itemList[position]

        val personId = item.person.person_id

        val personContainer = LinearLayout(holder.rowLayout.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 90)
            }
        }

        val scrollView = HorizontalScrollView(holder.rowLayout.context)
        val tableLayout = TableLayout(holder.rowLayout.context)
        scrollView.addView(tableLayout)

        val titleTextView = TextView(holder.rowLayout.context).apply {
            val formattedName = item.person.name?.split(" ")
                ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            text = "#$formattedName"
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 5, 0, 5)
        }

        personContainer.addView(titleTextView)
        personContainer.addView(scrollView)
        holder.rowLayout.addView(personContainer)

        val shotsPerSession = mutableMapOf<Pair<Int, Int>, MutableList<ShotData>>()
        tableLayout.removeAllViews()
        val totalColumns = shots + 3

        val headerRow = TableRow(holder.rowLayout.context)
        val headerTitles =
            listOf("Rambahan") + (1..shots).map { "Shot $it" } + listOf("Total", "End")

        val headerBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(holder.rowLayout.context, R.color.grey))
            cornerRadius = 8f
        }

        headerTitles.forEachIndexed { index, title ->
            val textView = TextView(holder.rowLayout.context).apply {
                text = title
                gravity = Gravity.CENTER
                setPadding(10, 8, 10, 8)
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                background = headerBackground
                if (index > 0) {
                    val params = TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(2, 0, 0, 0)
                    layoutParams = params
                }
            }
            headerRow.addView(textView)
        }
        tableLayout.addView(headerRow)

        for (i in 1..shots) {
            val sessionKey = Pair(personId, i)
            if (!shotsPerSession.containsKey(sessionKey)) {
                shotsPerSession[sessionKey] = mutableListOf()
            }
        }

        val personShots = item.shots
        personShots.forEach { shot ->
            if (shot.session!! <= sessions && shot.shot_number!! <= shots) {
                val scoreType = shot.scoretype ?: "0"
                val shotData = ShotData(
                    shotNumber = shot.shot_number!!,
                    score = shot.score!!,
                    scoreType = scoreType
                )

                val sessionKey = Pair(personId, shot.session!!)
                shotsPerSession.getOrPut(sessionKey) { mutableListOf() }.add(shotData)


            }
        }

        shotsPerSession.forEach { (sessionKey, shotsList) ->
            shotsList.sortByDescending { it.getSortingValue() }
        }

        val totalTextViews = mutableListOf<TextView>()
        val endTextViews = mutableListOf<TextView>()

        val evenRowColor = Color.parseColor("#3C3D37")
        val oddRowColor = Color.parseColor("#3C3D37")

        for (i in 1..sessions) {
            val tableRow = TableRow(holder.rowLayout.context).apply {
                if (i % 2 == 0) {
                    setBackgroundColor(evenRowColor)
                } else {
                    setBackgroundColor(oddRowColor)
                }
            }

            val sessionTextView = TextView(holder.rowLayout.context).apply {
                text = i.toString()
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(10, 8, 10, 8)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.TRANSPARENT)
                    cornerRadius = 2f
                }
            }
            tableRow.addView(sessionTextView)

            val sessionKey = Pair(personId, i)
            val sessionShots = shotsPerSession[sessionKey] ?: mutableListOf()

            val rowEditTexts = mutableListOf<EditText>()

            for (j in 1..shots) {
                val editText = EditText(holder.rowLayout.context).apply {
                    inputType = InputType.TYPE_CLASS_TEXT
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    imeOptions = EditorInfo.IME_ACTION_NEXT

                    val displayValue = if (j <= sessionShots.size) {
                        val shotData = sessionShots[j - 1]
                        Log.d("SHOT_DATA", shotData.getDisplayValue())
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
                        setMargins(2, 1, 0, 1)
                    }
                    setPadding(10, 8, 10, 8)

                    showSoftInputOnFocus = false

                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            Log.i("FOCUS_EDIT_TEXT",this.text.toString())
                            showKeyboard(this)
//                            currentEditText = this
//                            customKeyboard.visibility = View.VISIBLE
                        }
                    }

                    setOnClickListener {
                        Log.i("FOCUS_EDIT_TEXT",this.text.toString())
                        showKeyboard(this)
//                        currentEditText = this
//                        customKeyboard.visibility = View.VISIBLE
                        requestFocus()
                    }

                    tag = j
                }

                rowEditTexts.add(editText)
//                allEditTexts.add(editText)
                tableRow.addView(editText)
            }

            for (j in 0 until shots) {
                val editText = rowEditTexts[j]
                editText.addTextChangedListener(
                    createSortedTextWatcher(
                        i,
                        personId,
                        shots,
                        rowEditTexts,
                        totalTextViews,
                        endTextViews
                    )
                )
            }

            val totalTextView = TextView(holder.rowLayout.context).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(10, 8, 10, 8)

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
                    setMargins(2, 1, 0, 1)
                }
                minimumWidth = 30

                val sessionKey = Pair(personId, i)
                val rowTotal = shotsPerSession[sessionKey]?.sumOf { it.getDisplayScore() } ?: 0
                if (rowTotal > 0) {
                    text = rowTotal.toString()
//                    allScores[Triple(personId, i, shots + 1)] = rowTotal
//                    allScoreTypes[Triple(personId, i, shots + 1)] = "0"
                }
                tag = "total"
            }
            tableRow.addView(totalTextView)
            totalTextViews.add(totalTextView)

            val endTextView = TextView(holder.rowLayout.context).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(10, 8, 10, 8)

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
                    setMargins(2, 1, 0, 1)
                }
                minimumWidth = 30

                val cumulativeTotal = totalTextViews.mapIndexed { index, textView ->
                    if (index <= i - 1) textView.text.toString().toIntOrNull() ?: 0 else 0
                }.sum()
                if (cumulativeTotal > 0) {
                    text = cumulativeTotal.toString()
//                    allScores[Triple(personId, i, shots + 2)] = cumulativeTotal
//                    allScoreTypes[Triple(personId, i, shots + 2)] = "0"
//                    finalScores[personId] = cumulativeTotal
                }
            }
            tableRow.addView(endTextView)
            endTextViews.add(endTextView)

            tableLayout.addView(tableRow)
        }
    }

    override fun getItemCount() = itemList.size
}


class DataActivity : AppCompatActivity(), CustomKeyboard.KeyboardListener {

    private var hasUnsavedChanges = false
    private var isNewData = false
    private var hasLoaded = false

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
    private lateinit var recyclerView: RecyclerView
    private lateinit var collapsingToolbar: CollapsingToolbarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        initializeViews()
        setupViewModel()
        loadIntentData()
        setupUI()
        loadExistingShots()

//        val rangkingButton: CardView = findViewById(R.id.card_rangking)
//        rangkingButton.setOnClickListener {
//                        saveRankings()
//            val intent = Intent(this@DataActivity, MainActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//            startActivity(intent)
//        }
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

        collapsingToolbar = findViewById(R.id.collapsingToolbar)

        recyclerView = findViewById(R.id.personShotRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        //var mScrollY = 0F

//        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//                mScrollY += dy.toFloat()
//                mScrollY = max(mScrollY, 0F)
//                collapsingToolbar.translationY = min(-mScrollY, 0F)
//            }
//
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                super.onScrollStateChanged(recyclerView, newState)
//                when (newState) {
//                    RecyclerView.SCROLL_STATE_IDLE -> println("Scroll stopped")
//                    RecyclerView.SCROLL_STATE_DRAGGING -> println("Scrolling by user")
//                    RecyclerView.SCROLL_STATE_SETTLING -> println("Scroll settling")
//                }
//            }
//        })


    }

    private fun setupViewModel() {
        val factory = DataViewModelFactory(application)
        dataViewModel = ViewModelProvider(this, factory)[DataViewModel::class.java]
    }

    private fun loadIntentData() {
//        val selectedNames = intent.getStringExtra("selected_names")?.split(", ") ?: listOf("No Selection")
//        val selectedPersonIdsString = intent.getStringExtra("selected_person_ids")?.split(", ") ?: emptyList<String>()
//        val selectedPersonIds = selectedPersonIdsString.mapNotNull { it.toIntOrNull() }

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

//        Log.d("DataActivity", "Selected Names: $selectedNames")
        Log.d("DataActivity", "Session: $session")
        Log.d("DataActivity", "Score: $score")
        Log.d("DataActivity", "Date: $date")
        Log.d("DataActivity", "Description: $description")
        Log.d("DataActivity", "Token: $token")
        Log.d("DataActivity", "Training ID: $trainingid")
    }

    private fun setupUI() {
//        val container: LinearLayout = findViewById(R.id.container)
//        container.removeAllViews()
        allEditTexts.clear()
    }

    private fun loadExistingShots() {

        val session = intent.getStringExtra("session")?.toIntOrNull() ?: 0
        val score = intent.getStringExtra("score")?.toIntOrNull() ?: 0

        val selectedNames =
            intent.getStringExtra("selected_names")?.split(", ") ?: listOf("No Selection")
        val selectedPersonIdsString =
            intent.getStringExtra("selected_person_ids")?.split(", ") ?: emptyList<String>()
        val selectedPersonIds = selectedPersonIdsString.mapNotNull { it.toIntOrNull() }

        if (selectedPersonIdsString.isEmpty()) {
            dataViewModel.getDistinctPersonByTraining(trainingIdInt).observe(this) { personIds ->
                val selectedPersonIds = personIds



                if (personIds.isNotEmpty()) {
                    var itemList = mutableListOf<PersonShotItem>()
                    val nameList = mutableListOf<String>()
                    selectedPersonIds.forEach { personId ->


                        dataViewModel.getNamePerson(personId).observe(this) { name ->
                            Log.i("NAMA", name.toString())
                            nameList.add(name ?: "Nama tidak ditemukan")

                            dataViewModel.getShotsForPerson(personId, trainingIdInt)
                                .observe(this) { shotsList ->
                                    if (allDataLoaded) {
                                        return@observe
                                    }

                                    itemList.add(
                                        PersonShotItem(
                                            person = Person(personId, name),
                                            shots = shotsList
                                        )
                                    )

                                    existingShots[personId] = shotsList.toMutableList()
                                    existingShots[personId]?.forEach { shot ->

                                        shot.score?.let { it1 ->
                                            if (it1 > 0) {
                                                Log.i("DATA SHOT", shot.toString())
                                            }
                                        }
                                    }
                                    Log.d(
                                        "DataActivity",
                                        "Loaded ${shotsList.size} shots for person $personId"
                                    )

                                    if (existingShots.size == nameList.size) {


                                        var totalShots = 0
                                        existingShots.forEach { (_, shots) -> totalShots += shots.size }
                                        isNewData = totalShots == 0

                                        if (isNewData) {
                                            hasUnsavedChanges = true
                                        }

                                        createTablesWithData(nameList, selectedPersonIds, session, score)


                                        recyclerView.adapter = PersonShotItemAdapter(
                                            itemList,
                                            session,
                                            score,
                                            trainingIdInt,
                                            { session, personId, totalShots, rowEditTexts, totalTextViews, endTextViews ->
                                                createSortedTextWatcher(
                                                    session,
                                                    personId,
                                                    totalShots,
                                                    rowEditTexts,
                                                    totalTextViews,
                                                    endTextViews
                                                )
                                            },
                                            { editText ->
                                                currentEditText = editText
                                                customKeyboard.visibility = View.VISIBLE
                                            }
                                        )



                                        allDataLoaded = true

                                    }

                                }
                        }
                    }
                } else {

                }
            }
        } else {
            var itemList = mutableListOf<PersonShotItem>()
            val nameList = mutableListOf<String>()
            selectedPersonIds.forEach { personId ->

                dataViewModel.getNamePerson(personId).observe(this) { name ->
                    Log.i("NAMA", name.toString())
                    nameList.add(name ?: "Nama tidak ditemukan")

                    dataViewModel.getShotsForPerson(personId, trainingIdInt)
                        .observe(this) { shotsList ->
                            Log.i("shotsList", shotsList.size.toString())
                            if (allDataLoaded) {
                                return@observe
                            }

                            itemList.add(
                                PersonShotItem(
                                    person = Person(personId, name),
                                    shots = shotsList
                                )
                            )

                            existingShots[personId] = shotsList.toMutableList()
                            existingShots[personId]?.forEach { shot ->

                                shot.score?.let { it1 ->
                                    if (it1 > 0) {
                                        Log.i("DATA SHOT", shot.toString())
                                    }
                                }
                            }
                            Log.d(
                                "DataActivity",
                                "Loaded ${shotsList.size} shots for person $personId"
                            )

                            if (existingShots.size == nameList.size) {


                                var totalShots = 0
                                existingShots.forEach { (_, shots) -> totalShots += shots.size }
                                isNewData = totalShots == 0

                                if (isNewData) {
                                    hasUnsavedChanges = true
                                }

                                createTablesWithData(nameList, selectedPersonIds, session, score)


                                recyclerView.adapter = PersonShotItemAdapter(
                                    itemList,
                                    session,
                                    score,
                                    trainingIdInt,
                                    { session, personId, totalShots, rowEditTexts, totalTextViews, endTextViews ->
                                        createSortedTextWatcher(
                                            session,
                                            personId,
                                            totalShots,
                                            rowEditTexts,
                                            totalTextViews,
                                            endTextViews
                                        )
                                    },
                                    { editText ->
                                        currentEditText = editText
                                        customKeyboard.visibility = View.VISIBLE
                                    })

                                allDataLoaded = true

                            }

                        }
                }

            }
        }


        setupSaveButton(selectedPersonIds, trainingIdInt.toString())

    }



    private fun createTablesWithData(
        selectedNames: List<String>,
        selectedPersonIds: List<Int>,
        session: Int,
        shots: Int
    ) {
        Log.i("selectedNames", selectedNames.size.toString())
        Log.i("selectedPersonIds", selectedPersonIds.size.toString())
        selectedNames.forEachIndexed { index, name ->
            val personId = selectedPersonIds.getOrElse(index) { 0 }
            Log.i("personId", personId.toString())
            Log.i("personName", name.toString())
            nameMapping[personId] = name

            createStyledTable(
                session,
                shots,
                personId
            )
        }
    }

    private fun createStyledTable(
        rows: Int,
        shots: Int,
        personId: Int
    ) {






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

                allScores[Triple(
                    personId,
                    shot.session,
                    shot.shot_number
                ) as Triple<Int, Int, Int>] = shot.score!!
                allScoreTypes[Triple(
                    personId,
                    shot.session,
                    shot.shot_number
                ) as Triple<Int, Int, Int>] = scoreType
                Log.i("ada_personId",personId.toString())
            } else {
                Log.i("tidak_personId",personId.toString())
            }
        }

        shotsPerSession.forEach { (sessionKey, shotsList) ->
            shotsList.sortByDescending { it.getSortingValue() }
        }


        var totalTextViews : MutableList<String> = mutableListOf()

        for (i in 1..rows) {



            val sessionKey = Pair(personId, i)
            val sessionShots = shotsPerSession[sessionKey] ?: mutableListOf()
            Log.i("sessionKey",sessionKey.toString())
            Log.i("sessionShots",sessionShots.toString())

            val rowTotal = shotsPerSession[sessionKey]?.sumOf { it.getDisplayScore() } ?: 0
            allScores[Triple(personId, i, shots + 1)] = rowTotal
            allScoreTypes[Triple(personId, i, shots + 1)] = "0"
            totalTextViews.add(rowTotal.toString())

            val cumulativeTotal = totalTextViews.mapIndexed { index, textView ->
                if (index <= i - 1) textView.toIntOrNull() ?: 0 else 0
            }.sum()
            if (cumulativeTotal > 0) {

                allScores[Triple(personId, i, shots + 2)] = cumulativeTotal
                allScoreTypes[Triple(personId, i, shots + 2)] = "0"
                finalScores[personId] = cumulativeTotal
            }

        }




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
                    Log.i("onTextChanged", input)
                    Log.i("session", session.toString())
                    Log.i("totalShots", totalShots.toString())
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

                        Log.i("visualPos: ", visualPos.toString())
                        Log.i("sessionShots: ", sessionShots.toString())


                        var updatedExisting = false
                        for (i in sessionShots.indices) {
                            Log.i("sessionShots[i]: ", sessionShots[i].toString())
                            if (sessionShots[i].shotNumber == visualPos) {
                                Log.i("UPDATE SCORE : ", scoreValue.toString())
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

                        updateSortedTotals(
                            session,
                            personId,
                            totalShots,
                            totalTextViews,
                            endTextViews
                        )
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
                }
                else {
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

            var newShots: MutableList<Shot> = mutableListOf()



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
                        val index =
                            newShots.indexOfFirst { it.shot_id == existingShot.shot_id && it.session == session && it.person_id == personId && it.training_id == trainingIdInt && it.shot_number == sortedShotNumber }
                        if (index != -1) {
                            newShots[index] = newShots[index].copy(
                                score = shotData.score,
                                scoretype = shotData.scoreType
                            )
                        } else {
                            newShots.add(updatedShot)
                        }

                        //newShots.add(updatedShot)
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
                        val index =
                            newShots.indexOfFirst { it.session == session && it.person_id == personId && it.training_id == trainingIdInt && it.shot_number == sortedShotNumber }
                        if (index != -1) {
                            newShots[index] = newShots[index].copy(
                                score = shotData.score,
                                scoretype = shotData.scoreType
                            )
                        } else {
                            newShots.add(newShot)
                        }
                        //newShots.add(newShot)
                        dataViewModel.insert(newShot)
                        countSaved++
                    }
                }
            }


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
                        val index =
                            newShots.indexOfFirst { it.shot_id == existingTotalShot.shot_id && it.session == session && it.person_id == personId && it.training_id == trainingIdInt }
                        if (index != -1) {
                            newShots[index] =
                                newShots[index].copy(score = totalScore, scoretype = totalScoreType)
                        } else {
                            newShots.add(updatedTotalShot)
                        }
                        //newShots.add(updatedTotalShot)
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
                        val index =
                            newShots.indexOfFirst { it.session == session && it.person_id == personId && it.training_id == trainingIdInt }
                        if (index != -1) {
                            newShots[index] =
                                newShots[index].copy(score = totalScore, scoretype = totalScoreType)
                        } else {
                            newShots.add(newTotalShot)
                        }
                        //newShots.add(newTotalShot)
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
                        newShots.add(updatedEndShot)
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
                        newShots.add(newEndShot)
                        dataViewModel.insert(newEndShot)
                        countSaved++
                    }
                }
            }
            hasUnsavedChanges = false
            isNewData = false
            Log.i("SAVED", countSaved.toString())


            saveRankings()

            newShots.forEach { shot ->

                shot.score?.let { it1 ->
                    if (it1 > 0) {
                        Log.i("DATA SHOT", shot.toString())
                    }
                }
            }


            //dataViewModel.saveData(trainingIdInt,newShots)


            Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
            //saveRankings()
//            val intent = Intent(this@DataActivity, MainActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//            startActivity(intent)
            finish()
        }
    }

    private fun saveRankings() {
        val xCounts = mutableMapOf<Int, Int>()

        shotsPerSession.forEach { (sessionKey, shotsList) ->
            val personId = sessionKey.first

            shotsList.forEach { shotData ->
                if (shotData.scoreType == "X") {
                    xCounts[personId] = (xCounts[personId] ?: 0) + 1
                    Log.d(
                        "XCount",
                        "Added X for Person ID: $personId, Shot: ${shotData.shotNumber}, Total X: ${xCounts[personId]}"
                    )
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
            Log.d(
                "Ranking",
                "Rank ${index + 1}: Person: $name (ID: $personId), Score: $score, X Count: $xCount"
            )
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
                    dataViewModel.updateLeaderboard(updatedEntry)
                    Log.d(
                        "Leaderboard",
                        "Updated: Rank $rank, Person: $name (ID: $personId), Score: $score, X count: $xCount"
                    )
                } else {
                    val newEntry = Leaderboard(
                        person_id = personId,
                        score = score,
                        the_champion = rank,
                        training_id = trainingId
                    )
                    dataViewModel.insertLeaderboard(newEntry)
                    Log.d(
                        "Leaderboard",
                        "Inserted: Rank $rank, Person: $name (ID: $personId), Score: $score, X count: $xCount"
                    )
                }
            }
        }
    }

    override fun onKeyPressed(value: String) {
//        currentEditText?.let { editText ->
//            editText.setText("HUBLA")
//        }
        currentEditText?.let { editText ->
            if (value == "ENTER") {
                if (editText.text.isNotEmpty()) {
                    editText.setBackgroundColor(Color.WHITE)
                    focusNextEditText(editText)
                } else {
                    editText.setBackgroundColor(Color.rgb(255, 240, 240))
                }
            } else {
                Log.i("onKeyPressed",value)
                editText.setText(value)
                Log.i("editText",editText.text.toString())
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