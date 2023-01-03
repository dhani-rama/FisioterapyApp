package id.research.fisioterapyfirstapp.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.mediapipe.components.*
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import com.google.protobuf.InvalidProtocolBufferException
import id.research.fisioterapyfirstapp.databinding.ActivityFirstDetectPoseBinding
import id.research.fisioterapyfirstapp.model.KeypointEntity
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


class FirstDetectPoseActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Tes"
        private const val HASIL = "hasil"
        private const val KEYEX = "KeyEx"
        private const val TOTAL = "Total"
        private const val BINARY_GRAPH_NAME = "pose_tracking_gpu.binarypb"
        private const val INPUT_VIDEO_STREAM_NAME = "input_video"
        private const val OUTPUT_VIDEO_STREAM_NAME = "output_video"
        private const val OUTPUT_LANDMARKS_STREAM_NAME = "pose_landmarks"

        // private static final CameraHelper.CameraFacing CAMERA_FACING = CameraHelper.CameraFacing.FRONT;
        private val CAMERA_FACING = CameraHelper.CameraFacing.FRONT

        // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
        // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
        // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
        // corner, whereas MediaPipe in general assumes the image origin is at top-left.
        private const val FLIP_FRAMES_VERTICALLY = true

        init {
            // Load all native libraries needed by the app.
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }

        private fun getLandmarksDebugString(landmarks: LandmarkProto.NormalizedLandmarkList): String {
            var landmarksString = ""
            for ((landmarkIndex, landmark) in landmarks.landmarkList.withIndex()) {
                landmarksString += """Landmark[$landmarkIndex]: (${landmark.x}, ${landmark.y}, ${landmark.z})"""
            }
            return landmarksString
        }
    }

    private lateinit var mFirstDetectPoseBinding: ActivityFirstDetectPoseBinding
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mKeypoint: ArrayList<KeypointEntity>

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private var previewFrameTexture: SurfaceTexture? = null

    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private var previewDisplayView: SurfaceView? = null

    // Creates and manages an {@link EGLContext}.
    private var eglManager: EglManager? = null

    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private var processor: FrameProcessor? = null

    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private var converter: ExternalTextureConverter? = null

    // ApplicationInfo for retrieving metadata defined in the manifest.
    private var mInfo: ApplicationInfo? = null

    // Handles camera access via the {@link CameraX} Jetpack support library.
    private var cameraHelper: CameraXPreviewHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFirstDetectPoseBinding = ActivityFirstDetectPoseBinding.inflate(layoutInflater)
        setContentView(mFirstDetectPoseBinding.root)

        try {
            mInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Cannot find application info: $e")
        }

        previewDisplayView = SurfaceView(this@FirstDetectPoseActivity)

        mKeypoint = arrayListOf()

        setupPreviewDisplayView()

        displayKeypointImage()

        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        AndroidAssetUtil.initializeNativeAssetManager(this)
        eglManager = EglManager(null)
        processor = FrameProcessor(
            this,
            eglManager!!.nativeContext,
            BINARY_GRAPH_NAME,
            INPUT_VIDEO_STREAM_NAME,
            OUTPUT_VIDEO_STREAM_NAME
        )
        processor!!.videoSurfaceOutput.setFlipY(FLIP_FRAMES_VERTICALLY)
        PermissionHelper.checkAndRequestCameraPermissions(this)
        val packetCreator = processor!!.packetCreator
        val inputSidePackets: Map<String, Packet> = HashMap()
        //        inputSidePackets.put(INPUT_NUM_HANDS_SIDE_PACKET_NAME, packetCreator.createInt32(NUM_HANDS));
//        processor.setInputSidePackets(inputSidePackets);

        // To show verbose logging, run:
        // adb shell setprop log.tag.MainActivity VERBOSE
