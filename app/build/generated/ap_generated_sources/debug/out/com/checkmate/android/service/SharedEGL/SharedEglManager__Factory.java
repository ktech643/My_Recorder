package com.checkmate.android.service.SharedEGL;

import java.lang.Override;
import toothpick.Factory;
import toothpick.Scope;

public final class SharedEglManager__Factory implements Factory<SharedEglManager> {
  @Override
  public SharedEglManager createInstance(Scope scope) {
    SharedEglManager sharedEglManager = new SharedEglManager();
    return sharedEglManager;
  }

  @Override
  public Scope getTargetScope(Scope scope) {
    return scope.getRootScope();
  }

  @Override
  public boolean hasScopeAnnotation() {
    return true;
  }

  @Override
  public boolean hasSingletonAnnotation() {
    return true;
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
