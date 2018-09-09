package com.tasneembohra.plasticmobiletakehome

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Tasneem
 *
 * TODO Rotate item while dragging
 */
class MainActivity : AppCompatActivity(), View.OnDragListener, View.OnLongClickListener {
    companion object {
        const val TAG = "MainActivity"
        const val REQUEST_URL = "https://dateandtimeasjson.appspot.com/"
        const val RESPONSE_DATE_FORMAT = "yyyy-MM-DD HH:mm:ss.SSSSSS"
        const val RESPONSE_DATE_KEY = "datetime"
        const val DISPLAY_FORMAT = "%d:%02d:%02d"
    }

    private lateinit var mCircleAnimation: Animation
    private lateinit var mVolleyQueue: RequestQueue
    private val mResponseDateFormat = SimpleDateFormat(RESPONSE_DATE_FORMAT, Locale.CANADA)
    private val mCalender = Calendar.getInstance()!!

    //runs without a timer by reposting this handler at the end of the runnable
    private val mTimerHandler = Handler()
    private val mTimerRunnable = object : Runnable {
        override fun run() {
            // Request a string response from the provided URL.
            val request = JsonObjectRequest(Request.Method.GET, REQUEST_URL, null,
                    Response.Listener<JSONObject> { response ->
                        try {
                            val dateString = response.optString(RESPONSE_DATE_KEY, null)
                            Log.d(TAG, dateString)
                            if (dateString != null) {
                                mCalender.time = mResponseDateFormat.parse(dateString)
                                timerTV.text = String.format(DISPLAY_FORMAT,
                                        mCalender.get(Calendar.HOUR_OF_DAY),
                                        mCalender.get(Calendar.MINUTE),
                                        mCalender.get(Calendar.SECOND))
                            }
                        } catch (error: ParseException) { Log.e(TAG, error.message, error) }
                    },
                    Response.ErrorListener {error -> Log.e(TAG, error.message, error) })

            // Add the request to the RequestQueue.
            request.tag = TAG
            mVolleyQueue.add(request)
            mTimerHandler.postDelayed(this, 1000)
        }
    }

    override fun onDrag(view: View?, event: DragEvent?): Boolean {
        when (event?.action) {

            DragEvent.ACTION_DRAG_STARTED -> {
                Log.d(TAG, "Action is DragEvent.ACTION_DRAG_STARTED")
                // Increase bottom layout height as drag started and drop layout is bottom layout
                if (view?.id == R.id.bottomLL) {
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0)
                    lp.weight = 4F
                    bottomLL?.layoutParams = lp
                }
            }

            DragEvent.ACTION_DRAG_ENTERED -> Log.d(TAG, "Action is DragEvent.ACTION_DRAG_ENTERED")

            DragEvent.ACTION_DRAG_EXITED -> Log.d(TAG, "Action is DragEvent.ACTION_DRAG_EXITED")

            DragEvent.ACTION_DRAG_LOCATION -> Log.d(TAG, "Action is DragEvent.ACTION_DRAG_LOCATION")

            DragEvent.ACTION_DRAG_ENDED -> Log.d(TAG, "Action is DragEvent.ACTION_DRAG_ENDED")

            DragEvent.ACTION_DROP -> {
                Log.d(TAG,  "ACTION_DROP event")

                // Decrease bottom layout height if drop layout is top layout
                if (view?.id == R.id.topLL) {
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0)
                    lp.weight = 1F
                    bottomLL?.layoutParams = lp
                }

                val v = event.localState as View
                val owner = v.parent as ViewGroup
                //remove the dragged view
                owner.removeView(v)
                //caste the view into LinearLayout as our drag acceptable layout is LinearLayout
                val container = view as LinearLayout
                //Add the dragged view
                container.addView(v)
                //finally set Visibility to VISIBLE
                v.visibility = View.VISIBLE
                // Start rotation animation
                v.startAnimation(mCircleAnimation)

            }
        }
        return true
    }

    override fun onLongClick(v: View?): Boolean {
        // Create a new ClipData.
        // This is done in two steps to provide clarity. The convenience method
        // ClipData.newPlainText() can create a plain text ClipData in one step.

        // Create a new ClipData.Item from the TextView object's tag
        val item = ClipData.Item(v?.tag as CharSequence)

        // Create a new ClipData using the tag as a label, the plain text MIME type, and
        // the already-created item. This will create a new ClipDescription object within the
        // ClipData, and set its MIME type entry to "text/plain"
        val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)

        val dragData = ClipData(v?.tag.toString(), mimeTypes, item)

        // Instantiates the drag shadow builder.
        val dragShadowBuilder = View.DragShadowBuilder(v)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            v?.startDragAndDrop(dragData, dragShadowBuilder, v, 0)
        } else {
            v?.startDrag(dragData, dragShadowBuilder, v, 0)
        }
        v?.clearAnimation()
        v?.visibility = View.INVISIBLE
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create rotation animation
        mCircleAnimation = AnimationUtils.loadAnimation(this, R.anim.rotation)
        mCircleAnimation.reset()
        timerTV.clearAnimation()
        timerTV.startAnimation(mCircleAnimation)

        // Set on Long click listener to start dragging
        timerTV.setOnLongClickListener(this)

        // Initialize drag listener to top and bottom layout to accept drop
        bottomLL.setOnDragListener(this)
        topLL.setOnDragListener(this)

        // Hit server to fetch time every second
        mVolleyQueue = Volley.newRequestQueue(this)
    }

    override fun onResume() {
        super.onResume()
        mTimerHandler.postDelayed(mTimerRunnable, 0)
    }

    override fun onStop() {
        super.onStop()
        mTimerHandler.removeCallbacks(mTimerRunnable)
        // Cancel all volley requests
        mVolleyQueue.cancelAll(TAG)
    }
}
