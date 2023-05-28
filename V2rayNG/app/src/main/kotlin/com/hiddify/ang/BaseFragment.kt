package com.hiddify.ang

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.v2ray.ang.ui.BaseActivity
import com.v2ray.ang.ui.HomeActivity
import com.v2ray.ang.util.Utils
import java.nio.charset.Charset

open class BaseFragment: Fragment() {

    fun setTitle(title:CharSequence){
        (requireActivity() as BaseActivity)?.supportActionBar?.title=title
    }

    open fun onTitleClick(){
        Utils.openUri(requireActivity(),"https://play.google.com/store/apps/details?id=ang.hiddify.com")
    }

    open fun handleDeepLink(shareUrl:String?) {

    }

    fun homeActivity(): HomeActivity {
        return super.requireActivity() as HomeActivity
    }

}