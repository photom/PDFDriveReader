package com.hitsuji.pdfdrivereader.di

import android.content.Context
import androidx.room.Room
import com.hitsuji.pdfdrivereader.data.local.AppDatabase
import com.hitsuji.pdfdrivereader.data.local.dao.PdfDao
import com.hitsuji.pdfdrivereader.data.local.datastore.AppConfigurationRepositoryImpl
import com.hitsuji.pdfdrivereader.data.local.scanner.LocalFileScanner
import com.hitsuji.pdfdrivereader.data.mapper.DocumentMapper
import com.hitsuji.pdfdrivereader.data.mapper.ReadingSessionMapper
import com.hitsuji.pdfdrivereader.data.remote.GoogleDriveService
import com.hitsuji.pdfdrivereader.data.remote.impl.GoogleDriveServiceImpl
import com.hitsuji.pdfdrivereader.data.renderer.PdfRendererWrapper
import com.hitsuji.pdfdrivereader.data.repository.RoomPdfRepository
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing data-layer dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindPdfRepository(
        impl: RoomPdfRepository
    ): PdfRepository

    @Binds
    @Singleton
    abstract fun bindAppConfigurationRepository(
        impl: AppConfigurationRepositoryImpl
    ): AppConfigurationRepository

    @Module
    @InstallIn(SingletonComponent::class)
    object Providers {

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "pdf_drive_reader.db"
            ).fallbackToDestructiveMigration().build()
        }

        @Provides
        fun providePdfDao(db: AppDatabase): PdfDao = db.pdfDao()

        @Provides
        @Singleton
        fun provideDocumentMapper(scanner: LocalFileScanner): DocumentMapper = DocumentMapper(scanner)

        @Provides
        @Singleton
        fun provideSessionMapper(): ReadingSessionMapper = ReadingSessionMapper()

        @Provides
        @Singleton
        fun provideLocalFileScanner(@ApplicationContext context: Context): LocalFileScanner = LocalFileScanner(context)

        @Provides
        @Singleton
        fun providePdfRendererWrapper(): PdfRendererWrapper = PdfRendererWrapper()

        @Provides
        @Singleton
        fun provideGoogleDriveService(@ApplicationContext context: Context): GoogleDriveService {
            return GoogleDriveServiceImpl(context)
        }
    }
}
