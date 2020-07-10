package axxes.prototype.trucktracker.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import axxes.prototype.trucktracker.R

class FragmentEnd: Fragment()
{

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_end, container, false)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }
}