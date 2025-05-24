package com.htduc.socialmediaapplication.moderation

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TextClassifier(private val context: Context) {

    private val modelPath = "Text_CNN_model_v13.tflite"
    private val tokenizerPath = "tokenizer.json"
    private val stopwordsPath = "vietnamese-stopwords-dash.txt"
    private val offensiveWordsPath = "offensive-words.txt"

    private val sequenceLength = 100
    private val numClasses = 3

    private var interpreter: Interpreter
    private var tokenizer: Map<String, Int> = emptyMap()
    private var stopwords: Set<String> = emptySet()
    private var offensiveWords: Set<String> = emptySet()

    init {
        interpreter = Interpreter(loadModelFile())
        tokenizer = loadTokenizer()
        stopwords = loadStopwords()
        offensiveWords = loadOffensiveWords()
    }

    private fun loadModelFile(): MappedByteBuffer {
        context.assets.openFd(modelPath).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                return fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
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

    private fun loadOffensiveWords(): Set<String> {
        context.assets.open(offensiveWordsPath).bufferedReader().useLines { lines ->
            return lines.map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
    }

    private fun tokenize(text: String): List<String> {
        val lowerText = text.lowercase()
        val regex = "\\b[\\p{L}\\p{Nd}_]+\\b".toRegex()
        val tokens = regex.findAll(lowerText).map { it.value }.toList()
        return tokens.filter { it !in stopwords }
    }

    fun preprocess(text: String): IntArray {
        val filteredTokens = tokenize(text)
        val tokenIds = filteredTokens.map { tokenizer[it] ?: 0 }
        val padded = IntArray(sequenceLength) { 0 }
        tokenIds.take(sequenceLength).forEachIndexed { i, id -> padded[i] = id }
        return padded
    }

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

    private fun containsOffensiveWords(text: String): Boolean {
        val tokens = tokenize(text)
        return tokens.any { it in offensiveWords }
    }

    private fun predictWithFallback(text: String): Pair<String, Float> {
        val (label, confidence) = predict(text)
        if (label == "clean" && containsOffensiveWords(text)) {
            return "offensive" to 1.0f
        }
        return label to confidence
    }

    private val slogans: Map<String, List<String>> by lazy {
        loadSlogans()
    }

    private fun loadSlogans(): Map<String, List<String>> {
        val inputStream = context.assets.open("slogans.json")
        val jsonStr = inputStream.bufferedReader().readText()
        val jsonObject = JSONObject(jsonStr)

        val slogansMap = mutableMapOf<String, List<String>>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val array = jsonObject.getJSONArray(key)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            slogansMap[key] = list
        }
        return slogansMap
    }

    fun cleanTextIfToxic(text: String, type: String): String {
        val (label, _) = predictWithFallback(text)
        if (label != "clean") {
            val safeList = slogans[type] ?: listOf("Hãy lan tỏa điều tích cực!")
            return safeList.random()
        }
        return text
    }

    fun close() {
        interpreter.close()
    }
}