//        if (Log.isLoggable(TAG, Log.VERBOSE)) {
        processor!!.addPacketCallback(
            OUTPUT_LANDMARKS_STREAM_NAME
        ) { packet: Packet ->
            Log.v(TAG, "Received human body pose landmarks packet.")
            Log.v(TAG, packet.toString())
            val landmarksRaw = PacketGetter.getProtoBytes(packet)
            try {
                val landmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw)
                if (landmarks == null) {
                    Log.v(TAG, "[TS:" + packet.timestamp + "] No pose landmarks.")
                    return@addPacketCallback
                }
                // Note: If eye_presence is false, these landmarks are useless.
                Log.v(
                    TAG,
                    "[TS:" + packet.timestamp + "] #Landmarks for human body pose: " + landmarks.landmarkCount
                )
                Log.v(TAG, getLandmarksDebugString(landmarks))

                var valueA = 0.0
                var valueB = 0.0
                var sigmaAB = 0.0
                val key = mKeypoint
                var xSum = 0.0
                var ySum = 0.0



                for ((landmarksIndex, landmark) in landmarks.landmarkList.withIndex()) {
//                    val vectorA = ("${key[landmarksIndex].x}, ${key[landmarksIndex].y}")
//                    val vectorB = ("${landmark.x}, ${landmark.y}")
//
//                    Log.i(KEYEX, "VectorA[$landmarksIndex] = $vectorA")
//                    Log.i(KEYEX, "VectorB[$landmarksIndex] = $vectorB")
//
//                    sigmaAB += BigDecimal()*vectorB[landmarksIndex]

                    //cosine

//                    val vectorA = arrayListOf(key[landmarksIndex].x, key[landmarksIndex].y)
//                    val vectorB = arrayListOf(landmark.x.toDouble(), landmark.y.toDouble())
//
//                    cosineSimilarity(vectorA, vectorB)


//                    sigmaAB += vectorA[landmarksIndex]*vectorB[landmarksIndex]


////                    val roundOff = (sigmaAB * 1000.0).roundToInt() / 1000.0
//
//                    valueA += vectorA[landmarksIndex].pow(2)
//                    valueB += vectorB[landmarksIndex].pow(2)
////
//                    val result = sigmaAB / sqrt(valueA)* sqrt(valueB)
//
//                    Log.i(HASIL, "Hasil[$landmarksIndex]= $sigmaAB")


                    val xValue = landmark.x - key[landmarksIndex].x
                    val yValue = landmark.y - key[landmarksIndex].y

                    xSum += xValue.pow(2.0)
                    ySum += yValue.pow(2.0)

                    val valueX = xSum / 33.00
                    val valueY = ySum / 33.00

                    val rmseX = sqrt(valueX)
                    val rmseY = sqrt(valueY)

                    val rmseValue = ((rmseX + rmseY) / 2) * 10

                    Log.i(TOTAL, "Total = $rmseValue")
//                        Log.i(HASIL, "e[$landmarksIndex][$keyIndex] = $xValue")
                    Log.i(KEYEX, "Landmark[$landmarksIndex] = $xValue")


                    val roundOff = (rmseValue * 1000.0).roundToInt() / 1000.0
                    mDatabaseReference = FirebaseDatabase.getInstance().getReference("rmse")
                    mDatabaseReference.child("rmseValue").setValue(rmseValue)

                }
            } catch (e: InvalidProtocolBufferException) {
                Log.e(TAG, "Couldn't Exception received - $e")
                return@addPacketCallback
            }
        }

        displayCategory()
    }

    private fun cosineSimilarity(vectorA: ArrayList<Double>, vectorB: ArrayList<Double>) {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i].pow(2.0)
            normB += vectorB[i].pow(2.0)
        }

        val result = (dotProduct / (sqrt(normA) * sqrt(normB))) * 100
        Log.i(HASIL, "Hasil cosinus = $result")

        mDatabaseReference = FirebaseDatabase.getInstance().getReference("cosinussimilarity")
        mDatabaseReference.child("cosinusvalue").setValue(result)

