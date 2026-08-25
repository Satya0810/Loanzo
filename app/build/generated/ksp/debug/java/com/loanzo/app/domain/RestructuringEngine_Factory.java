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
public final class RestructuringEngine_Factory implements Factory<RestructuringEngine> {
  @Override
  public RestructuringEngine get() {
    return newInstance();
  }

  public static RestructuringEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RestructuringEngine newInstance() {
    return new RestructuringEngine();
  }

  private static final class InstanceHolder {
    private static final RestructuringEngine_Factory INSTANCE = new RestructuringEngine_Factory();
  }
}
