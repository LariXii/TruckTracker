package axxes.prototype.trucktracker.fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import axxes.prototype.trucktracker.R
import axxes.prototype.trucktracker.model.Journey
import axxes.prototype.trucktracker.model.ServiceInformations
import kotlinx.android.synthetic.main.fragment_journey.*
import org.w3c.dom.Text
import java.time.format.DateTimeFormatter

class FragmentJourney: Fragment() {

    private var listener: ListenerFragmentJourney? = null

    interface ListenerFragmentJourney {
        fun onClickJourney()
    }

    private lateinit var btnJourney: Button

    private lateinit var tvGpsUnable: TextView
    private lateinit var tvMsgErr: TextView
    private lateinit var containerJourneyInfo: LinearLayout
    private lateinit var tvSendFile: TextView
    private lateinit var tvNumberLoc: TextView
    private lateinit var tvStartJourney: TextView

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
        tvGpsUnable = view.findViewById(R.id.fj_tv_gps_enabled)
        tvMsgErr = view.findViewById(R.id.fj_tv_msg_err)
        containerJourneyInfo = view.findViewById(R.id.fj_container_journey_infos)
        tvSendFile = view.findViewById(R.id.fj_tv_send_file)
        tvNumberLoc = view.findViewById(R.id.fj_tv_number_loc)
        tvStartJourney = view.findViewById(R.id.fj_tv_start_journey)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ListenerFragmentJourney
        if (listener == null) {
            throw ClassCastException("$targetFragment must implement ListenerFragmentJourney")
        }
    }

    fun serviceInformationsToScreen(serviceInfos: ServiceInformations) {
        if(serviceInfos.isGpsUsable){
            tvGpsUnable.text = "Activé"
            tvGpsUnable.setTextColor(Color.GREEN)
            tvMsgErr.visibility = View.INVISIBLE
        }
        else{
            tvGpsUnable.text = "Désactivé"
            tvGpsUnable.setTextColor(Color.RED )
            tvMsgErr.visibility = View.VISIBLE
        }

        if(serviceInfos.isSending)
            tvSendFile.visibility = View.VISIBLE
        else
            tvSendFile.visibility = View.INVISIBLE
    }

    fun journeyInformationsToScreen(journey: Journey) {
        if(journey.isPending())
            containerJourneyInfo.visibility = View.VISIBLE
        else
            containerJourneyInfo.visibility = View.INVISIBLE
        tvNumberLoc.text = journey.numberOfLocalisation().toString()
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        tvStartJourney.text = journey.getStartDateTime().format(dateFormatter)
    }
}