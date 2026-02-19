package com.kresshy.weatherstation.fakes;

import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

import dagger.hilt.android.testing.HiltTestApplication;

/**
 * Custom AndroidJUnitRunner that uses {@link HiltTestApplication} as the application class. This is
 * necessary for Hilt to properly inject dependencies during Robolectric unit tests.
 */
public class CustomHiltTestRunner extends AndroidJUnitRunner {
    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        // Force the use of HiltTestApplication
        return super.newApplication(cl, HiltTestApplication.class.getName(), context);
    }
}
