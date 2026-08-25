package com.loanzo.app.ui.loan;

import com.loanzo.app.util.TranslationHelper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class TranslationViewModel_Factory implements Factory<TranslationViewModel> {
  private final Provider<TranslationHelper> translationHelperProvider;

  public TranslationViewModel_Factory(Provider<TranslationHelper> translationHelperProvider) {
    this.translationHelperProvider = translationHelperProvider;
  }

  @Override
  public TranslationViewModel get() {
    return newInstance(translationHelperProvider.get());
  }

  public static TranslationViewModel_Factory create(
      Provider<TranslationHelper> translationHelperProvider) {
    return new TranslationViewModel_Factory(translationHelperProvider);
  }

  public static TranslationViewModel newInstance(TranslationHelper translationHelper) {
    return new TranslationViewModel(translationHelper);
  }
}
