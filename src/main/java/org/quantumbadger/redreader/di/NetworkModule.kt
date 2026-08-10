/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.di

import android.content.Context
import com.squareup.okhttp3.Call
import com.squareup.okhttp3.OkHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.EventListener
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing network-related dependencies.
 * Provides OkHttpClient with appropriate configuration and HTTPBackend.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .eventListenerFactory(OkHttpLoggerFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideHttpBackend(okHttpClient: OkHttpClient): HTTPBackend {
        return HTTPBackend(okHttpClient)
    }
}

/**
 * Factory for creating OkHttp EventListeners for logging.
 */
class OkHttpLoggerFactory : EventListener.Factory {
    override fun create(call: Call): EventListener {
        return OkHttpLogger()
    }
}

/**
 * Custom EventListener for logging HTTP requests.
 */
class OkHttpLogger : EventListener() {
    override fun connectFailed(
        call: Call,
        proxy: Proxy,
        addr: InetSocketAddress,
        connectException: java.io.IOException
    ) {
        // Log connection failure
    }
}
