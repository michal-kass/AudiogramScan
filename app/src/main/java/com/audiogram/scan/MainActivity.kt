package com.audiogram.scan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.provider.MediaStore.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.audiogram.scan.ml.Hearinglosstypes
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import kotlin.math.abs


class MainActivity : AppCompatActivity() {

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

    var dbvaluesRect: Array<Rect?> = arrayOfNulls(dbvalues.size)
    var fzvaluesRect: Array<Rect?> = arrayOfNulls(fzvalues.size)

    var dbvaluesRect_lines: Array<Rect?> = arrayOfNulls(dbvalues.size)
    var fzvaluesRect_lines: Array<Rect?> = arrayOfNulls(fzvalues.size)


    var dbvaluesRect_full: Array<Rect?> = arrayOfNulls(dbvalues_full.size)
    var dbvaluesRect_full_lines: Array<Rect?> = arrayOfNulls(dbvalues_full.size)

    var air_results: Array<Int?> = arrayOfNulls(fzvalues.size)
    var bone_results: Array<Int?> = arrayOfNulls(fzvalues.size)

    var audiogram_list = mutableListOf<Bitmap>()
    var audiogram_index_selection = 0


    private lateinit var firstPageView: ImageView
    private var mImageIndex = 0
    private val mTestImages = arrayOf("ag3.png")
    var recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


    private lateinit var mImageView: ImageView
    private lateinit var mImageView2: ImageView
    private lateinit var mResultView: ImageView
    private lateinit var mButtonDetect: Button
    private lateinit var mButtonDetectSymbols: Button
    private lateinit var mbutton_opencv: Button
    private lateinit var mbutton_classify: Button
    private lateinit var mbutton_switch: Button
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mBitmap: Bitmap
    private lateinit var newBitmap: Bitmap
    private lateinit var newBitmap_left: Bitmap
    private lateinit var newBitmap_right: Bitmap
    private lateinit var bmp2: Bitmap
    private lateinit var mModule: Module
    private lateinit var mModule_symbols: Module
    private lateinit var mModule_numbers: Module
    private lateinit var textv: TextView
    private lateinit var text_left_results: TextView
    private lateinit var text_right_results: TextView
    private lateinit var textv_results: TextView
    private var mImgScaleX: Float = 0.0f
    private var mImgScaleY: Float = 0f
    private var mIvScaleX: Float = 0f
    private var mIvScaleY: Float = 0f
    private var mStartX: Float = 0f
    private var mStartY: Float = 0f
    private var results_symbols = ArrayList<Result>()


