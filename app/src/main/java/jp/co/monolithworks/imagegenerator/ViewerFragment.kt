package jp.co.monolithworks.imagegenerator

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_viewer_using_recycler.*
import kotlinx.android.synthetic.main.pdf_item.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FilenameFilter
import android.R.attr.path
import android.graphics.BitmapFactory



/**
 * Created by take on 2018/07/02.
 */
class ViewerFragment : Fragment() {

    private var mPageCount = 0
    private var pages: MutableList<Int> = mutableListOf()
    val Int.dp: Int
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()

    companion object {
        private val FILENAME = "ura01a_torisetsu.pdf"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_viewer_using_recycler, container, false)
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
            val buffer = ByteArray(2048)
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
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.let { descriptor ->
                PdfRenderer(descriptor).let { renderer ->
                    mPageCount = renderer.pageCount

                    launch(UI) {
                        recycler.adapter = PageAdapter(listOf())
                        recycler.setItemViewCacheSize(3)
                        recycler.layoutManager = LinearLayoutManager(this@ViewerFragment.context, LinearLayoutManager.VERTICAL, false)
                    }
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
                                if (files.size > 0) {
                                    launch(UI) {
                                        (recycler.adapter as? PageAdapter)?.let {
                                            it.add(files.first())
                                        }
                                    }
                                } else {
                                    renderer.openPage(index)?.let { page ->

                                        var multiplier = 1.0F
                                        if (page.width > page.height) {
                                            multiplier = 1.5F
                                        }

                                        val margin = 10.dp
                                        val ratio = ((getScreenWidthInDPs() * 4).toDouble() / page.width) * multiplier
                                        val width = Math.round(ratio * page.width).toInt()
                                        val height = Math.round(ratio * page.height).toInt()

                                        val layoutWidth = (recycler.width - margin).toInt()
                                        val layoutHeight = Math.round((recycler.width.toFloat() - margin) * height.toFloat() / width.toFloat())
                                        val file = File(dir, image.name + "_" + layoutWidth.toString() + "x" + layoutHeight.toString())
                                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)?.let {
                                            it.eraseColor(Color.WHITE)
                                            page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                            FileOutputStream(file).use { out ->
                                                it.compress(Bitmap.CompressFormat.JPEG, 60, out)
                                            }
                                        }
                                        page.close()
                                        launch(UI) {
                                            (recycler.adapter as? PageAdapter)?.let {
                                                it.add(file)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    renderer.close()
                }
                descriptor.close()
            }
        } catch (e: Exception) {
            Log.d("exception", e.toString())
        }
    }

    fun getScreenWidthInDPs(): Int {
        val dm = DisplayMetrics()
        val windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(dm)
        return Math.round(dm.widthPixels / dm.density)
    }

    private inner class PageAdapter(items: List<File>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val _list = ArrayList(items)

        fun add(page: File) {
            _list.add(page)

            notifyItemInserted(_list.size - 1)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent!!.context).inflate(R.layout.pdf_item, parent, false)

            return PdfViewHolder(view)
        }

        override fun getItemCount(): Int {
            return _list.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            (holder as? PdfViewHolder)?.let { viewHolder ->
                val imageFile = _list[position]

                val option = BitmapFactory.Options()
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, option)
                viewHolder.imageView.setImageBitmap(bitmap)
            }
        }
    }
}
