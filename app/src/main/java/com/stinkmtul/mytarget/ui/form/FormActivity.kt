package com.stinkmtul.mytarget.ui.form

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stinkmtul.mytarget.R
import com.stinkmtul.mytarget.data.databases.entity.person.Person
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.databinding.ActivityFormBinding
import com.stinkmtul.mytarget.ui.data.DataActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class FormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormBinding // ViewBinding
    private val selectedItems = mutableListOf<String>()
    private val selectedPersonIds = mutableListOf<Int>()
    private val personViewModel: PersonViewModel by viewModels()
    private val formViewModel: FormViewModel by viewModels()
    private lateinit var uniqueToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.session.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        binding.score.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.tanggal.text = getCurrentDate()
        uniqueToken = generateUniqueToken()

        Log.d("FormActivity", "Generated Token: $uniqueToken")

        personViewModel.allPersons.observe(this) { persons ->
            val namaList = persons.map { it.name ?: "" }
            binding.person.setOnClickListener { showMultiSelectDialog(namaList) }
        }

        binding.Next.setOnClickListener {
            saveTrainingToDatabase()
            sendDataToNextActivity()
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun generateUniqueToken(): String {
        val timestamp = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(Date())
        val randomNumber = Random.nextInt(100, 999)
        return "$timestamp$randomNumber"
    }

    private fun showMultiSelectDialog(namaList: List<String>) {
        val selectedArray = BooleanArray(namaList.size) { selectedItems.contains(namaList[it]) }
        val tempSelected = mutableListOf<String>().apply { addAll(selectedItems) }

        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Nama")
            .setMultiChoiceItems(namaList.toTypedArray(), selectedArray) { _, which, isChecked ->
                val name = namaList[which]
                if (isChecked) {
                    tempSelected.add(name)
                    formViewModel.getPersonId(name).observe(this) { personId ->
                        personId?.let {
                            selectedPersonIds.add(it)
                            Log.d("FormActivity", "Added person_id: $it for name: $name")
                        }
                    }
                } else {
                    tempSelected.remove(name)
                    formViewModel.getPersonId(name).observe(this) { personId ->
                        personId?.let {
                            selectedPersonIds.remove(it)
                            Log.d("FormActivity", "Removed person_id: $it for name: $name")
                        }
                    }
                }
            }
            .setPositiveButton("OK") { _, _ ->
                selectedItems.clear()
                selectedItems.addAll(tempSelected)
                updateSelectedNamesUI()
            }
            .setNegativeButton("Batal", null)
            .setNeutralButton("Tambah Orang") { _, _ -> showAddPersonDialog() }
            .show()
    }

    private fun updateSelectedNamesUI() {
        binding.selectedNames.text = if (selectedItems.isNotEmpty()) {
            selectedItems.joinToString(", ")
        } else {
            "Tidak ada nama yang dipilih"
        }
    }

    private fun showAddPersonDialog() {
        val inputField = android.widget.EditText(this).apply {
            hint = "Masukkan nama baru"
            textSize = 20f
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Tambah Orang")
            .setView(inputField)
            .setPositiveButton("Tambah") { _, _ ->
                val newName = inputField.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val newPerson = Person(name = newName)
                    personViewModel.insert(newPerson)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveTrainingToDatabase() {
        val date = binding.tanggal.text.toString()
        val description = binding.description.text.toString().trim()
        val sessionText = binding.session.text.toString().trim()
        val scoreText = binding.score.text.toString().trim()
        val sessionCount = sessionText.toIntOrNull() ?: 0
        val shotCount = scoreText.toIntOrNull() ?: 0

        if (description.isEmpty()) {
            binding.textdescription.error = "Deskripsi kegiatan wajib diisi!"
            return
        } else {
            binding.textdescription.error = null
        }

        if (selectedPersonIds.isEmpty()) {
            Toast.makeText(this, "Pilih minimal 1 orang!", Toast.LENGTH_SHORT).show()
            return
        }

        if (sessionText.isEmpty()) {
            binding.textsession.error = "Rambahan wajib diisi!"
            return
        } else {
            binding.textsession.error = null
        }

        if (scoreText.isEmpty()) {
            binding.textscore.error = "Anak panah wajib diisi!"
            return
        } else {
            binding.textscore.error = null
        }

        val training = Training(
            date = date,
            description = description,
            session_count = sessionCount,
            shot_count = shotCount,
            token = uniqueToken
        )

        formViewModel.insert(training)
    }

    private fun sendDataToNextActivity() {
        val selectedNames = selectedItems.joinToString(", ").ifEmpty { "No Selection" }
        val selectedPersonIdsString = selectedPersonIds.joinToString(", ")
        val sessionValue = binding.session.text.toString().trim().ifEmpty { "No Data" }
        val scoreValue = binding.score.text.toString().trim().ifEmpty { "No Data" }
        val dateValue = binding.tanggal.text.toString().trim().ifEmpty { "No Data" }
        val descriptionValue = binding.description.text.toString().trim().ifEmpty { "No Data" }

        formViewModel.getTrainingId(dateValue, uniqueToken).observe(this) { trainingId ->
            if (trainingId != null) {
                val intent = Intent(this, DataActivity::class.java).apply {
                    putExtra("selected_names", selectedNames)
                    putExtra("selected_person_ids", selectedPersonIdsString)
                    putExtra("session", sessionValue)
                    putExtra("score", scoreValue)
                    putExtra("date", dateValue)
                    putExtra("description", descriptionValue)
                    putExtra("token", uniqueToken)
                    putExtra("trainingid", trainingId.toString())
                }
                startActivity(intent)
            } else {
                Log.e("FormActivity", "Training ID is null")
            }
        }
    }
}

