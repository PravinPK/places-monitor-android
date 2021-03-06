/*
 Copyright 2019 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
*/

//
// PlacesLocationManagerTests.java
//

package com.adobe.marketing.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Context.class, App.class, LocationServices.class, PendingIntent.class, ActivityCompat.class, PlacesMonitorDispatcher.class})
public class PlacesLocationManagerTests {
    private final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private PlacesLocationManager locationManager;

    @Mock
    Context context;

    @Mock
    FusedLocationProviderClient locationProviderClient;

    @Mock
    PendingIntent pendingIntent;

    @Mock
    Activity activity;

    @Mock
    SettingsClient mockSettingsClient;

    @Mock
    Task<Void> mockTask;

    @Mock
    LocationSettingsResponse mockLocationSettingsResponse;

    @Mock
    Task<Location> mockTaskLocation;

    @Mock
    Task<LocationSettingsResponse> mockTaskSettingsResponse;

    @Mock
    Location mockLocation;

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(App.class);
        PowerMockito.mockStatic(LocationServices.class);
        PowerMockito.mockStatic(PendingIntent.class);
        PowerMockito.mockStatic(ActivityCompat.class);
        PowerMockito.mockStatic(PlacesMonitorDispatcher.class);

        locationManager = new PlacesLocationManager();

        // mock static methods
        Mockito.when(App.getAppContext()).thenReturn(context);
        Mockito.when(App.getCurrentActivity()).thenReturn(activity);
        Mockito.when(LocationServices.getFusedLocationProviderClient(context)).thenReturn(locationProviderClient);
        Mockito.when(PendingIntent.getBroadcast(eq(context),eq(0), any(Intent.class), eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(pendingIntent);
        Mockito.when(App.getAppContext()).thenReturn(context);
        Mockito.when(ActivityCompat.shouldShowRequestPermissionRationale(activity, FINE_LOCATION)).thenReturn(false);
        Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_GRANTED);
        Mockito.when(LocationServices.getSettingsClient(context)).thenReturn(mockSettingsClient);

        // mock instance methods
        Mockito.when(locationProviderClient.removeLocationUpdates(pendingIntent)).thenReturn(mockTask);
        Mockito.when(mockSettingsClient.checkLocationSettings(any(LocationSettingsRequest.class))).thenReturn(mockTaskSettingsResponse);
        Mockito.when(locationProviderClient.getLastLocation()).thenReturn(mockTaskLocation);
    }


    // ========================================================================================
    // startMonitoring
    // ========================================================================================

    @Test
    public void test_startMonitoring() {
        // setup
        final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
        final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
        final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor= ArgumentCaptor.forClass(LocationRequest.class);

        // test
        locationManager.startMonitoring();

        // verify
        verify(mockTaskSettingsResponse, times(1)).addOnSuccessListener(onSuccessCallback.capture());
        verify(mockTaskSettingsResponse, times(1)).addOnFailureListener(onFailureCallback.capture());

        // trigger the success callback
        onSuccessCallback.getValue().onSuccess(mockLocationSettingsResponse);

        // verify if location updates are requested
        verify(locationProviderClient, times(1)).requestLocationUpdates(locationRequestArgumentCaptor.capture(), eq(pendingIntent));
        Boolean isRequestingLocationUpdates = Whitebox.getInternalState(locationManager, "isRequestingLocationUpdates");
        assertTrue("The location update flag should be set to true" ,isRequestingLocationUpdates);

        // verify the location request parameters
        assertEquals("the location request interval should be correct", PlacesMonitorTestConstants.Location.REQUEST_INTERVAL ,locationRequestArgumentCaptor.getValue().getInterval());
        assertEquals("the location fastest request interval should be correct", PlacesMonitorTestConstants.Location.REQUEST_FASTEST_INTERVAL ,locationRequestArgumentCaptor.getValue().getFastestInterval());
        assertEquals("the location small displacement should be correct", PlacesMonitorTestConstants.Location.REQUEST_SMALLEST_DISPLACEMENT ,locationRequestArgumentCaptor.getValue().getSmallestDisplacement(),0.0);
    }


