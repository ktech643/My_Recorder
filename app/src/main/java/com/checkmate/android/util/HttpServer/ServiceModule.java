// com.checkmate.android.di.ServiceModule.java
package com.checkmate.android.util.HttpServer;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import com.checkmate.android.util.HttpServer.ServiceManager;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {

    @Provides
    @Singleton
    @Named("camera")
    BgService provideCameraService(CameraServiceController controller) {
        return controller;
    }

    @Provides
    @Singleton
    @Named("cast")
    BgService provideCastService(CameraServiceController controller) {
        return controller;
    }

    @Provides
    @Singleton
    @Named("usb")
    BgService provideUsbService(CameraServiceController controller) {
        return controller;
    }

    @Provides
    @Singleton
    ServiceManager provideServiceManager(
            @Named("camera") BgService cameraService,
            @Named("cast") BgService castService,
            @Named("usb") BgService usbService) {
        return new ServiceManager(cameraService, castService, usbService);
    }
}