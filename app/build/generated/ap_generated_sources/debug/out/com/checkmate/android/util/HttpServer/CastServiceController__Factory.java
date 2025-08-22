package com.checkmate.android.util.HttpServer;

import com.checkmate.android.service.BgCastService;
import java.lang.Override;
import toothpick.Factory;
import toothpick.Scope;

public final class CastServiceController__Factory implements Factory<CastServiceController> {
  @Override
  public CastServiceController createInstance(Scope scope) {
    scope = getTargetScope(scope);
    BgCastService param1 = scope.getInstance(BgCastService.class);
    CastServiceController castServiceController = new CastServiceController(param1);
    return castServiceController;
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
