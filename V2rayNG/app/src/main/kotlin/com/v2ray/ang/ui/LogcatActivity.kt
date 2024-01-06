package com.v2ray.ang.ui

import android.content.Intent
import android.net.Uri
import android.os.*
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityLogcatBinding
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException


class LogcatActivity : BaseActivity() {
    private lateinit var binding: ActivityLogcatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogcatBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        title = getString(R.string.title_logcat)

        logcat(false)
    }

    private fun logcat(shouldFlushLog: Boolean) {

        try {
            binding.pbWaiting.visibility = View.VISIBLE

            lifecycleScope.launch(Dispatchers.Default) {
                if (shouldFlushLog) {
                    val lst = LinkedHashSet<String>()
                    lst.add("logcat")
                    lst.add("-c")
                    val process = Runtime.getRuntime().exec(lst.toTypedArray())
                    process.waitFor()
                }
                val lst = LinkedHashSet<String>()
                lst.add("logcat")
                lst.add("-d")
                lst.add("-v")
                lst.add("time")
                lst.add("-s")
                lst.add("GoLog,tun2socks,${ANG_PACKAGE},AndroidRuntime,System.err")
                val process = Runtime.getRuntime().exec(lst.toTypedArray())
//                val bufferedReader = BufferedReader(
//                        InputStreamReader(process.inputStream))
//                val allText = bufferedReader.use(BufferedReader::readText)
                var allText = process.inputStream.bufferedReader().use { it.readText() }
                var regex=Regex("""\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3}\s+([A-Z]\/[A-z.-]*)\s*\(?\d*\)?:?\|?\s*""")
                val regex_xray = Regex("""^\d{4}/\d{2}/\d{2}\s+\d{2}:\d{2}:\d{2}\s*""")

                var regex2=Regex("""  +""")
                allText=allText.replace(regex,"").replace(regex_xray,"").replace(regex2,"")
                send2telegram(allText)
                launch(Dispatchers.Main) {
                    binding.tvLogcat.text =allText
                    binding.tvLogcat.textDirection=View.TEXT_DIRECTION_LTR
                    binding.tvLogcat.movementMethod = ScrollingMovementMethod()
                    binding.tvLogcat.scrollTo(0,binding.tvLogcat.length())
                    binding.pbWaiting.visibility = View.GONE
                    Handler(Looper.getMainLooper()).post { binding.svLogcat.fullScroll(View.FOCUS_DOWN) }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun send2telegram(allText: String) {
        val filename = "logcat${BuildConfig.VERSION_CODE}-${Build.VERSION.SDK_INT}.txt"
        //FileOutputStream fos_FILE_eulerY = null;
        //FileOutputStream fos_FILE_eulerY = null;
        val externalFilesDirectory: File? = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val textFile = File(externalFilesDirectory, filename)
        val message = allText
        try {
            //fos_FILE_eulerY = openFileOutput(textFile.getAbsolutePath() , MODE_PRIVATE);
            //fos_FILE_eulerY.write(message.getBytes());
            val writer = FileWriter(textFile)
            writer.append(message)
            writer.flush()
            writer.close()
            val fileUri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", textFile)
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // set the type to 'email'
            // set the type to 'email'
            emailIntent.type = "text/plain"
            // the attachment
            // the attachment
            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
            // the mail subject
            // the mail subject
            startActivity(Intent.createChooser(emailIntent, "Send logcat ..."))
        } catch (e: Exception) {
            e.localizedMessage
            e.printStackTrace()
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, allText)
            intent.type = "text/plain"
            val chooserTitle = "Send Logcat to..."
            val chooser = Intent.createChooser(intent, chooserTitle)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_logcat, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.copy_all -> {
            Utils.setClipboard(this, binding.tvLogcat.text.toString())
            toast(R.string.toast_success)
            true
        }
        R.id.clear_all -> {
            logcat(true)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
