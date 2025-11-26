package com.example.assignment_01

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class VideoCallActivity : AppCompatActivity() {

    // Agora
    private val APP_ID = "89bd7cb27c8541b999f7a34df1917a7a"
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private var mRtcEngine: RtcEngine? = null
    private var channelName: String = ""
    private var otherUserName: String = ""
    private var isMuted = false
    private var isVideoOff = false

    // Views
    private lateinit var localContainer: FrameLayout
    private lateinit var remoteContainer: FrameLayout
    private lateinit var userNameText: TextView
    private lateinit var endCallButton: ImageView
    private lateinit var muteButton: ImageView
    private lateinit var videoButton: ImageView
    private lateinit var switchCameraButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        // Get channel name and user name from intent
        channelName = intent.getStringExtra("CHANNEL_NAME") ?: ""
        otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "User"

        if (channelName.isEmpty()) {
            Toast.makeText(this, "Invalid call", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        localContainer = findViewById(R.id.local_video_view_container)
        remoteContainer = findViewById(R.id.remote_video_view_container)
        userNameText = findViewById(R.id.callUserName)
        endCallButton = findViewById(R.id.btn_end_call)
        muteButton = findViewById(R.id.btn_mute)
        videoButton = findViewById(R.id.btn_video)
        switchCameraButton = findViewById(R.id.btn_switch_camera)

        userNameText.text = otherUserName

        // Set button listeners
        endCallButton.setOnClickListener { endCall() }
        muteButton.setOnClickListener { toggleMute() }
        videoButton.setOnClickListener { toggleVideo() }
        switchCameraButton.setOnClickListener { switchCamera() }

        // Check permissions
        if (checkSelfPermission()) {
            initializeAndJoinChannel()
        }
    }

    private fun checkSelfPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                initializeAndJoinChannel()
            } else {
                Toast.makeText(this, "Camera and microphone permissions required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initializeAndJoinChannel() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler

            mRtcEngine = RtcEngine.create(config)

            // Enable video
            mRtcEngine?.enableVideo()

            // Setup local video
            setupLocalVideo()

            // Join channel
            val options = ChannelMediaOptions()
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER

            mRtcEngine?.joinChannel(null, channelName, 0, options)

            Log.d("VideoCall", "Joined channel: $channelName")

        } catch (e: Exception) {
            Log.e("VideoCall", "Error: ${e.message}")
            Toast.makeText(this, "Failed to initialize video call", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupLocalVideo() {
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        surfaceView.setZOrderMediaOverlay(true)
        localContainer.addView(surfaceView)

        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        mRtcEngine?.startPreview()
    }

    private fun setupRemoteVideo(uid: Int) {
        runOnUiThread {
            val surfaceView = RtcEngine.CreateRendererView(baseContext)
            remoteContainer.addView(surfaceView)

            mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d("VideoCall", "Remote user joined: $uid")
                Toast.makeText(this@VideoCallActivity, "$otherUserName joined", Toast.LENGTH_SHORT).show()
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.d("VideoCall", "Remote user left: $uid")
                Toast.makeText(this@VideoCallActivity, "$otherUserName left the call", Toast.LENGTH_SHORT).show()
                remoteContainer.removeAllViews()
                // Optionally end call when other user leaves
                endCall()
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d("VideoCall", "Join channel success: $channel")
            }
        }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        mRtcEngine?.muteLocalAudioStream(isMuted)

        muteButton.setImageResource(
            if (isMuted) android.R.drawable.ic_lock_silent_mode
            else android.R.drawable.ic_btn_speak_now
        )

        Toast.makeText(this, if (isMuted) "Muted" else "Unmuted", Toast.LENGTH_SHORT).show()
    }

    private fun toggleVideo() {
        isVideoOff = !isVideoOff
        mRtcEngine?.muteLocalVideoStream(isVideoOff)

        localContainer.visibility = if (isVideoOff) View.GONE else View.VISIBLE

        videoButton.setImageResource(
            if (isVideoOff) android.R.drawable.presence_invisible
            else android.R.drawable.presence_video_online
        )

        Toast.makeText(this, if (isVideoOff) "Video Off" else "Video On", Toast.LENGTH_SHORT).show()
    }

    private fun switchCamera() {
        mRtcEngine?.switchCamera()
    }

    private fun endCall() {
        mRtcEngine?.leaveChannel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
    }
}