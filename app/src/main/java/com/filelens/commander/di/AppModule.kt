package com.filelens.commander.di

import com.filelens.commander.domain.usecase.BrowseFilesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBrowseFilesUseCase(): BrowseFilesUseCase {
        return BrowseFilesUseCase()
    }
}
