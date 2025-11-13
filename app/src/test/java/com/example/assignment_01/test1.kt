package com.example.assignment_01

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoryUploadTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)

    @Test
    fun testStoryUploadFlow() {
        Intents.init()
        val scenario = ActivityScenario.launch(page_5::class.java)

        // Mock gallery intent result
        val resultData = Intent()
        val imageUri = android.net.Uri.parse("content://media/external/images/media/1")
        resultData.data = imageUri
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)

        // When user taps story circle
        Intents.intending(hasAction(Intent.ACTION_PICK)).respondWith(result)
        onView(withId(R.id.img_84)).perform(click())

        // Verify gallery opened
        Intents.intended(allOf(hasAction(Intent.ACTION_PICK)))

        Intents.release()
    }
}
