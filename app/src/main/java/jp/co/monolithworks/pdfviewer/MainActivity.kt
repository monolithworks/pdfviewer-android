package jp.co.monolithworks.pdfviewer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

/**
 * Created by adeliae on 2018/07/11.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
                .add(R.id.container, ViewerFragment())
                .commit()
    }

}
