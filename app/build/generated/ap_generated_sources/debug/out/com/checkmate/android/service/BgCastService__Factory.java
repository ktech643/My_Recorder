package com.checkmate.android.service;

import java.lang.Override;
import toothpick.Factory;
import toothpick.MemberInjector;
import toothpick.Scope;

public final class BgCastService__Factory implements Factory<BgCastService> {
  private MemberInjector<BaseBackgroundService> memberInjector = new com.checkmate.android.service.BaseBackgroundService__MemberInjector();

  @Override
  public BgCastService createInstance(Scope scope) {
    scope = getTargetScope(scope);
    BgCastService bgCastService = new BgCastService();
    memberInjector.inject(bgCastService, scope);
    return bgCastService;
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
