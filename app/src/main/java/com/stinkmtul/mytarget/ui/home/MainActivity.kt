package com.stinkmtul.mytarget.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.databinding.ActivityMainBinding
import com.stinkmtul.mytarget.ui.data.DataActivity
import com.stinkmtul.mytarget.ui.detail.DetailActivity
import com.stinkmtul.mytarget.ui.form.FormActivity

class MainActivity : AppCompatActivity() {
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabCustom: FloatingActionButton
    private lateinit var fabNonCustom: FloatingActionButton
    private var isFabOpen = false

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels { MainViewModelFactory(application) }
    private lateinit var adapter: MainAdapter

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()

        fabMain = binding.fab
        fabCustom = binding.fabCustom
        fabNonCustom = binding.fabNonCustom

        fabMain.setOnClickListener {
            //if (isFabOpen) closeFABMenu() else openFABMenu()
            startActivity(Intent(this, FormActivity::class.java))
        }

        fabCustom.setOnClickListener {
            startActivity(Intent(this, FormActivity::class.java))
            closeFABMenu()
        }

        fabNonCustom.setOnClickListener {
            Toast.makeText(this, "Non-Custom Selected", Toast.LENGTH_SHORT).show()
            closeFABMenu()
        }

        /*binding.menu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }*/
    }

    private fun loadDistinctPersons(trainingId: Int) {
        mainViewModel.getDistinctPersonByTraining(trainingId).observe(this) { personIds ->
            if (personIds.isNotEmpty()) {
                Log.d(TAG, "Person IDs yang ikut training: $personIds")

                personIds.forEach { personId ->
                    mainViewModel.getNamePerson(personId).observe(this) { name ->
                        Log.d(TAG, "Person ID: $personId, Name: ${name ?: "Nama tidak ditemukan"}")
                    }
                }
            } else {
                Log.d(TAG, "Tidak ada person untuk training ini")
            }
        }
    }


    private fun setupRecyclerView() {
        adapter = MainAdapter(object : MainAdapter.OnItemClickListener {
            override fun onItemClick(training: Training) {
                Log.d(TAG, "Item clicked: ${training.training_id}")
                val intent = Intent(this@MainActivity, DetailActivity::class.java).apply {
                    putExtra("training_id", training.training_id.toString()) // Kirim sebagai String
                }
                startActivity(intent)

                training.training_id?.let { trainingId ->
                    loadDistinctPersons(trainingId)
                }
            }

            override fun onDeleteClick(training: Training) {
                showDeleteConfirmationDialog(training)
            }

            override fun onClickItem(training: Training) {
                Log.d(TAG, "Edit clicked: ${training.training_id}")
                training.training_id?.let { trainingId ->
                    sendPersonsToDataActivity(trainingId, training)
                }
            }
        })

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        mainViewModel.getAllTraining().observe(this, Observer { trainingEvents ->
            Log.d(TAG, "Data training observed: ${trainingEvents.size} items")
            adapter.submitList(trainingEvents)

            if (trainingEvents.isNullOrEmpty()) {
                binding.tvEmptyData.visibility = View.VISIBLE
            } else {
                binding.tvEmptyData.visibility = View.GONE
            }
        })
    }

    private fun showDeleteConfirmationDialog(training: Training) {
        AlertDialog.Builder(this).apply {
            setTitle("Hapus Data")
            setMessage("Apakah Anda yakin ingin menghapus data ini?")
            setPositiveButton("Ya") { _, _ ->
                mainViewModel.deleteTraining(training)
                Toast.makeText(this@MainActivity, "Data dihapus", Toast.LENGTH_SHORT).show()
            }
            setNegativeButton("Batal", null)
            show()
        }
    }

    private fun openFABMenu() {
        fabCustom.visibility = View.VISIBLE
        fabNonCustom.visibility = View.VISIBLE
        fabCustom.animate().translationY(-140f).alpha(1f).setDuration(300).start()
        fabNonCustom.animate().translationY(-220f).alpha(1f).setDuration(300).start()
        isFabOpen = true
    }

    private fun closeFABMenu() {
        fabCustom.animate().translationY(0f).alpha(0f).setDuration(300).start()
        fabNonCustom.animate().translationY(0f).alpha(0f).setDuration(300).start()
        fabCustom.visibility = View.GONE
        fabNonCustom.visibility = View.GONE
        isFabOpen = false
    }

    private fun sendPersonsToDataActivity(trainingId: Int, training: Training) {
        val intent = Intent(this, DataActivity::class.java).apply {
            putExtra("trainingid", trainingId.toString())
//            putExtra("selected_person_ids", idsString)
//            putExtra("selected_names", namesString)

            putExtra("date", training.date ?: "No Date")
            putExtra("description", training.description ?: "No Description")
            putExtra("session", training.session_count?.toString() ?: "0")
            putExtra("score", training.shot_count?.toString() ?: "0")
        }
        startActivity(intent)
//        mainViewModel.getDistinctPersonByTraining(trainingId).observe(this) { personIds ->
//            if (personIds.isNotEmpty()) {
//                val personIdList = mutableListOf<Int>()
//                val nameList = mutableListOf<String>()
//                var counter = 0
//
//                personIds.forEach { personId ->
//                    mainViewModel.getNamePerson(personId).observe(this) { name ->
//                        personIdList.add(personId)
//                        nameList.add(name ?: "Nama tidak ditemukan")
//
//                        counter++
//                        if (counter == personIds.size) {
//                            val idsString = personIdList.joinToString(", ")
//                            val namesString = nameList.joinToString(", ")
//
//                            val intent = Intent(this, DataActivity::class.java).apply {
//                                putExtra("trainingid", trainingId.toString())
//                                putExtra("selected_person_ids", idsString)
//                                putExtra("selected_names", namesString)
//
//                                putExtra("date", training.date ?: "No Date")
//                                putExtra("description", training.description ?: "No Description")
//                                putExtra("session", training.session_count?.toString() ?: "0")
//                                putExtra("score", training.shot_count?.toString() ?: "0")
//                            }
//                            startActivity(intent)
//                        }
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Tidak ada person dalam training ini", Toast.LENGTH_SHORT).show()
//
//                val intent = Intent(this, DataActivity::class.java).apply {
//                    putExtra("trainingid", trainingId.toString())
//                    putExtra("selected_person_ids", "")
//                    putExtra("selected_names", "")
//                    putExtra("date", training.date ?: "No Date")
//                    putExtra("description", training.description ?: "No Description")
//                    putExtra("session", training.session_count?.toString() ?: "0")
//                    putExtra("score", training.shot_count?.toString() ?: "0")
//                }
//                startActivity(intent)
//            }
//        }
    }
}