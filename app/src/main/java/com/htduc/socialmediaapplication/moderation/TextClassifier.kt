package com.htduc.socialmediaapplication.moderation

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TextClassifier(private val context: Context) {

    private val modelPath = "Text_CNN_model_v13.tflite"
    private val tokenizerPath = "tokenizer.json"
    private val stopwordsPath = "vietnamese-stopwords-dash.txt"

    private val sequenceLength = 100
    private val numClasses = 3

    private var interpreter: Interpreter
    private var tokenizer: Map<String, Int> = emptyMap()
    private var stopwords: Set<String> = emptySet()

    init {
        interpreter = Interpreter(loadModelFile())
        tokenizer = loadTokenizer()
        stopwords = loadStopwords()
    }

    private fun loadModelFile(): MappedByteBuffer {
        context.assets.openFd(modelPath).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
            }
        }
    }

    private fun loadTokenizer(): Map<String, Int> {
        context.assets.open(tokenizerPath).bufferedReader().use { reader ->
            val jsonStr = reader.readText()
            val jsonObject = JSONObject(jsonStr)
            val map = mutableMapOf<String, Int>()
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.getInt(key)
            }
            return map
        }
    }

    private fun loadStopwords(): Set<String> {
        context.assets.open(stopwordsPath).bufferedReader().useLines { lines ->
            return lines.map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
    }

    // Tokenize nâng cao hơn: tách từ bằng regex, loại bỏ ký tự không phải chữ số, bỏ stopwords
    private fun tokenize(text: String): List<String> {
        // 1. Chuyển về chữ thường
        val lowerText = text.lowercase()

        // 2. Tách từ bằng regex, chỉ lấy chữ cái và số, bỏ dấu câu, ký tự đặc biệt
        val regex = "\\b[\\p{L}\\p{Nd}_]+\\b".toRegex()
        val tokens = regex.findAll(lowerText).map { it.value }.toList()

        // 3. Lọc stopwords
        return tokens.filter { it !in stopwords }
    }

    // Tiền xử lý: tokenize nâng cao, chuyển thành token ids, padding
    fun preprocess(text: String): IntArray {
        val filteredTokens = tokenize(text)
        val tokenIds = filteredTokens.map { tokenizer[it] ?: 0 }
        val padded = IntArray(sequenceLength) { 0 }
        tokenIds.take(sequenceLength).forEachIndexed { i, id -> padded[i] = id }
        return padded
    }

    // Hàm debug giúp xem tokens đã lọc (stopwords và regex)
    fun preprocessDebug(text: String): Pair<List<String>, IntArray> {
        val filteredTokens = tokenize(text)
        val tokenIds = filteredTokens.map { tokenizer[it] ?: 0 }
        val padded = IntArray(sequenceLength) { 0 }
        tokenIds.take(sequenceLength).forEachIndexed { i, id -> padded[i] = id }
        return filteredTokens to padded
    }

    fun predict(text: String): Pair<String, Float> {
        val inputIds = preprocess(text)
        val inputBuffer = ByteBuffer.allocateDirect(4 * sequenceLength).order(ByteOrder.nativeOrder())
        inputIds.forEach { inputBuffer.putInt(it) }
        inputBuffer.rewind()

        val outputBuffer = ByteBuffer.allocateDirect(4 * numClasses).order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val probs = FloatArray(numClasses) { outputBuffer.float }
        val labels = arrayOf("clean", "offensive", "hate")
        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
        return labels[maxIdx] to probs[maxIdx]
    }

    fun close() {
        interpreter.close()
    }
}
