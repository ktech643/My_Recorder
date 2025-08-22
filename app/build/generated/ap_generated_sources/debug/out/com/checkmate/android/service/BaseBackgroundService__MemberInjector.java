package com.checkmate.android.service;

import com.checkmate.android.service.SharedEGL.SharedEglManager;
import java.lang.Override;
import toothpick.MemberInjector;
import toothpick.Scope;

public final class BaseBackgroundService__MemberInjector implements MemberInjector<BaseBackgroundService> {
  @Override
  public void inject(BaseBackgroundService target, Scope scope) {
    target.mEglManager = scope.getInstance(SharedEglManager.class);
  }
}
