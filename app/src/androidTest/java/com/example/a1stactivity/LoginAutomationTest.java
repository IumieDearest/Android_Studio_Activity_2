package com.example.a1stactivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.startsWith;

import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

// =====================================================================
//  LOGIN AUTOMATION (app/src/androidTest/.../LoginAutomationTest.java)
//
//  This is an automated UI test made with Espresso, Android's testing tool.
//  When you run it, the emulator opens the app by itself, types into the
//  fields, taps Sign In, and checks the result from the Render server.
//
//    Scenario 1: Invalid Login -> wrong password, must stay on the Login screen
//    Scenario 2: Valid Login   -> correct account, opens the OTP screen,
//                                 types the code, and opens the Users list
// =====================================================================

// Tells Android to run this class as an instrumented test (on the emulator)
@RunWith(AndroidJUnit4.class)
// Runs the tests in alphabetical order, so Scenario 1 always runs before Scenario 2
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoginAutomationTest {

    // How long to wait for the Render server. 90 seconds, because a sleeping
    // free Render server can take about a minute to wake up.
    private static final long SERVER_TIMEOUT_MS = 90000;

    // NEW: delays so a person watching can follow every step (in milliseconds).
    // Make these bigger to slow the test down, or smaller to speed it up.
    private static final long TYPING_DELAY_MS = 150;  // pause between each typed letter
    private static final long STEP_DELAY_MS = 1500;   // pause between steps
    private static final long RESULT_DELAY_MS = 3000; // pause to look at the result

    // Before EACH test, open a fresh Login screen (MainActivity)
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // -----------------------------------------------------------------
    //  SCENARIO 1: INVALID LOGIN
    //  A registered username with the WRONG password.
    //  Expected: the server rejects it and the app stays on the Login screen.
    // -----------------------------------------------------------------
    @Test
    public void scenario1_invalidLogin() {
        // Give the viewer a moment to see the Login screen first
        pause(STEP_DELAY_MS);

        // 1. Type the username and a wrong password, one letter at a time
        typeSlowly(R.id.etUsername, "student");
        pause(STEP_DELAY_MS);
        typeSlowly(R.id.etPassword, "wrongpassword");
        pause(STEP_DELAY_MS);

        // 2. Tap Sign In (scrollTo() makes sure the button is on screen first)
        onView(withId(R.id.btnLogin)).perform(scrollTo(), click());

        // 3. Wait for the server's reply. While waiting, the button is disabled
        //    and says "Signing in…". When it's enabled and says "Sign In" again,
        //    the server has answered.
        waitUntil(withId(R.id.btnLogin),
                allOf(isEnabled(), withText(R.string.btn_sign_in)),
                SERVER_TIMEOUT_MS);

        // 4. CHECK: we are still on the Login screen...
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()));

        // 5. CHECK: ...and the OTP screen did NOT open
        onView(withId(R.id.etOtp)).check(doesNotExist());

        // Keep the screen up so the viewer can read the error message
        pause(RESULT_DELAY_MS);
    }

    // -----------------------------------------------------------------
    //  SCENARIO 2: VALID LOGIN
    //  A registered account with the correct password.
    //  Expected: the server accepts it and the app opens the OTP screen.
    // -----------------------------------------------------------------
    @Test
    public void scenario2_validLogin() {
        pause(STEP_DELAY_MS);

        // 1. Type a registered account (the test account on the Render server)
        typeSlowly(R.id.etUsername, "student");
        pause(STEP_DELAY_MS);
        typeSlowly(R.id.etPassword, "password123");
        pause(STEP_DELAY_MS);

        // 2. Tap Sign In
        onView(withId(R.id.btnLogin)).perform(scrollTo(), click());

        // 3. CHECK: wait until the OTP code box appears, which means
        //    the login was accepted and the OTP screen opened
        waitUntil(withId(R.id.etOtp), isDisplayed(), SERVER_TIMEOUT_MS);

        // 4. CHECK: the code from the server is shown on the OTP screen
        onView(withId(R.id.tvDemoCode)).check(matches(isDisplayed()));
        pause(STEP_DELAY_MS);

        // ===== OTP PART =====
        // 5. Read the code from the grey box. The box says "Your code: 4821",
        //    so replaceAll("[^0-9]", "") removes everything that isn't a digit -> "4821"
        String boxText = getTextFromView(R.id.tvDemoCode);
        String otpCode = boxText.replaceAll("[^0-9]", "");

        // 6. Type the code one digit at a time, like a person would.
        //    (The code expires in 30 seconds, but this whole part takes only a few.)
        typeSlowly(R.id.etOtp, otpCode);
        pause(STEP_DELAY_MS);

        // 7. Tap Verify
        onView(withId(R.id.btnVerify)).perform(scrollTo(), click());

        // 8. CHECK: wait until the Users screen opens AND the list has loaded.
        //    The count text says "Loading users…" first, then "Registered users: 2".
        waitUntil(withId(R.id.tvUserCount),
                withText(startsWith("Registered users")),
                SERVER_TIMEOUT_MS);

        // Keep the Users list up so the viewer can see the final result
        pause(RESULT_DELAY_MS);
    }

    // -----------------------------------------------------------------
    //  NEW HELPER: reads the text that is currently shown in a TextView.
    //
    //  Espresso normally only CHECKS views, so we make our own small
    //  ViewAction that copies the text out into a String we can use.
    // -----------------------------------------------------------------
    private String getTextFromView(int viewId) {
        // An array is used because values changed inside the inner class must be final
        final String[] textHolder = {""};

        onView(withId(viewId)).perform(new ViewAction() {

            // This action only works on TextViews (and things like EditText that extend it)
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            // A short description, shown in the error message if something goes wrong
            @Override
            public String getDescription() {
                return "read the text of a TextView";
            }

            // The actual work: copy the text into textHolder
            @Override
            public void perform(UiController uiController, View view) {
                textHolder[0] = ((TextView) view).getText().toString();
            }
        });

        return textHolder[0];
    }

    // -----------------------------------------------------------------
    //  NEW HELPER: types text into a field one letter at a time,
    //  so it looks like a person typing instead of pasting.
    //
    //  How it works: for "student" it shows "s", then "st", then "stu"...
    //  waiting a little between each one. replaceText() is used for each
    //  step because it works the same on every keyboard and for password fields.
    // -----------------------------------------------------------------
    private void typeSlowly(int fieldId, String text) {
        // Start with an empty box (in case an email was pre-filled)
        onView(withId(fieldId)).perform(replaceText(""));

        for (int i = 1; i <= text.length(); i++) {
            // substring(0, i) = the first i letters of the text
            onView(withId(fieldId)).perform(replaceText(text.substring(0, i)));
            pause(TYPING_DELAY_MS);
        }

        // Hide the keyboard so it doesn't cover the Sign In button
        onView(withId(fieldId)).perform(closeSoftKeyboard());
    }

    // -----------------------------------------------------------------
    //  NEW HELPER: waits for the given time, just so the viewer can follow along.
    // -----------------------------------------------------------------
    private void pause(long milliseconds) {
        SystemClock.sleep(milliseconds);
    }

    // -----------------------------------------------------------------
    //  HELPER: keeps checking a view every half second until it matches
    //  the condition, or gives up (fails the test) after the timeout.
    //
    //  Why we need this: Espresso does not know our app is waiting for the
    //  internet, so without this it would check the screen too early.
    // -----------------------------------------------------------------
    private void waitUntil(Matcher<View> viewMatcher, Matcher<View> condition, long timeoutMs) {
        long endTime = System.currentTimeMillis() + timeoutMs;

        while (true) {
            try {
                // Try the check. If it passes, we're done waiting.
                onView(viewMatcher).check(matches(condition));
                return;
            } catch (Throwable error) {
                // Not ready yet. If time is up, fail the test with the last error.
                if (System.currentTimeMillis() > endTime) {
                    throw error;
                }
                // Otherwise wait half a second and try again
                SystemClock.sleep(500);
            }
        }
    }
}