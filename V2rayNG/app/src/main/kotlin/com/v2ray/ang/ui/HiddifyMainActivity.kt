package com.v2ray.ang.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.*
import android.content.res.ColorStateList
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.gson.Gson
import com.hiddify.ang.BaseFragment
import com.tapadoo.alerter.Alerter

import com.tbruyelle.rxpermissions.RxPermissions
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityHiddifyMainBinding
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.SubscriptionItem
import com.v2ray.ang.extension.*
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.ui.bottomsheets.AddConfigBottomSheets
import com.v2ray.ang.ui.bottomsheets.BottomSheetPresenter
import com.v2ray.ang.ui.bottomsheets.ProfilesBottomSheets
import com.v2ray.ang.ui.bottomsheets.SettingBottomSheets
import com.v2ray.ang.util.*
import com.v2ray.ang.viewmodel.HiddifyMainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min


class HiddifyMainActivity(val hiddifyMainViewModel: HiddifyMainViewModel) : BaseFragment(), /*NavigationView.OnNavigationItemSelectedListener,*/
    AddConfigBottomSheets.Callback, ProfilesBottomSheets.Callback,SettingBottomSheets.Callback,HiddifyAdvancedFragment.Callback {
    private var state: String=""
    private lateinit var binding: ActivityHiddifyMainBinding
    private val subStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SUB, MMKV.MULTI_PROCESS_MODE) }
    private val adapter by lazy { HiddifyMainRecyclerAdapter(this, this.hiddifyMainViewModel) }
    private val mainStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }
    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            startV2Ray()
        }
    }
    private var connect_mode=HiddifyUtils.getMode();//1=smart 2=loadbalance 3=manual
    private var mItemTouchHelper: ItemTouchHelper? = null

    private val bottomSheetPresenter = BottomSheetPresenter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val proxies=(requireActivity() as HomeActivity).getProxyDataFromIntent(requireActivity().intent)
        handleDeepLink(proxies)
    }
    fun setTitleVersion(){
        val title=requireActivity().getString(R.string.title_hiddify)

        val spannableString = SpannableString(title+" "+ BuildConfig.VERSION_NAME.toPersianDigit(activity))

        spannableString.setSpan(RelativeSizeSpan(0.6f), title.length+1, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        spannableString.setSpan(BackgroundColorSpan(getColorEx(R.color.colorAccent)),binding.toolbar.title.length+1,spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        setTitle(spannableString)

    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)
        setTitleVersion()

        binding = ActivityHiddifyMainBinding.inflate(inflater)
        val view = binding.root
//        setContentView(view)
//        title = ""


        //val toggle = ActionBarDrawerToggle(requireActivity(), binding.drawerLayout, binding.toolbar.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        //binding.drawerLayout.addDrawerListener(toggle)
        //toggle.syncState()
        //binding.navView.setNavigationItemSelectedListener(requireActivity())
        //binding.version.text = "v${BuildConfig.VERSION_NAME} (${SpeedtestUtil.getLibVersion()})"

        setupViewModel()


        init()

        showLangDialog()

        return  binding.root
    }

    private fun showGooglePlayReview() {
        if (settingsStorage?.containsKey(AppConfig.PREF_REVIEW_TIME)==true) {
            return
        }

        settingsStorage?.encode(AppConfig.PREF_REVIEW_TIME, System.currentTimeMillis())
        val manager = ReviewManagerFactory.create(requireActivity())
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = request.result
                val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                }
            } else {
                // There was some problem, continue regardless of the result.
            }
        }
    }

    fun showLangDialog(){
        if (settingsStorage?.containsKey(AppConfig.PREF_LANGUAGE)==true || Utils.isTestDevice()) {
            showCountryDialog()
            return
        }
        MaterialAlertDialogBuilder(requireActivity(),R.style.AppTheme_ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle(R.string.title_language)
            .setCancelable(false)
            .setItems(R.array.language_select) { dialog, which ->
                val lang = resources.getStringArray(R.array.language_select_value)[which]
                settingsStorage?.encode(AppConfig.PREF_LANGUAGE, lang)
                dialog.dismiss()
                val locale=Utils.getLocale(requireActivity());
                Locale.setDefault(locale)


                val resources = context?.resources!!
                val configuration = resources.configuration
                configuration.locale = locale
                configuration.setLayoutDirection(locale)

                resources.updateConfiguration(configuration, resources.displayMetrics)
//                setContentView(R.layout.activity_hiddify_main);
//                recreate()
                requireActivity().finish();
                startActivity(requireActivity().intent);

//
//                  restartActivity();

            }
            .show()
    }
    fun checkForUpdate(){
        if (System.currentTimeMillis()-(settingsStorage?.getLong(AppConfig.PREF_UPDATE_TIME,0)?:0)>1000*60*60*24) {
            return
        }
        settingsStorage?.edit()?.putLong(AppConfig.PREF_UPDATE_TIME,System.currentTimeMillis())?.apply()

        val appUpdateManager = AppUpdateManagerFactory.create(requireActivity())
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

    // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                // requireActivity() example applies an immediate update. To apply a flexible update
                // instead, pass in AppUpdateType.FLEXIBLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                    AppUpdateType.IMMEDIATE,
                    // The current activity making the update request.
                    requireActivity(),
                    // Include a request code to later monitor requireActivity() update request.
                    0)
            }
        }
    }
    fun showCountryDialog(){
        if (settingsStorage?.containsKey(AppConfig.PREF_COUNTRY)==true) {
            return
        }
        MaterialAlertDialogBuilder(requireActivity(),R.style.AppTheme_ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle(R.string.title_country)
            .setCancelable(false)

            .setItems(R.array.country_select) { dialog, which ->
                val country = resources.getStringArray(R.array.country_select_value)[which]
                HiddifyUtils.setCountry(country)
                dialog.dismiss()
            }
            .show()
    }
    private fun init() {

        binding.pingLayout.click {
            binding.ping.text="..."
            hiddifyMainViewModel.testCurrentServerRealPing()
        }

        binding.importFromClipBoard.click {
            importClipboard()
            importConfigViaSub(HiddifyUtils.getSelectedSubId())
        }

        binding.scanQrCode.click {
            importQRcode(true)
            importConfigViaSub(HiddifyUtils.getSelectedSubId())
        }

        binding.startButtonIcon.click {

            if (hiddifyMainViewModel.isRunning.value == true || state=="loading") {
                Utils.stopVService(requireActivity())
                updateCircleState("ready")
            } else if (settingsStorage?.decodeString(AppConfig.PREF_MODE) ?: "VPN" == "VPN") {
                val intent = VpnService.prepare(requireActivity())
                if (intent == null) {
                    startV2Ray()
                } else {
                    requestVpnPermission.launch(intent)
                }
            } else {
                startV2Ray()
            }
        }


        
        binding.advanced.click {
            var current= hiddifyMainViewModel.currentSubscription()
            connect_mode=HiddifyUtils.getMode()
            bottomSheetPresenter.show(parentFragmentManager, SettingBottomSheets.newInstance(connect_mode))
        }

    }
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            R.id.setting -> {
//                // Handle custom menu item 1 click
//                bottomSheetPresenter.show(parentFragmentManager, AddConfigBottomSheets.newInstance())
//                return true
//            }
//            R.id.test -> {
//                open_old_v2ray()
//                return true
//            }
//            // Handle other custom menu items similarly
//        }
//        return super.onOptionsItemSelected(item)
//    }
    fun open_old_v2ray(){
    requireActivity().runOnUiThread{
//            val intent = Intent(requireActivity(), MainActivity::class.java)
//            startActivity(intent)
        (requireActivity() as HomeActivity).gotoFragment(1)
        }
    }
    override fun onClipBoard() {
        if(importClipboard())
            importConfigViaSub(HiddifyUtils.getSelectedSubId())
    }

    override fun onQrCode() {
        importQRcode(true)
        importConfigViaSub(HiddifyUtils.getSelectedSubId())
    }

    private fun setupViewModel() {
        hiddifyMainViewModel.updateListAction.observe(this) { index ->
            if (index >= 0) {
                adapter.notifyItemChanged(index)
            } else {
                adapter.notifyDataSetChanged()
            }
        }
        hiddifyMainViewModel.updateTestResultAction.observe(this) { setTestState(it) }
        hiddifyMainViewModel.isRunning.observe(this) { isRunning ->

            adapter.isRunning = isRunning
            if (isRunning) {
                updateCircleState("connected")
                showGooglePlayReview()
                hiddifyMainViewModel.testCurrentServerRealPing()//hiddify
            } else {
                updateCircleState("ready")
                hiddifyMainViewModel.subscriptionsAddedCheck()
            }
            hideCircle()
        }

        hiddifyMainViewModel.subscriptionsAdded.observe(this) { check ->
            if (!check) {
                updateCircleState("default")
            } else {
                val enableSubscription = hiddifyMainViewModel.currentSubscription()

                enableSubscription?.let { subscription ->
                    binding.profileName.text = subscription.second.remarks

                    binding.time.text = HiddifyUtils.timeToRelativeDate(
                        subscription.second.expire, subscription.second.total,
                        subscription.second.used, requireActivity()
                    )
                    binding.time.showGone(subscription.second.expire > (0).toLong() && (subscription.second.expire-System.currentTimeMillis()<1000L*60*60*24*1000))

                    binding.consumerTrafficValue.text = HiddifyUtils.toTotalUsedGig(
                        subscription.second.total,
                        subscription.second.used,
                        requireActivity()
                    )
                    binding.consumerTrafficValue.showGone(subscription.second.total > (0).toLong())
                    binding.consumerTraffic.showGone(subscription.second.total > (0).toLong())

                    binding.progress.progress = (subscription.second.used / 1000000000).toInt()
                    binding.progress.max = (subscription.second.total / 1000000000).toInt()
                    binding.progress.showGone(subscription.second.total > (0).toLong())
                    binding.supportLink.showGone(!subscription.second.support_link.isNullOrEmpty())
                    binding.show.click {
                        if (subscription.second.home_link.isNullOrEmpty())
                            return@click
                        Utils.openUri(requireActivity(),subscription.second.home_link)

                    }
                    binding.supportLink.click {
                        if (subscription.second.support_link.isNullOrEmpty())
                            return@click
                        Utils.openUri(requireActivity(),subscription.second.support_link)

                    }
                    binding.show.showGone(!subscription.second.home_link.isNullOrEmpty())


                    binding.profileName.click {
                        bottomSheetPresenter.show(
                            parentFragmentManager,
                            ProfilesBottomSheets.newInstance()
                        )
                    }
                    binding.addProfile.click {
                        bottomSheetPresenter.show(
                            parentFragmentManager,
                            AddConfigBottomSheets.newInstance()
                        )
                    }
                    binding.updateSubscription.click {
                        importConfigViaSub(HiddifyUtils.getSelectedSubId())
                    }
                    binding.updateSubscription.showGone(subscription.second.url.startsWith("http"))
                    binding.profileBox.show()
                } ?: also {
                    binding.profileBox.gone()
                }
                if(this.state=="default")
                    updateCircleState("ready")
            }
        }
//        hiddifyMainViewModel.subscriptionsAddedCheck()
//        hiddifyMainViewModel.startListenBroadcast()
    }





    private fun startV2Ray() {
        if (mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER).isNullOrEmpty()) {
            context.toast(R.string.no_server_selected)
            return
        }
        updateCircleState("loading")
//        context.toast(R.string.toast_services_start)
        val enableSubscription = hiddifyMainViewModel.currentSubscription()
        MmkvManager.sortByTestResults()
        enableSubscription?.let { subscription ->
            if (connect_mode != 3) {
                HiddifyUtils.setMode(connect_mode)
            }
        }

        V2RayServiceManager.startV2Ray(requireActivity())
        hideCircle()
    }


    private fun restartV2Ray() {
        if (hiddifyMainViewModel.isRunning.value == true) {
            Utils.stopVService(requireActivity())
        }
        Observable.timer(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                startV2Ray()
            }
    }

    public override fun onResume() {
        super.onResume()

        onSelectSub(HiddifyUtils.getSelectedSubId(),false)
        if (V2RayServiceManager.v2rayPoint.isRunning) {
            hiddifyMainViewModel.isRunning.value=true
            updateCircleState("connected")
            hiddifyMainViewModel.testCurrentServerRealPing()
        }





        setTitleVersion()
//        if(hiddifyMainViewModel.serverList.isEmpty())
//            bottomSheetPresenter.show(parentFragmentManager,AddConfigBottomSheets())
    }


    public override fun onPause() {
        super.onPause()
    }

    

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_add_profile->{
                bottomSheetPresenter.show(
                    parentFragmentManager,
                    AddConfigBottomSheets.newInstance()
                )
            true
        }
        R.id.import_qrcode -> {
            importQRcode(true)
            importConfigViaSub(HiddifyUtils.getSelectedSubId())
            true
        }
        R.id.import_clipboard -> {
            importClipboard()
            importConfigViaSub(HiddifyUtils.getSelectedSubId())
            true
        }
        R.id.import_manually_vmess -> {
            importManually(EConfigType.VMESS.value)
            true
        }
        R.id.import_manually_vless -> {
            importManually(EConfigType.VLESS.value)
            true
        }
        R.id.import_manually_ss -> {
            importManually(EConfigType.SHADOWSOCKS.value)
            true
        }
        R.id.import_manually_socks -> {
            importManually(EConfigType.SOCKS.value)
            true
        }
        R.id.import_manually_trojan -> {
            importManually(EConfigType.TROJAN.value)
            true
        }
        R.id.import_config_custom_clipboard -> {
            importConfigCustomClipboard()
            true
        }
        R.id.import_config_custom_local -> {
            importConfigCustomLocal()
            true
        }
        R.id.import_config_custom_url -> {
            importConfigCustomUrlClipboard()
            true
        }
        R.id.import_config_custom_url_scan -> {
            importQRcode(false)
            true
        }

//        R.id.sub_setting -> {
//            startActivity<SubSettingActivity>()
//            true
//        }

        R.id.sub_update -> {
            importConfigViaSub(HiddifyUtils.getSelectedSubId())
            true
        }

        R.id.export_all -> {
            if (AngConfigManager.shareNonCustomConfigsToClipboard(requireActivity(), hiddifyMainViewModel.serverList) == 0) {
                context.toast(R.string.toast_success)
            } else {
                context.toast(R.string.toast_failure)
            }
            true
        }

        R.id.ping_all -> {
            hiddifyMainViewModel.testAllTcping()
            true
        }

        R.id.real_ping_all -> {
            hiddifyMainViewModel.testAllRealPing()
            true
        }

        R.id.service_restart -> {
            restartV2Ray()
            true
        }

        R.id.del_all_config -> {
            AlertDialog.Builder(requireActivity()).setMessage(R.string.del_config_comfirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->

                    MmkvManager.removeSubscription(HiddifyUtils.getSelectedSubId())
                    hiddifyMainViewModel.reloadServerList()
                    hiddifyMainViewModel.reloadSubscriptionsState()
                }
                .show()
            true
        }
        R.id.del_duplicate_config-> {
            AlertDialog.Builder(requireActivity()).setMessage(R.string.del_config_comfirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    hiddifyMainViewModel.removeDuplicateServer()
                }
                .show()
            true
        }
        R.id.del_invalid_config -> {
            AlertDialog.Builder(requireActivity()).setMessage(R.string.del_config_comfirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    MmkvManager.removeInvalidServer()
                    hiddifyMainViewModel.reloadServerList()
                    hiddifyMainViewModel.reloadSubscriptionsState()
                }
                .show()
            true
        }
        R.id.sort_by_test_results -> {
            MmkvManager.sortByTestResults()
            hiddifyMainViewModel.reloadServerList()
            hiddifyMainViewModel.reloadSubscriptionsState()
            true
        }
        R.id.filter_config -> {
            hiddifyMainViewModel.filterConfig(requireActivity())
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun importManually(createConfigType : Int) {
        startActivity(
            Intent()
                .putExtra("createConfigType", createConfigType)
                .putExtra("subscriptionId", hiddifyMainViewModel.subscriptionId.value)
                .setClass(requireActivity(), ServerActivity::class.java)
        )
    }

    /**
     * import config from qrcode
     */
    private fun importQRcode(forConfig: Boolean): Boolean {
//        try {
//            startActivityForResult(Intent("com.google.zxing.client.android.SCAN")
//                    .addCategory(Intent.CATEGORY_DEFAULT)
//                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), requestCode)
//        } catch (e: Exception) {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .subscribe {
                if (it)
                    if (forConfig)
                        scanQRCodeForConfig.launch(Intent(requireActivity(), ScannerActivity::class.java))
                    else
                        scanQRCodeForUrlToCustomConfig.launch(Intent(requireActivity(), ScannerActivity::class.java))
                else
                    context.toast(R.string.toast_permission_denied)
            }
//        }
        return true
    }

    private val scanQRCodeForConfig = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            importBatchConfig(it.data?.getStringExtra("SCAN_RESULT"), selectSub = true)
        }
    }

    private val scanQRCodeForUrlToCustomConfig = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            importConfigCustomUrl(it.data?.getStringExtra("SCAN_RESULT"))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
    }
    /**
     * import config from clipboard
     */
    private fun importClipboard(): Boolean {
        try {
            val clipboard = Utils.getClipboard(requireActivity())
            showAlarmIfnotSublink(clipboard)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }
    private fun importBatchConfig(
        server: String?,
        subid: String = "",
        selectSub: Boolean,
        append: Boolean = false
    ) {

        return importBatchConfig(Utils.Response(null, server),subid,append,selectSub)
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
        if (count > 0) {
            if(selectSub) {
                HiddifyUtils.setMode(connect_mode)
                onSelectSub(subid)
            }
            hiddifyMainViewModel.testAllRealPing()
            Alerter.create(requireActivity())
                .setIcon(com.google.android.material.R.drawable.mtrl_ic_check_mark)
                .setBackgroundResource(R.drawable.bg_h_green)
                .enableSwipeToDismiss()

                .setText(R.string.import_success).show()
            hiddifyMainViewModel.reloadServerList()
            hiddifyMainViewModel.reloadSubscriptionsState()
        } else {

            Alerter.create(requireActivity())
                .setIcon(com.google.android.material.R.drawable.mtrl_ic_error)
//                .setBackgroundColorRes(com.google.android.material.R.color.design_error)
                .setBackgroundResource(R.drawable.bg_h_red)
                .enableSwipeToDismiss()
                .setText(R.string.paste_failed).show()
        }
    }

    private fun importConfigCustomClipboard(): Boolean {
        try {
            val configText = Utils.getClipboard(requireActivity())
            if (TextUtils.isEmpty(configText)) {
                context.toast(R.string.toast_none_data_clipboard)
                return false
            }
            importCustomizeConfig(Utils.Response(null, configText))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * import config from local config file
     */
    private fun importConfigCustomLocal(): Boolean {
        try {
            showFileChooser()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun importConfigCustomUrlClipboard()
            : Boolean {
        try {
            val url = Utils.getClipboard(requireActivity())
            if (TextUtils.isEmpty(url)) {
                context.toast(R.string.toast_none_data_clipboard)
                return false
            }
            return importConfigCustomUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * import config from url
     */
    private fun importConfigCustomUrl(url: String?): Boolean {
        try {
            if (!Utils.isValidUrl(url)) {
                context.toast(R.string.toast_invalid_url)
                return false
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val configText = try {
                    Utils.getUrlContentWithCustomUserAgent(url)
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Log.e(context?.packageName,e.toString())
                        Log.e(context?.packageName,e.stackTraceToString())
                        context.toast(getString(R.string.toast_failure)+" "+e.toString())
//                            context.toast("\"" + it.second.remarks + "\" " + getString(R.string.toast_failure))
                    }
                    Utils.Response(null, "")
                }
                launch(Dispatchers.Main) {
                    importCustomizeConfig(configText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * import config from sub
     */
    fun importConfigViaSub(subid: String?=null) : Boolean {
        try {
//            context.toast(R.string.title_sub_update)
            MmkvManager.decodeSubscriptions().forEach {
                if (subid!=null&&it.first!=subid)return@forEach
                if (TextUtils.isEmpty(it.first)
                    || TextUtils.isEmpty(it.second.remarks)
                    || TextUtils.isEmpty(it.second.url)
                ) {
                    return@forEach
                }
                if (!it.second.enabled) {
                    return@forEach
                }
                val url = Utils.idnToASCII(it.second.url)
                if (!Utils.isValidUrl(url)) {
                    return@forEach
                }
                Log.d(AppConfig.ANG_PACKAGE, url)
                lifecycleScope.launch(Dispatchers.IO) {
                    val configText = try {
                        Utils.getUrlContentWithCustomUserAgent(url)
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            Log.e(context?.packageName,e.toString())
                            Log.e(context?.packageName,e.stackTraceToString())
                            context.toast(getString(R.string.toast_failure)+" "+e.toString())
//                            context.toast("\"" + it.second.remarks + "\" " + getString(R.string.toast_failure))
                        }
                        return@launch
                    }
                    launch(Dispatchers.Main) {
                        importBatchConfig(configText, it.first,false)
                        hiddifyMainViewModel.testAllRealPing()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            context.toast(R.string.title_sub_update_failed)
            hiddifyMainViewModel. testAllRealPing()
            return false
        }
        return true
    }

    /**
     * show file chooser
     */
    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            chooseFileForCustomConfig.launch(Intent.createChooser(intent, getString(R.string.title_file_chooser)))
        } catch (ex: ActivityNotFoundException) {
            context.toast(R.string.toast_require_file_manager)
        }
    }

    private val chooseFileForCustomConfig = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data
        if (it.resultCode == RESULT_OK && uri != null) {
            readContentFromUri(uri)
        }
    }

    /**
     * read content from uri
     */
    private fun readContentFromUri(uri: Uri) {
        RxPermissions(requireActivity())
            .request(Manifest.permission.READ_EXTERNAL_STORAGE)
            .subscribe {
                if (it) {
                    try {
                        context?.contentResolver?.openInputStream(uri)?.use { input ->

                            importCustomizeConfig(input?.bufferedReader()?.readText())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else
                    context.toast(R.string.toast_permission_denied)
            }
    }

    /**
     * import customize config
     */
    private fun importCustomizeConfig(response: String?) {
        return importCustomizeConfig(Utils.Response(null, response))
    }

    private fun importCustomizeConfig(response: Utils.Response) {
        val server=response?.content
        try {
            if (server == null || TextUtils.isEmpty(server)) {
                context.toast(R.string.toast_none_data)
                return
            }
            hiddifyMainViewModel.appendCustomConfigServer(response)
            hiddifyMainViewModel.reloadServerList()
            hiddifyMainViewModel.reloadSubscriptionsState()
            context.toast(R.string.toast_success)
            //adapter.notifyItemInserted(hiddifyMainViewModel.serverList.lastIndex)
        } catch (e: Exception) {
            context.toast("${getString(R.string.toast_malformed_josn)} ${e.cause?.message}", Toast.LENGTH_LONG)
            e.printStackTrace()
            return
        }
    }

    private fun setTestState(content: Pair<Long,String>?) {
        //binding.tvTestState.text = content
        if (content==null)return

        var text=if (content!!.first>=0)content!!.first.toString().toPersianDigit(requireActivity())+" ms" else getString(R.string.toast_failure)
        binding.ping.text=text
    }

//    val mConnection = object : ServiceConnection {
//        override fun onServiceDisconnected(name: ComponentName?) {
//        }
//
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            sendMsg(AppConfig.MSG_REGISTER_CLIENT, "")
//        }
//    }

//    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            moveTaskToBack(false)
//            return true
//        }
//        return super.onKeyDown(keyCode, event)
//    }

    fun updateCircleState(state: String) {
        if(activity==null)return
        binding.pingLayout.showHide(state=="connected")
        binding.ping.text="..."
        this.state=state
        when(state) {
            "loading" -> {
                binding.importButtons.gone()
                binding.startButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_circle_connecting)

                binding.startButtonIcon.imageTintList = ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorYellow))
                binding.connectState.text = getString(R.string.connecting)
                binding.connectState.setTextColor(ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorYellow)))
                binding.advanced.setTextColor(ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorText)))
                binding.advanced.iconTint = ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorText))
                binding.advanced.isEnabled = true
            }
            "connected" -> {
                binding.importButtons.gone()

                binding.startButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_circle_connect)
                binding.startButtonIcon.imageTintList = ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorGreen))
                binding.connectState.text = getString(R.string.connected)
                binding.connectState.setTextColor(ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorGreen)))
                binding.advanced.setTextColor(ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorText)))
                binding.advanced.iconTint = ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorText))
                binding.advanced.isEnabled = true
            }
            "ready" -> {
                binding.importButtons.gone()
                binding.startButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_circle_ready)
