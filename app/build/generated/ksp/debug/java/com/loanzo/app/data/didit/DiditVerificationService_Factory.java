package com.loanzo.app.data.didit;

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
public final class DiditVerificationService_Factory implements Factory<DiditVerificationService> {
  @Override
  public DiditVerificationService get() {
    return newInstance();
  }

  public static DiditVerificationService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DiditVerificationService newInstance() {
    return new DiditVerificationService();
  }

  private static final class InstanceHolder {
    private static final DiditVerificationService_Factory INSTANCE = new DiditVerificationService_Factory();
  }
}
