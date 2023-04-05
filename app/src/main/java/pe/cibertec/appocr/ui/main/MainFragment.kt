package pe.cibertec.appocr.ui.main

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.codelab.mlkit.databinding.FragmentMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import pe.cibertec.appocr.tools.FaceContourGraphic
import pe.cibertec.appocr.tools.GraphicOverlay
import pe.cibertec.appocr.tools.TextGraphic
import java.io.IOException
import java.io.InputStream
import java.util.*

class MainFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var mImageView: ImageView? = null
    private var mTextButton: Button? = null
    private var mFaceButton: Button? = null
    private var mSelectedImage: Bitmap? = null
    private var mGraphicOverlay: GraphicOverlay? = null
    private var _binding: FragmentMainBinding? = null
    val binding get() = _binding!!
    // Max width (portrait mode)
    private var mImageMaxWidth: Int? = null

    // Max height (portrait mode)
    private var mImageMaxHeight: Int? = null

    /**
     * Number of results to show in the UI.
     */
    private val RESULTS_TO_SHOW = 3

    /**
     * Dimensions of inputs.
     */
    private val DIM_IMG_SIZE_X = 224
    private val DIM_IMG_SIZE_Y = 224

    private val sortedLabels = PriorityQueue<Map.Entry<String, Float>>(
        RESULTS_TO_SHOW,
        object : Comparator<Map.Entry<String?, Float?>?> {
            override fun compare(
                o1: Map.Entry<String?, Float?>?,
                o2: Map.Entry<String?, Float?>?
            ): Int {
                return o1!!.value!!.compareTo(o2!!.value!!)
            }
        })

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        mTextButton!!.setOnClickListener { runTextRecognition() }
        mFaceButton!!.setOnClickListener { runFaceContourDetection() }
        val items = arrayOf("Test Image 1 (Text)", "Test Image 2 (Face)")
        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), R.layout.simple_spinner_dropdown_item, items)


    }
    private fun runFaceContourDetection() {
        val image = InputImage.fromBitmap(mSelectedImage!!, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        mFaceButton!!.isEnabled = false
        val detector = FaceDetection.getClient(options)
        detector.process(image)
            .addOnSuccessListener { faces ->
                mFaceButton!!.isEnabled = true
                processFaceContourDetectionResult(faces)
            }
            .addOnFailureListener { e -> // Task failed with an exception
                mFaceButton!!.isEnabled = true
                e.printStackTrace()
            }
    }
    private fun processFaceContourDetectionResult(faces: List<Face>) {
        // Task completed successfully
        if (faces.size == 0) {
            showToast("No face found")
            return
        }
        mGraphicOverlay!!.clear()
        for (i in faces.indices) {
            val face = faces[i]
            val faceGraphic = FaceContourGraphic(mGraphicOverlay)
            mGraphicOverlay!!.add(faceGraphic)
            faceGraphic.updateFace(face)
        }
    }
    private fun runTextRecognition() {
        val image = InputImage.fromBitmap(mSelectedImage!!, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        mTextButton!!.setEnabled(false)
        recognizer.process(image)
            .addOnSuccessListener { texts ->
                mTextButton!!.setEnabled(true)
                processTextRecognitionResult(texts)
            }
            .addOnFailureListener { e -> // Task failed with an exception
                mTextButton!!.setEnabled(true)
                e.printStackTrace()
            }
    }

    override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
        mGraphicOverlay!!.clear()
        when (position) {
            0 -> mSelectedImage =
                 getBitmapFromAsset(requireContext(), "Please_walk_on_the_grass.jpg")
            1 ->                 // Whatever you want to happen when the thrid item gets selected
                mSelectedImage = getBitmapFromAsset(requireContext(), "grace_hopper.jpg")
        }
        if (mSelectedImage != null) {
            // Get the dimensions of the View
            val targetedSize = getTargetedWidthHeight()
            val targetWidth = targetedSize!!.first
            val maxHeight = targetedSize.second

            // Determine how much to scale down the image
            val scaleFactor = Math.max(
                mSelectedImage!!.getWidth().toFloat() / targetWidth.toFloat(),
                mSelectedImage!!.getHeight().toFloat() / maxHeight.toFloat()
            )
            val resizedBitmap = Bitmap.createScaledBitmap(
                mSelectedImage!!,
                (mSelectedImage!!.getWidth() / scaleFactor).toInt(),
                (mSelectedImage!!.getHeight() / scaleFactor).toInt(),
                true
            )
            mImageView!!.setImageBitmap(resizedBitmap)
            mSelectedImage = resizedBitmap
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    private fun getTargetedWidthHeight(): Pair<Int, Int>? {
        val targetWidth: Int
        val targetHeight: Int
        val maxWidthForPortraitMode = getImageMaxWidth()!!
        val maxHeightForPortraitMode = getImageMaxHeight()!!
        targetWidth = maxWidthForPortraitMode
        targetHeight = maxHeightForPortraitMode
        return Pair(targetWidth, targetHeight)
    }
    // Functions for loading images from app assets.
    private fun getImageMaxWidth(): Int? {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = mImageView!!.width
        }
        return mImageMaxWidth
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private fun getImageMaxHeight(): Int? {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxHeight = mImageView!!.height
        }
        return mImageMaxHeight
    }
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun getBitmapFromAsset(context: Context, filePath: String?): Bitmap? {
        val assetManager = context.assets
        val `is`: InputStream
        var bitmap: Bitmap? = null
        try {
            `is` = assetManager.open(filePath!!)
            bitmap = BitmapFactory.decodeStream(`is`)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmap
    }
    private fun processTextRecognitionResult(texts: Text) {
        val blocks: List<Text.TextBlock> = texts.getTextBlocks()
        if (blocks.size == 0) {
          //  Toast.makeText(this,"No text found",Toast.LENGTH_LONG).
            //    showToast("No text found")

        }
        mGraphicOverlay?.clear()
        for (i in blocks.indices) {
            val lines: List<Text.Line> = blocks[i].getLines()
            for (j in lines.indices) {
                val elements: List<Text.Element> = lines[j].getElements()
                for (k in elements.indices) {
                    val textGraphic: GraphicOverlay.Graphic = TextGraphic(mGraphicOverlay, elements[k])
                    mGraphicOverlay?.add(textGraphic)
                }
            }
        }
    }
}