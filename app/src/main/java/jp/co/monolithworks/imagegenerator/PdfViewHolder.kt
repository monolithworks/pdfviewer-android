package jp.co.monolithworks.imagegenerator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.ImageView
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

/**
 * Created by take on 2018/07/07.
 */
class PdfViewHolder(view: View): RecyclerView.ViewHolder(view) {

    val imageView = view.findViewById(R.id.image) as ImageView
    var bitmap: Bitmap? = null
    var animation: Animation? = null
    private val option = BitmapFactory.Options()

    fun setBitmap(filePath: String, context: Context) {
        launch(UI) {
            async(CommonPool) {
                bitmap = BitmapFactory.decodeFile(filePath, option)
            }.await()

            createEmptyBitmap(option.outWidth, option.outHeight)
            animation = loadAnimation(context, R.anim.fade_in)
            animation?.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationRepeat(p0: Animation?) {}
                        override fun onAnimationEnd(p0: Animation?) {}
                        override fun onAnimationStart(p0: Animation?) {
                            imageView.setImageBitmap(bitmap)
                        }
                    })
            imageView.startAnimation(animation)
        }
    }

    fun createEmptyBitmap(width: Int, height: Int) {
        val emptyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        imageView.setImageBitmap(emptyBitmap)
    }

}
