package com.audiogram.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.pow

val dbvalues = arrayOf(
    "-10",
    "0",
    "10",
    "20",
    "30",
    "40",
    "50",
    "60",
    "70",
    "80",
    "90",
    "100",
    "110",
    "120"
)
val fzvalues = arrayOf("125", "250", "500", "1k", "2k", "4k", "8k")


val dbvalues_full = arrayOf(
    "-10",
    "-5",
    "0",
    "5",
    "10",
    "15",
    "20",
    "25",
    "30",
    "35",
    "40",
    "45",
    "50",
    "55",
    "60",
    "65",
    "70",
    "75",
    "80",
    "85",
    "90",
    "95",
    "100",
    "105",
    "110",
    "115",
    "120"
)

fun audiogram_bin(mBitmap : Bitmap) : Bitmap {
    val mat = Mat()
    Utils.bitmapToMat(mBitmap, mat)
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
    Imgproc.threshold(mat, mat, 0.0, 255.0, Imgproc.THRESH_OTSU);
    val newBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, newBitmap)
    return newBitmap
}

fun hlines(bitmap : Bitmap): Mat {
    var Hlines = Mat()
    val mat = Mat()
    var edges = Mat()


    Utils.bitmapToMat(bitmap, mat)

    var meansrc = MatOfDouble()
    var stdsrc = MatOfDouble()
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

    Core.meanStdDev(mat, meansrc, stdsrc);
    var v = meansrc.get(0,0)[0]



    Imgproc.Canny(mat, edges, v/10, v/5)
    Imgproc.HoughLinesP(
        edges,
        Hlines,
        1.0,
        Math.PI / 180,
        100,
        mat.height() * 0.8,
        mat.height() * 0.5
    )
    for (x in 0 until Hlines.rows()) {
        val l: DoubleArray = Hlines.get(x, 0)
        Imgproc.line(
            mat,
            Point(l[0], l[1]),
            Point(l[2], l[3]),
            Scalar(0.0, 0.0, 255.0),
            3,
            Imgproc.LINE_AA,
            0
        )
    }
    val newBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, newBitmap)

    return Hlines
}

fun print_rec(rec_arr : Array<Rect?>, bmp2: Bitmap){
    val canvas = Canvas(bmp2)
    var mPaintRectangle = Paint()
    mPaintRectangle.strokeWidth = 8f
    mPaintRectangle.style = Paint.Style.STROKE
    mPaintRectangle.color = Color.YELLOW
    for(element in rec_arr){
        canvas.drawRect(element!!, mPaintRectangle)

    }
}

fun find(a: Array<String>, element: String): Boolean {
    val found = Arrays.stream(a).anyMatch { t -> t == element }
    return found
}

fun linear_reg(arr: Array<Rect?>): List<Double> {
    var non_null_values = 0
    for (elemnt in arr) {
        if (elemnt != null) {
            non_null_values++
        }
    }
    val xs = mutableListOf<Int>()
    val ys = mutableListOf<Int>()

    for (elemnt in arr) {
        if (elemnt != null) {
            xs.add(elemnt.centerX())
            ys.add(elemnt.centerY())

        }
    }
    val variance = xs.sumOf { x -> (x - xs.average()).toDouble().pow(2) }
    val covariance = xs.zip(ys) { x, y -> (x - xs.average()) * (y - ys.average()) }.sum()
    val slope = covariance / variance
    val yIntercept = ys.average() - slope * xs.average()



    return listOf(slope, yIntercept)

}

