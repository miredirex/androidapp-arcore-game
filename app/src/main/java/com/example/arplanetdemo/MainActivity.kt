package com.example.arplanetdemo

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize scene manager and create a new scene
        val sceneManager = SceneManagerAR()
        sceneManager.newScene(this)

        // Set the onclick listener for our button. Add planet on tap.

        floatingActionButton.setOnClickListener { sceneManager.addObject(Urn(), Uri.parse("urn_gltf.sfb")) }
        someButton.setOnClickListener { sceneManager.initiateBallAndThrow() }
    }

    override fun onPause() {
        super.onPause()
        sceneform_sceneview.pause()
    }

    override fun onResume() {
        super.onResume()
        sceneform_sceneview.resume()
    }
}
