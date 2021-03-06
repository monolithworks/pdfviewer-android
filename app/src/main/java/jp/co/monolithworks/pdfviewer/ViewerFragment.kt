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
import jp.co.monolithworks.pdfviewer.common.CustomizableLinearLayoutManager
import jp.co.monolithworks.pdfviewer.recyclerView.viewHolder.ViewerItemViewHolder
import kotlinx.android.synthetic.main.fragment_viewer.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by adeliae on 2018/07/11.
 */
class ViewerFragment : Fragment() {

    private var files: ArrayList<File> = arrayListOf()
    private var mPageCount = 0
    private var pages: MutableList<Int> = mutableListOf()

    companion object {
        private val VALUES = listOf<Int>(1024, 1024 * 3)
        private val FILENAME = "ura01a_torisetsu.pdf"
        //private val FILENAME = "853_811064_302_a.pdf"
        //private val FILENAME = "TV_sou.pdf"
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

        context?.let {context ->
            recycler.adapter = PageAdapter(listOf())
            recycler.setItemViewCacheSize(3)
            recycler.layoutManager = CustomizableLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

            async {
                getPDF(context, FILENAME).let {
                    generate(it)
                }
            }
        }
    }

    fun getPDF(context: Context, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        if (!file.exists()) {
            val asset = context.assets.open(fileName)
            val output = FileOutputStream(file)
            val buffer = ByteArray(asset.available())
            var size: Int = -1

            while (asset.read(buffer).let { size = it; it != -1 }) {
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
                                        for (value in VALUES) {
                                            var width = value
                                            var height = Math.round(value.toDouble() * (page.height.toDouble() / page.width.toDouble())).toInt()
                                            if (page.width > page.height) {
                                                height = value
                                                width = Math.round(value.toDouble() * (page.width.toDouble() / page.height.toDouble())).toInt()
                                            }
                                            var file = File(dir, image.name)
                                            if (file.exists()) {
                                                file = File(dir, image.name + "_l")

                                                width = value
                                                height = Math.round(value.toDouble() * (page.height.toDouble() / page.width.toDouble())).toInt()
                                                if (page.width < page.height) {
                                                    height = value
                                                    width = Math.round(value.toDouble() * (page.width.toDouble() / page.height.toDouble())).toInt()
                                                }
                                            }

                                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)?.let {
                                                it.eraseColor(Color.WHITE)
                                                page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                                FileOutputStream(file).use { out ->
                                                    it.compress(Bitmap.CompressFormat.JPEG, 60, out)
                                                }
                                            }
                                        }

                                        page.close()
                                        launch(UI) {
                                            (recycler.adapter as? PageAdapter)?.let {
                                                it.add(image)
                                            }
                                        }
                                    }
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
    }

