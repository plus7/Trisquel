package net.tnose.app.trisquel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class FilmRollFragment : androidx.fragment.app.Fragment() {
    private var mListener: OnListFragmentInteractionListener? = null
    private var mFilmRollViewModel: FilmRollViewModel? = null
    private var _currentFilter: Pair<Int, ArrayList<String>> = Pair(0, arrayListOf(""))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filtertype = arguments?.getInt(ARG_FILTER_TYPE) ?: 0
        val filterstr = arguments?.getStringArrayList(ARG_FILTER_VALUE) ?: arrayListOf("")
        _currentFilter = Pair(filtertype, filterstr)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mFilmRollViewModel = ViewModelProvider(this).get(FilmRollViewModel::class.java)

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    val filmrolls by mFilmRollViewModel!!.allFilmRollAndRels.observeAsState(initial = emptyList())
                    
                    FilmRollListScreen(
                        filmrolls = filmrolls,
                        onItemClick = { mListener?.onListFragmentInteraction(FilmRoll.fromEntity(it), false) },
                        onItemLongClick = { mListener?.onListFragmentInteraction(FilmRoll.fromEntity(it), true) },
                        emptyMessage = getString(R.string.warning_filmroll_not_registered)
                    )
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    var currentFilter: Pair<Int, ArrayList<String>>
        get() = _currentFilter
        set(value) {
            _currentFilter = value
            val searchStr = when(value.first){
                1 -> value.second[0].toInt().toString()
                2 -> value.second[1]
                else -> ""
            }

            val pref = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
            val key = pref.getInt("filmroll_sortkey", 0)
            mFilmRollViewModel!!.viewRule.value = Pair(key, Pair(value.first, searchStr))
        }

    fun changeSortKey(key: Int) {
        currentFilter = _currentFilter
    }

    fun insertFilmRoll(filmRoll: FilmRoll) {
        mFilmRollViewModel!!.insert(filmRoll.toEntity())
    }

    fun updateFilmRoll(filmRoll: FilmRoll) {
        mFilmRollViewModel!!.update(filmRoll.toEntity())
    }

    fun deleteFilmRoll(id: Int) {
        mFilmRollViewModel!!.delete(id)
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: FilmRoll, isLong: Boolean)
    }

    companion object {
        private val ARG_FILTER_TYPE = "filter-type"
        private val ARG_FILTER_VALUE = "filter-val"

        fun newInstance(filterType: Int, filterValues: ArrayList<String>): FilmRollFragment {
            val fragment = FilmRollFragment()
            val args = Bundle()
            args.putInt(ARG_FILTER_TYPE, filterType)
            args.putStringArrayList(ARG_FILTER_VALUE, filterValues)
            fragment.arguments = args
            return fragment
        }
    }
}

fun getDateRange(dates: List<String>): String {
    if (dates.isEmpty()) return ""

    var minDate = Date(Long.MAX_VALUE)
    var maxDate = Date(0)
    val sdf = SimpleDateFormat("yyyy/MM/dd")
    sdf.timeZone = TimeZone.getTimeZone("UTC")

    for (date in dates) {
        var d = Date(0)
        try {
            d = sdf.parse(date) ?: Date(0)
        } catch (e: ParseException) {
        }

        if (minDate.after(d)) minDate = d
        if (maxDate.before(d)) maxDate = d
    }

    return if (minDate == maxDate) {
        sdf.format(minDate)
    } else {
        sdf.format(minDate) + "-" + sdf.format(maxDate)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilmRollListScreen(
    filmrolls: List<FilmRollAndRels>,
    onItemClick: (FilmRollAndRels) -> Unit,
    onItemLongClick: (FilmRollAndRels) -> Unit,
    emptyMessage: String
) {
    if (filmrolls.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(filmrolls, key = { it.filmRoll.id }) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    if (item.filmRoll.name.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.empty_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontStyle = FontStyle.Italic
                        )
                    } else {
                        Text(
                            text = item.filmRoll.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    
                    val cameraBrandStr = "${item.camera?.manufacturer ?: ""} ${item.camera?.modelName ?: ""}   ${item.filmRoll.manufacturer} ${item.filmRoll.brand}"
                    Text(
                        text = cameraBrandStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val exp = item.photoDates.size
                    val array = arrayListOf<String>()
                    val dateRange = getDateRange(item.photoDates)
                    if (dateRange.isNotEmpty()) array.add(dateRange)
                    array.add(if (exp == 1) "%d shot".format(exp) else "%d shots".format(exp))
                    
                    Text(
                        text = array.joinToString("   "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
