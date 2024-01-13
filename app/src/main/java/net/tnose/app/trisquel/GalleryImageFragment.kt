package net.tnose.app.trisquel

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase
import net.tnose.app.trisquel.databinding.FragmentGalleryImageBinding
import java.io.File


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM_PHOTOID = "photoId"
private const val ARG_PARAM_SUPPIMGIDX = "suppImgIdx"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [GalleryImageFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [GalleryImageFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class GalleryImageFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var photoId: Int? = 0
    private var suppImgIdx: Int? = 0
    private var listener: OnFragmentInteractionListener? = null
    private var _binding: FragmentGalleryImageBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            photoId = it.getInt(ARG_PARAM_PHOTOID)
            suppImgIdx = it.getInt(ARG_PARAM_SUPPIMGIDX)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment

        val dao = TrisquelDao(this.context)
        dao.connection()
        val p = dao.getPhoto(photoId!!)
        dao.close()

        _binding = FragmentGalleryImageBinding.inflate(inflater, container, false)

        //val view = inflater.inflate(R.layout.fragment_gallery_image, container, false)
        val imageView = binding.imageView
        imageView.displayType = ImageViewTouchBase.DisplayType.FIT_TO_SCREEN

        imageView.setSingleTapListener {
            //listener?.onFragmentInteraction()
        }

        if(p!!.supplementalImages.size == 0){
            Glide.with(imageView.context)
                    .load(R.drawable.general_image_gray)
                    .into(imageView)
        }else {
            val path = p.supplementalImages[suppImgIdx!!]
            val rb = if(path.startsWith("/")){
                Glide.with(imageView.context).load(File(path))
            }else{
                Glide.with(imageView.context).load(Uri.parse(path))
            }
            rb.apply(RequestOptions()
                    .centerInside()
                    .error(R.drawable.ic_error_circle))
            .into(imageView)
        }

        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment GalleryImageFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(photoId: Int, suppImgIdx: Int) =
                GalleryImageFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_PARAM_PHOTOID, photoId)
                        putInt(ARG_PARAM_SUPPIMGIDX, suppImgIdx)
                    }
                }
    }
}
