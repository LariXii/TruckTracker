package axxes.prototype.trucktracker.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import axxes.prototype.trucktracker.R

class FragmentJourney: Fragment() {

    private var listener: ListenerFragmentJourney? = null

    interface ListenerFragmentJourney {
        fun onClickJourney()
    }

    private lateinit var btnJourney: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_journey, container, false)
        btnJourney = view.findViewById(R.id.fj_btn_location)
        btnJourney.setOnClickListener {
            listener?.onClickJourney()
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ListenerFragmentJourney
        if (listener == null) {
            throw ClassCastException("$targetFragment must implement ListenerFragmentJourney")
        }
    }
}