//package com.hiddify.ang.downloader
//
//import android.util.Log
//import com.v2ray.ang.AppConfig
//import com.v2ray.ang.BuildConfig
//import com.v2ray.ang.service.V2RayServiceManager
//import com.v2ray.ang.util.HiddifyUtils
//import com.v2ray.ang.util.Utils
//import okhttp3.*
//import okhttp3.internal.immutableListOf
//import org.http4k.client.*
//import java.net.Inet4Address
//import java.net.InetAddress
//import java.net.URL
//import java.security.NoSuchAlgorithmException
//import java.util.concurrent.TimeUnit
//import javax.net.ssl.HostnameVerifier
//import javax.net.ssl.SSLContext
//import javax.net.ssl.SSLSession
//import javax.net.ssl.SSLSocketFactory
//
//class OkHTTPDownloader:HTTPDownloader {
//
//
//    override fun download(urlStr:String,timeout: Int): Utils.Response {
//        val HIDDIFY_TLS = ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
//            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
//            .build()
//        val timeout=timeout.toLong()
//        val clients= mutableListOf(
//
//            OkHttpClient.Builder(),
//            if(V2RayServiceManager.v2rayPoint.isRunning)
//            OkHttpClient.Builder().proxy(HiddifyUtils.socksProxy())else null,
//        )
//        var error:java.lang.Exception?=null;
//
//        for (client in clients){
//            if (client==null)continue
//            try{
//                client.readTimeout(timeout, TimeUnit.MILLISECONDS)
//                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
//                .protocols(immutableListOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
//                .connectTimeout(timeout/4, TimeUnit.MILLISECONDS)
//                .dns(PreferIPv4DnsSelector())
////                    .socketFactory(TLSSocketFactory.socketFactory)
////                .hostnameVerifier(CustomHostnameVerifier())
//                .connectionSpecs(mutableListOf(
//                        HIDDIFY_TLS,
//                        ConnectionSpec.CLEARTEXT
//                    ))
//
//                return _download(urlStr,client.build())
//            }catch (e:java.lang.Exception){
//                error=e
//            }
//        }
//        throw error!!
//    }
//    fun _download(urlStr: String, client: OkHttpClient): Utils.Response {
//
//
//
//
//        // Create URL
//        val url = URL(urlStr)
//        // Build request
//        val requestBuilder = okhttp3.Request.Builder()
//            .url(url)
//                .header("User-Agent", "HiddifyNG/${BuildConfig.VERSION_NAME}")
//
//                .header("Connection", "close")
//        url.userInfo?.let {
//            requestBuilder.header("Authorization", "Basic ${Utils.encode(Utils.urlDecode(it))}")
//        }
//
//        val request = requestBuilder.build()
//        // Execute request
//        val response = client.newCall(request).execute()
//        val headers = response.headers.toMultimap()
//        val contentBytes=response.body?.bytes()
//        if(contentBytes==null||contentBytes.isEmpty() || !response.isSuccessful) {
//            Log.e(AppConfig.ANG_PACKAGE,"download not success!! ${response.isSuccessful}  ")
//            throw Exception("No Content")
//        }
//        val content=String(contentBytes!!, response.body?.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
////            val content = response.body?.string()
//
//        response.close()
//
//        return Utils.Response(headers, content, urlStr, contentBytes)
//
//    }
//    class PreferIPv4DnsSelector() : Dns {
//        override fun lookup(hostname: String): List<InetAddress> {
//            val alldns= Dns.SYSTEM.lookup(hostname);
//            val filtered=alldns.filter { Inet4Address::class.java.isInstance(it) }
//            return filtered.ifEmpty { alldns }
//        }
//    }
//    class CustomHostnameVerifier : HostnameVerifier {
//        override fun verify(hostname: String?, session: SSLSession?): Boolean {
//            // Implement your custom hostname verification logic here
//            // Return true if the hostname is considered valid, false otherwise
//
//            // Example: Allowing all hostnames (not recommended for production)
//            return true
//        }
//    }
//
//    object TLSSocketFactory {
//        @get:Throws(NoSuchAlgorithmException::class)
//        val socketFactory: SSLSocketFactory
//            get() {
//                val sslContext: SSLContext = SSLContext.getInstance("TLSv1.3")
//                sslContext.init(null, null, null)
//                return sslContext.getSocketFactory()
//            }
//    }
//}