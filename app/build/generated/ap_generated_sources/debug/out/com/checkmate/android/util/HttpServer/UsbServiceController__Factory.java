package com.checkmate.android.util.HttpServer;

import com.checkmate.android.service.BgUSBService;
import java.lang.Override;
import toothpick.Factory;
import toothpick.Scope;

public final class UsbServiceController__Factory implements Factory<UsbServiceController> {
  @Override
  public UsbServiceController createInstance(Scope scope) {
    scope = getTargetScope(scope);
    BgUSBService param1 = scope.getInstance(BgUSBService.class);
    UsbServiceController usbServiceController = new UsbServiceController(param1);
    return usbServiceController;
  }

  @Override
  public Scope getTargetScope(Scope scope) {
    return scope;
  }

  @Override
  public boolean hasScopeAnnotation() {
    return false;
  }

  @Override
  public boolean hasSingletonAnnotation() {
    return false;
  }

  @Override
  public boolean hasReleasableAnnotation() {
    return false;
  }

  @Override
  public boolean hasProvidesSingletonAnnotation() {
    return false;
  }

  @Override
  public boolean hasProvidesReleasableAnnotation() {
    return false;
  }
}
