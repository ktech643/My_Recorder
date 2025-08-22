package com.checkmate.android.util.HttpServer;

import java.lang.Override;
import toothpick.MemberInjector;
import toothpick.Scope;

public final class ServiceManager__MemberInjector implements MemberInjector<ServiceManager> {
  @Override
  public void inject(ServiceManager target, Scope scope) {
    target.cameraService = scope.getInstance(BgService.class, "camera");
    target.castService = scope.getInstance(BgService.class, "cast");
    target.audioService = scope.getInstance(BgService.class, "audio");
    target.usbService = scope.getInstance(BgService.class, "usb");
    target.initialize();
  }
}
