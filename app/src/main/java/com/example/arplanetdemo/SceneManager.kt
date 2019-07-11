package com.example.arplanetdemo

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.Toast
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.android.synthetic.main.activity_main.*

@Suppress("SpellCheckingInspection")

class SceneManager {
    private lateinit var context: Activity
    private lateinit var scene: Scene
    lateinit var physicsManager: PhysicsManager

    /* Array containing all user-spawned objects on a scene */
    private var sceneObjects: Array<Node> = arrayOf()

    /* Create a new scene in activity */
    fun newScene(context: Context) {
        this.context = context as MainActivity
        this.scene = context.sceneform_sceneview.scene

        // Hide fragment ar element cuz we're in non-AR mode
        context.sceneform_fragment.view?.visibility = View.GONE

        scene.view.setBackgroundColor(Color.CYAN)
        scene.camera.apply {
            worldPosition = Vector3(0f, 0.2f, 0f)
            localRotation = Quaternion.axisAngle(Vector3.left(), 30f)
        }

        // Init physics
        physicsManager = PhysicsManager()
        physicsManager.init()

        scene.addOnUpdateListener { frameTime ->
            /* Initiate onUpdate methods for all user-created objects in scene */
            for (sceneObj in sceneObjects) {
                sceneObj.onUpdate(frameTime)
            }
            physicsManager.updatePhysicsObjects()
            physicsManager.stepPhysicsWorld(frameTime.deltaSeconds)

        }
    }

    /**
     * load the 3D model in the space
     * @param parse URI of the model, imported using Sceneform plugin
     */
    fun addObject(node: Node, parse: Uri) {
        ModelRenderable.builder()
            .setSource(context, parse)
            .build()
            .thenAccept {
                addNodeToScene(node, it)
            }
            .exceptionally {
                Toast.makeText(context, "Error", Toast.LENGTH_LONG).show()
                return@exceptionally null
            }

    }

    /**
     * Throws a box from the camera eye
     */
    fun createPhysicsBoxFromEye() {
        val mainCamera = scene.camera
        val lookat = mainCamera.forward
        val camPos = mainCamera.worldPosition
        val size = Vector3(2f, 2f, 2f)

        ModelRenderable.builder()
            .setSource(context, Uri.parse("jupiter.sfb"))
            .build()
            .thenAccept {
                physicsManager.createBoxPhysicsNodeFromEye(
                    it, scene,
                    size, Vector3(0f, 0f, -0.2f), lookat, 10f, 10f
                )
            }
            .exceptionally {
                Toast.makeText(context, "Error", Toast.LENGTH_LONG).show()
                return@exceptionally null
            }
    }

    /**
     * Adds a node to the current scene
     * @param model - rendered model
     */
    private fun addNodeToScene(node: Node, model: ModelRenderable?) {
        val nodePhysicsId: Long = physicsManager.createPhysicsSphere(2f, Vector3(0f, 0f, -0.2f), 5f, node)
        model?.let {
            node.apply {
                setParent(scene)
                localPosition = Vector3(0f, 0f, -0.2f)
                localScale = Vector3(1f, 1f, 1f)
                name = "Planet"
                renderable = it
            }
            scene.addChild(node)
        }
        sceneObjects += node
    }
}