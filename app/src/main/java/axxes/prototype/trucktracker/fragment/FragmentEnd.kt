package axxes.prototype.trucktracker.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import axxes.prototype.trucktracker.R

class FragmentEnd: Fragment()
{
    private var listener: ListenerFragmentEnd? = null
    interface ListenerFragmentEnd{
        fun onClickOk()
    }

    private lateinit var btnOk: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_end, container, false)
        btnOk = view.findViewById(R.id.fe_btn_ok)
        btnOk.setOnClickListener {
            listener?.onClickOk()
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ListenerFragmentEnd
        if (listener == null) {
            throw ClassCastException("$context must implement ListenerFragmentEnd")
        }
    }
}