fun filling_gaps_db(dbvaluesRect : Array<Rect?>, bmp2 : Bitmap) {
    var coun = 0
    for (i in dbvaluesRect.indices) {
        if (dbvaluesRect[i] != null) {
            coun++
        }
    }


//println(dbvalues.joinToString(", "))
//println(dbvaluesRect.joinToString(", "))
//val bmp3: Bitmap = mBitmap.copy(bmp2.getConfig(), true)
    val canvas = Canvas(bmp2)
    var mPaintRectangle = Paint()
    mPaintRectangle.strokeWidth = 4f
    mPaintRectangle.style = Paint.Style.STROKE
    mPaintRectangle.color = Color.MAGENTA

// co jesli mamy x x x v x v, co zrobic z tymi pierszymi x
// co jeśli tylko 0 i ostatni sie zrobil
    if (coun >= 2) {
        for (i in dbvaluesRect.indices) {
            if (dbvaluesRect[i] == null && i != dbvaluesRect.size - 1 && i != 0) {
                mPaintRectangle.color = Color.MAGENTA
                if (dbvaluesRect[i - 1] != null && dbvaluesRect[i + 1] != null) {
                    var left =
                        (dbvaluesRect[i + 1]?.left!! - dbvaluesRect[i - 1]?.left!!) / 2 + dbvaluesRect[i - 1]?.left!!
                    var right =
                        (dbvaluesRect[i + 1]?.right!! - dbvaluesRect[i - 1]?.right!!) / 2 + dbvaluesRect[i - 1]?.right!!
                    var top =
                        (dbvaluesRect[i + 1]?.top!! - dbvaluesRect[i - 1]?.top!!) / 2 + dbvaluesRect[i - 1]?.top!!
                    var bottom =
                        (dbvaluesRect[i + 1]?.bottom!! - dbvaluesRect[i - 1]?.bottom!!) / 2 + dbvaluesRect[i - 1]?.bottom!!
                    var newRect = Rect(left, top, right, bottom)
                    canvas.drawRect(newRect, mPaintRectangle)
                    //Glide.with(this).load(bmp3).into(mImageView)
                    dbvaluesRect[i] = newRect
                }
                if (dbvaluesRect[i - 1] != null && dbvaluesRect[i + 1] == null) {
                    var find = -1
                    var count = 0
                    for (k in i + 1..dbvaluesRect.size - 1) {
                        if (dbvaluesRect[k] != null) {
                            find = k
                            break
                        } else {
                            count++
                        }
                    }
                    // println(find)
                    //println(count)
                    if (find != -1) {
                        var left =
                            (dbvaluesRect[find]?.left!! - dbvaluesRect[i - 1]?.left!!) / (count + 2) + dbvaluesRect[i - 1]?.left!!
                        var right =
                            (dbvaluesRect[find]?.right!! - dbvaluesRect[i - 1]?.right!!) / (count + 2) + dbvaluesRect[i - 1]?.right!!
                        var top =
                            (dbvaluesRect[find]?.top!! - dbvaluesRect[i - 1]?.top!!) / (count + 2) + dbvaluesRect[i - 1]?.top!!
                        var bottom =
                            (dbvaluesRect[find]?.bottom!! - dbvaluesRect[i - 1]?.bottom!!) / (count + 2) + dbvaluesRect[i - 1]?.bottom!!
                        var newRect = Rect(left, top, right, bottom)
                        canvas.drawRect(newRect, mPaintRectangle)
                        dbvaluesRect[i] = newRect
                    }
                }
            }
        }
        for (i in dbvaluesRect.size - 1 downTo 1) {
            if (i != 0 && dbvaluesRect[i - 1] == null && dbvaluesRect[i] != null) {
                var find = -1
                var count = 0
                for (k in i + 1..dbvaluesRect.size) {
                    if (dbvaluesRect[k] != null) {
                        find = k
                        break
                    } else {
                        count++
                    }
                }
                //println("test")
                mPaintRectangle.color = Color.MAGENTA
                if (find != -1) {
                    var left =
                        dbvaluesRect[i]?.left!! - (dbvaluesRect[find]?.left!! - dbvaluesRect[i]?.left!!) / (count + 1)
                    var right =
                        dbvaluesRect[i]?.right!! - (dbvaluesRect[find]?.right!! - dbvaluesRect[i]?.right!!) / (count + 1)
                    var top =
                        dbvaluesRect[i]?.top!! - (dbvaluesRect[find]?.top!! - dbvaluesRect[i]?.top!!) / (count + 1)
                    var bottom =
                        dbvaluesRect[i]?.bottom!! - (dbvaluesRect[find]?.bottom!! - dbvaluesRect[i]?.bottom!!) / (count + 1)
                    var newRect = Rect(left, top, right, bottom)
                    canvas.drawRect(newRect, mPaintRectangle)
                    dbvaluesRect[i - 1] = newRect
                }


            }

        }
        for (i in 3..<dbvaluesRect.size) {
            if (dbvaluesRect[i] == null && dbvaluesRect[i-1] != null && dbvaluesRect[i-2] != null ) {
                var left =
                    (dbvaluesRect[i - 1]?.left!! - dbvaluesRect[i - 2]?.left!!) / 1 + dbvaluesRect[i - 1]?.left!!
                var right =
                    (dbvaluesRect[i - 1]?.right!! - dbvaluesRect[i - 2]?.right!!) / 1 + dbvaluesRect[i - 1]?.right!!
                var top =
                    (dbvaluesRect[i - 1]?.top!! - dbvaluesRect[i - 2]?.top!!) / 1 + dbvaluesRect[i - 1]?.top!!
                var bottom =
                    (dbvaluesRect[i - 1]?.bottom!! - dbvaluesRect[i - 2]?.bottom!!) / 1 + dbvaluesRect[i - 1]?.bottom!!
                var newRect = Rect(left, top, right, bottom)
                canvas.drawRect(newRect, mPaintRectangle)
                dbvaluesRect[i] = newRect
            }

        }
//Glide.with(this).load(bmp2).into(mImageView)

    }
    else{
        //(Toast.makeText(this, "OCV failed in DB, only " + coun.toString(), Toast.LENGTH_LONG)).show()

    }
}
fun filling_gaps_fz(fzvaluesRect: Array<Rect?>, bmp2: Bitmap) {


    val canvas = Canvas(bmp2)
    var mPaintRectangle = Paint()
    mPaintRectangle.strokeWidth = 4f
    mPaintRectangle.style = Paint.Style.STROKE
    mPaintRectangle.color = Color.MAGENTA


    for (i in fzvaluesRect.indices) {
        if (fzvaluesRect[i] == null && i != 0) {
            var left = false
            var right = false
            var find = -1
            var count = 0
            mPaintRectangle.color = Color.MAGENTA
            if (i != fzvaluesRect.size - 1 && fzvaluesRect[i - 1] != null && fzvaluesRect[i + 1] != null) {
                var left =
                    (fzvaluesRect[i + 1]?.left!! - fzvaluesRect[i - 1]?.left!!) / 2 + fzvaluesRect[i - 1]?.left!!
                var right =
                    (fzvaluesRect[i + 1]?.right!! - fzvaluesRect[i - 1]?.right!!) / 2 + fzvaluesRect[i - 1]?.right!!
                var top =
                    (fzvaluesRect[i + 1]?.top!! - fzvaluesRect[i - 1]?.top!!) / 2 + fzvaluesRect[i - 1]?.top!!
                var bottom =
                    (fzvaluesRect[i + 1]?.bottom!! - fzvaluesRect[i - 1]?.bottom!!) / 2 + fzvaluesRect[i - 1]?.bottom!!
                var newRect = Rect(left, top, right, bottom)
                canvas.drawRect(newRect, mPaintRectangle)
                //Glide.with(this).load(bmp3).into(mImageView)
                fzvaluesRect[i] = newRect
            }
            if (i != fzvaluesRect.size - 1 && fzvaluesRect[i - 1] != null && fzvaluesRect[i + 1] == null) {
                left = false
                right = false
                find = -1
                count = 0
                for (k in i + 1..fzvaluesRect.size - 1) {
                    if (fzvaluesRect[k] != null) {
                        find = k
                        right = true
                        break
                    } else {
                        count++
                    }
                }
                if (find == -1) {
                    count = 0
                    for (k in i - 2 downTo 0) {
                        if (fzvaluesRect[k] != null) {
                            find = k
                            left = true
                            break
                        } else {
                            count++
                        }
                    }

                }


                if (find != -1) {
                    if (right) {
                        var left =
                            (fzvaluesRect[find]?.left!! - fzvaluesRect[i - 1]?.left!!) / (count + 2) + fzvaluesRect[i - 1]?.left!!
                        var right =
                            (fzvaluesRect[find]?.right!! - fzvaluesRect[i - 1]?.right!!) / (count + 2) + fzvaluesRect[i - 1]?.right!!
                        var top =
                            (fzvaluesRect[find]?.top!! - fzvaluesRect[i - 1]?.top!!) / (count + 2) + fzvaluesRect[i - 1]?.top!!
                        var bottom =
                            (fzvaluesRect[find]?.bottom!! - fzvaluesRect[i - 1]?.bottom!!) / (count + 2) + fzvaluesRect[i - 1]?.bottom!!
                        var newRect = Rect(left, top, right, bottom)
                        canvas.drawRect(newRect, mPaintRectangle)
                        fzvaluesRect[i] = newRect
                    }
                    if (left) {

                        var left =
                            (fzvaluesRect[i - 1]?.left!! - fzvaluesRect[find]?.left!!) / (count + 1) + fzvaluesRect[i - 1]?.left!!
                        var right =
                            (fzvaluesRect[i - 1]?.right!! - fzvaluesRect[find]?.right!!) / (count + 1) + fzvaluesRect[i - 1]?.right!!
                        var top =
                            (fzvaluesRect[i - 1]?.top!! - fzvaluesRect[find]?.top!!) / (count + 1) + fzvaluesRect[i - 1]?.top!!
                        var bottom =
                            (fzvaluesRect[i - 1]?.bottom!! - fzvaluesRect[find]?.bottom!!) / (count + 1) + fzvaluesRect[i - 1]?.bottom!!
                        var newRect = Rect(left, top, right, bottom)
                        if (fzvalues[i - 1].length == 3 && fzvalues[find].length == 3 && fzvalues[i].length == 2) {
                            var wid = newRect.width()
                            newRect = Rect(left, top, right - wid / 5, bottom)
                        }
                        canvas.drawRect(newRect, mPaintRectangle)
                        fzvaluesRect[i] = newRect
                    }
                }

            }
            if (fzvaluesRect[i - 1] != null && i == fzvaluesRect.size - 1) {
                left = false
                count = 0
                for (k in i - 2 downTo 0) {
                    if (fzvaluesRect[k] != null) {
                        find = k
                        left = true
                        break
                    } else {
                        count++
                    }
                }
                if (left) {

                    var left =
                        (fzvaluesRect[i - 1]?.left!! - fzvaluesRect[find]?.left!!) / (count + 1) + fzvaluesRect[i - 1]?.left!!
                    var right =
                        (fzvaluesRect[i - 1]?.right!! - fzvaluesRect[find]?.right!!) / (count + 1) + fzvaluesRect[i - 1]?.right!!
                    var top =
                        (fzvaluesRect[i - 1]?.top!! - fzvaluesRect[find]?.top!!) / (count + 1) + fzvaluesRect[i - 1]?.top!!
                    var bottom =
                        (fzvaluesRect[i - 1]?.bottom!! - fzvaluesRect[find]?.bottom!!) / (count + 1) + fzvaluesRect[i - 1]?.bottom!!
                    var newRect = Rect(left, top, right, bottom)
                    canvas.drawRect(newRect, mPaintRectangle)
                    fzvaluesRect[i] = newRect
                }
            }
        }
    }
    for (i in fzvaluesRect.size - 1 downTo 1) {
        if (i != 0 && fzvaluesRect[i - 1] == null && fzvaluesRect[i] != null) {
            var find = -1
            var count = 0
            for (k in i + 1..fzvaluesRect.size) {
                if (fzvaluesRect[k] != null) {
                    find = k
                    break
                } else {
                    count++
                }
            }
            mPaintRectangle.color = Color.MAGENTA
            if (find != -1) {
                var left =
                    fzvaluesRect[i]?.left!! - (fzvaluesRect[find]?.left!! - fzvaluesRect[i]?.left!!) / (count + 1)
                var right =
                    fzvaluesRect[i]?.right!! - (fzvaluesRect[find]?.right!! - fzvaluesRect[i]?.right!!) / (count + 1)
                var top =
                    fzvaluesRect[i]?.top!! - (fzvaluesRect[find]?.top!! - fzvaluesRect[i]?.top!!) / (count + 1)
                var bottom =
                    fzvaluesRect[i]?.bottom!! - (fzvaluesRect[find]?.bottom!! - fzvaluesRect[i]?.bottom!!) / (count + 1)
                var newRect = Rect(left, top, right, bottom)
                canvas.drawRect(newRect, mPaintRectangle)
                fzvaluesRect[i - 1] = newRect
            }


        }

    }

}
fun fill_db_values(dbvaluesRect: Array<Rect?>, dbvaluesRect_full: Array<Rect?>, bmp2: Bitmap) {
    var count_null = 0
    for (element in dbvaluesRect) {
        if (element == null) {
            count_null++
        }
    }
    if (count_null == 0) {
        for (i in 0..dbvaluesRect_full.size step 2) {
            dbvaluesRect_full[i] = dbvaluesRect[i / 2]
        }

        val canvas = Canvas(bmp2)
        var mPaintRectangle = Paint()
        mPaintRectangle.strokeWidth = 4f
        mPaintRectangle.style = Paint.Style.STROKE
        for (i in dbvaluesRect_full.indices) {
            if (dbvaluesRect_full[i] == null) {
                mPaintRectangle.color = Color.DKGRAY
                var left =
                    (dbvaluesRect_full[i + 1]?.left!! - dbvaluesRect_full[i - 1]?.left!!) / 2 + dbvaluesRect_full[i - 1]?.left!!
                var right =
                    (dbvaluesRect_full[i + 1]?.right!! - dbvaluesRect_full[i - 1]?.right!!) / 2 + dbvaluesRect_full[i - 1]?.right!!
                var top =
                    (dbvaluesRect_full[i + 1]?.top!! - dbvaluesRect_full[i - 1]?.top!!) / 2 + dbvaluesRect_full[i - 1]?.top!!
                var bottom =
                    (dbvaluesRect_full[i + 1]?.bottom!! - dbvaluesRect_full[i - 1]?.bottom!!) / 2 + dbvaluesRect_full[i - 1]?.bottom!!
                var newRect = Rect(left, top, right, bottom)
                canvas.drawRect(newRect, mPaintRectangle)
                dbvaluesRect_full[i] = newRect
            }
        }

    }


}