    // function aims to obtain the path to the file from the application resources and save it in local memory
    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String?): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        context.assets.open(assetName!!).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while ((`is`.read(buffer).also { read = it }) != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    // the main activity of the entire application
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        try {
            mModule = LiteModuleLoader.load(
                assetFilePath(
                    applicationContext,
                    "audiogram_detection.torchscript.pt"
                )
            )

            mModule_symbols = LiteModuleLoader.load(
                assetFilePath(
                    applicationContext,
                    "symbols.torchscript.pt"
                )
            )
            mModule_numbers = LiteModuleLoader.load(
                assetFilePath(
                    applicationContext,
                    "numbers.torchscript.pt"
                )
            )
        } catch (e: IOException) {
            Log.e("Object Detection", "Error reading assets", e)
            finish()
        }

        if (OpenCVLoader.initLocal()) {
            Log.i("TAG", "OpenCV loaded successfully")
        } else {
            Log.e("TAG", "OpenCV initialization failed!")
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show()
            return
        }

        var Hlines = Mat()




        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }


        try {
            mBitmap = BitmapFactory.decodeStream(assets.open(mTestImages[mImageIndex]))
        } catch (e: IOException) {
            Log.e("Object Detection", "Error reading assets", e)
            finish()
        }

        mImageView = findViewById(R.id.imageView)
        mImageView2 = findViewById(R.id.imageView2)
        mImageView.setImageBitmap(mBitmap)
        mResultView = findViewById(R.id.resultView)
        textv = findViewById(R.id.textView2)

        text_left_results = findViewById(R.id.left_results)
        text_right_results = findViewById(R.id.right_results)


        textv_results = findViewById(R.id.text_results)
        mImageView.visibility = View.INVISIBLE
        mImageView2.visibility = View.INVISIBLE
        mResultView.visibility = View.INVISIBLE

        val buttonTest = findViewById<Button>(R.id.testButton)
        val button_opencv = findViewById<Button>(R.id.button_opencv)
        val button_classify = findViewById<Button>(R.id.classify)
        val button_switch = findViewById<Button>(R.id.switch_audiogram)

        button_opencv.visibility = View.INVISIBLE
        button_classify.visibility = View.INVISIBLE
        button_switch.visibility = View.INVISIBLE
        buttonTest.visibility = View.INVISIBLE


        button_switch.visibility = View.INVISIBLE

        buttonTest.text = "Test Image 1/3"


        button_opencv.setOnClickListener {
            var Hlines = hlines(mBitmap)
        }





        // button for switching images in debug mode
        buttonTest.setOnClickListener {
            button_switch.visibility = View.INVISIBLE
            textv.text = ""
            textv_results.text = ""
            mResultView.visibility = View.INVISIBLE
            mImageIndex = (mImageIndex + 1) % mTestImages.size
            buttonTest.text =
                String.format("Text Image %d/%d", mImageIndex + 1, mTestImages.size)
            try {
                mBitmap = BitmapFactory.decodeStream(assets.open(mTestImages[mImageIndex]))
                mImageView.setImageBitmap(mBitmap)

            } catch (e: IOException) {
                Log.e("Object Detection", "Error reading assets", e)
                finish()
            }
        }

        mButtonDetectSymbols = findViewById(R.id.symbol_scan)
        mButtonDetect = findViewById(R.id.detectButton)


        mButtonDetect.visibility = View.INVISIBLE
        mButtonDetectSymbols.visibility = View.INVISIBLE

        mProgressBar = findViewById<View>(R.id.progressBar) as ProgressBar

        // button to launch the function responsible for determining how many audiograms have been found in the image
        mButtonDetect.setOnClickListener(View.OnClickListener {
            mButtonDetect.isEnabled = false
            mProgressBar.visibility = ProgressBar.VISIBLE
            mButtonDetect.text = "Analyzed"

             audiogram_list = audiogram_det(mBitmap)
            if (audiogram_list.size > 0) {
                mBitmap = audiogram_list[audiogram_index_selection]
                Glide.with(this).load(mBitmap).into(mImageView)
            }
            Toast.makeText(this@MainActivity, "Detected " + audiogram_list.size + " audiograms", Toast.LENGTH_SHORT).show()
            mButtonDetectSymbols.visibility = View.VISIBLE
            mButtonDetect.isEnabled = false



        })
        // button for switching between audiograms from the left ear and right ear
        button_switch.setOnClickListener(View.OnClickListener {
            if (audiogram_list.size > 0) {
                if (audiogram_index_selection == 0){
                    mBitmap = audiogram_list[audiogram_index_selection+1]
                    audiogram_index_selection = audiogram_index_selection+1
                    Glide.with(this).load(mBitmap).into(mImageView)
                }
                else{
                    mBitmap = audiogram_list[audiogram_index_selection-1]
                    audiogram_index_selection = audiogram_index_selection-1
                    Glide.with(this).load(mBitmap).into(mImageView)
                }
            }


        })
        // button initialising the process of digitising detected audiograms
        mButtonDetectSymbols.setOnClickListener(View.OnClickListener {
            textv_results.text = ""
            text_left_results.text = ""
            text_right_results.text = ""

            mButtonDetect.visibility = View.INVISIBLE

            mImageView.visibility = ProgressBar.INVISIBLE
            mImageView2.visibility = ProgressBar.INVISIBLE

            mProgressBar.visibility = ProgressBar.VISIBLE

            if (audiogram_list.size == 1){
                mImageView.setImageBitmap(audiogram_list[0])
                var audiogram_result =
                    analyze_audiograms(audiogram_list[0], mImageView, text_right_results)
            }
            if (audiogram_list.size > 1) {

                mImageView.setImageBitmap(audiogram_list[0])
                var audiogram_result =
                    analyze_audiograms(audiogram_list[0], mImageView, text_right_results)


                mImageView2.setImageBitmap(audiogram_list[1])
                var audiogram_result2 =
                    analyze_audiograms(audiogram_list[1], mImageView2, text_left_results)
            }
            mProgressBar.visibility = ProgressBar.INVISIBLE
            mButtonDetectSymbols.isEnabled = false



        })


        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()


        val scanner = GmsDocumentScanning.getClient(options)
        
        // function responsible for processing and saving the hearing test result
        fun handleActivityResult(activityResult: ActivityResult) {

            val resultCode = activityResult.resultCode
            val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            if (resultCode == RESULT_OK && result != null) {
                Toast.makeText(this@MainActivity, "Photo uploaded", Toast.LENGTH_SHORT).show()
                mButtonDetect.visibility = View.VISIBLE



                val pages = result.pages
                if (pages != null && pages.isNotEmpty()) {
                    Glide.with(this).load(pages[0].imageUri).into(mImageView)
                    var bitmap: Bitmap
                    bitmap = Images.Media.getBitmap(
                        this.contentResolver,
                        pages[0].imageUri
                    )
                    mBitmap = bitmap
                }

            }
        }

        var scannerLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                handleActivityResult(result)
            }
        // button for restoring default settings after classifying audiograms
        findViewById<Button>(R.id.button2)
            .setOnClickListener {
                text_left_results.text = ""
                text_right_results.text = ""

                mButtonDetect.text = "Detect audiograms"
                mButtonDetectSymbols.text = "Analyze"



                mButtonDetectSymbols.visibility = ProgressBar.INVISIBLE
                mButtonDetectSymbols.isEnabled = true

                mButtonDetect.visibility = ProgressBar.INVISIBLE
                mButtonDetect.isEnabled = true

                button_switch.visibility = View.INVISIBLE

                textv.text = ""
                textv_results.text = ""
                scanner.getStartScanIntent(this)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener {

                    }
            }

    }
    // function responsible for detecting audiograms from photographs
    fun audiogram_det(mBitmap : Bitmap): MutableList<Bitmap> {
        var audiograms = mutableListOf<Bitmap>()

        mImgScaleX = mBitmap.width.toFloat() / PrePostProcessor.mInputWidth
        mImgScaleY = mBitmap.height.toFloat() / PrePostProcessor.mInputHeight

        mIvScaleX =
            (if (mBitmap.width > mBitmap.height) mImageView.width.toFloat() / mBitmap.width else mImageView.height.toFloat() / mBitmap.height)
        mIvScaleY =
            (if (mBitmap.height > mBitmap.width) mImageView.height.toFloat() / mBitmap.height else mImageView.width.toFloat() / mBitmap.width)

        mStartX = (mImageView.width - mIvScaleX * mBitmap.width) / 2
        mStartY = (mImageView.height - mIvScaleY * mBitmap.height) / 2

        Log.e("mImageView.w", mImageView.width.toString())
        Log.e("mImageView.h", mImageView.height.toString())

        Log.e("mStartX", mStartX.toString())
        Log.e("mStartY", mStartY.toString())

        val resizedBitmap = Bitmap.createScaledBitmap(
            mBitmap,
            PrePostProcessor.mInputWidth,
            PrePostProcessor.mInputHeight,
            true
        )
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            PrePostProcessor.NO_MEAN_RGB,
            PrePostProcessor.NO_STD_RGB
        )
        val outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple()
        val outputTensor = outputTuple[0].toTensor()
        val outputs = outputTensor.dataAsFloatArray
        Log.e("test_first_model", outputs.size.toString())
        val results = PrePostProcessor.outputsToNMSPredictions(
            6,
            outputs,
            mImgScaleX,
            mImgScaleY,
            mIvScaleX,
            mIvScaleY,
            mStartX,
            mStartY
        )

        mButtonDetect.isEnabled = true
        mButtonDetect.text = "Detected"
        mProgressBar.visibility = ProgressBar.INVISIBLE

        if (results.size == 0) {

        } else {
            if (results.size == 1){
                var rec = results[0].rect
                newBitmap = Bitmap.createBitmap(
                    mBitmap,
                    rec.left,
                    rec.top,
                    rec.width(),
                    rec.height()
                )
                audiograms.add(newBitmap)

            }
            if (results.size == 2){

                var left_rec = Rect()
                var right_rec = Rect()

                if(results[0].rect.left < results[1].rect.left){
                    left_rec = results[0].rect
                    right_rec = results[1].rect
                }
                else{
                    left_rec = results[1].rect
                    right_rec = results[0].rect
                }

                newBitmap_left = Bitmap.createBitmap(
                    mBitmap,
                    left_rec.left - 5,
                    left_rec.top,
                    left_rec.width() + (left_rec.width() * 0.10).toInt(),
                    left_rec.height()
                )
                newBitmap_right = Bitmap.createBitmap(
                    mBitmap,
                    right_rec.left - (right_rec.width() * 0.01).toInt(),
                    right_rec.top,
                    right_rec.width() + (right_rec.width() * 0.10).toInt(),
                    right_rec.height() + + (left_rec.height() * 0.05).toInt()
                )

                audiograms.add(newBitmap_left)
                audiograms.add(newBitmap_right)

                mResultView.invalidate()
                mResultView.visibility = View.VISIBLE
            }
        }
        return audiograms
    }
    // the main function of the activity performing audiograms digitalisation
    fun analyze_audiograms(mBitmap: Bitmap, mImageView : ImageView, textv : TextView): Array<String> {
        var final_class = arrayOf("","")
        var results_symbols = ArrayList<Result>()
        textv.text = ""
        var dbvaluesRect: Array<Rect?> = arrayOfNulls(dbvalues.size)
        var fzvaluesRect: Array<Rect?> = arrayOfNulls(fzvalues.size)

        var dbvaluesRect_lines: Array<Rect?> = arrayOfNulls(dbvalues.size)
        var fzvaluesRect_lines: Array<Rect?> = arrayOfNulls(fzvalues.size)


        var dbvaluesRect_full: Array<Rect?> = arrayOfNulls(dbvalues_full.size)
        var dbvaluesRect_full_lines: Array<Rect?> = arrayOfNulls(dbvalues_full.size)

        mButtonDetectSymbols.isEnabled = false
        mProgressBar.visibility = ProgressBar.VISIBLE
        mButtonDetectSymbols.text = "Analyzed"

        var mImgScaleX = mBitmap.width.toFloat() / PrePostProcessor.mInputWidth
        var mImgScaleY = mBitmap.height.toFloat() / PrePostProcessor.mInputHeight


        var mIvScaleX =
            (if (mBitmap.width > mBitmap.height) mImageView.width.toFloat() / mBitmap.width else mImageView.height.toFloat() / mBitmap.height)
        var mIvScaleY =
            (if (mBitmap.height > mBitmap.width) mImageView.height.toFloat() / mBitmap.height else mImageView.width.toFloat() / mBitmap.width)

        var mStartX = (mImageView.width - mIvScaleX * mBitmap.width) / 2
        var mStartY = (mImageView.height - mIvScaleY * mBitmap.height) / 2

        var bin_mBitmap = audiogram_bin(mBitmap)

        val bin_resizedBitmap = Bitmap.createScaledBitmap(
            bin_mBitmap,
            PrePostProcessor.mInputWidth,
            PrePostProcessor.mInputHeight,
            true
        )
        val bin_inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bin_resizedBitmap,
            PrePostProcessor.NO_MEAN_RGB,
            PrePostProcessor.NO_STD_RGB
        )


        val resizedBitmap = Bitmap.createScaledBitmap(
            mBitmap,
            PrePostProcessor.mInputWidth,
            PrePostProcessor.mInputHeight,
            true
        )
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            PrePostProcessor.NO_MEAN_RGB,
            PrePostProcessor.NO_STD_RGB
        )
        val outputTuple = mModule_symbols.forward(IValue.from(bin_inputTensor)).toTuple()
        val outputTensor = outputTuple[0].toTensor()
        val outputs = outputTensor.dataAsFloatArray
        val results = PrePostProcessor.outputsToNMSPredictions(
            5 + 8,
            outputs,
            mImgScaleX,
            mImgScaleY,
            mIvScaleX,
            mIvScaleY,
            mStartX,
            mStartY
        )

        val outputTuple_numbers = mModule_numbers.forward(IValue.from(inputTensor)).toTuple()
        val outputTensor_numbers = outputTuple_numbers[0].toTensor()
        val outputs_numbers = outputTensor_numbers.dataAsFloatArray
        val results_numbers = PrePostProcessor.outputsToNMSPredictions(
            5 + 21,
            outputs_numbers,
            mImgScaleX,
            mImgScaleY,
            mIvScaleX,
            mIvScaleY,
            mStartX,
            mStartY
        )


        mButtonDetectSymbols.isEnabled = true
        mProgressBar.visibility = ProgressBar.INVISIBLE

        bmp2 = mBitmap.copy(mBitmap.config, true)
        results_symbols = results

        val canvas = Canvas(bmp2)
        var mPaintRectangle = Paint()
        mPaintRectangle.strokeWidth = 3f
        mPaintRectangle.style = Paint.Style.STROKE
        mPaintRectangle.color = Color.WHITE

        val image = InputImage.fromBitmap(mBitmap, 0)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val resultText = visionText.text
                for (block in visionText.textBlocks) {
                    val blockText = block.text
                    val blockCornerPoints = block.cornerPoints
                    val blockFrame = block.boundingBox
                    for (line in block.lines) {
                        val lineText = line.text
                        val lineCornerPoints = line.cornerPoints
                        val lineFrame = line.boundingBox
                        for (element in line.elements) {
                            val elementText = element.text
                            if (find(dbvalues, elementText) == true) {
                                val index = dbvalues.indexOf(elementText)
                                if ( dbvaluesRect[index] == null) {
                                    dbvaluesRect[index] = element.boundingBox
                                    if (element.boundingBox != null) {
                                        canvas.drawRect(element.boundingBox!!, mPaintRectangle)
                                    }
                                }

                            }
                            if (find(fzvalues, elementText) == true) {
                                val index = fzvalues.indexOf(elementText)
                                if (fzvaluesRect[index] == null) {
                                    fzvaluesRect[index] = element.boundingBox
                                    if (element.boundingBox != null) {
                                        canvas.drawRect(element.boundingBox!!, mPaintRectangle)
                                    }
                                }

                            }

                        }
                    }
                }

                for (i in results.indices) {
                    if (results[i].classIndex == 0){
                        mPaintRectangle.color = Color.RED
                        canvas.drawRect(results[i].rect, mPaintRectangle)
                    }
                    else{
                        mPaintRectangle.color = Color.WHITE
                        canvas.drawRect(results[i].rect, mPaintRectangle)
                    }
                    mPaintRectangle.color = Color.WHITE

                }

                for (i in results_numbers.indices) {
                    mPaintRectangle.color = Color.WHITE
                    var value = PrePostProcessor.mClasses_numbers[results_numbers[i].classIndex]
                    if (find(dbvalues, value) == true){
                        val index = dbvalues.indexOf(value)
                        dbvaluesRect[index] = results_numbers[i].rect
                    }
                    if (find(fzvalues, value) == true){
                        val index = fzvalues.indexOf(value)
                        if (fzvaluesRect[index] == null) {
                            canvas.drawRect(results_numbers[i].rect, mPaintRectangle)
                            fzvaluesRect[index] = results_numbers[i].rect
                        }
                    }
                }

                var Hlines = hlines(mBitmap)

                filling_gaps_db(dbvaluesRect,bmp2)

                filling_gaps_fz(fzvaluesRect,bmp2)

                fill_db_values(dbvaluesRect, dbvaluesRect_full,bmp2)
                lines_processing_hz(Hlines,fzvaluesRect,fzvaluesRect_lines, bmp2)
                lines_processing_db(Hlines,dbvaluesRect ,dbvaluesRect_lines, bmp2)

                filling_gaps_fz(fzvaluesRect_lines,bmp2)

                filling_gaps_db(dbvaluesRect_lines,bmp2)


                fill_db_values(dbvaluesRect_lines, dbvaluesRect_full_lines, bmp2)
                lines_processing_db_lengthen(dbvaluesRect_full_lines)


                Glide.with(this).load(bmp2).into(mImageView)

                try {
                     final_class =
                        classfiy(results_symbols, fzvaluesRect_lines, dbvaluesRect_full_lines)

                    println(final_class)
                    if (final_class[0] != ""){
                    textv.text = final_class[1].replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    } + " Ear:" + "\n" + final_class[0]
                        }
                    else {
                        textv.text = final_class[1] + " ear:" + "Could not process an audiogram in the photograph. Please take the photograph in good lighting conditions and make sure that the entire audiogram is visible."
                    }
                }catch(exception: Exception){
                    (Toast.makeText(this, "Classification failed", Toast.LENGTH_LONG)).show()
                    textv.text = final_class[1] + " ear:" + "Could not process an audiogram in the photograph. Please take the photograph in good lighting conditions and make sure that the entire audiogram is visible."

                }


                }
            .addOnFailureListener { e ->
            }







        mPaintRectangle.color = Color.WHITE


        var fz_lines = fzvaluesRect_lines
        var db_lines = dbvaluesRect_full_lines
        var symbols_array = results_symbols
        return final_class


    }
    // function responsible for classifying audiograms
    fun classfiy(results_symbols : ArrayList<Result>, fzvaluesRect_lines : Array<Rect?>, dbvaluesRect_full_lines : Array<Rect?>): Array<String> {
        var air_results: Array<Int?> = arrayOfNulls(fzvalues.size)
        var bone_results: Array<Int?> = arrayOfNulls(fzvalues.size)
        val canvas = Canvas(bmp2)
        var mPaintRectangle = Paint()
        mPaintRectangle.strokeWidth = 8f
        mPaintRectangle.style = Paint.Style.STROKE
        mPaintRectangle.color = Color.BLUE
        var air_symbols = arrayOf("circle","cross","square","triangle")
        var symbos_count = arrayOf(0,0,0,0,0,0,0,0)
        var bone_symbols = arrayOf("close_bracket","greater_than","less_than","open_bracket")
        var left = 0
        var right = 0
        var side = ""
        for(i in results_symbols){
            symbos_count[i.classIndex] ++
        }
        for(i in 0..symbos_count.size-1){
            if(i == 0 || i==4 || i==5 || i ==7){
                right += symbos_count[i]
            }
            else{
                left += symbos_count[i]
            }
        }
        if (left > right){
            side = "left"
        }
        else{
            side = "right"
        }
        var avg_dis = 0
        var c = 0
        for (i in 1..fzvaluesRect_lines.size-2){
            if(fzvaluesRect_lines[i-1] != null && fzvaluesRect_lines[i+1] != null) {
                avg_dis =
                    +abs(fzvaluesRect_lines[i - 1]?.left!! - fzvaluesRect_lines[i + 1]?.left!!)
                c++
            }
        }
        avg_dis = avg_dis / c

        var fz_index = 0
        for(line in fzvaluesRect_lines){
            for (symbol in results_symbols){

                var db_value_list : MutableList<Rect> = arrayListOf()
                if((Rect.intersects(line!!, symbol.rect) || symbol.rect.contains(line)) && (PrePostProcessor.mClasses_symbols[symbol.classIndex] == "circle" || PrePostProcessor.mClasses_symbols[symbol.classIndex] == "cross" || PrePostProcessor.mClasses_symbols[symbol.classIndex] == "square" || PrePostProcessor.mClasses_symbols[symbol.classIndex] == "triangle")){
                    var finded_rec = symbol.rect


                    var db_index = 0
                    for (db_value in dbvaluesRect_full_lines){
                        if(db_value != null && finded_rec != null){
                            if (Rect.intersects(db_value,finded_rec) || finded_rec.contains(
                                    db_value
                                )){
                                db_value_list.add(db_value)

                            }
                        }
                        db_index ++
                    }

                    if(db_value_list.size == 1){
                        air_results[fz_index] = dbvalues_full[dbvaluesRect_full_lines.indexOf(db_value_list[0])].toInt()
                    }
                    else if(db_value_list.size > 1){
                        var db_temp_index = 0
                        var centerY = finded_rec.centerY()
                        var distance = abs(centerY-db_value_list[0].centerY())
                        for(ele in db_value_list){
                            if (distance > abs(centerY-ele.centerY())){
                                distance = abs(centerY-ele.centerY())
                                db_temp_index = db_value_list.indexOf(ele)

                            }
                        }
                        air_results[fz_index] = dbvalues_full[dbvaluesRect_full_lines.indexOf(db_value_list[db_temp_index])].toInt()


                    }

                }

            }
            fz_index ++
        }
        fz_index = 0
        for(line in fzvaluesRect_lines){
            if(fzvalues[fzvaluesRect_lines.indexOf(line)] == "125" || fzvalues[fzvaluesRect_lines.indexOf(line)] == "8k") {
                bone_results[fzvaluesRect_lines.indexOf(line)] = -100
                fz_index ++
                continue
            }
            for (symbol in results_symbols){
                var db_value_list : MutableList<Rect> = arrayListOf()
                if((Rect.intersects(line!!, symbol.rect) || symbol.rect.contains(line))  && (PrePostProcessor.mClasses_symbols[symbol.classIndex] == "close_bracket" || PrePostProcessor.mClasses_symbols[symbol.classIndex] == "greater_than" || PrePostProcessor.mClasses_symbols[symbol.classIndex] == "less_than" || PrePostProcessor.mClasses_symbols[symbol.classIndex] == "open_bracket")){
                    var finded_rec = symbol.rect
                    var db_index = 0
                    for (db_value in dbvaluesRect_full_lines){
                        if (Rect.intersects(db_value!!,finded_rec) || finded_rec.contains(db_value)){
                            db_value_list.add(db_value)
                        }
                        db_index ++
                    }
                    db_index++
                    if(db_value_list.size == 1){
                        bone_results[fz_index] = dbvalues_full[dbvaluesRect_full_lines.indexOf(db_value_list[0])].toInt()
                    }
                    else if(db_value_list.size > 1){
                        var db_temp_index = 0
                        var centerY = finded_rec.centerY()
                        var distance = abs(centerY-db_value_list[0].centerY())
                        for(ele in db_value_list){
                            if (distance > abs(centerY-ele.centerY())){
                                distance = abs(centerY-ele.centerY())
                                db_temp_index = db_value_list.indexOf(ele)

                            }
                        }
                        bone_results[fz_index] = dbvalues_full[dbvaluesRect_full_lines.indexOf(db_value_list[db_temp_index])].toInt()


                    }
                }

            }
            fz_index++
        }
        for(i in 0..bone_results.size-1){
            if (bone_results[i] == null){
                var temp_line = fzvaluesRect_lines[i]
                for(d in 1..avg_dis/2){
                    if(side=="left" && symbos_count[5] == 0) {
                        temp_line?.left = temp_line?.left!! +  1
                        temp_line.right = temp_line.right!! + 1
                    }
                    else if(side=="left" && symbos_count[5] != 0){
                        temp_line?.left = temp_line?.left!! + 1
                        temp_line.right = temp_line.right!! + 1
                    }
                    else if(side=="right" ){
                        temp_line?.left = temp_line?.left!! - 1
                        temp_line.right = temp_line.right!! - 1
                    }
                    for (symbol in results_symbols){
                        if((Rect.intersects(temp_line!!, symbol.rect) || symbol.rect.contains(
                                temp_line
                            ))  && (PrePostProcessor.mClasses_symbols[symbol.classIndex] == "close_bracket" || PrePostProcessor.mClasses_symbols[symbol.classIndex] == "greater_than" || PrePostProcessor.mClasses_symbols[symbol.classIndex] == "less_than" || PrePostProcessor.mClasses_symbols[symbol.classIndex] == "open_bracket")){
                            var finded_rec = symbol.rect

                            for (db_value in dbvaluesRect_full_lines){
                                if (Rect.intersects(db_value!!,finded_rec) || finded_rec.contains(
                                        db_value
                                    )){


                                    bone_results[fzvaluesRect_lines.indexOf(fzvaluesRect_lines[i])] = dbvalues_full[dbvaluesRect_full_lines.indexOf(db_value)].toInt()
                                }
                            }

                        }

                    }
                    if(bone_results[i] != null){
                        break
                    }

                }
            }
        }


        println(fzvalues.joinToString(", "))
        println(air_results.joinToString(", "))
        println(fzvalues.joinToString(", "))
        println(bone_results.joinToString(", "))
        var byteBuffer3 : ByteBuffer = ByteBuffer.allocateDirect(56)
        var names_class = arrayOf("Normal hearing","Mixed Hearing Loss","Conductive Hearing Loss","Sensorineural Hearing Loss")
        var temp_i = 0
        try {
            byteBuffer3.order(ByteOrder.nativeOrder())
            for (i in 0..air_results.size - 1) {
                byteBuffer3.putFloat(air_results[i]!!.toFloat())
                byteBuffer3.putFloat(bone_results[i]!!.toFloat())
            }
            val model = Hearinglosstypes.newInstance(this)
            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 7, 2), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer3)
            val outputs = model.process(inputFeature0)
            val outputFeature: FloatArray = outputs.outputFeature0AsTensorBuffer.floatArray
            var temp_max = outputFeature[0]

            for(i in 0..3){
                if (outputFeature[i] > temp_max){
                    temp_max = outputFeature[i]
                    temp_i = i
                }
            }
            model.close()

        }catch(e : Exception){
            (Toast.makeText(this, "Klasyfikacja nieudana", Toast.LENGTH_LONG)).show()
            return arrayOf("", side)
        }
        return arrayOf(names_class[temp_i], side)

    }







}


