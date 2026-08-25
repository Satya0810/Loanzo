package com.loanzo.app.data.network;

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
public final class LeegalityService_Factory implements Factory<LeegalityService> {
  @Override
  public LeegalityService get() {
    return newInstance();
  }

  public static LeegalityService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LeegalityService newInstance() {
    return new LeegalityService();
  }

  private static final class InstanceHolder {
    private static final LeegalityService_Factory INSTANCE = new LeegalityService_Factory();
  }
}
