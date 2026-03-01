package com.kresshy.weatherstation.domain;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.bluetooth.WeatherConnectionController;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConnectToDeviceUseCaseTest {

    @Mock private WeatherConnectionController connectionController;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;
    @Mock private Context context;
    
    private ConnectToDeviceUseCase useCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        
        // Return a dummy key for the preference
        when(context.getString(R.string.PREFERENCE_DEVICE_ADDRESS)).thenReturn("PREFERENCE_DEVICE_ADDRESS");
        
        useCase = new ConnectToDeviceUseCase(connectionController, sharedPreferences, context);
    }

    private String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }

    @Test
    public void execute_callsControllerWithAddressAndPersistsIt() {
        String address = "00:11:22:33:44:55";
        useCase.execute(address);
        
        verify(editor).putString("PREFERENCE_DEVICE_ADDRESS", address);
        verify(editor).apply();
        verify(connectionController).connectToDeviceAddress(address);
    }
}
