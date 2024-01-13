package net.tnose.app.trisquel

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.tnose.app.trisquel.databinding.ActivityGalleryBinding

class GalleryActivity : AppCompatActivity(), GalleryImageFragment.OnFragmentInteractionListener {

    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val manager = supportFragmentManager
        val photo = intent.getParcelableExtra<Photo>("photo")
        val favList = intent.getParcelableArrayListExtra<Photo>("favList")
        val adapter = GalleryFragmentPagerAdapter(manager, photo!!, favList!!)
        binding.pager.adapter = adapter

        var currentPos = 0
        for(i in favList.indices){
            if(favList[i].id == photo.id) break
            currentPos += favList[i].supplementalImages.size
        }

        binding.pager.setCurrentItem(currentPos, false)
    }

    override fun onFragmentInteraction(uri: Uri){

    }
}
