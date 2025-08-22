package com.checkmate.android.service;

import java.lang.Override;
import toothpick.Factory;
import toothpick.MemberInjector;
import toothpick.Scope;

public final class BgUSBService__Factory implements Factory<BgUSBService> {
  private MemberInjector<BaseBackgroundService> memberInjector = new com.checkmate.android.service.BaseBackgroundService__MemberInjector();

  @Override
  public BgUSBService createInstance(Scope scope) {
    scope = getTargetScope(scope);
    BgUSBService bgUSBService = new BgUSBService();
    memberInjector.inject(bgUSBService, scope);
    return bgUSBService;
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
