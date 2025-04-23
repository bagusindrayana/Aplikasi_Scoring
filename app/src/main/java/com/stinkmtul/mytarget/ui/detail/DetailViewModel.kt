package com.stinkmtul.mytarget.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.data.repository.LeaderboardRepository
import com.stinkmtul.mytarget.data.repository.PersonRepository
import com.stinkmtul.mytarget.data.repository.ShotRepository
import com.stinkmtul.mytarget.data.repository.TrainingRepository
import com.stinkmtul.mytarget.ui.detail.TrainingCounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val leaderboardRepository: LeaderboardRepository = LeaderboardRepository(application)
    private val personRepository: PersonRepository = PersonRepository(application)
    private val trainingRepository: TrainingRepository = TrainingRepository(application)
    private val shotRepository: ShotRepository = ShotRepository(application)

    fun getLeaderboardByTrainingId(trainingId: Int): LiveData<List<Leaderboard>> {
        return leaderboardRepository.getAllLeaderboard(trainingId)
    }

    fun getNamePerson(personId: Int): LiveData<String?> {
        return personRepository.getNamePerson(personId)
    }

    fun getTrainingCounts(trainingId: Int): LiveData<TrainingCounts> {
        return trainingRepository.getTrainingCounts(trainingId)
    }

    fun getDistinctPersonByTraining(trainingId: Int): LiveData<List<Int>> {
        return shotRepository.getDistinctPersonByTraining(trainingId)
    }

    fun getShotsForPerson(personId: Int, trainingId: Int): LiveData<List<Shot>> {
        return shotRepository.getShotsForPerson(personId, trainingId)
    }

    suspend fun exportTrainingToExcel(
        context: Context,
        trainingId: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val leaderboardEntries = leaderboardRepository.getAllLeaderboardSync(trainingId)

            if (leaderboardEntries.isEmpty()) {
                return@withContext false
            }

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Training_$trainingId")
            val styles = createStyles(workbook)
            var currentRow = 0

            val titleRow = sheet.createRow(currentRow++)
            val titleCell = titleRow.createCell(0)
            titleCell.setCellValue("Training #$trainingId - Leaderboard Export")
            titleCell.cellStyle = styles["header"]

            currentRow++

            for (leaderboard in leaderboardEntries) {
                val personId = leaderboard.person_id ?: 0
                val ranking = leaderboard.the_champion ?: 0

                val personName = personRepository.getNamePersonSync(personId) ?: "Unknown"

                val shots = shotRepository.getShotsForPersonSync(personId, trainingId)

                currentRow = createTableForPerson(
                    sheet,
                    styles,
                    shots,
                    personName,
                    personId,
                    currentRow,
                    ranking
                )

                currentRow++
            }

            val fileName = "Training_${trainingId}_Export.xlsx"
            val file = File(context.getExternalFilesDir(null), fileName)

            if (file.exists()) {
                file.delete()
            }

            val fileOut = FileOutputStream(file)
            workbook.write(fileOut)
            fileOut.close()
            workbook.close()
            return@withContext true
        } catch (e: Exception) {
            Log.e("ExportToExcel", "Error exporting to Excel", e)
            return@withContext false
        }
    }

    private fun createTableForPerson(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        styles: Map<String, XSSFCellStyle>,
        shots: List<Shot>,
        personName: String,
        personId: Int,
        startRow: Int,
        ranking: Int = 0
    ): Int {
        var currentRow = startRow

        val maxShotNumber = shots.maxOfOrNull { it.shot_number ?: 0 } ?: 0
        val sessions = shots.map { it.session ?: 0 }.distinct().sorted()

        if (sessions.isEmpty()) {
            val personHeaderRow = sheet.createRow(currentRow++)
            val personHeaderCell = personHeaderRow.createCell(0)
            personHeaderCell.setCellValue("Rank #$ranking - $personName")
            personHeaderCell.cellStyle = styles["header"]

            val noDataRow = sheet.createRow(currentRow++)
            val noDataCell = noDataRow.createCell(0)
            noDataCell.setCellValue("No data available for this person")

            return currentRow
        }

        val personHeaderRow = sheet.createRow(currentRow++)
        val personHeaderCell = personHeaderRow.createCell(0)
        personHeaderCell.setCellValue("Rank #$ranking - $personName")
        personHeaderCell.cellStyle = styles["header"]

        val headerRow = sheet.createRow(currentRow++)
        val headers = mutableListOf("Rambahan")

        for (i in 1..(maxShotNumber - 2)) {
            headers.add("Shot $i")
        }

        headers.add("Total")
        headers.add("End")

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = styles["columnHeader"]
        }

        val shotsBySession = shots.groupBy { it.session }

        var runningTotal = 0

        for (session in sessions) {
            val row = sheet.createRow(currentRow++)
            val sessionShots = shotsBySession[session] ?: listOf()

            row.createCell(0).apply {
                setCellValue(session.toDouble())
                cellStyle = styles["center"]
            }

            var sessionTotal = 0

            for (shotNumber in 1..maxShotNumber - 2) {
                val shot = sessionShots.find { it.shot_number == shotNumber }
                val score = shot?.score ?: 0
                val scoreType = shot?.scoretype

                val cell = row.createCell(shotNumber)

                if (scoreType == "X" || scoreType == "M") {
                    cell.setCellValue(scoreType)
                } else {
                    cell.setCellValue(score.toDouble())
                }

                cell.cellStyle = styles["center"]
                sessionTotal += score
            }

            row.createCell(maxShotNumber - 1).apply {
                setCellValue(sessionTotal.toDouble())
                cellStyle = styles["totalCell"]
            }

            runningTotal += sessionTotal
            row.createCell(maxShotNumber).apply {
                setCellValue(runningTotal.toDouble())
                cellStyle = styles["endCell"]
            }
        }

        return currentRow
    }

    /*suspend fun getNamePersonSync(personId: Int): String? {
        val result = withContext(Dispatchers.IO) {
            try {
                val liveData = personRepository.getNamePerson(personId)
                var name: String? = null
                val latch = java.util.concurrent.CountDownLatch(1)

                withContext(Dispatchers.Main) {
                    liveData.observeForever { value ->
                        name = value
                        latch.countDown()
                    }
                }
                latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                name
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error getting person name", e)
                null
            }
        }
        return result
    }*/

    private fun createStyles(workbook: XSSFWorkbook): Map<String, XSSFCellStyle> {
        val styles = mutableMapOf<String, XSSFCellStyle>()

        // Style untuk header
        val headerStyle = workbook.createCellStyle()
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerFont.fontHeightInPoints = 14
        headerStyle.setFont(headerFont)
        headerStyle.alignment = HorizontalAlignment.LEFT
        styles["header"] = headerStyle

        // Style untuk header kolom
        val columnHeaderStyle = workbook.createCellStyle()
        val columnHeaderFont = workbook.createFont()
        columnHeaderFont.bold = true
        columnHeaderStyle.setFont(columnHeaderFont)
        columnHeaderStyle.alignment = HorizontalAlignment.CENTER
        columnHeaderStyle.fillForegroundColor = IndexedColors.GREY_50_PERCENT.index
        columnHeaderStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        columnHeaderStyle.setFont(columnHeaderFont)
        styles["columnHeader"] = columnHeaderStyle

        // Style untuk sel dengan perataan tengah
        val centerStyle = workbook.createCellStyle()
        centerStyle.alignment = HorizontalAlignment.CENTER
        styles["center"] = centerStyle

        // Style untuk Total cell
        val totalCellStyle = workbook.createCellStyle()
        totalCellStyle.alignment = HorizontalAlignment.CENTER
        val totalFont = workbook.createFont()
        totalFont.bold = true
        totalCellStyle.setFont(totalFont)
        styles["totalCell"] = totalCellStyle

        val endCellStyle = workbook.createCellStyle()
        endCellStyle.alignment = HorizontalAlignment.CENTER
        val endFont = workbook.createFont()
        endFont.bold = true
        endCellStyle.setFont(endFont)
        styles["endCell"] = endCellStyle

        return styles
    }
}