fun lines_processing_hz(Hlines: Mat, fzvaluesRect : Array<Rect?>, fzvaluesRect_lines : Array<Rect?>, bmp2: Bitmap){
    val canvas = Canvas(bmp2)
    var mPaintRectangle = Paint()
    mPaintRectangle.strokeWidth = 4f
    mPaintRectangle.style = Paint.Style.STROKE
    mPaintRectangle.color = Color.WHITE
    var RectList = mutableListOf<Rect>()
    for (x in 0 until Hlines.rows()) {
        val l: DoubleArray = Hlines.get(x, 0)

        var rect: Rect = Rect(l[0].toInt(),l[1].toInt(), l[2].toInt(),l[3].toInt())
        if(abs(rect.left - rect.right) <bmp2.width*0.05){
            RectList.add(rect)

        }
    }
    mPaintRectangle.color = Color.WHITE

    //println(RectList.size)
    var k = 0
    for (rec_fz in fzvaluesRect){
        if(rec_fz == null){
            continue
        }
        var similar_rec = mutableListOf<Rect>()
        var similar_rec_x = mutableListOf<Int>()
        //println(rec_fz?.centerX())
        for (rec_line in RectList){
            if (Rect.intersects(rec_fz!!, rec_line) || rec_fz!!.contains(rec_line)){

                similar_rec.add(rec_line)
                similar_rec_x.add(rec_line.left)
            }
        }
        similar_rec_x.sort()
        if (k == 0){
            var break_point = 0
            var center = rec_fz?.centerX()
            if (rec_fz != null) {
                for (i in 1..rec_fz.width()){
                    if (similar_rec_x.contains(center!!-i)){
                        continue
                    }
                    else{
                        break_point = center!!-i
                        break
                    }
                }
                print(break_point)
            }
            var similar_rec_fix = mutableListOf<Rect>()
            for (element in similar_rec){
                if (element.left >= break_point){
                    similar_rec_fix.add(element)
                }
            }
            similar_rec = similar_rec_fix
        }
        if (similar_rec.size > 0) {
            var avg_top = 0
            var avg_bottom = 0
            var avg_left = 0
            var avg_right = 0
            for (rec in similar_rec) {
                avg_top += rec.top
                avg_bottom += rec.bottom
                avg_left += rec.left
                avg_right += rec.right

            }

            avg_top = 0
            avg_bottom = bmp2.height-1
            avg_left = avg_left / similar_rec.size
            avg_right = avg_right / similar_rec.size
            var avg_poz = (avg_left + avg_right)/2

            var r = Rect(avg_poz, avg_top, avg_poz, avg_bottom)
            fzvaluesRect_lines[k] = r
            mPaintRectangle.color = Color.WHITE
            canvas.drawRect(r, mPaintRectangle)
            k++
        }
        else{
            k++
        }
    }


}
fun lines_processing_db(Hlines: Mat, dbvaluesRect : Array<Rect?>, dbvaluesRect_lines : Array<Rect?>, bmp2: Bitmap) {
    val canvas = Canvas(bmp2)
    var mPaintRectangle = Paint()
    mPaintRectangle.strokeWidth = 4f
    mPaintRectangle.style = Paint.Style.STROKE
    mPaintRectangle.color = Color.WHITE
    var RectList = mutableListOf<Rect>()
    for (x in 0 until Hlines.rows()) {
        val l: DoubleArray = Hlines.get(x, 0)
        var rect: Rect = Rect(l[0].toInt(), l[1].toInt(), l[2].toInt(), l[3].toInt())
        if(abs(rect.left - rect.right) >bmp2.width*0.10){
            RectList.add(rect)
        }

    }
    var new_RectList = RectList.toMutableList()
    for (rec in RectList){
        if(rec.height() > 0){
            var upper_rect = Rect(rec.left,rec.top,rec.right,rec.top)
            var bottom_rect = Rect(rec.left,rec.bottom,rec.right,rec.bottom)
            var avg_rect = Rect(rec.left,(rec.bottom+rec.top)/2,rec.right,(rec.bottom+rec.top)/2)
            new_RectList.add(upper_rect)
            new_RectList.add(bottom_rect)
            new_RectList.add(avg_rect)

            new_RectList.remove(rec)
        }

    }
    var k = 0
    for (rec_db in dbvaluesRect) {
        var similar_rec = mutableListOf<Rect>()
        var similar_rec_x = mutableListOf<Int>()
        if (rec_db == null){
            k++
            continue
        }
        for (rec_line in new_RectList) {
            if (Rect.intersects(rec_db!!, rec_line) || rec_db!!.contains(rec_line)) {

                similar_rec.add(rec_line)
                if (rec_line.height()<1) {
                    similar_rec.add(rec_line)

                }
            }
        }
        if (similar_rec.size != 0) {
            var avg_top = 0
            var avg_bottom = 0
            var avg_left = 0
            var avg_right = 0
            for (rec in similar_rec) {
                avg_top += rec.top
                avg_bottom += rec.bottom
                avg_left += rec.left
                avg_right += rec.right

            }
            var temp_h = ((avg_bottom / similar_rec.size) + (avg_bottom / similar_rec.size))/2
            avg_bottom = rec_db.centerY()
            avg_top = temp_h
            avg_left = avg_left / similar_rec.size
            avg_right = avg_right / similar_rec.size
            var r = Rect(avg_left, avg_top, avg_right, avg_bottom)
            dbvaluesRect_lines[k] = r
            canvas.drawRect(r, mPaintRectangle)
            k++
        }
        else{
            //(Toast.makeText(this, "Proszę wykonać zdjęcie ponownie", Toast.LENGTH_LONG)).show()
            //break
            k++
        }
    }
}

fun lines_processing_db_lengthen(dbvaluesRect_full_lines : Array<Rect?>){
    var max = 0
    for(element in dbvaluesRect_full_lines){
        if(element?.right != null){
            if(element?.right!! > max){
                max = element?.right!!
            }
        }
    }
    for(i in 0..dbvaluesRect_full_lines.size-1){
        dbvaluesRect_full_lines[i]?.right = max
    }
}