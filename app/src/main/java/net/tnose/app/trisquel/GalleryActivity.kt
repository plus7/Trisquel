package net.tnose.app.trisquel

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_gallery.*

class GalleryActivity : AppCompatActivity(), GalleryImageFragment.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        val manager = supportFragmentManager
        val photo = intent.getParcelableExtra<Photo>("photo")
        val favList = intent.getParcelableArrayListExtra<Photo>("favList")
        val adapter = GalleryFragmentPagerAdapter(manager, photo, favList)
        pager.adapter = adapter

        var currentPos = 0
        for(i in favList.indices){
            if(favList[i].id == photo.id) break
            currentPos += favList[i].supplementalImages.size
        }

        pager.setCurrentItem(currentPos, false)
    }

    override fun onFragmentInteraction(uri: Uri){

    }
}
