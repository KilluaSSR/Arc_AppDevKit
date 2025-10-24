package killua.dev.core.init

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object StartupModule {
    
    @Provides
    @Singleton
    fun provideTaskMonitor(): TaskMonitor {
        return LoggingTaskMonitor()
    }
    
    @Provides
    @Singleton
    fun provideStartupOrchestrator(
        taskMonitor: TaskMonitor
    ): StartupOrchestrator {
        return StartupOrchestrator(taskMonitor)
    }
    
    @Provides
    @Singleton
    fun provideStartupConfig(
        @ApplicationContext context: Context
    ): StartupConfig {
        return DefaultStartupConfig(context)
    }
}

/**
 * Example of how to provide custom startup configuration
 * 
 * Business logic should create their own module like this:
 * 
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object AppStartupModule {
 *     
 *     @Provides
 *     @Singleton
 *     fun provideStartupConfig(
 *         @ApplicationContext context: Context
 *     ): StartupConfig = context.startupConfig {
 *         requirePermissions(
 *             Manifest.permission.CAMERA,
 *             Manifest.permission.WRITE_EXTERNAL_STORAGE,
 *             required = true
 *         )
 *         
 *         requireConsent(required = true)
 *         
 *         addTasks(
 *             DatabaseInitTask(),
 *             NetworkConfigTask(),
 *             CacheWarmupTask()
 *         )
 *         
 *         setNextIntent { context ->
 *             Intent(context, MainActivity::class.java)
 *         }
 *     }
 * }
 */
