package jp.co.monolithworks.pdfviewer

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
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
import android.widget.ImageView
import jp.co.monolithworks.pdfviewer.recyclerView.viewHolder.ViewerItemViewHolder
import kotlinx.android.synthetic.main.fragment_viewer.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FilenameFilter

/**
 * Created by adeliae on 2018/07/11.
 */
class ViewerFragment : Fragment() {

    private var files: ArrayList<File> = arrayListOf()
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
        val root = inflater.inflate(R.layout.fragment_viewer, container, false)
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
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.let {
                PdfRenderer(it).let { renderer ->
                    mPageCount = renderer.pageCount
                    pages = MutableList(mPageCount, { it })

                    launch(UI) {
                        recycler.adapter = PageAdapter(listOf())
                        recycler.setItemViewCacheSize(3)
                        recycler.layoutManager = LinearLayoutManager(this@ViewerFragment.context, LinearLayoutManager.VERTICAL, false)
                    }

                    while ( pages.count() > 0) {
                        pages.removeAt(0).let { index ->
                            val image = File(dir, index.toString())
                            when (image.exists()) {
                                true -> {
                                    launch(UI) {
                                        (recycler.adapter as? PageAdapter)?.let {
                                            it.add(image)
                                        }
                                    }
                                }
                                else -> {
                                    renderer.openPage(index)?.let { page ->
                                        var multiplier = 1.0F
                                        if (page.width > page.height) {
                                            multiplier = 1.5F
                                        }

                                        val ratio = ((getScreenWidthInDPs() * 4).toDouble() / page.width) * multiplier
                                        val width = Math.round(ratio * page.width).toInt()
                                        val height = Math.round(ratio * page.height).toInt()
                                        val file = File(dir, image.name)

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

                            files.add(image)
                        }
                    }
                    renderer.close()
                }
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent!!.context).inflate(R.layout.layout_viewer_item, parent, false)

            return ViewerItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return _list.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as? ViewerItemViewHolder)?.let { viewHolder ->
                val imageFile = _list[position]
                var bitmap: Bitmap? = null
                val option = BitmapFactory.Options()

                launch(UI) {
                    async(CommonPool) {
                        bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, option)
                    }.await()

                    holder.imageView.setImageBitmap(bitmap)
                }
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            (holder as? ViewerItemViewHolder)?.let { viewHolder ->

            }
            super.onViewRecycled(holder)
        }
    }
}
