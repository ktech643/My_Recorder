package com.checkmate.android.util.HttpServer;

import com.checkmate.android.service.BgCameraService;
import java.lang.Override;
import toothpick.Factory;
import toothpick.Scope;

public final class CameraServiceController__Factory implements Factory<CameraServiceController> {
  @Override
  public CameraServiceController createInstance(Scope scope) {
    scope = getTargetScope(scope);
    BgCameraService param1 = scope.getInstance(BgCameraService.class);
    CameraServiceController cameraServiceController = new CameraServiceController(param1);
    return cameraServiceController;
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
