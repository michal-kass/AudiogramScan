package com.audiogram.scan

import android.graphics.Rect
import java.util.Arrays
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

class Result(@JvmField var classIndex: Int, @JvmField var score: Float, @JvmField var rect: Rect)

object PrePostProcessor {
    // for yolov5 model, no need to apply MEAN and STD
    var NO_MEAN_RGB: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    var NO_STD_RGB: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f)

    // model input image size
    var mInputWidth: Int = 640
    var mInputHeight: Int = 640

    // model output is of size 25200*(num_of_class+5)
    private const val mOutputRow =
        25200 // as decided by the YOLOv5 model for input image of size 640*640
    private const val mOutputColumn = 6 // left, top, right, bottom, score and 80 class probability
    private const val mThreshold = 0.60f // score above which a detection is generated
    private const val mNmsLimit = 30

    @JvmField
    var mClasses: Array<String> = arrayOf("ag")
    var mClasses_symbols: Array<String> = arrayOf(
        "circle",
        "close_bracket",
        "cross",
        "greater_than",
        "less_than",
        "open_bracket",
        "square",
        "triangle"
    )
    var mClasses_numbers: Array<String> = arrayOf(
        "-10",
        "0",
        "10",
        "100",
        "110",
        "120",
        "125",
        "1k",
        "20",
        "250",
        "2k",
        "30",
        "40",
        "4k",
        "50",
        "500",
        "60",
        "70",
        "80",
        "8k",
        "90"
    )


    // The two methods nonMaxSuppression and IOU below are ported from https://github.com/hollance/YOLO-CoreML-MPSNNGraph/blob/master/Common/Helpers.swift
    /**
     * Removes bounding boxes that overlap too much with other boxes that have
     * a higher score.
     * - Parameters:
     * - boxes: an array of bounding boxes and their scores
     * - limit: the maximum number of boxes that will be selected
     * - threshold: used to decide whether boxes overlap too much
     */
    fun nonMaxSuppression(
        boxes: ArrayList<Result>,
        limit: Int,
        threshold: Float
    ): ArrayList<Result> {
        // Do an argsort on the confidence scores, from high to low.

        Collections.sort(
            boxes
        ) { o1, o2 -> o1.score.compareTo(o2.score) }

        val selected = ArrayList<Result>()
        val active = BooleanArray(boxes.size)
        Arrays.fill(active, true)
        var numActive = active.size

        // The algorithm is simple: Start with the box that has the highest score.
        // Remove any remaining boxes that overlap it more than the given threshold
        // amount. If there are any boxes left (i.e. these did not overlap with any
        // previous boxes), then repeat this procedure, until no more boxes remain
        // or the limit has been reached.
        var done = false
        var i = 0
        while (i < boxes.size && !done) {
            if (active[i]) {
                val boxA = boxes[i]
                selected.add(boxA)
                if (selected.size >= limit) break

                for (j in i + 1 until boxes.size) {
                    if (active[j]) {
                        val boxB = boxes[j]
                        if (IOU(boxA.rect, boxB.rect) > threshold) {
                            active[j] = false
                            numActive -= 1
                            if (numActive <= 0) {
                                done = true
                                break
                            }
                        }
                    }
                }
            }
            i++
        }
        return selected
    }

    /**
     * Computes intersection-over-union overlap between two bounding boxes.
     */
    fun IOU(a: Rect, b: Rect): Float {
        val areaA = ((a.right - a.left) * (a.bottom - a.top)).toFloat()
        if (areaA <= 0.0) return 0.0f

        val areaB = ((b.right - b.left) * (b.bottom - b.top)).toFloat()
        if (areaB <= 0.0) return 0.0f

        val intersectionMinX = max(a.left.toDouble(), b.left.toDouble()).toFloat()
        val intersectionMinY = max(a.top.toDouble(), b.top.toDouble()).toFloat()
        val intersectionMaxX = min(a.right.toDouble(), b.right.toDouble())
            .toFloat()
        val intersectionMaxY = min(a.bottom.toDouble(), b.bottom.toDouble())
            .toFloat()
        val intersectionArea = (max(
            (intersectionMaxY - intersectionMinY).toDouble(),
            0.0
        ) * max((intersectionMaxX - intersectionMinX).toDouble(), 0.0)).toFloat()
        return intersectionArea / (areaA + areaB - intersectionArea)
    }

    fun outputsToNMSPredictions(
        mOutputColumn: Int,
        outputs: FloatArray,
        imgScaleX: Float,
        imgScaleY: Float,
        ivScaleX: Float,
        ivScaleY: Float,
        startX: Float,
        startY: Float
    ): ArrayList<Result> {
        val results = ArrayList<Result>()
        for (i in 0 until mOutputRow) {
            if (outputs[i * mOutputColumn + 4] > mThreshold) {
                val x = outputs[i * mOutputColumn]
                val y = outputs[i * mOutputColumn + 1]
                val w = outputs[i * mOutputColumn + 2]
                val h = outputs[i * mOutputColumn + 3]

                val left = imgScaleX * (x - w / 2)
                val top = imgScaleY * (y - h / 2)
                val right = imgScaleX * (x + w / 2)
                val bottom = imgScaleY * (y + h / 2)

                var max = outputs[i * mOutputColumn + 5]
                var cls = 0
                for (j in 0 until mOutputColumn - 5) {
                    if (outputs[i * mOutputColumn + 5 + j] > max) {
                        max = outputs[i * mOutputColumn + 5 + j]
                        cls = j
                    }
                }

                val rect = Rect(
                    left.toInt(),
                    top.toInt(),
                    right.toInt(),
                    bottom.toInt()
                )
                val result = Result(cls, outputs[i * mOutputColumn + 4], rect)
                results.add(result)
            }
        }
        return nonMaxSuppression(results, mNmsLimit, mThreshold)
    }
}
