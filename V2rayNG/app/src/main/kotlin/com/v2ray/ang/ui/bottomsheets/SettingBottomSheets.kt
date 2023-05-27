package com.v2ray.ang.ui.bottomsheets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

import com.hiddify.ang.speedtest.SpeedTestActivity
import com.v2ray.ang.R
import com.v2ray.ang.databinding.BottomsheetSettingBinding
import com.v2ray.ang.extension.click
import com.v2ray.ang.ui.PerAppProxyActivity
import com.v2ray.ang.util.CallbackUtil
import com.v2ray.ang.util.HiddifyUtils
import com.v2ray.ang.util.Utils

class SettingBottomSheets(var mode: Int) : BaseExpandedBottomSheet() {
    public lateinit var binding: BottomsheetSettingBinding

    interface Callback {
        fun onModeChange(mode: Int)
        fun onPerAppProxyModeChange(mode: HiddifyUtils.PerAppProxyMode)
        fun onFragmentModeChange(mode: HiddifyUtils.FragmentMode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogCurveStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        inflater.inflate(R.layout.bottomsheet_setting, container, false)
        binding = BottomsheetSettingBinding.inflate(inflater, container, false)

        binding.connectToggleButton.check(when (mode) {
            1 -> binding.smart.id
            2 -> binding.loadBalance.id
            else -> binding.manual.id
        })
        binding.connectToggleButton.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                mode = when (checkedId) {
                    binding.smart.id ->1
                    binding.loadBalance.id ->2
                    else ->3
                }
                callback()?.onModeChange(mode)
            }else if (group.checkedButtonIds.isEmpty()) {
                // No buttons are selected, select the first one by default

                group.check(when (mode) {
                    1 -> binding.smart.id
                    2 -> binding.loadBalance.id
                    else -> binding.manual.id
                })
            }
        }







        binding.speedtest.click{
            //SpeedTester.showSpeedTestDialog(this)
            startActivity(Intent(context, SpeedTestActivity::class.java))
        }


        binding.proxyToggleButton.check(when (HiddifyUtils.getPerAppProxyMode()) {
            HiddifyUtils.PerAppProxyMode.NotOpened -> binding.notOpened.id
            HiddifyUtils.PerAppProxyMode.Blocked -> binding.filteredSites.id
            else -> binding.sitesAll.id
        })
        binding.proxyToggleButton.setOnLongClickListener{
            startActivity(Intent(activity, PerAppProxyActivity::class.java))
            dialog?.dismiss()
            true;
        }
        binding.sitesAll.setOnLongClickListener{
            HiddifyUtils.setPerAppProxyMode(HiddifyUtils.PerAppProxyMode.NotOpened)
            startActivity(Intent(activity, PerAppProxyActivity::class.java))
            dialog?.dismiss()
            true;
        }
        binding.notOpened.setOnLongClickListener{
            HiddifyUtils.setPerAppProxyMode(HiddifyUtils.PerAppProxyMode.NotOpened)
            startActivity(Intent(activity, PerAppProxyActivity::class.java))
            dialog?.dismiss()
            true;
        }
        binding.filteredSites.setOnLongClickListener{
            HiddifyUtils.setPerAppProxyMode(HiddifyUtils.PerAppProxyMode.Blocked)
            startActivity(Intent(activity, PerAppProxyActivity::class.java))
            dialog?.dismiss()
            true;
        }
        binding.proxyToggleButton.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    binding.notOpened.id->HiddifyUtils.PerAppProxyMode.NotOpened
                    binding.filteredSites.id->HiddifyUtils.PerAppProxyMode.Blocked
                    else -> HiddifyUtils.PerAppProxyMode.Global
                }
                callback()?.onPerAppProxyModeChange(mode)
            }else if (group.checkedButtonIds.isEmpty()) {
                // No buttons are selected, select the first one by default

                group.check(when (HiddifyUtils.getPerAppProxyMode()) {
                    HiddifyUtils.PerAppProxyMode.NotOpened -> binding.notOpened.id
                    HiddifyUtils.PerAppProxyMode.Blocked -> binding.filteredSites.id
                    else -> binding.sitesAll.id
                })
            }
        }
        binding.fragment.check(when (HiddifyUtils.getFragmentMode()) {
            HiddifyUtils.FragmentMode.SNI -> binding.fragmentSni.id
            HiddifyUtils.FragmentMode.Random -> binding.fragmentRandom.id
            else -> binding.fragmentDefault.id
        })
        binding.fragment.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    binding.fragmentSni.id->HiddifyUtils.FragmentMode.SNI
                    binding.fragmentRandom.id->HiddifyUtils.FragmentMode.Random
                    else -> HiddifyUtils.FragmentMode.Default
                }
                callback()?.onFragmentModeChange(mode)
            }else if (group.checkedButtonIds.isEmpty()) {
                // No buttons are selected, select the first one by default
//                callback()?.onFragmentModeChange(mode)
            }
        }

        binding.proxymodeHelp.click {
            Utils.openUri(requireContext(),requireContext().getString(R.string.proxymode_help_url))
        }
        binding.fragmentHelp.click {
            Utils.openUri(requireContext(),requireContext().getString(R.string.fragment_help_url))
        }
        binding.connectionModeHelp.click {
            Utils.openUri(requireContext(),requireContext().getString(R.string.connection_mode_help_url))
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun callback(): Callback? {
        return CallbackUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        fun newInstance(mode: Int): SettingBottomSheets {
            return SettingBottomSheets(mode)
        }
    }
}
