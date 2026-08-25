package com.loanzo.app.domain;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class PenaltyEngine_Factory implements Factory<PenaltyEngine> {
  @Override
  public PenaltyEngine get() {
    return newInstance();
  }

  public static PenaltyEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PenaltyEngine newInstance() {
    return new PenaltyEngine();
  }

  private static final class InstanceHolder {
    private static final PenaltyEngine_Factory INSTANCE = new PenaltyEngine_Factory();
  }
}
