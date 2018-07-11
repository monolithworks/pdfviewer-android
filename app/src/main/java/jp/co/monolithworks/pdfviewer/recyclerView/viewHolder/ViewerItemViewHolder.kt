package jp.co.monolithworks.pdfviewer.recyclerView.viewHolder

import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import jp.co.monolithworks.pdfviewer.R

/**
 * Created by adeliae on 2018/07/11.
 */
class ViewerItemViewHolder(view: View): RecyclerView.ViewHolder(view) {

    val imageView = view.findViewById(R.id.imageView) as ImageView
    var bitmap: Bitmap? = null

}