    inner class PageAdapter(items: List<File>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val TYPE_REGULAR_WITH_MARGINETOP = 0
        private val TYPE_REGULAR = 1

        private val _list = ArrayList(items)
        private var _requiredDetailResource = false
        private var _currentScaleFactror = 1.0f

        lateinit var _recyclerView: RecyclerView

        var scaleFactor: Float
            get() = _currentScaleFactror
            set(value) {
                _currentScaleFactror = value

                (_recyclerView.layoutManager as? CustomizableLinearLayoutManager)?.let {
                    it.scaleFactror = _currentScaleFactror
                }

                when (_requiredDetailResource) {
                    true ->
                        if (_currentScaleFactror < 2.0f) {
                            _requiredDetailResource = false

                            (_recyclerView.layoutManager as? LinearLayoutManager)?.let {
                                var first = it.findFirstVisibleItemPosition() - 1
                                if (first < 0) {
                                    first = 0
                                }
                                var last = it.findLastVisibleItemPosition() + 1
                                if (last > _list.size - 1) {
                                    last = _list.size - 1
                                }

                                for (position in first..last) {
                                    (_recyclerView.findViewHolderForAdapterPosition(position) as? ViewerItemViewHolder)?.let { viewHolder ->
                                        val imageFile = _list[position]
                                        var bitmap: Bitmap? = null
                                        val option = BitmapFactory.Options()
                                        viewHolder.job?.cancel()

                                        viewHolder.job = launch {
                                            async {
                                                viewHolder.isDetail = false
                                                bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, option)
                                            }.await()

                                            launch(UI) {
                                                viewHolder.bitmap = bitmap
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    false ->
                        if (_currentScaleFactror > 2.0f) {
                            _requiredDetailResource = true

                            (_recyclerView.layoutManager as? LinearLayoutManager)?.let {
                                var first = it.findFirstVisibleItemPosition() - 1
                                if (first < 0) {
                                    first = 0
                                }
                                var last = it.findLastVisibleItemPosition() + 1
                                if (last > _list.size - 1) {
                                    last = _list.size - 1
                                }

                                for (position in first..last) {
                                    (_recyclerView.findViewHolderForAdapterPosition(position) as? ViewerItemViewHolder)?.let { viewHolder ->
                                        val imageFile = _list[position]
                                        var bitmap: Bitmap? = null
                                        val option = BitmapFactory.Options()
                                        viewHolder.job?.cancel()

                                        viewHolder.job = launch {
                                            async {
                                                viewHolder.isDetail = true
                                                bitmap = BitmapFactory.decodeFile(imageFile.absolutePath + "_l", option)
                                            }.await()

                                            launch(UI) {
                                                viewHolder.bitmap = bitmap
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            (_recyclerView.layoutManager as? LinearLayoutManager)?.let {
                                var first = it.findFirstVisibleItemPosition() - 1
                                if (first < 0) {
                                    first = 0
                                }
                                var last = it.findLastVisibleItemPosition() + 1
                                if (last > _list.size - 1) {
                                    last = _list.size - 1
                                }

                                for (position in first..last) {
                                    (_recyclerView.findViewHolderForAdapterPosition(position) as? ViewerItemViewHolder)?.let { viewHolder ->
                                        if (viewHolder.isDetail == true) {
                                            val imageFile = _list[position]
                                            var bitmap: Bitmap? = null
                                            val option = BitmapFactory.Options()
                                            viewHolder.job?.cancel()

                                            viewHolder.job = launch {
                                                async {
                                                    viewHolder.isDetail = false
                                                    bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, option)
                                                }.await()

                                                launch(UI) {
                                                    viewHolder.bitmap = bitmap
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }

            }

        fun add(page: File) {
            _list.add(page)

            notifyItemInserted(_list.size - 1)
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)

            _recyclerView = recyclerView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                TYPE_REGULAR_WITH_MARGINETOP -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_viewer_item_with_margintop, parent, false)
                    return ViewerItemViewHolder(view)
                }
                else -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_viewer_item, parent, false)
                    return ViewerItemViewHolder(view)
                }
            }
        }

        override fun getItemCount(): Int {
            return _list.size
        }

        override fun getItemViewType(position: Int): Int {
            return when(position) {
                0 ->
                    TYPE_REGULAR_WITH_MARGINETOP
                else ->
                    TYPE_REGULAR
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            (_recyclerView.layoutManager as? LinearLayoutManager)?.let {
                if ((it.findLastVisibleItemPosition() + 1 < position) || (it.findFirstVisibleItemPosition() - 1 > position)) {
                    return
                }
            }

            (holder as? ViewerItemViewHolder)?.let { viewHolder ->
                val imageFile = _list[position]
                var bitmap: Bitmap? = null
                val option = BitmapFactory.Options()
                viewHolder.job?.cancel()

                viewHolder.job = launch {
                    async {
                        when (_requiredDetailResource) {
                            true -> {
                                viewHolder.isDetail = true
                                bitmap = BitmapFactory.decodeFile(imageFile.absolutePath + "_l", option)
                            }
                            else -> {
                                viewHolder.isDetail = false
                                bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, option)
                            }
                        }
                    }.await()

                    launch(UI) {
                        holder.bitmap = bitmap
                    }
                }
            }
        }
    }
}