    @Test
    public void test_startMonitoring_when_contextIsNull() {
        // setup
        Mockito.when(App.getAppContext()).thenReturn(null);

        // test
        locationManager.startMonitoring();

        // verify if location updates are not requested
        verify(mockSettingsClient, times(0)).checkLocationSettings(any(LocationSettingsRequest.class));
        verify(locationProviderClient, times(0)).requestLocationUpdates(any(LocationRequest.class), eq(pendingIntent));
        Boolean isRequestingLocationUpdates = Whitebox.getInternalState(locationManager, "isRequestingLocationUpdates");
        assertFalse("The location update flag should be set to false" ,isRequestingLocationUpdates);
    }


    @Test
    public void test_startMonitoring_when_fusedLocationProviderClient_null() {
        // setup
        final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
        final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor= ArgumentCaptor.forClass(LocationRequest.class);
        Mockito.when(LocationServices.getFusedLocationProviderClient(context)).thenReturn(null);

        // test
        locationManager.startMonitoring();

        // verify
        verify(mockTaskSettingsResponse, times(1)).addOnSuccessListener(onSuccessCallback.capture());

        // trigger the success callback
        onSuccessCallback.getValue().onSuccess(mockLocationSettingsResponse);

        // verify that the location updates are not requested
        verify(locationProviderClient, times(0)).requestLocationUpdates(locationRequestArgumentCaptor.capture(), eq(pendingIntent));
        Boolean isRequestingLocationUpdates = Whitebox.getInternalState(locationManager, "isRequestingLocationUpdates");
        assertFalse("The location update flag should be set to false" ,isRequestingLocationUpdates);
    }

