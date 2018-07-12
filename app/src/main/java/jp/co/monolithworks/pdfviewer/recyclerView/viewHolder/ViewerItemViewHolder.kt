package jp.co.monolithworks.pdfviewer.recyclerView.viewHolder

import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import jp.co.monolithworks.pdfviewer.R
import android.util.Log
import kotlinx.coroutines.experimental.Job
import java.util.*

/**
 * Created by adeliae on 2018/07/11.
 */
class ViewerItemViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private var _bitmap: Bitmap? = null
    private var _shadowedBitmap: Bitmap? = null
    private val imageView = view.findViewById(R.id.imageView) as ImageView

    var job: Job? = null
    var bitmap: Bitmap?
    get() = _bitmap
    set(value) {
        _bitmap?.let {
            _shadowedBitmap = it
        }

        _bitmap = value
        _bitmap?.let {
            imageView.setImageBitmap(it)
        }

        _shadowedBitmap?.recycle()
    }

}
