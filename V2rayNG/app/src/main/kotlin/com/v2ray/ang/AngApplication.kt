package com.v2ray.ang

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging

import androidx.work.Configuration
import com.tencent.mmkv.MMKV
import com.v2ray.ang.util.HiddifyUtils
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import org.conscrypt.Conscrypt
//import org.conscrypt.Conscrypt
import java.io.File
import java.io.FileOutputStream
import java.security.Security



class AngApplication : MultiDexApplication(), Configuration.Provider {
    companion object {
        const val PREF_LAST_VERSION = "pref_last_version"
        lateinit var appContext: Context
                private set

        lateinit var application: AngApplication
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        application = this
    }

//    var update = false
//        private set

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
//        LeakCanary.install(this)
//        FirebaseApp.initializeApp(this);
//        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
//        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(this))
//        Firebase.messaging.subscribeToTopic("all")

        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val update = defaultSharedPreferences.getInt(PREF_LAST_VERSION, 0) != BuildConfig.VERSION_CODE
        if (update) {
            copyAssets()
            defaultSharedPreferences.edit().putInt(PREF_LAST_VERSION, BuildConfig.VERSION_CODE).apply()
        }

        //Logger.init().logLevel(if (BuildConfig.DEBUG) LogLevel.FULL else LogLevel.NONE)
        MMKV.initialize(this)
        MmkvManager.getDefaultSubscription()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 0)
        }
        HiddifyUtils.setDarkMode()
//        System.setProperty("javax.net.debug", "ssl")

//        val sslContext: SSLContext = SSLContext.getInstance("TLSv1.3")
//        sslContext.init(null, null, null)
//        val engine: SSLEngine = sslContext.createSSLEngine()
    }


    private fun copyAssets() {
            val extFolder = Utils.userAssetPath(this)
//        lifecycleScope.launch(Dispatchers.IO){
            try {
                val geo = arrayOf("geosite.dat", "geoip.dat")
                assets.list("")
                    ?.filter { geo.contains(it) }
//                    ?.filter { !File(extFolder, it).exists() || File(extFolder, it).lastModified()<=File(it).lastModified()}
                    ?.forEach {
                        val target = File(extFolder, it)
                        assets.open(it).use { input ->
                            FileOutputStream(target).use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.i(AppConfig.ANG_PACKAGE, "Copied from apk assets folder to ${target.absolutePath}")
                    }
            } catch (e: Exception) {
                Log.e(AppConfig.ANG_PACKAGE, "asset copy failed", e)
            }

    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setDefaultProcessName("${BuildConfig.APPLICATION_ID}:bg")
            .build()
    }
}
