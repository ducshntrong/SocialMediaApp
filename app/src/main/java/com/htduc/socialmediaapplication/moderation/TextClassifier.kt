package com.htduc.socialmediaapplication.moderation

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.Normalizer

class TextClassifier(private val context: Context) {

    // Đường dẫn đến các file mô hình và dữ liệu trong thư mục assets
    private val modelPath = "Text_CNN_model_v13.tflite"
    private val tokenizerPath = "tokenizer.json"
    private val stopwordsPath = "vietnamese-stopwords-dash.txt"
    private val offensiveWordsPath = "offensive-words.txt"

    private val sequenceLength = 100 // Độ dài chuỗi token đầu vào
    private val numClasses = 3 // Số nhãn: clean, offensive, hate

    private var interpreter: Interpreter
    private var tokenizer: Map<String, Int> = emptyMap()  // Từ điển từ -> số
    private var stopwords: Set<String> = emptySet()
    private var offensiveWords: Set<String> = emptySet()// Danh sách từ tục

    init {
        interpreter = Interpreter(loadModelFile())// Load mô hình TFLite
        tokenizer = loadTokenizer() // Load tokenizer từ JSON
        stopwords = loadStopwords() // Load stopword
        offensiveWords = loadOffensiveWords()// Load từ tục
    }

