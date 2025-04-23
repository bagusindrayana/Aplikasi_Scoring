package com.stinkmtul.mytarget.ui.detail

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stinkmtul.mytarget.R
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.ui.detail.adapter.DetailAdapter
import com.stinkmtul.mytarget.ui.home.MainActivity
import com.stinkmtul.mytarget.viewmodel.DetailViewModel
import kotlinx.coroutines.launch
import java.io.File

class DetailActivity : AppCompatActivity() {
    private val detailViewModel: DetailViewModel by viewModels()
    private lateinit var container: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DetailAdapter
    private var currentTrainingId: Int = 0

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
        private const val MANAGE_EXTERNAL_STORAGE_REQUEST = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        recyclerView = findViewById(R.id.recyclerView)
        adapter = DetailAdapter(detailViewModel)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        container = findViewById(R.id.container)
        container.removeAllViews()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val trainingid = intent.getStringExtra("training_id") ?: "0"
        val trainingIdInt = trainingid.toIntOrNull() ?: 0
        currentTrainingId = trainingIdInt

        Log.d("DetailActivity", "Training ID: $trainingIdInt")

        detailViewModel.getLeaderboardByTrainingId(trainingIdInt).observe(this, Observer { leaderboardList ->
            adapter.submitList(leaderboardList)
            createTablesBasedOnRanking(leaderboardList, trainingIdInt)
        })
    }

    private fun createTablesBasedOnRanking(leaderboardList: List<Leaderboard>, trainingIdInt: Int) {
        container.removeAllViews()

        val processedPersonIds = HashSet<Int>()

        val sortedLeaderboard = leaderboardList.sortedBy { it.the_champion ?: Int.MAX_VALUE }

        detailViewModel.getTrainingCounts(trainingIdInt).observe(this) { counts ->
            val sessionCount = counts.session_count
            val shotCount = counts.shot_count

            Log.d("DetailActivity", "Session Count: $sessionCount, Shot Count: $shotCount")

            sortedLeaderboard.forEach { leaderboard ->
                val personId = leaderboard.person_id ?: 0

                if (personId in processedPersonIds) {
                    return@forEach
                }

                processedPersonIds.add(personId)

                detailViewModel.getNamePerson(personId).observe(this) { personName ->
                    createDynamicTable(
                        personId = personId,
                        personName = personName ?: "Unknown",
                        sessionCount = sessionCount,
                        shotCount = shotCount,
                        trainingId = trainingIdInt,
                        ranking = leaderboard.the_champion ?: 0
                    )
                }
            }
        }
    }

    private fun createDynamicTable(
        personId: Int,
        personName: String,
        sessionCount: Int,
        shotCount: Int,
        trainingId: Int,
        ranking: Int
    ) {
        val cardView = androidx.cardview.widget.CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            radius = 16f
            cardElevation = 8f
            setCardBackgroundColor(Color.WHITE)
        }

        val cardContent = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val titleTextView = TextView(this).apply {
            text = "Rank #$ranking - $personName"
            textSize = 18f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.START
            setPadding(8, 8, 8, 16)
        }

        val scrollView = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = true
        }

        val tableLayout = TableLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isStretchAllColumns = true
            setPadding(4, 4, 4, 4)
        }

        val headerRow = TableRow(this)
        val headerTitles = listOf("Rambahan") +
                (1..shotCount).map { "Shot $it" } +
                listOf("Total", "End")

        val headerBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(this@DetailActivity, R.color.grey))
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

        val evenRowColor = Color.parseColor("#F5F5F5")
        val oddRowColor = Color.WHITE

        for (session in 1..sessionCount) {
            val tableRow = TableRow(this).apply {
                if (session % 2 == 0) {
                    setBackgroundColor(evenRowColor)
                } else {
                    setBackgroundColor(oddRowColor)
                }
            }

            val cellBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.TRANSPARENT)
                cornerRadius = 4f
            }

            val sessionTextView = TextView(this).apply {
                text = session.toString()
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(20, 16, 20, 16)
                background = cellBackground
            }
            tableRow.addView(sessionTextView)

            for (shot in 1..shotCount) {
                val shotTextView = TextView(this).apply {
                    text = ""
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    setPadding(20, 16, 20, 16)

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
                }
                tableRow.addView(shotTextView)
            }

            val totalTextView = TextView(this).apply {
                text = "" // Total per sesi
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
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(4, 2, 0, 2)
                layoutParams = params
            }
            tableRow.addView(totalTextView)

            val endTextView = TextView(this).apply {
                text = "" // Total kumulatif
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
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(4, 2, 0, 2)
                layoutParams = params
            }
            tableRow.addView(endTextView)

            tableLayout.addView(tableRow)
        }

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            )
            setBackgroundColor(Color.LTGRAY)
        }

        scrollView.addView(tableLayout)
        cardContent.addView(titleTextView)
        cardContent.addView(scrollView)
        cardContent.addView(divider)
        cardView.addView(cardContent)

        cardView.tag = ranking

        addCardToContainerInOrder(cardView)

        fetchAndPopulateTableData(personId, trainingId, tableLayout)
        setupExportButton(trainingId)
    }

    private fun addCardToContainerInOrder(cardView: androidx.cardview.widget.CardView) {
        val ranking = cardView.tag as Int
        var insertPosition = 0

        for (i in 0 until container.childCount) {
            val existingCard = container.getChildAt(i) as? androidx.cardview.widget.CardView
            val existingRanking = existingCard?.tag as? Int ?: 0

            if (existingRanking > ranking) {
                break
            }
            insertPosition++
        }

        container.addView(cardView, insertPosition)
    }

    private fun fetchAndPopulateTableData(
        personId: Int,
        trainingId: Int,
        tableLayout: TableLayout
    ) {
        detailViewModel.getShotsForPerson(personId, trainingId).observe(this) { shots ->
            val scoreMap = shots.associate {
                Pair(it.session ?: 0, it.shot_number ?: 0) to Pair(it.score ?: 0, it.scoretype)
            }

            for (rowIndex in 1 until tableLayout.childCount) {
                val tableRow = tableLayout.getChildAt(rowIndex) as TableRow

                val sessionTextView = tableRow.getChildAt(0) as TextView
                val session = sessionTextView.text.toString().toInt()

                var sessionTotal = 0

                for (shotIndex in 1..tableRow.childCount - 3) {
                    val shotTextView = tableRow.getChildAt(shotIndex) as TextView
                    val scoreData = scoreMap[Pair(session, shotIndex)]
                    val score = scoreData?.first ?: 0
                    val scoreType = scoreData?.second

                    when (scoreType) {
                        "X" -> {
                            shotTextView.text = "X"
                            shotTextView.setTextColor(Color.parseColor("#D32F2F"))
                            shotTextView.setTypeface(null, Typeface.BOLD)
                        }
                        "M" -> {
                            shotTextView.text = "M"
                            shotTextView.setTextColor(Color.parseColor("#FF6D00"))
                            shotTextView.setTypeface(null, Typeface.BOLD)
                        }
                        else -> {
                            if (score > 0) {
                                shotTextView.text = score.toString()
                                when {
                                    score >= 9 -> shotTextView.setTextColor(Color.parseColor("#2E7D32"))
                                    score >= 7 -> shotTextView.setTextColor(Color.parseColor("#388E3C"))
                                    score >= 5 -> shotTextView.setTextColor(Color.parseColor("#689F38"))
                                    else -> shotTextView.setTextColor(Color.BLACK)
                                }
                            } else {
                                shotTextView.text = ""
                            }
                        }
                    }

                    sessionTotal += score
                }

                val totalTextView = tableRow.getChildAt(tableRow.childCount - 2) as TextView
                totalTextView.text = sessionTotal.toString()

                if (rowIndex > 1) {
                    val previousTotalTextView = (tableLayout.getChildAt(rowIndex - 1) as TableRow)
                        .getChildAt(tableRow.childCount - 1) as TextView
                    val previousEndTotal = previousTotalTextView.text.toString().toIntOrNull() ?: 0
                    val endTextView = tableRow.getChildAt(tableRow.childCount - 1) as TextView
                    endTextView.text = (previousEndTotal + sessionTotal).toString()
                } else {
                    val endTextView = tableRow.getChildAt(tableRow.childCount - 1) as TextView
                    endTextView.text = sessionTotal.toString()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun setupExportButton(trainingId: Int) {
        val exportButton = findViewById<Button>(R.id.exportButton)
        exportButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                performExport(trainingId)
            }
        }
    }

    private fun performExport(trainingId: Int) {
        lifecycleScope.launch {
            val success = detailViewModel.exportTrainingToExcel(this@DetailActivity, trainingId)
            if (success) {
                Toast.makeText(this@DetailActivity, "Export berhasil", Toast.LENGTH_SHORT).show()
                openExportedFile(trainingId)
            } else {
                Toast.makeText(this@DetailActivity, "Export gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                return true
            } else {
                showStoragePermissionDialogForAndroid11()
                return false
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
                return false
            }
        }
        return true
    }

    private fun showStoragePermissionDialogForAndroid11() {
        AlertDialog.Builder(this)
            .setTitle("Izin Penyimpanan Diperlukan")
            .setMessage("Untuk melakukan ekspor data, aplikasi memerlukan izin akses penyimpanan. Silakan berikan izin 'Akses semua file' pada halaman pengaturan berikutnya.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin Penyimpanan Diberikan", Toast.LENGTH_SHORT).show()
                performExport(currentTrainingId)
            } else {
                Toast.makeText(this, "Izin Penyimpanan Ditolak", Toast.LENGTH_SHORT).show()
                showPermissionExplanationDialog()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Izin Penyimpanan Diberikan", Toast.LENGTH_SHORT).show()
                    performExport(currentTrainingId)
                } else {
                    Toast.makeText(this, "Izin Penyimpanan Ditolak", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Izin Diperlukan")
            .setMessage("Aplikasi memerlukan izin penyimpanan untuk menyimpan file Excel. Tanpa izin ini, fitur ekspor tidak dapat digunakan.")
            .setPositiveButton("Minta Izin Lagi") { _, _ ->
                checkAndRequestPermissions()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun openExportedFile(trainingId: Int) {
        lifecycleScope.launch {
            val fileName = "Training_${trainingId}_Export.xlsx"
            val file = File(getExternalFilesDir(null), fileName)

            if (!file.exists()) {
                Toast.makeText(this@DetailActivity, "File tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val uri = FileProvider.getUriForFile(
                this@DetailActivity,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this@DetailActivity, "Tidak ada aplikasi untuk membuka file Excel", Toast.LENGTH_SHORT).show()
            }
        }
    }
}