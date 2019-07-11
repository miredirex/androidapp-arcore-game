package com.example.arplanetdemo

import android.util.Log
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3


class Planet : Node() {

    private val speedRotateMultiplier = 12f

    // Called once when added to a scene
    override fun onActivate() {
        super.onActivate()
        Log.d("PLANET", "Spawned")
    }

    // Called each frame
    override fun onUpdate(frameTime: FrameTime) {
        super.onUpdate(frameTime)
        rotate(frameTime.deltaSeconds)
    }

    /* Some class functions */

    private fun rotate(delta: Float) {
        this.localRotation = Quaternion.multiply(this.localRotation, Quaternion.axisAngle(Vector3.up(), delta * speedRotateMultiplier))
    }

}
