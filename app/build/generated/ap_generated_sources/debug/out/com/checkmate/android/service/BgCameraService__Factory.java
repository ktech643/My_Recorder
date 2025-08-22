package com.checkmate.android.service;

import java.lang.Override;
import toothpick.Factory;
import toothpick.MemberInjector;
import toothpick.Scope;

public final class BgCameraService__Factory implements Factory<BgCameraService> {
  private MemberInjector<BaseBackgroundService> memberInjector = new com.checkmate.android.service.BaseBackgroundService__MemberInjector();

  @Override
  public BgCameraService createInstance(Scope scope) {
    scope = getTargetScope(scope);
    BgCameraService bgCameraService = new BgCameraService();
    memberInjector.inject(bgCameraService, scope);
    return bgCameraService;
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