    // Đọc file mô hình .tflite từ assets
    private fun loadModelFile(): MappedByteBuffer {
        context.assets.openFd(modelPath).use { fd ->
            FileInputStream(fd.fileDescriptor).use { inputStream ->
                val channel = inputStream.channel
                return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }

    // Load tokenizer từ tokenizer.json
    private fun loadTokenizer(): Map<String, Int> {
        context.assets.open(tokenizerPath).bufferedReader().use { reader ->
            val json = JSONObject(reader.readText())
            val map = mutableMapOf<String, Int>()
            json.keys().forEach { key ->
                map[key] = json.getInt(key)
            }
            return map
        }
    }

    // Load danh sách từ dừng từ file .txt
    private fun loadStopwords(): Set<String> {
        context.assets.open(stopwordsPath).bufferedReader().useLines { lines ->
            return lines.map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
    }

    // Load danh sách từ tục (từ nhạy cảm) từ file .txt
    private fun loadOffensiveWords(): Set<String> {
        context.assets.open(offensiveWordsPath).bufferedReader().useLines { lines ->
            return lines.map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
    }

    // Tách từ: chuẩn hóa văn bản, lọc từ không trong stopword
    private fun tokenize(text: String): List<String> {
        val lowerText = text.lowercase()
        val regex = "\\b[\\p{L}\\p{Nd}_]+\\b".toRegex() // chỉ lấy từ và số
        val tokens = regex.findAll(lowerText).map { it.value }.toList()
        return tokens.filter { it !in stopwords }
    }

    // Tiền xử lý: chuyển từ thành số, padding cho đủ độ dài
    private fun preprocess(text: String): IntArray {
        val tokens = tokenize(text)
        val ids = tokens.map { tokenizer[it] ?: 0 }
        val padded = IntArray(sequenceLength) { 0 }
        ids.take(sequenceLength).forEachIndexed { i, id -> padded[i] = id }
        return padded
    }

    // Giống preprocess nhưng trả thêm danh sách từ đã lọc (debug)
    fun preprocessDebug(text: String): Pair<List<String>, IntArray> {
        val tokens = tokenize(text)
        val ids = tokens.map { tokenizer[it] ?: 0 }
        val padded = IntArray(sequenceLength) { 0 }
        ids.take(sequenceLength).forEachIndexed { i, id -> padded[i] = id }
        return tokens to padded
    }

    // Dự đoán nhãn đầu ra: clean, offensive, hate kèm xác suất
    private fun predict(text: String): Pair<String, Float> {
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

    // Kiểm tra văn bản có chứa từ tục không (độc lập với model)
    private fun containsOffensiveWords(text: String): Boolean {
        val tokens = tokenize(text)
        val ngrams = mutableSetOf<String>()

        // Thêm đơn từ
        ngrams.addAll(tokens)

        // Thêm cụm 2 từ liên tiếp (bi-gram)
        for (i in 0 until tokens.size - 1) {
            ngrams.add("${tokens[i]} ${tokens[i + 1]}")
        }

        return ngrams.any { it in offensiveWords }
    }

    // Dự đoán có fallback: nếu model trả "clean" nhưng có từ tục → trả "offensive"
    private fun predictWithFallback(text: String): Pair<String, Float> {
        val (label, confidence) = predict(text)
        if (label == "clean" && containsOffensiveWords(text)) {
            return "offensive" to 1.0f
        }
        return label to confidence
    }

    // Lazy load các câu slogan thay thế từ slogans.json
    private val slogans: Map<String, List<String>> by lazy {
        loadSlogans()
    }

    // Load slogan từ file slogans.json: key là loại nhãn → danh sách câu thay thế
    private fun loadSlogans(): Map<String, List<String>> {
        val jsonStr = context.assets.open("slogans.json").bufferedReader().readText()
        val jsonObject = JSONObject(jsonStr)
        val map = mutableMapOf<String, List<String>>()
        jsonObject.keys().forEach { key ->
            val arr = jsonObject.getJSONArray(key)
            val list = List(arr.length()) { i -> arr.getString(i) }
            map[key] = list
        }
        return map
    }

    // Nếu là comment xấu thì thay bằng câu slogan phù hợp
    fun cleanTextIfToxic(text: String, type: String): String {
        val (label, _) = predictWithFallback(text)
        if (label != "clean") {
            val list = slogans[type] ?: listOf("Hãy lan tỏa điều tích cực!")
            return list.random()
        }
        return text
    }

    // Đóng model khi không dùng nữa
    fun close() {
        interpreter.close()
    }
}








//class TextClassifier(private val context: Context) {
//
//    // Đường dẫn đến các file mô hình và dữ liệu trong thư mục assets
//    private val modelPath = "Text_CNN_model_v13.tflite"
//    private val tokenizerPath = "tokenizer.json"
//    private val stopwordsPath = "vietnamese-stopwords-dash.txt"
//    private val offensiveWordsPath = "offensive-words.txt"
//
//    private val sequenceLength = 100 // Độ dài chuỗi token đầu vào
//    private val numClasses = 3 // Số nhãn: clean, offensive, hate
//
//    private var interpreter: Interpreter
//    private var tokenizer: Map<String, Int> = emptyMap()  // Từ điển từ -> số
//    private var stopwords: Set<String> = emptySet()
//    private var offensiveWords: Set<String> = emptySet()// Danh sách từ tục
//
//    init {
//        interpreter = Interpreter(loadModelFile())// Load mô hình TFLite
//        tokenizer = loadTokenizer() // Load tokenizer từ JSON
//        stopwords = loadStopwords() // Load stopword
//        offensiveWords = loadOffensiveWords()// Load từ tục
//    }
//
//    // Đọc file mô hình .tflite từ assets
//    private fun loadModelFile(): MappedByteBuffer {
//        context.assets.openFd(modelPath).use { fd ->
//            FileInputStream(fd.fileDescriptor).use { inputStream ->
//                val channel = inputStream.channel
//                return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
//            }
//        }
//    }
//
//    // Load tokenizer từ tokenizer.json
//    private fun loadTokenizer(): Map<String, Int> {
//        context.assets.open(tokenizerPath).bufferedReader().use { reader ->
//            val json = JSONObject(reader.readText())
//            val map = mutableMapOf<String, Int>()
//            json.keys().forEach { key ->
//                map[key] = json.getInt(key)
//            }
//            return map
//        }
//    }
//
//    // Load danh sách từ dừng từ file .txt
//    private fun loadStopwords(): Set<String> {
//        context.assets.open(stopwordsPath).bufferedReader().useLines { lines ->
//            return lines.map { it.trim() }
//                .filter { it.isNotEmpty() }
//                .toSet()
//        }
//    }
//
//    // Load danh sách từ tục (từ nhạy cảm) từ file .txt
//    private fun loadOffensiveWords(): Set<String> {
//        context.assets.open(offensiveWordsPath).bufferedReader().useLines { lines ->
//            return lines.map { it.trim().lowercase() }
//                .filter { it.isNotEmpty() }
//                .toSet()
//        }
//    }
//
//    // Tách từ: chuẩn hóa văn bản, lọc từ không trong stopword
//    private fun tokenize(text: String): List<String> {
//        val lowerText = text.lowercase()
//        val regex = "\\b[\\p{L}\\p{Nd}_]+\\b".toRegex() // chỉ lấy từ và số
//        val tokens = regex.findAll(lowerText).map { it.value }.toList()
//        return tokens.filter { it !in stopwords }
//    }
//
//    // Tiền xử lý: chuyển từ thành số, padding cho đủ độ dài
//    fun preprocess(text: String): IntArray {
//        val tokens = tokenize(text).filter { it !in offensiveWords }
//        val ids = tokens.map { tokenizer[it] ?: 0 }
//        val padded = IntArray(sequenceLength) { 0 }
//        ids.take(sequenceLength).forEachIndexed { i, id -> padded[i] = id }
//        return padded
//    }
//
//
//    // Giống preprocess nhưng trả thêm danh sách từ đã lọc (debug)
//    fun preprocessDebug(text: String): Pair<List<String>, IntArray> {
//        val tokens = tokenize(text)
//        val ids = tokens.map { tokenizer[it] ?: 0 }
//        val padded = IntArray(sequenceLength) { 0 }
//        ids.take(sequenceLength).forEachIndexed { i, id -> padded[i] = id }
//        return tokens to padded
//    }
//
//    // Dự đoán nhãn đầu ra: clean, offensive, hate kèm xác suất
//    private fun predict(text: String): Pair<String, Float> {
//        val inputIds = preprocess(text)
//        val inputBuffer = ByteBuffer.allocateDirect(4 * sequenceLength).order(ByteOrder.nativeOrder())
//        inputIds.forEach { inputBuffer.putInt(it) }
//        inputBuffer.rewind()
//
//        val outputBuffer = ByteBuffer.allocateDirect(4 * numClasses).order(ByteOrder.nativeOrder())
//        interpreter.run(inputBuffer, outputBuffer)
//        outputBuffer.rewind()
//
//        val probs = FloatArray(numClasses) { outputBuffer.float }
//        val labels = arrayOf("clean", "offensive", "hate")
//        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
//        return labels[maxIdx] to probs[maxIdx]
//    }
//
//    // Kiểm tra văn bản có chứa từ tục không (độc lập với model)
//    private fun containsOffensiveWords(text: String): Boolean {
//        val normalized = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
//            .replace("\\p{Mn}+".toRegex(), "") // Bỏ dấu
//            .replace("[^\\p{L}\\p{Nd}]".toRegex(), " ") // Bỏ ký tự đặc biệt, giữ từ
//            .replace("\\s+".toRegex(), " ") // Rút gọn khoảng trắng
//            .trim()
//
//        val tokens = normalized.split(" ")
//        val joined = tokens.joinToString("") // Toàn bộ câu dính liền (không dấu, không cách)
//
//        // Kiểm tra theo n-gram từ 1 đến 4 từ
//        for (n in 1..4) {
//            for (window in tokens.windowed(n, 1)) {
//                val phraseWithSpace = window.joinToString(" ")
//                val phraseNoSpace = window.joinToString("")
//                if (phraseWithSpace in offensiveWords || phraseNoSpace in offensiveWords) {
//                    return true
//                }
//            }
//        }
//
//        // Kiểm tra toàn bộ câu đã dính liền với từng từ cấm (ngăn lách dấu, viết liền)
//        return offensiveWords.any { joined.contains(it) }
//    }
//
//
//    // Dự đoán có fallback: nếu model trả "clean" nhưng có từ tục → trả "offensive"
//    private fun predictWithFallback(text: String): Pair<String, Float> {
//        val (label, confidence) = predict(text)
//        val hasOffensiveWords = containsOffensiveWords(text)
//
//        // Nếu model dự đoán nhầm clean → nhưng có từ tục thì sửa
//        if (label == "clean" && hasOffensiveWords) {
//            return "offensive" to 1.0f
//        }
//
//        // Nếu model là offensive nhưng confidence thấp và không có từ tục → có thể là nhầm
//        if (label == "offensive" && confidence < 0.6f && !hasOffensiveWords) {
//            return "clean" to 0.6f // fallback nhẹ
//        }
//
//        return label to confidence
//    }
//
//
//    // Lazy load các câu slogan thay thế từ slogans.json
//    private val slogans: Map<String, List<String>> by lazy {
//        loadSlogans()
//    }
//
//    // Load slogan từ file slogans.json: key là loại nhãn → danh sách câu thay thế
//    private fun loadSlogans(): Map<String, List<String>> {
//        val jsonStr = context.assets.open("slogans.json").bufferedReader().readText()
//        val jsonObject = JSONObject(jsonStr)
//        val map = mutableMapOf<String, List<String>>()
//        jsonObject.keys().forEach { key ->
//            val arr = jsonObject.getJSONArray(key)
//            val list = List(arr.length()) { i -> arr.getString(i) }
//            map[key] = list
//        }
//        return map
//    }
//
//    // Nếu là comment xấu thì thay bằng câu slogan phù hợp
//    fun cleanTextIfToxic(text: String, type: String): String {
//        val (label, _) = predictWithFallback(text)
//        if (label != "clean") {
//            val list = slogans[type] ?: listOf("Hãy lan tỏa điều tích cực!")
//            return list.random()
//        }
//        return text
//    }
//
//    // Đóng model khi không dùng nữa
//    fun close() {
//        interpreter.close()
//    }
//}

