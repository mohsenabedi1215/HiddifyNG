package com.hiddify.ang.downloader

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.util.HiddifyUtils
import com.v2ray.ang.util.Utils
import java.net.URL
import java.nio.charset.Charset
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine

class OldDownloader:HTTPDownloader {
    override fun download(urlStr: String, timeout: Int): Utils.Response {
        try {
            return _download_proxy(urlStr, timeout,false)
        }catch (e1:java.lang.Exception){
            try {
                if(V2RayServiceManager.v2rayPoint.isRunning)
                    return _download_proxy(urlStr, timeout,true)
            }catch (e:java.lang.Exception){

            }
            throw e1
        }
    }
    fun _download_proxy(urlStr: String, timeout: Int,proxy:Boolean): Utils.Response {
        if(urlStr.startsWith("https")) {
            try {
                return _download(urlStr, timeout, "TLSv1.3",proxy=proxy)
            }catch (e:java.lang.Exception){
                return _download(urlStr, timeout, "TLSv1.2",proxy=proxy)
            }

        }
        return _download(urlStr, timeout, null,proxy=proxy)
    }
    fun _download(urlStr: String, timeout: Int,tls:String?,proxy:Boolean): Utils.Response {
        Log.d(ANG_PACKAGE,"Downlaoding by tls=$tls proxy=$proxy")
        try {
            val url = URL(urlStr)
            val conn = if (proxy)
                    url.openConnection(HiddifyUtils.socksProxy())
                else
                    url.openConnection()
            if(tls!=null) {
                val sc: SSLContext = SSLContext.getInstance(tls)
                sc.init(null, null, SecureRandom())
                sc.getProtocol()
                sc.getSupportedSSLParameters()
                sc.getDefaultSSLParameters()
                val engine: SSLEngine = sc.createSSLEngine()
                (conn as HttpsURLConnection).sslSocketFactory=sc.socketFactory
            }

            conn.connectTimeout = timeout.toInt()
            conn.readTimeout = timeout.toInt()
            conn.setRequestProperty("Connection", "close")
            conn.setRequestProperty("User-agent", "HiddifyNG/${BuildConfig.VERSION_NAME}")
            url.userInfo?.let {
                conn.setRequestProperty(
                    "Authorization",
                    "Basic ${Utils.encode(Utils.urlDecode(it))}"
                )
            }
            conn.useCaches = false





            val headers = conn.headerFields

            val contentBytes = conn.inputStream.use {
                it.readBytes()
            }
            val content=String(contentBytes, try{
                Charset.forName(conn.contentEncoding)}catch (e:Exception){Charsets.UTF_8})

            return Utils.Response(headers, content, urlStr, contentBytes)
        }catch (e:Exception){
            Log.e(AppConfig.ANG_PACKAGE,"Traditional download way also failed!!!")
            Log.e(AppConfig.ANG_PACKAGE,e.toString())
            Log.e(AppConfig.ANG_PACKAGE,e.stackTraceToString())
            throw  e
        }
    }
}