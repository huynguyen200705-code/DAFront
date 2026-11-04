package com.example.adr_nhom_da.data.local

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot

object FaceBiometricHelper {
    private const val TAG = "FaceBiometricHelper"
    const val DEFAULT_MATCH_THRESHOLD = 0.85f

    /**
     * Extracts a scale-invariant, rotation-compensated geometric feature vector from a ML Kit Face.
     */
    fun extractFaceSignature(face: Face): String? {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position
        val rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position

        val bb = face.boundingBox
        val bbWidth = bb.width().toFloat().coerceAtLeast(10f)
        val bbHeight = bb.height().toFloat().coerceAtLeast(10f)
        val bbAspect = bbWidth / bbHeight

        // Base unit: Distance between both eyes or 35% of bounding box width
        val eyeDist = if (leftEye != null && rightEye != null) {
            hypot(leftEye.x - rightEye.x, leftEye.y - rightEye.y).coerceAtLeast(10f)
        } else {
            bbWidth * 0.35f
        }

        val eyeCenterX = if (leftEye != null && rightEye != null) {
            (leftEye.x + rightEye.x) / 2f
        } else {
            bb.exactCenterX()
        }

        val eyeCenterY = if (leftEye != null && rightEye != null) {
            (leftEye.y + rightEye.y) / 2f
        } else {
            bb.top + bbHeight * 0.35f
        }

        // Feature 1: Ratio of eye distance to face width
        val f1 = eyeDist / bbWidth

        // Feature 2: Ratio of eye-center-to-nose to eye distance
        val f2 = if (nose != null) {
            hypot(eyeCenterX - nose.x, eyeCenterY - nose.y) / eyeDist
        } else 0.55f

        // Feature 3: Ratio of nose to mouth bottom
        val f3 = if (nose != null && mouthBottom != null) {
            hypot(nose.x - mouthBottom.x, nose.y - mouthBottom.y) / eyeDist
        } else 0.50f

        // Feature 4: Ratio of mouth width to eye distance
        val f4 = if (mouthLeft != null && mouthRight != null) {
            hypot(mouthLeft.x - mouthRight.x, mouthLeft.y - mouthRight.y) / eyeDist
        } else 0.65f

        // Feature 5: Symmetry ratio between left and right eye to nose
        val f5 = if (leftEye != null && rightEye != null && nose != null) {
            val dL = hypot(leftEye.x - nose.x, leftEye.y - nose.y)
            val dR = hypot(rightEye.x - nose.x, rightEye.y - nose.y).coerceAtLeast(1f)
            dL / dR
        } else 1.0f

        // Feature 6: Ratio of cheek distance to eye distance
        val f6 = if (leftCheek != null && rightCheek != null) {
            hypot(leftCheek.x - rightCheek.x, leftCheek.y - rightCheek.y) / eyeDist
        } else 1.25f

        // Feature 7: Ratio of left eye to left mouth corner
        val f7 = if (leftEye != null && mouthLeft != null) {
            hypot(leftEye.x - mouthLeft.x, leftEye.y - mouthLeft.y) / eyeDist
        } else 0.95f

        // Feature 8: Ratio of right eye to right mouth corner
        val f8 = if (rightEye != null && mouthRight != null) {
            hypot(rightEye.x - mouthRight.x, rightEye.y - mouthRight.y) / eyeDist
        } else 0.95f

        // Feature 9: Face bounding box aspect ratio
        val f9 = bbAspect

        // Feature 10: Lower chin proportion
        val f10 = if (nose != null) {
            (bb.bottom.toFloat() - nose.y).coerceAtLeast(0f) / bbHeight
        } else 0.45f

        val vector = listOf(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10)
        return serializeVector(vector)
    }

    private fun serializeVector(vector: List<Float>): String {
        val arr = JSONArray()
        for (v in vector) {
            arr.put(Math.round(v * 1000.0) / 1000.0)
        }
        val obj = JSONObject().apply {
            put("v", 1)
            put("vector", arr)
            put("timestamp", System.currentTimeMillis())
        }
        return obj.toString()
    }

    fun parseVector(signature: String?): List<Float>? {
        if (signature.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(signature)
            val arr = obj.optJSONArray("vector") ?: return null
            val list = mutableListOf<Float>()
            for (i in 0 until arr.length()) {
                list.add(arr.getDouble(i).toFloat())
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing vector: ${e.message}")
            null
        }
    }

    /**
     * Averages multiple frame vectors to build a clean biometric template.
     */
    fun averageSignatures(signatures: List<String>): String? {
        val vectors = signatures.mapNotNull { parseVector(it) }
        if (vectors.isEmpty()) return null
        val dim = vectors.first().size
        val avg = FloatArray(dim)
        for (vec in vectors) {
            for (i in 0 until minOf(dim, vec.size)) {
                avg[i] += vec[i]
            }
        }
        val result = avg.map { it / vectors.size }
        return serializeVector(result)
    }

    /**
     * Calculates similarity score between 0.0 (completely different) to 1.0 (exact match).
     */
    fun calculateSimilarity(sig1: String?, sig2: String?): Float {
        val v1 = parseVector(sig1) ?: return 0f
        val v2 = parseVector(sig2) ?: return 0f
        val n = minOf(v1.size, v2.size)
        if (n == 0) return 0f

        var totalDiff = 0.0
        for (i in 0 until n) {
            val a = v1[i]
            val b = v2[i]
            val denom = maxOf(abs(a), abs(b), 0.001f)
            totalDiff += abs(a - b) / denom
        }
        val avgDiff = (totalDiff / n).toFloat()
        return (1.0f - avgDiff).coerceIn(0f, 1f)
    }

    /**
     * Determines whether two face signatures belong to the same person.
     */
    fun isMatch(sig1: String?, sig2: String?, threshold: Float = DEFAULT_MATCH_THRESHOLD): Boolean {
        val score = calculateSimilarity(sig1, sig2)
        return score >= threshold
    }

    /**
     * Generates a deterministic mock signature for testing on emulators or when camera is disabled.
     */
    fun createMockSignature(seedText: String): String {
        val hash = abs(seedText.hashCode())
        val f1 = 0.35f + (hash % 10) * 0.01f
        val f2 = 0.50f + ((hash / 10) % 10) * 0.01f
        val f3 = 0.48f + ((hash / 100) % 10) * 0.01f
        val f4 = 0.62f + ((hash / 1000) % 10) * 0.01f
        val f5 = 1.00f + ((hash / 10000) % 5) * 0.01f
        val f6 = 1.20f + ((hash / 100000) % 10) * 0.01f
        val f7 = 0.92f + ((hash / 1000000) % 10) * 0.01f
        val f8 = 0.92f + ((hash / 10000000) % 10) * 0.01f
        val f9 = 0.78f + ((hash / 100000000) % 10) * 0.01f
        val f10 = 0.44f + (hash % 7) * 0.01f
        return serializeVector(listOf(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10))
    }
}

