package com.hiddify.ang.downloader

import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.util.Utils

import java.net.URL
import java.nio.charset.Charset

class MixedDownloader:HTTPDownloader {
    override fun download(urlStr: String, timeout: Int): Utils.Response {
        val clients= mutableListOf(
            OldDownloader(),
//            CURLDownloader(),
//            KHttpDownloader(),
//            OkHTTPDownloader(),
//            HTTP4KDownloader(),

        )
        var error:java.lang.Exception?=null;
        for (client in clients){
            try{
                Log.d(ANG_PACKAGE,"Downloading using "+client.javaClass.name)
                return client.download(urlStr,timeout)
            }catch (e:java.lang.Exception){
                Log.e(ANG_PACKAGE,client.javaClass.name+" Error "+e.toString())
                error=e
            }
        }
        throw error!!
    }
}