package app.nanotv.tv

import android.app.Application
import app.nanotv.tv.data.local.AppPreferences
import app.nanotv.tv.data.local.NanoTvDatabase
import app.nanotv.tv.data.repository.CatalogRepository
import app.nanotv.tv.server.LocalCurationServer
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NanoTvApp : Application() {

    lateinit var repository: CatalogRepository
        private set

    lateinit var curationServer: LocalCurationServer
        private set

    override fun onCreate() {
        super.onCreate()

        val okHttp = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val database = NanoTvDatabase.getInstance(this)
        val preferences = AppPreferences(this)

        repository = CatalogRepository(
            dao = database.catalogDao(),
            httpClient = okHttp,
            preferences = preferences
        )

        // Port 8485 so NanoTV and personal builds can even run side-by-side without port conflict
        curationServer = LocalCurationServer(
            context = applicationContext,
            repository = repository,
            port = 8485
        )
        curationServer.start()
    }
}
