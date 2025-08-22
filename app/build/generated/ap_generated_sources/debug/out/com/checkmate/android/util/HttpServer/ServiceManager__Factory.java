package com.checkmate.android.util.HttpServer;

import java.lang.Override;
import toothpick.Factory;
import toothpick.MemberInjector;
import toothpick.Scope;

public final class ServiceManager__Factory implements Factory<ServiceManager> {
  private MemberInjector<ServiceManager> memberInjector = new com.checkmate.android.util.HttpServer.ServiceManager__MemberInjector();

  @Override
  public ServiceManager createInstance(Scope scope) {
    scope = getTargetScope(scope);
    ServiceManager serviceManager = new ServiceManager();
    memberInjector.inject(serviceManager, scope);
    return serviceManager;
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
