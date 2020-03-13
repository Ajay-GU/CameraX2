package com.example.camerax2

import android.graphics.Matrix
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.CameraX
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.Navigation
import java.io.File
import java.util.concurrent.Executors


class Camera : Fragment() {


    private lateinit var viewFinder: TextureView
    private   lateinit var captureButton: ImageButton
    private lateinit var container:ConstraintLayout


    companion object {
        fun newInstance() = Camera()
    }

    private lateinit var viewModel: CameraViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CameraViewModel::class.java)
        // TODO: Use the ViewModel
        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)
        captureButton = container.findViewById(R.id.capture_button)


        // user could have removed them while the app was in paused state.
        if (!Permission_Fragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
               R.id.action_camera_to_permission_Fragment
            )
        }
        else{
            viewFinder.post { startCamera() }

        }





    }




    private val executor = Executors.newSingleThreadExecutor()

    private fun startCamera() {


        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()



        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }
        Log.i("Debug","2")

        // viewFinder.surfaceTexture = it.surfaceTexture
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .build()
        Log.i("Debug","3")

        val imageCapture = ImageCapture(imageCaptureConfig)
        container.findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
            val file = File(
                Environment.getDataDirectory(),
                "${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(file, executor,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(

                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,

                        exc: Throwable?
                    ) {
                        Log.i("Debug","4")

                        val msg = "Photo capture failed: $message"
                        Log.e("CameraXApp", msg, exc)
                        viewFinder.post {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }


                    override fun onImageSaved(file: File) {
                        Log.i("Debug","5")

                        val msg = "Photo capture succeeded: ${file.absolutePath}"
                        Log.d("CameraXApp", msg)
                        viewFinder.post {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(activity, preview, imageCapture)

    }
    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }






    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the

    }

}
