// com.checkmate.android.di.ServiceModule.java
package com.checkmate.android.util.HttpServer;

import javax.inject.Named;
import toothpick.config.Module;

import com.google.common.util.concurrent.ServiceManager;
public class ServiceModule extends Module {
    public ServiceModule() {
        // Ensure all controllers are properly bound
        bind(BgService.class).withName("camera").to(CameraServiceController.class).singleton();
        bind(BgService.class).withName("cast").to(CastServiceController.class).singleton();
        bind(BgService.class).withName("audio").to(AudioServiceController.class).singleton();
        bind(BgService.class).withName("usb").to(UsbServiceController.class).singleton();
        bind(ServiceManager.class).to(ServiceManager.class).singleton();
    }
}