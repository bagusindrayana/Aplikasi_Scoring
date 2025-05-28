package com.stinkmtul.mytarget.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.data.databases.entity.training.Training
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
        training: Training
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val leaderboardEntries = leaderboardRepository.getAllLeaderboardSync(training.training_id)

            if (leaderboardEntries.isEmpty()) {
                return@withContext false
            }

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Training_${training.training_id}")
            val styles = createStyles(workbook)
            var currentRow = 0

            val titleRow = sheet.createRow(currentRow++)
            val titleCell = titleRow.createCell(0)
            titleCell.setCellValue("#${training.description} - Leaderboard Export")
            titleCell.cellStyle = styles["header"]

            currentRow++

            for (leaderboard in leaderboardEntries) {
                val personId = leaderboard.person_id ?: 0
                val ranking = leaderboard.the_champion ?: 0

                val personName = personRepository.getNamePersonSync(personId) ?: "Unknown"

                val shots = shotRepository.getShotsForPersonSync(personId, training.training_id)

                currentRow = createTableForPerson(
                    sheet,
                    styles,
                    shots,
                    personName,
                    currentRow,
                    ranking
                )

                currentRow++
            }

            //sheet 2
            val sheet2 = workbook.createSheet("Score_${training.training_id}")

            var currentRow2 = 0
            val titleRow2 = sheet2.createRow(currentRow2++)
            val titleCell2 = titleRow2.createCell(0)
            titleCell2.setCellValue("Score #${training.description}")
            titleCell2.cellStyle = styles["header"]

            //table header
            val headerRow2 = sheet2.createRow(currentRow2++)
            val headers2 = mutableListOf("No", "Nama")

            //get session_count
            val trainingCount : TrainingCounts = trainingRepository.getTrainingCountsSync(training.training_id)

            for (sessionNumber in 1..trainingCount.session_count){
                headers2.add("Rambahan $sessionNumber")

            }

            headers2.add("Total")

            headers2.forEachIndexed { index, header ->
                val cell = headerRow2.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = styles["columnHeader"]
            }

            var no = 1
            for (leaderboard in leaderboardEntries) {
                val newRow = sheet2.createRow(currentRow2++)

                val personId = leaderboard.person_id ?: 0
                val personName = personRepository.getNamePersonSync(personId)
                    ?.split(" ")
                    ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    ?: "Unknown"
                val noCell = newRow.createCell(0)
                noCell.setCellValue(no.toDouble())
                noCell.cellStyle = styles["center"]

                val nameCell = newRow.createCell(1)
                nameCell.setCellValue(personName)
                nameCell.cellStyle = styles["center"]

                val shots = shotRepository.getShotsForPersonSync(personId, training.training_id)

                var totalScore = 0
                for (sessionNumber in 1..trainingCount.session_count){
                    var score = 0
                    for (shootNumber in 1..trainingCount.shot_count){
                        score += shots.find { shot -> shot.session == sessionNumber && shot.shot_number == shootNumber }?.score ?: 0
                    }
                    val sessionCell = newRow.createCell(sessionNumber+1)
                    sessionCell.setCellValue("${score}")
                    sessionCell.cellStyle = styles["center"]
                    totalScore += score
                }


                val totalCell = newRow.createCell(trainingCount.session_count+2)
                totalCell.setCellValue(totalScore.toDouble())
                totalCell.cellStyle = styles["center"]
                no++

            }

            val fileName = "Training_${training.training_id}_Export.xlsx"
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
        startRow: Int,
        ranking: Int = 0
    ): Int {
        var currentRow = startRow

        val maxShotNumber = shots.maxOfOrNull { it.shot_number ?: 0 } ?: 0
        val sessions = shots.map { it.session ?: 0 }.distinct().sorted()

        if (sessions.isEmpty()) {
            val personHeaderRow = sheet.createRow(currentRow++)
            val personHeaderCell = personHeaderRow.createCell(0)
            personHeaderCell.setCellValue("Rank $ranking - $personName")
            personHeaderCell.cellStyle = styles["header"]

            val noDataRow = sheet.createRow(currentRow++)
            val noDataCell = noDataRow.createCell(0)
            noDataCell.setCellValue("No data available for this person")

            return currentRow
        }

        val personHeaderRow = sheet.createRow(currentRow++)
        val personHeaderCell = personHeaderRow.createCell(0)
        val formattedName = personName
            ?.split(" ")
            ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            ?: "Unknown"

        personHeaderCell.setCellValue("Rank #$ranking - $formattedName")
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

    private fun createStyles(workbook: XSSFWorkbook): Map<String, XSSFCellStyle> {
        val styles = mutableMapOf<String, XSSFCellStyle>()

        val headerStyle = workbook.createCellStyle()
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerFont.fontHeightInPoints = 14
        headerStyle.setFont(headerFont)
        headerStyle.alignment = HorizontalAlignment.LEFT
        styles["header"] = headerStyle

        val columnHeaderStyle = workbook.createCellStyle()
        val columnHeaderFont = workbook.createFont()
        columnHeaderFont.bold = true
        columnHeaderStyle.setFont(columnHeaderFont)
        columnHeaderStyle.alignment = HorizontalAlignment.CENTER
        columnHeaderStyle.fillForegroundColor = IndexedColors.GREY_50_PERCENT.index
        columnHeaderStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        columnHeaderStyle.setFont(columnHeaderFont)
        styles["columnHeader"] = columnHeaderStyle

        val centerStyle = workbook.createCellStyle()
        centerStyle.alignment = HorizontalAlignment.CENTER
        styles["center"] = centerStyle

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

    fun getTrainingById(trainingId: Int) : LiveData<Training> {
        return trainingRepository.getTrainingById(trainingId)
    }
}