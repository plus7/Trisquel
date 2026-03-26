package net.tnose.app.trisquel

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class GalleryFragmentPagerAdapter(fa: FragmentActivity, var photo: Photo, var favList: ArrayList<Photo>) : FragmentStateAdapter(fa) {

    override fun createFragment(position: Int): Fragment {
        var currentPhotoPos = 0
        var photoIdx = 0
        for(i in favList.indices){
            if(currentPhotoPos <= position && position < currentPhotoPos + favList[i].supplementalImages.size){
                photoIdx = i
                break
            }
            currentPhotoPos += favList[i].supplementalImages.size
        }
        return GalleryImageFragment.newInstance(favList[photoIdx].id, position - currentPhotoPos)
    }

    override fun getItemCount(): Int {
        return favList.sumBy { it.supplementalImages.size }
    }
}
