package com.example.assignment_01

class test2package com.example.assignment_01

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoryViewTest {

    @Test
    fun testStoryDisplayActivityOpens() {
        val intent = android.content.Intent()
        intent.putExtra("USER_ID", "testUser123")

        val scenario = ActivityScenario.launch<page_14>(intent)

        // Verify that the story content ImageView is displayed
        onView(withId(R.id.storyContent)).check(matches(isDisplayed()))

        // Verify username text view exists
        onView(withId(R.id.storyUsername)).check(matches(withText("user1")))
    }
}
{
}