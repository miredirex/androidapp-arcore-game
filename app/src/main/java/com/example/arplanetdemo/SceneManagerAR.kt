package com.example.arplanetdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.*
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.android.synthetic.main.activity_main.*


@Suppress("SpellCheckingInspection")

class SceneManagerAR {

    /* Context is a reference for MainActivity. With the help of context you can access MainActivity fields  */
    private lateinit var context: Activity

    private lateinit var arFragment: ArFragment
    private lateinit var scene: Scene
    private lateinit var sceneView: ArSceneView
    private lateinit var physicsManager: PhysicsManager

    var timeElapsed: Float = 0.0f

    private var isTracking: Boolean = false
    private var isHitting: Boolean = false

    /* Array containing all user-spawned objects on a scene */
    private var sceneObjects: Array<Node> = arrayOf()

    /* Initialize and run a new scene in Activity's fragment */
    fun newScene(context: Context) {
        this.context = context as MainActivity
        this.arFragment = context.sceneform_fragment as ArFragment
        this.sceneView = arFragment.arSceneView
        this.scene = sceneView.scene

        // Hide sceneview cuz we're in AR mode
        context.sceneform_sceneview.visibility = View.GONE

        // Initialize physics
        this.physicsManager = PhysicsManager()
        physicsManager.init()

        runScene()
    }

    /* Update scene each frame */
    private fun runScene() {
        scene.addOnUpdateListener { frameTime ->
            arFragment.onUpdate(frameTime)
            onUpdate(frameTime)
        }
    }

    private fun createStaticFloor(location: Vector3) {
        val size = Vector3(0.3f, 0f, 0.3f)
        MaterialFactory.makeOpaqueWithColor(context, Color())
            .thenAccept {
                val floor = ShapeFactory.makeCube(size, Vector3.zero(), it)
                //staticFloor = physicsManager.createGroundPhysicsNode(floor, scene, size, location)
            }
            .exceptionally {
                Toast.makeText(context, "err", Toast.LENGTH_SHORT).show()
                return@exceptionally null
            }

    }



    // Simple function to show/hide our planet spawn buttons
    @SuppressLint("RestrictedApi")
    fun showHideButton(button: FloatingActionButton, enabled: Boolean) {
        if (enabled) {
            button.isEnabled = true
            button.visibility = View.VISIBLE
        } else {
            button.isEnabled = false
            button.visibility = View.GONE
        }
    }

    // Updates the tracking state. This function executes each frame
    private fun onUpdate(frameTime: FrameTime) {
        timeElapsed += frameTime.deltaSeconds

        updateTracking()
        physicsManager.updatePhysicsObjects()
        physicsManager.stepPhysicsWorld(frameTime.deltaSeconds)

        // Check if the devices gaze is hitting a plane detected by ARCore
        if (isTracking) {
            val hitTestChanged = updateHitTest()

            if (hitTestChanged) {
                showHideButton(context.floatingActionButton, isHitting)
            }
        }

        /* Initiate onUpdate() methods for all user-spawned objects on scene
        *  ---------------------------------------------------- */

        for (sceneObj in sceneObjects) {
            sceneObj.onUpdate(frameTime)
        }
    }

    // Performs frame.HitTest and returns if a hit is detected
    private fun updateHitTest(): Boolean {
        val frame = sceneView.arFrame
        val point = getScreenCenter()
        val hits: List<HitResult>
        val wasHitting = isHitting
        isHitting = false
        if (frame != null) {
            hits = frame.hitTest(point.x, point.y)
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting != isHitting
    }


    // Makes use of ARCore's camera state and returns true if the tracking state has changed
    private fun updateTracking(): Boolean {
        val frame = sceneView.arFrame
        val wasTracking = isTracking
        isTracking = frame?.camera?.trackingState == TrackingState.TRACKING
        return isTracking != wasTracking
    }

    // Simply returns the center of the screen
    private fun getScreenCenter(): Vector3 {
        val view = context.findViewById<View>(android.R.id.content)
        return Vector3(view.width / 2f, view.height / 2f, 0f)
    }

    /**
     * @param sfbAsset The Uri of our 3D sfb file
     *
     * This method takes in our 3D model and performs a hit test to determine where to place it
     */
    fun addObject(node: Node, sfbAsset: Uri) {
        val frame = sceneView.arFrame
        val point = getScreenCenter()
        if (frame != null) {
            val hits = frame.hitTest(point.x, point.y)
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    makeAsset(hit.createAnchor(), sfbAsset, node)
                    break
                }
            }
        }
    }

    fun pushUp() {
        physicsManager.applyForce(sceneObjects[0].name.toLong(), Vector3(5f, 5f, 5f), sceneObjects[0].localPosition)
    }

    /**
     * @param fragment our fragment
     * @param anchor ARCore anchor from the hit test
     * @param sfbAsset our 3D model of choice
     *
     * Uses the ARCore anchor from the hitTest result and builds the Sceneform nodes.
     * It starts the asynchronous loading of the 3D model using the ModelRenderable builder.
     */
    private fun makeAsset(anchor: Anchor, sfbAsset: Uri, node: Node) {
        ModelRenderable.builder()
            .setSource(context, sfbAsset)
            .build()
            .thenAccept {
                addNodeToScene(anchor, node, it)
            }
            .exceptionally {
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                return@exceptionally null
            }
    }

    /**
     * @param anchor ARCore anchor
     * @param renderable our model created as a Sceneform Renderable
     *
     * This method builds two nodes and attaches them to our scene
     * The Anchor nodes is positioned based on the pose of an ARCore Anchor. They stay positioned in the sample place relative to the real world.
     * The Transformable node is our Model
     * Once the nodes are connected we select the TransformableNode so it is available for interactions
     */
    private fun addNodeToScene(anchor: Anchor, node: Node, renderable: ModelRenderable) {
        val anchorNode = AnchorNode(anchor)
        node.renderable = renderable
        node.setParent(anchorNode)
        val assetManager = context.assets
        //physicsManager.createPhysicsCylinder(Vector3(0.05f, 0.3f, 0.05f), anchorNode.worldPosition, 0f, node)
        physicsManager.createPhysicsBodyWithCollisionShape(anchorNode.worldPosition, 0f, node, assetManager)

        scene.addChild(anchorNode)

        // add to array of all scene objects!
//        if (sceneObjects.isEmpty()) {
//            createStaticFloor(Vector3(anchorNode.worldPosition.x, -0.2f, anchorNode.worldPosition.y))
//        }
        sceneObjects += node
    }

    fun initiateBallAndThrow() {
        val cam = scene.camera
        // build ball's renderable
        ModelRenderable.builder()
            .setSource(context, Uri.parse("ball_gltf.sfb"))
            .build()
            .thenAccept {
                physicsManager.createSpherePhysicsNodeFromEye(
                    it,
                    scene,
                    0.02f,
                    Vector3.subtract(cam.worldPosition, Vector3(0f, 0.05f, 0f)),
                    Vector3.add(cam.forward, cam.up.scaled(0.5f)),
                    0.8f,
                    0.3f)
            }
            .exceptionally {
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                return@exceptionally null
            }

    }
}
