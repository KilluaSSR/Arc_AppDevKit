package killua.dev.core.log.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import killua.dev.core.log.LogcatCaptureService
import killua.dev.core.log.domain.LogExportFormat
import killua.dev.core.log.repository.LogRepository
import killua.dev.core.log.repository.LogcatRepository
import killua.dev.core.log.service.LogExportService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LogModule {

    companion object {
        @Provides
        @Singleton
        fun provideLogRepository(
            @ApplicationContext context: Context
        ): LogRepository {
            return LogcatRepository(context)
        }

        @Provides
        @Singleton
        fun provideLogExportService(
            @ApplicationContext context: Context,
            logRepository: LogRepository
        ): LogExportService {
            return LogExportService(context, logRepository)
        }

        @Provides
        @Singleton
        fun provideLogcatCaptureService(
            logRepository: LogRepository,
            logExportService: LogExportService
        ): LogcatCaptureService {
            return LogcatCaptureService(logRepository, logExportService)
        }

        @Provides
        fun provideSupportedExportFormats(): List<LogExportFormat> {
            return LogExportFormat.entries
        }
    }
}