    @Test
    public void test_startMonitoring_when_pendingIntent_null() {
        // setup
        final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
        final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor= ArgumentCaptor.forClass(LocationRequest.class);
        Mockito.when(PendingIntent.getBroadcast(eq(context),eq(0), any(Intent.class), eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

        // test
        locationManager.startMonitoring();

        // verify
        verify(mockTaskSettingsResponse, times(1)).addOnSuccessListener(onSuccessCallback.capture());

        // trigger the success callback
        onSuccessCallback.getValue().onSuccess(mockLocationSettingsResponse);

        // verify that the location updates are not requested
        verify(locationProviderClient, times(0)).requestLocationUpdates(locationRequestArgumentCaptor.capture(), eq(pendingIntent));
        Boolean isRequestingLocationUpdates = Whitebox.getInternalState(locationManager, "isRequestingLocationUpdates");
        assertFalse("The location update flag should be set to false" ,isRequestingLocationUpdates);
    }

    @Test
    public void test_startMonitoring_when_failure_withStatusCode_ResolutionRequired() {
        // setup
        final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
        final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor= ArgumentCaptor.forClass(LocationRequest.class);
        Mockito.when(PendingIntent.getBroadcast(eq(context),eq(0), any(Intent.class), eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

        // test
        locationManager.startMonitoring();

        // verify
        verify(mockTaskSettingsResponse, times(1)).addOnFailureListener(onFailureCallback.capture());

        // trigger that success callback
        Status status = new Status(LocationSettingsStatusCodes.RESOLUTION_REQUIRED);
        ApiException exception = new ApiException(status);
        onFailureCallback.getValue().onFailure(exception );

        // verify that the location updates are not requested
        verify(locationProviderClient, times(0)).requestLocationUpdates(locationRequestArgumentCaptor.capture(), eq(pendingIntent));
        Boolean isRequestingLocationUpdates = Whitebox.getInternalState(locationManager, "isRequestingLocationUpdates");
        assertFalse("The location update flag should be set to false" ,isRequestingLocationUpdates);
    }

    @Test
    public void test_startMonitoring_when_failure_withStatusCode_settingsChangeUnavailable() {
        // setup
        final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
        final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor= ArgumentCaptor.forClass(LocationRequest.class);
        Mockito.when(PendingIntent.getBroadcast(eq(context),eq(0), any(Intent.class), eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

        // test
        locationManager.startMonitoring();

        // verify
        verify(mockTaskSettingsResponse, times(1)).addOnFailureListener(onFailureCallback.capture());

        // trigger that success callback
        Status status = new Status(LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE);
        ApiException exception = new ApiException(status);
        onFailureCallback.getValue().onFailure(exception );

        // verify that the location updates are not requested
        verify(locationProviderClient, times(0)).requestLocationUpdates(locationRequestArgumentCaptor.capture(), eq(pendingIntent));
        Boolean isRequestingLocationUpdates = Whitebox.getInternalState(locationManager, "isRequestingLocationUpdates");
        assertFalse("The location update flag should be set to false" ,isRequestingLocationUpdates);
    }


    @Test
    public void test_startMonitoring_when_permissionNotGranted() {
        // setup
        Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);


        // test
        locationManager.startMonitoring();

        // verify
        verifyStatic(ActivityCompat.class, Mockito.times(1));
        ActivityCompat.requestPermissions(activity, new String[] {FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
    }


    @Test
    public void test_startMonitoring_when_permissionNotGranted_AndActivityIsNull() {
        // setup
        Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);
        Mockito.when(App.getCurrentActivity()).thenReturn(null);

        // test
        locationManager.startMonitoring();

        // verify
        verifyStatic(ActivityCompat.class, Mockito.times(0));
        ActivityCompat.requestPermissions(any(Activity.class), any(String[].class), anyInt());
    }

    @Test
    public void test_startMonitoring_when_permissionNotGranted_and_shouldShowRequestPermissionRationaleTrue() {
        // setup
        Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);
        Mockito.when(ActivityCompat.shouldShowRequestPermissionRationale(activity, FINE_LOCATION)).thenReturn(true);

        // test
        locationManager.startMonitoring();

        // verify
        verifyStatic(ActivityCompat.class, Mockito.times(0));
        ActivityCompat.requestPermissions(any(Activity.class), any(String[].class), anyInt());
    }


    // ========================================================================================
    // stopMonitoring
    // ========================================================================================
    @Test
    public void test_stopMonitoring() {
        // setup
        final ArgumentCaptor<OnCompleteListener> onCompleteCallback = ArgumentCaptor.forClass(OnCompleteListener.class);

        // test
        locationManager.stopMonitoring();

        // verify
        verify(mockTask, times(1)).addOnCompleteListener(onCompleteCallback.capture());
        verify(locationProviderClient, times(1)).removeLocationUpdates(pendingIntent);

        // trigger that callback
        onCompleteCallback.getValue().onComplete(mockTask);
        Boolean isRequestingLocationUpdates = Whitebox.getInternalState(locationManager, "isRequestingLocationUpdates");
        assertFalse("The location update flag should be reset to false" ,isRequestingLocationUpdates);
    }

    @Test
    public void test_stopMonitoring_when_fusedLocationProviderClient_null() {
        // setup
        final ArgumentCaptor<OnCompleteListener> onCompleteCallback = ArgumentCaptor.forClass(OnCompleteListener.class);
        Mockito.when(LocationServices.getFusedLocationProviderClient(context)).thenReturn(null);

        // test
        locationManager.stopMonitoring();

        // verify
        verify(mockTask, times(0)).addOnCompleteListener(onCompleteCallback.capture());
        verify(locationProviderClient, times(0)).removeLocationUpdates(pendingIntent);
    }

    @Test
    public void test_stopMonitoring_when_context_null() {
        // setup
        final ArgumentCaptor<OnCompleteListener> onCompleteCallback = ArgumentCaptor.forClass(OnCompleteListener.class);
        Mockito.when(App.getAppContext()).thenReturn(null);

        // test
        locationManager.stopMonitoring();

        // verify
        verify(mockTask, times(0)).addOnCompleteListener(onCompleteCallback.capture());
        verify(locationProviderClient, times(0)).removeLocationUpdates(pendingIntent);
    }


    @Test
    public void test_stopMonitoring_when_pendingIntent_null() {
        // setup
        final ArgumentCaptor<OnCompleteListener> onCompleteCallback = ArgumentCaptor.forClass(OnCompleteListener.class);
        Mockito.when(PendingIntent.getBroadcast(eq(context),eq(0), any(Intent.class), eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

        // test
        locationManager.stopMonitoring();

        // verify
        verify(mockTask, times(0)).addOnCompleteListener(onCompleteCallback.capture());
        verify(locationProviderClient, times(0)).removeLocationUpdates(pendingIntent);
    }
    
    // ========================================================================================
    // updateLocation
    // ========================================================================================
    
    @Test
    public void test_updateLocation() {
        // setup
        final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
        final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
        Whitebox.setInternalState(locationManager, "isRequestingLocationUpdates", true);
        Whitebox.setInternalState(locationManager, "fusedLocationClient", locationProviderClient);
        
        // test
        locationManager.updateLocation();

        // verify
        verify(mockTaskLocation, times(1)).addOnSuccessListener(onSuccessCallback.capture());
        verify(mockTaskLocation, times(1)).addOnFailureListener(onFailureCallback.capture());

        // trigger the success callback
        onSuccessCallback.getValue().onSuccess(mockLocation);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(1));
        PlacesMonitorDispatcher.dispatchLocation(mockLocation);
    }


    @Test
    public void test_updateLocation_when_successButNullLocation() {
        // setup
        final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
        final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
        Whitebox.setInternalState(locationManager, "isRequestingLocationUpdates", true);

        // test
        locationManager.updateLocation();

        // verify
        verify(mockTaskLocation, times(1)).addOnSuccessListener(onSuccessCallback.capture());
        verify(mockTaskLocation, times(1)).addOnFailureListener(onFailureCallback.capture());

        // trigger the success callback
        onSuccessCallback.getValue().onSuccess(null);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(mockLocation);
    }

    @Test
    public void test_updateLocation_when_monitoringNotStarted() {
        // setup
        final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
        Whitebox.setInternalState(locationManager, "isRequestingLocationUpdates", false);

        // test
        locationManager.updateLocation();

        // verify
        verify(mockTaskLocation, times(0)).addOnSuccessListener(onSuccessCallback.capture());

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(mockLocation);
    }


    @Test
    public void test_updateLocation_when_locationClient_null() {
        // setup
        final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
        Whitebox.setInternalState(locationManager, "isRequestingLocationUpdates", true);
        Mockito.when(LocationServices.getFusedLocationProviderClient(context)).thenReturn(null);

        // test
        locationManager.updateLocation();

        // verify
        verify(mockTaskLocation, times(0)).addOnSuccessListener(onSuccessCallback.capture());

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(mockLocation);
    }

    @Test
    public void test_updateLocation_when_FailureToReceiveLocation() {
        // setup
        final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
        final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
        Whitebox.setInternalState(locationManager, "isRequestingLocationUpdates", true);

        // test
        locationManager.updateLocation();

        // verify
        verify(mockTaskLocation, times(1)).addOnSuccessListener(onSuccessCallback.capture());
        verify(mockTaskLocation, times(1)).addOnFailureListener(onFailureCallback.capture());

        // trigger the failure callback
        onFailureCallback.getValue().onFailure(new Exception());

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(mockLocation);
    }

    // ========================================================================================
    // GetPendingIntent
    // ========================================================================================
    // testing private method to get code coverage to centum.
    @Test
    public void test_getPendingIntent_when_context_isNull() throws Exception {
        // setup
        Mockito.when(App.getAppContext()).thenReturn(null);

        // test
        PendingIntent intent = Whitebox.invokeMethod(locationManager, "getPendingIntent");

        // verify
        assertNull(intent);
    }

}
