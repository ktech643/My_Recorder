package com.checkmate.android.service;

import java.lang.Override;
import toothpick.Factory;
import toothpick.MemberInjector;
import toothpick.Scope;

public final class BgAudioService__Factory implements Factory<BgAudioService> {
  private MemberInjector<BaseBackgroundService> memberInjector = new com.checkmate.android.service.BaseBackgroundService__MemberInjector();

  @Override
  public BgAudioService createInstance(Scope scope) {
    scope = getTargetScope(scope);
    BgAudioService bgAudioService = new BgAudioService();
    memberInjector.inject(bgAudioService, scope);
    return bgAudioService;
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
