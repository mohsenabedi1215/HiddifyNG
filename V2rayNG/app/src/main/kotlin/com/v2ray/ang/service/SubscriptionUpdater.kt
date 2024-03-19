package com.v2ray.ang.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tapadoo.alerter.Alerter
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.HiddifyUtils
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SubscriptionUpdater {

    const val notificationChannel = "subscription_update_channel"

    class UpdateTask(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

        private val notificationManager = NotificationManagerCompat.from(applicationContext)
        private val notification =
            NotificationCompat.Builder(applicationContext, notificationChannel)
                .setWhen(0)
                .setTicker("Update")
                .setContentTitle(context.getString(R.string.title_pref_auto_update_subscription))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        @SuppressLint("MissingPermission")
        override suspend fun doWork(): Result {
            Log.d(AppConfig.ANG_PACKAGE, "subscription automatic update starting")

            val subs = MmkvManager.decodeSubscriptions().filter { it.second.autoUpdate }

            for (i in subs) {
                val subscription = i.second

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notification.setChannelId(notificationChannel)
                    val channel =
                        NotificationChannel(
                            notificationChannel,
                            "Subscription Update Service",
                            NotificationManager.IMPORTANCE_MIN
                        )
                    notificationManager.createNotificationChannel(channel)
                }
                notificationManager.notify(3, notification.build())
                Log.d(
                    AppConfig.ANG_PACKAGE,
                    "subscription automatic update: ---${subscription.remarks}"
                )
                val configText = try {
                    Utils.getUrlContentWithCustomUserAgent(subscription.url)
                } catch (e: Exception) {
                        notification.setContentText("Updating Failed ${subscription.remarks}")
                        Log.e(AppConfig.ANG_PACKAGE,e.toString(),e)
                    return Result.failure()
                }

                    importBatchConfig(configText, i.first,false)


                notification.setContentText("Updating ${subscription.remarks}")
            }
            notificationManager.cancel(3)
            return Result.success()
        }
    }
    private fun importBatchConfig(
        response: Utils.Response?,
        subid: String = "",
        selectSub: Boolean,
        append: Boolean = false
    ) {
        var server=response?.content
        val subid2 = if(subid.isNullOrEmpty()){
            if (server?.startsWith("http") == true)"" else "default"
        }else{
            subid
        }
        HiddifyUtils.extract_package_info_from_response(response,subid)

        val append = append||subid.isNullOrEmpty() || subid=="default"
        var count = AngConfigManager.importBatchConfig(server, subid2, append, selectSub = selectSub)
        if (count <= 0) {
            count = AngConfigManager.importBatchConfig(Utils.decode(server!!), subid2, append, selectSub = selectSub)
        }

        if (count <= 0) {
            AngConfigManager.appendCustomConfigServer(server, subid)
        }
    }

}