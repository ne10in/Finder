package com.example.finder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils

class Algo
{
    fun calculateFingerPrint(imgs: MutableList<Img>)
    {
        for (img in imgs)
        {
            val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(img.path),96, 96)

            val scale_width = 8.0f / bitmap.width
            val scale_height = 8.0f / bitmap.height

            val matrix = Matrix()
            matrix.postScale(scale_width, scale_height)

            val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
            img.hash = calcHash(scaledBitmap)

            bitmap.recycle()
            scaledBitmap.recycle()
        }
    }

    private fun calcHash(img: Bitmap): Long
    {
        val pixels = getGrayPixels(img)
        val avg = getAvg(pixels)
        val width = pixels[0].size
        val height = pixels.size
        val bytes = ByteArray(height * width)
        val stringBuilder = StringBuilder()

        for (i in 0 until width)
        {
            for (j in 0 until height)
            {

                if (pixels[i][j] >= avg)
                {
                    bytes[i * height + j] = 1
                    stringBuilder.append("1")
                }
                else
                {
                    bytes[i * height + j] = 0
                    stringBuilder.append("0")
                }
            }
        }

        var hash1: Long = 0
        var hash2: Long = 0

        for (i in 0..63)
        {
            if (i < 32)
            {
                hash1 += (bytes[63 - i].toInt() shl i).toLong()
            }
            else
            {
                hash2 += (bytes[63 - i].toInt() shl i - 31).toLong()
            }
        }

        return (hash2 shl 32) + hash1
    }

    private fun getAvg(pixels: Array<DoubleArray>): Double
    {
        val width = pixels[0].size
        val height = pixels.size
        var count = 0

        for (i in 0 until width)
        {
            for (j in 0 until height)
            {
                count += pixels[i][j].toInt()
            }
        }

        return (count / (width * height)).toDouble()
    }

    private fun getGrayPixels(bitmap: Bitmap): Array<DoubleArray>
    {
        val width = 8
        val height = 8
        val pixels = Array(height) { DoubleArray(width) }

        for (i in 0 until width)
        {
            for (j in 0 until height)
            {
                pixels[i][j] = convertGrayPixel(bitmap.getPixel(i, j))
            }
        }

        return pixels
    }

    private fun convertGrayPixel(pixel: Int): Double
    {
        val red = pixel shr 16 and 0xFF
        val green = pixel shr 8 and 0xFF
        val blue = pixel and 255

        return 0.3 * red + 0.59 * green + 0.11 * blue
    }

    fun hamDist(finger1: Long, finger2: Long): Int
    {
        var dist = 0
        var result = finger1 xor finger2

        while (result != 0L)
        {
            ++dist
            result = result and result - 1
        }

        return dist
    }
}