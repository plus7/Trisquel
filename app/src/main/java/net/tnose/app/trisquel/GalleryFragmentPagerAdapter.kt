package net.tnose.app.trisquel

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class GalleryFragmentPagerAdapter(fm: FragmentManager, var photo: Photo, var favList: ArrayList<Photo>) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment? {
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

    override fun getCount(): Int {
        return favList.sumBy { it.supplementalImages.size }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return "ページ" + (position + 1)
    }
}