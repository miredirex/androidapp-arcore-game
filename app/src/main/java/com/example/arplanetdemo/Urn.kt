package com.example.arplanetdemo

import android.util.Log
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3


class Urn : Node() {

    // Called once when added to a scene
    override fun onActivate() {
        super.onActivate()
        Log.d("URN", "Placed!")
    }

    // Called each frame
    override fun onUpdate(frameTime: FrameTime) {
        val delta = frameTime.deltaSeconds
        super.onUpdate(frameTime)
    }

    /* Some class functions */


}