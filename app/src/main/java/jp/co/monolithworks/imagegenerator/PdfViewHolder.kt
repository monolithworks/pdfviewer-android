package jp.co.monolithworks.imagegenerator

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView

/**
 * Created by take on 2018/07/07.
 */
class PdfViewHolder(view: View): RecyclerView.ViewHolder(view) {

    val imageView = view.findViewById(R.id.image) as ImageView

}
