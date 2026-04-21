package com.woliveiras.palabrita.core.common.di

import android.content.Context
import com.woliveiras.palabrita.core.common.DeviceCapabilities
import com.woliveiras.palabrita.core.common.DeviceTier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DeviceModule {

  @Provides
  @Singleton
  fun provideDeviceTier(@ApplicationContext context: Context): DeviceTier =
    DeviceCapabilities.getDeviceTier(context)
}