//                binding.startButton.backgroundTintList= ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorPrimary2))
//                binding.startButton.backgroundTintBlendMode=BlendMode.MULTIPLY;
                binding.startButtonIcon.imageTintList = ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorPrimary2))



                binding.connectState.text = getString(R.string.tab_to_connect)
                binding.connectState.setTextColor(ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorPrimary2)))
//                binding.advanced.setTextColor(ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorText)))
//                binding.advanced.iconTint = ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorText))
                binding.advanced.isEnabled = true
            }
            else -> {
                binding.importButtons.show()
                binding.startButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_circle_default)
                binding.startButtonIcon.imageTintList = ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorDisable))
                binding.connectState.text = getString(R.string.default_layout_description)
                binding.connectState.setTextColor(ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorPrimary2)))
//                binding.advanced.setTextColor(ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorBorder)))
//                binding.advanced.iconTint = ColorStateList.valueOf(requireActivity().getColorEx(R.color.colorBorder))
                binding.advanced.isEnabled = false
                binding.profileBox.gone()
            }
        }
    }

    fun hideCircle() {
        try {
            Observable.timer(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    try {
//                            if (binding.fabProgressCircle.isShown) {
//                                binding.fabProgressCircle.hide()
//                            }
                    } catch (e: Exception) {
                        Log.w(AppConfig.ANG_PACKAGE, e)
                    }
                }
        } catch (e: Exception) {
            Log.d(AppConfig.ANG_PACKAGE, e.toString())
        }
    }

    /*override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            //R.id.server_profile -> activityClass = MainActivity::class.java
            R.id.sub_setting -> {
                startActivity(Intent(requireActivity(), SubSettingActivity::class.java))
            }
            R.id.settings -> {
                startActivity(Intent(requireActivity(), SettingsActivity::class.java)
                    .putExtra("isRunning", hiddifyMainViewModel.isRunning.value == true))
            }
            R.id.user_asset_setting -> {
                startActivity(Intent(requireActivity(), UserAssetActivity::class.java))
            }
            R.id.feedback -> {
                Utils.openUri(requireActivity(), AppConfig.v2rayNGIssues)
            }
            R.id.promotion -> {
                Utils.openUri(requireActivity(), "${Utils.decode(AppConfig.promotionUrl)}")
            }
            R.id.logcat -> {
                startActivity(Intent(requireActivity(), LogcatActivity::class.java))
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
*/
    override fun onAddProfile() {
        bottomSheetPresenter.show(parentFragmentManager, AddConfigBottomSheets.newInstance())
    }

    override fun onImportQrCode() {
        importQRcode(true)
        importConfigViaSub(HiddifyUtils.getSelectedSubId())
    }

    override fun onSelectSub(subid: String) {
        onSelectSub(subid,true)
    }

    override fun onUpdateSubList() {
        importConfigViaSub()
    }

    fun onSelectSub(subid: String,do_ping:Boolean=true) {
        if (HiddifyUtils.getSelectedSubId()!=subid) {
            HiddifyUtils.setSelectedSub(subid)
            HiddifyUtils.setMode(connect_mode)
        }

        hiddifyMainViewModel.subscriptionId.value = subid


        hiddifyMainViewModel.reloadServerList()
        hiddifyMainViewModel.reloadSubscriptionsState()

        val enableSubscription = hiddifyMainViewModel.currentSubscription()
        if(HiddifyUtils.isInternetConnected(requireContext())) {
            if (enableSubscription?.second?.needUpdate() == true) {
                importConfigViaSub(HiddifyUtils.getSelectedSubId())
            } else if (do_ping) {
                hiddifyMainViewModel.testAllRealPing()
            }
        }else{
            Alerter.create(requireActivity())
                .setIcon(com.google.android.material.R.drawable.mtrl_ic_error)
                .setBackgroundResource(R.drawable.bg_h_red)
                .enableSwipeToDismiss()
                .setText(R.string.no_internet).show()
        }

    }

    override fun onRemoveSelectSub(subid: String) {
        if (subid=="default")return
        Alerter.create(requireActivity()).setTitle(R.string.del_config_comfirm)
            .addButton(getText(android.R.string.ok)) {
                MmkvManager.removeSubscription(subid)
                if (subid == HiddifyUtils.getSelectedSubId())
                    HiddifyUtils.setSelectedSub("default")
                hiddifyMainViewModel.reloadServerList()
                hiddifyMainViewModel.reloadSubscriptionsState()
            }
            .addButton(getText(android.R.string.no)) {

            }
            .show()

    }


    override fun onModeChange(mode: Int) {
        connect_mode=mode;
        if (mode==3){
            bottomSheetPresenter.dismiss()
            open_old_v2ray()
        }else{
            restartV2Ray()
        }
        Alerter.create(requireActivity())
            .setTitle(R.string.toast_success)
            .setIcon(R.drawable.ic_fab_check)
            .setBackgroundResource(R.drawable.bg_h_green)
            .show()
    }

    override fun onPerAppProxyModeChange(mode: HiddifyUtils.PerAppProxyMode) {
        HiddifyUtils.setPerAppProxyMode(mode)
        restartV2Ray()
        Alerter.create(requireActivity())
            .setTitle(R.string.toast_success)
            .setIcon(R.drawable.ic_fab_check)
            .setBackgroundResource(R.drawable.bg_h_green)
            .show()
    }

    override fun onFragmentModeChange(mode: HiddifyUtils.FragmentMode) {
        HiddifyUtils.setFragmentMode(mode)
        restartV2Ray()
        Alerter.create(requireActivity())
            .setTitle(R.string.toast_success)
            .setIcon(R.drawable.ic_fab_check)
            .setBackgroundResource(R.drawable.bg_h_green)
            .show()
    }

    @SuppressLint("ResourceAsColor")
    fun showAlarmIfnotSublink(content1: String) {
        if (content1.isNullOrEmpty()){
//            context.toast(R.string.title_sub_update_failed)
            Alerter.create(requireActivity())
                .setTitle(R.string.nothing_in_clipboard)
                .setText(content1)
                .setIcon(com.google.android.material.R.drawable.mtrl_ic_error)
                .show()
            return
        }
        Alerter.create(requireActivity())
            .setTitle(R.string.import_from_link)
            .setText(content1.substring(0, min(100,content1.length)))
            .enableProgress(true)
            .show()


        var content=if(content1.startsWith("hiddify"))Uri.parse(content1.trim()).getQueryParameter("url")?:"" else content1.trim()
        if (content.startsWith("http")){
            var subid=MmkvManager.importUrlAsSubscription(content)
            onSelectSub(subid)
//            importConfigViaSub(subid)
            return
        }
        if(content1.length<20) {
            Alerter.create(requireActivity())
                .setIcon(com.google.android.material.R.drawable.mtrl_ic_error)
                .setBackgroundResource(R.drawable.bg_h_red)
                .enableSwipeToDismiss()
                .setText(R.string.paste_failed).show()
            return
        }
        val subscriptions = MmkvManager.decodeSubscriptions().filter { it.second.enabled &&!Utils.isValidUrl(it.second.url) }
        val listId = subscriptions.map { it.first }.toList().toMutableList()
        val listRemarks = subscriptions.map { it.second.remarks }.toList().toMutableList()
        listId.add(0,"")
        listRemarks.add(0,getString(R.string.new_item))
        val context=requireActivity()
        var actv = Spinner(context)
        var ll=LinearLayout(context)
        ll.orientation=LinearLayout.VERTICAL
        val tv=TextView(context)
        tv.setText(R.string.no_sublink_found)
        ll.addView(tv)
        val params = tv.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(24, 0, 24, 0)
        tv.layoutParams = params
        ll.addView(actv)
        val customName=TextInputEditText(requireActivity())
        ll.addView(customName)
        customName.hint=getString(R.string.msg_enter_group_name)
        customName.visibility=View.GONE
        actv.setAdapter(ArrayAdapter<String>( context, android.R.layout.simple_spinner_dropdown_item, listRemarks))
        var selectedSubid=""
        actv.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle the selection of an item
//                val selectedItem = parent?.getItemAtPosition(position).toString()

                customName.visibility=if (position==0)View.VISIBLE else View.GONE

                selectedSubid=listId[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case where nothing is selected
            }
        }
            val selectedIndex=listId.indexOf(HiddifyUtils.getSelectedSubId())
            if (selectedIndex>=0)
                actv.setSelection(selectedIndex)

            //        actv.threshold = 1

    //        actv.completionHint=getString(R.string.msg_enter_keywords)
    //        actv.hint=getString(R.string.msg_enter_group_name)


            val builder = AlertDialog.Builder(context).setView(ll)
            builder.setTitle(R.string.autoconfig_link_not_found)
            builder.setPositiveButton(R.string.tasker_setting_confirm) { dialogInterface: DialogInterface?, _: Int ->
                try {
                    var selected_sub= if (selectedSubid.isNullOrEmpty())
                        Pair(Utils.getUuid(),SubscriptionItem(remarks = customName.text.toString()))
                    else
                        subscriptions.find { it.first==selectedSubid }!!
                    if (selectedSubid.isNullOrEmpty()){
                        subStorage?.encode(selected_sub.first, Gson().toJson(selected_sub.second))
                        hiddifyMainViewModel. reloadServerList()
                    }
//                    onSelectSub(selected_sub.first)
                    importBatchConfig(content, selected_sub.first,append = true, selectSub = true)
                    dialogInterface?.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            builder.show()


        }

    override fun handleDeepLink(shareUrl: String?) {
        if(!shareUrl.isNullOrEmpty())
            showAlarmIfnotSublink(shareUrl)
        super.handleDeepLink(shareUrl)
    }

}
