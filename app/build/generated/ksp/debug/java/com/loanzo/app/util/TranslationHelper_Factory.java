package com.loanzo.app.util;

import com.google.gson.Gson;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class TranslationHelper_Factory implements Factory<TranslationHelper> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<Gson> gsonProvider;

  public TranslationHelper_Factory(Provider<OkHttpClient> clientProvider,
      Provider<Gson> gsonProvider) {
    this.clientProvider = clientProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public TranslationHelper get() {
    return newInstance(clientProvider.get(), gsonProvider.get());
  }

  public static TranslationHelper_Factory create(Provider<OkHttpClient> clientProvider,
      Provider<Gson> gsonProvider) {
    return new TranslationHelper_Factory(clientProvider, gsonProvider);
  }

  public static TranslationHelper newInstance(OkHttpClient client, Gson gson) {
    return new TranslationHelper(client, gson);
  }
}
