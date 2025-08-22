package com.checkmate.android.util.HttpServer;

import com.checkmate.android.service.BgAudioService;
import java.lang.Override;
import toothpick.Factory;
import toothpick.Scope;

public final class AudioServiceController__Factory implements Factory<AudioServiceController> {
  @Override
  public AudioServiceController createInstance(Scope scope) {
    scope = getTargetScope(scope);
    BgAudioService param1 = scope.getInstance(BgAudioService.class);
    AudioServiceController audioServiceController = new AudioServiceController(param1);
    return audioServiceController;
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
