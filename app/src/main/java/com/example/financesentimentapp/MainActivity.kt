package com.example.financesentimentapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.financesentimentapp.model.SentimentResponse
import com.example.financesentimentapp.network.RetrofitClient
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputText = findViewById<EditText>(R.id.inputText)
        val analyzeButton = findViewById<Button>(R.id.analyzeButton)
        val uploadFileButton = findViewById<Button>(R.id.uploadFileButton)
        pieChart = findViewById(R.id.pieChart)

        // ActivityResultLauncher 초기화
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val uri: Uri = result.data?.data ?: return@registerForActivityResult
                handleFileUpload(uri)
            }
        }

        // 텍스트 감성 분석
        analyzeButton.setOnClickListener {
            val text = inputText.text.toString()
            if (text.isNotEmpty()) {
                analyzeSentiment(text)
            } else {
                Toast.makeText(this, "텍스트를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 파일 업로드 감성 분석
        uploadFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "text/plain" }
            filePickerLauncher.launch(intent)
        }
    }

    private fun analyzeSentiment(text: String) {
        RetrofitClient.instance.analyzeSentiment(text).enqueue(object : Callback<SentimentResponse> {
            override fun onResponse(call: Call<SentimentResponse>, response: Response<SentimentResponse>) {
                if (response.isSuccessful) {
                    val results = response.body()?.results
                    if (results != null && results.isNotEmpty()) {
                        showPieChart(results)
                    } else {
                        Toast.makeText(this@MainActivity, "No sentiment data returned.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "분석에 실패했습니다: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SentimentResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleFileUpload(uri: Uri) {
        val file = createFileFromUri(uri)

        val requestFile = RequestBody.create(MediaType.parse("text/plain"), file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        RetrofitClient.instance.analyzeFile(body).enqueue(object : Callback<SentimentResponse> {
            override fun onResponse(call: Call<SentimentResponse>, response: Response<SentimentResponse>) {
                if (response.isSuccessful) {
                    val results = response.body()?.results
                    if (results != null && results.isNotEmpty()) {
                        showPieChart(results)
                    } else {
                        Toast.makeText(this@MainActivity, "No sentiment data returned.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "파일 분석에 실패했습니다: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SentimentResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPieChart(results: Map<String, Float>?) {
        if (results == null || results.isEmpty()) {
            Toast.makeText(this, "No sentiment data available to display.", Toast.LENGTH_SHORT).show()
            return
        }

        val entries = results.map { PieEntry(it.value, it.key) }
        val dataSet = PieDataSet(entries, "Sentiment Results").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
        }
        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    private fun createFileFromUri(uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "uploaded_file.txt")

        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}
