package jp.co.monolithworks.imagegenerator

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import kotlinx.android.synthetic.main.fragment_viewer.*
import java.io.File
import java.io.FileOutputStream
import java.io.FilenameFilter

/**
 * Created by take on 2018/07/02.
 */
class ViewerFragment : Fragment() {

    private var mPageCount = 0
    private var pages: MutableList<Int> = mutableListOf()
    val Int.dp: Int
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()
    var imageView: ImageView? = null
    var test = ""

    companion object {
        private val FILENAME = "ura01a_torisetsu.pdf"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_viewer, container, false)
        imageView = root.findViewById<ImageView>(R.id.viewer)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        context?.let {
            getPDF(it).let {
                generate(it)
            }
        }

    }

    fun getPDF(context: Context): File {
        val file = File(context.cacheDir, FILENAME)
        if (!file.exists()) {
            val asset = context.assets.open(FILENAME)
            val output = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var size: Int = -1

            while (asset.read(buffer).let { size = it; it != 1 }) {
                output.write(buffer, 0, size)
            }

            asset.close()
            output.close()
        }

        return file
    }

    fun generate(file: File) {
        val dir = File(file.toString() + "_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.let {
                Log.d("test", "1")
                PdfRenderer(it).let { renderer ->
                    Log.d("test", "2")
                    mPageCount = renderer.pageCount
                    pages = MutableList(mPageCount, { it })

                    while ( pages.count() > 0) {
                        pages.removeAt(0).let { index ->
                            val image = File(dir, index.toString())
                            dir.listFiles(object: FilenameFilter {
                                override fun accept(p0: File?, p1: String?): Boolean {
                                    p1?.let {
                                        if (it.startsWith(index.toString() + "_")) {
                                            return true
                                        }
                                    }
                                    return false
                                }
                            }).let { files ->
                                renderer.openPage(index)?.let { page ->
                                    Log.d("test index", "" + index)

                                    var multiplier = 1.0F
                                    if (page.width > page.height) {
                                        multiplier = 1.5F
                                    }

                                    val margin = 10.dp
                                    val ratio = ((getScreenWidthInDPs() * 4).toDouble() / page.width) * multiplier
                                    val width = Math.round(ratio * page.width).toInt()
                                    val height = Math.round(ratio * page.height).toInt()

                                    val layoutWidth = (imageView!!.width - margin).toInt()
                                    val layoutHeight = Math.round((imageView!!.width.toFloat() - margin) * height.toFloat() / width.toFloat())
                                    val file = File(dir, image.name + "_" + layoutWidth.toString() + "x" + layoutHeight.toString())
                                    if (index == 0) {
                                        test = file.absolutePath
                                    }
                                    Bitmap.createBitmap(width * 3, height * 3, Bitmap.Config.ARGB_8888)?.let {
                                        Log.d("test", "8")
                                        it.eraseColor(Color.WHITE)
                                        page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                        FileOutputStream(file).use { out ->
                                            it.compress(Bitmap.CompressFormat.JPEG, 60, out)
                                        }
                                    }
                                    page.close()
                                }
                            }
                        }
                    }
                    renderer.close()
                }
            }
        } catch (e: Exception) {
            Log.d("exception", e.toString())
        }
        val image = File(test)
//        val image = File("")
        viewer.setImageURI(Uri.fromFile(image))
    }

    fun getScreenWidthInDPs(): Int {
        val dm = DisplayMetrics()
        val windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(dm)
        return Math.round(dm.widthPixels / dm.density)
    }
}