//        displayCategory()
    }

    private fun displayCategory() {

        mDatabaseReference = FirebaseDatabase.getInstance().getReference("rmse")
        mDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (item in snapshot.children) {
                        val cosinusValue = item.getValue(Double::class.java)!!
                        //mKeypoint.add(rmse!!)

                        with(mFirstDetectPoseBinding) {
                            layoutAccurate.visibility = View.GONE
                            layoutNoAccurate.visibility = View.GONE
                            tvValueCosineGreen.visibility = View.GONE
                        }

                        if (cosinusValue <= 1.2) {
                            mFirstDetectPoseBinding.layoutAccurate.visibility = View.VISIBLE
                            mFirstDetectPoseBinding.tvValueCosineGreen.visibility = View.VISIBLE
                            mFirstDetectPoseBinding.tvValueCosineGreen.text =
                                cosinusValue.toString()
                        } else if (cosinusValue > 1.2) {
                            mFirstDetectPoseBinding.layoutNoAccurate.visibility = View.VISIBLE
                            mFirstDetectPoseBinding.tvValueCosineRed.visibility = View.VISIBLE
                            mFirstDetectPoseBinding.tvValueCosineRed.text = cosinusValue.toString()
                        }


                        //mFirstDetectPoseBinding.tvValueCosine.text = cosinusValue.toString()
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }

    private fun displayKeypointImage() {
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("firstkeypoint")
        mDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (item in snapshot.children) {
                        val collectionKeypoint = item.getValue(KeypointEntity::class.java)
                        mKeypoint.add(collectionKeypoint!!)
                    }

                    var keypointString: String
                    for ((keypointIndex, key) in mKeypoint.withIndex()) {
                        keypointString = "Keypoint[$keypointIndex]: (${key.x}, ${key.y}, ${key.z})"
                        Log.d(KEYEX, keypointString)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FirstDetectPoseActivity, error.message, Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        converter = ExternalTextureConverter(
            eglManager!!.context, 2
        )
        converter!!.setFlipY(FLIP_FRAMES_VERTICALLY)
        converter!!.setConsumer(processor)
        if (PermissionHelper.cameraPermissionsGranted(this)) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        converter!!.close()

        // Hide preview display until we re-open the camera again.
        previewDisplayView!!.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun onCameraStarted(surfaceTexture: SurfaceTexture?) {
        previewFrameTexture = surfaceTexture
        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        previewDisplayView!!.visibility = View.VISIBLE
    }

    private fun cameraTargetResolution(): Size? {
        return null // No preference and let the camera (helper) decide.
    }

    private fun startCamera() {
        cameraHelper = CameraXPreviewHelper()
        cameraHelper!!.setOnCameraStartedListener { surfaceTexture: SurfaceTexture? ->
            onCameraStarted(
                surfaceTexture
            )
        }
        val cameraFacing = CAMERA_FACING
        cameraHelper!!.startCamera(
            this, cameraFacing,  /*unusedSurfaceTexture=*/null, cameraTargetResolution()
        )
    }

    private fun computeViewSize(width: Int, height: Int): Size {
        return Size(width, height)
    }


    fun onPreviewDisplaySurfaceChanged(
        holder: SurfaceHolder?, format: Int, width: Int, height: Int
    ) {
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        val viewSize = computeViewSize(width, height)
        val displaySize = cameraHelper!!.computeDisplaySizeFromViewSize(viewSize)
        val isCameraRotated = cameraHelper!!.isCameraRotated

        // Connect the converter to the camera-preview frames as its input (via
        // previewFrameTexture), and configure the output width and height as the computed
        // display size.
        converter!!.setSurfaceTextureAndAttachToGLContext(
            previewFrameTexture,
            if (isCameraRotated) displaySize.height else displaySize.width,
            if (isCameraRotated) displaySize.width else displaySize.height
        )
    }


    private fun setupPreviewDisplayView() {
        previewDisplayView!!.visibility = View.GONE
        //val viewGroup = findViewById<ViewGroup>(R.id.preview_display_layout)
        mFirstDetectPoseBinding.previewDisplayLayout.addView(previewDisplayView)
        previewDisplayView!!.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                processor!!.videoSurfaceOutput.setSurface(holder.surface)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int, width: Int, height: Int
            ) {
                onPreviewDisplaySurfaceChanged(holder, format, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                processor!!.videoSurfaceOutput.setSurface(null)
            }
        })
    }
}