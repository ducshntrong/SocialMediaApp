package com.htduc.socialmediaapplication.Moderation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Log
import org.tensorflow.lite.DataType
import java.io.InputStream

class NSFWDetector(context: Context) {
    private var interpreter: Interpreter? = null
    private var inputDataType: DataType? = null
    private var outputDataType: DataType? = null

    init {
        try {
            val modelBuffer = loadModelFile(context, "nsfw1.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)

            // Kiểm tra loại dữ liệu đầu vào & đầu ra của mô hình
            inputDataType = interpreter?.getInputTensor(0)?.dataType()
            outputDataType = interpreter?.getOutputTensor(0)?.dataType()

            Log.d("com.htduc.socialmediaapplication.moderation.NSFWDetector", "Model Input Type: $inputDataType, Output Type: $outputDataType")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val inputStream: InputStream = context.assets.open(modelName)
        val byteArray = inputStream.readBytes()
        inputStream.close()

        val buffer = ByteBuffer.allocateDirect(byteArray.size)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(byteArray)
        buffer.rewind()

        return buffer
    }

//    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
//        val fileDescriptor = context.assets.openFd(modelName)
//        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
//        val fileChannel = inputStream.channel
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
//    }

    fun detectNSFW(context: Context, imageUri: Uri): Float {
        val bitmap = uriToBitmap(context, imageUri) ?: return 0.0f
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // Chọn phương pháp chuyển đổi dữ liệu đúng
        val inputBuffer: Any = when (inputDataType) {
            DataType.UINT8 -> convertBitmapToByteBufferUINT8(resizedBitmap)
            DataType.FLOAT32 -> convertBitmapToByteBufferFLOAT32(resizedBitmap)
            else -> throw IllegalArgumentException("Unsupported model input type: $inputDataType")
        }

        // Khởi tạo output buffer phù hợp với kiểu dữ liệu của mô hình
        val output = when (outputDataType) {
            DataType.UINT8 -> Array(1) { ByteArray(2) }
            DataType.FLOAT32 -> Array(1) { FloatArray(2) }
            else -> throw IllegalArgumentException("Unsupported model output type: $outputDataType")
        }

        // Chạy mô hình
        interpreter?.run(inputBuffer, output)

        // Lấy giá trị NSFW & SFW từ output
        val (nsfwScore, sfwScore) = when (outputDataType) {
            DataType.UINT8 -> { 
                val byteArray = output as Array<ByteArray> // Ép kiểu an toàn
                val nsfw = (byteArray[0][1].toInt() and 0xFF) / 255.0f
                val sfw = (byteArray[0][0].toInt() and 0xFF) / 255.0f
                nsfw to sfw
            }
            DataType.FLOAT32 -> {
                val floatArray = output as Array<FloatArray>
                floatArray[0][1] to floatArray[0][0]
            }
            else -> 0.0f to 0.0f
        }

        Log.d("com.htduc.socialmediaapplication.moderation.NSFWDetector", "NSFW Score: $nsfwScore, SFW Score: $sfwScore")
        return nsfwScore
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Chuyển ảnh thành ByteBuffer dạng UINT8 (0-255)
    private fun convertBitmapToByteBufferUINT8(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(224 * 224 * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = bitmap.getPixel(x, y)
                inputBuffer.put((pixel shr 16 and 0xFF).toByte()) // Red
                inputBuffer.put((pixel shr 8 and 0xFF).toByte())  // Green
                inputBuffer.put((pixel and 0xFF).toByte())        // Blue
            }
        }

        inputBuffer.rewind()
        return inputBuffer
    }

    // Chuyển ảnh thành ByteBuffer dạng FLOAT32 (chuẩn hóa về 0-1)
    private fun convertBitmapToByteBufferFLOAT32(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4) // FLOAT32 (4 byte mỗi giá trị)
        inputBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = bitmap.getPixel(x, y)
                inputBuffer.putFloat((pixel shr 16 and 0xFF) / 255.0f) // Red
                inputBuffer.putFloat((pixel shr 8 and 0xFF) / 255.0f)  // Green
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)        // Blue
            }
        }

        inputBuffer.rewind()
        return inputBuffer
    }
}


