package com.kresshy.weatherstation.activity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;

import com.kresshy.weatherstation.fakes.FakeWeatherRepository;
import com.kresshy.weatherstation.repository.WeatherRepository;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import javax.inject.Inject;

/**
 * UI Integration tests for {@link WSActivity}. Verifies that the activity correctly handles
 * dependency injection and navigation transitions.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class, sdk = 34)
public class WSActivityTest {

    @Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject WeatherRepository weatherRepository;

    @Before
    public void init() {
        hiltRule.inject();
    }

    /** Verifies that Hilt correctly injects the {@link FakeWeatherRepository} into the activity. */
    @Test
    public void activityLaunch_InjectsFakeRepository() {
        try (ActivityScenario<WSActivity> scenario = ActivityScenario.launch(WSActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        assertNotNull(activity.weatherRepository);
                        assertNotNull(activity.connectionController);
                        assertTrue(
                                "Repository should be the fake one",
                                activity.weatherRepository instanceof FakeWeatherRepository);
                    });
        }
    }

    private void assertEquals(int expected, int actual) {
        if (expected != actual)
            throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
