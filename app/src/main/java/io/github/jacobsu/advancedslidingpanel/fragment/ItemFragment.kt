package io.github.jacobsu.advancedslidingpanel.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.jacobsu.advancedslidingpanel.R

import io.github.jacobsu.advancedslidingpanel.fragment.dummy.DummyContent
import io.github.jacobsu.advancedslidingpanel.widget.*
import kotlinx.android.synthetic.main.fragment_item_list.*

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ItemFragment.OnListFragmentInteractionListener] interface.
 */
class ItemFragment : Fragment(), IVerticalScrollableView {

    // TODO: Customize parameters
    private var columnCount = 1

    private var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }

        Log.e("fragment", "onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = MyItemRecyclerViewAdapter(DummyContent.ITEMS)
            }

            recyclerView = view
        }

        Log.e("fragment", "onCreateView ${view.id}")
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Log.e("fragment", "onAttach")
    }

    override fun onDetach() {
        Log.e("fragment", "onDetach")

        super.onDetach()
    }


    override val isAtTopPosition: Boolean
        get() = recyclerView?.isAtTopPosition ?: true
    override val isAtBottomPosition: Boolean
        get() = recyclerView?.isAtBottomPosition ?: true
    override val isAtMiddlePosition: Boolean
        get() = recyclerView?.isAtMiddlePosition ?: true
    override val isUnScrollable: Boolean
        get() = recyclerView?.isUnScrollable ?: true
    override val verticalScrollerState: VerticalScrollerState
        get() = recyclerView?.verticalScrollerState ?: VerticalScrollerState.Unscrollable
    override val verticalScrollableView: View
        get() = recyclerView ?: View(requireContext())

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            ItemFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}
