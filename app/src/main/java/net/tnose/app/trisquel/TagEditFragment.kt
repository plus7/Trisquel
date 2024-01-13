package net.tnose.app.trisquel

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import net.tnose.app.trisquel.databinding.FragmentTagEditBinding


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [TagEditFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [TagEditFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class TagEditFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var mFilmRollId: Int = -1
    private var mId: Int = -1
    private var listener: OnFragmentInteractionListener? = null
    private var mAllTags: ArrayList<String> = arrayListOf()
    private var mCheckState: ArrayList<Boolean> = arrayListOf()
    private var _binding: FragmentTagEditBinding? = null
    private val binding get() = _binding!!
    // SoAになってしまっている
    val allTags: ArrayList<String>
        get(){
            return mAllTags
        }
    val checkState: ArrayList<Boolean>
        get(){
            return mCheckState
        }
    var isDirty: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mFilmRollId = it.getInt("filmroll")
            mId          = it.getInt("id")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        _binding = FragmentTagEditBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun createNewChip(label: String): Chip {
        val newchip = Chip(activity!!)
        newchip.text = label
        val chipLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        newchip.layoutParams = chipLayoutParams
        newchip.isCheckable = true
        newchip.setOnCheckedChangeListener{
            buttonView, isChecked ->
            val i = mAllTags.indexOf(label)
            if(i >= 0) {
                mCheckState[i] = isChecked
                if (isResumed) isDirty = true
            }
        }
        binding.chipGroup.addView(newchip)
        return newchip
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.buttonAdd.isEnabled = false
        binding.buttonAdd.setOnClickListener {
            if(isResumed) isDirty = true
            val label = binding.editTagtext.text.toString()
            mAllTags.add(label)
            mCheckState.add(true)
            val chip = createNewChip(label)
            chip.isChecked = true
            binding.editTagtext.setText("")
        }

        binding.editTagtext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                binding.buttonAdd.isEnabled = !mAllTags.contains(binding.editTagtext.text.toString()) && binding.editTagtext.text.isNotEmpty()
            }
        })

        if(savedInstanceState == null){
            isDirty = false
            val dao = TrisquelDao(activity!!.applicationContext)
            dao.connection()
            val tags = dao.getTagsByPhoto(mId)
            val alltags = dao.allTags.sortedBy { it.label }
            dao.close()
            for(t in alltags){
                val chip = createNewChip(t.label)
                mAllTags.add(t.label)
                if (tags.find { it.id == t.id } != null){
                    mCheckState.add(true)
                    chip.isChecked = true
                }else{
                    mCheckState.add(false)
                }
            }
        }else{
            isDirty = savedInstanceState.getBoolean("isDirty")
            mAllTags = savedInstanceState.getStringArrayList("alltags") ?: arrayListOf()
            mCheckState = ArrayList<Boolean>((savedInstanceState.getBooleanArray("checkstate") ?: BooleanArray(0)).asList())
            for((i, v) in mAllTags.withIndex()){
                val chip = createNewChip(v)
                chip.isChecked = mCheckState[i]
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isDirty", isDirty)
        outState.putStringArrayList("alltags", mAllTags)
        outState.putBooleanArray("checkstate", mCheckState.toBooleanArray())
        super.onSaveInstanceState(outState)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
            listener?.onFragmentAttached(this)
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
        fun onFragmentAttached(f: TagEditFragment)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TagEditFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(filmroll: Int, id: Int) =
                TagEditFragment().apply {
                    arguments = Bundle().apply {
                        putInt("filmroll", filmroll)
                        putInt("id", id)
                    }
                }
    }
}
