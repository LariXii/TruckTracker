package axxes.prototype.trucktracker.fragment

import android.app.Activity
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import axxes.prototype.trucktracker.MainActivity
import axxes.prototype.trucktracker.R
import axxes.prototype.trucktracker.model.DSRCAttribut
import axxes.prototype.trucktracker.model.Journey
import axxes.prototype.trucktracker.model.ServiceInformations
import axxes.prototype.trucktracker.service.MainService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import kotlinx.android.synthetic.main.fragment_journey.*
import org.w3c.dom.Text
import java.time.format.DateTimeFormatter

class FragmentJourney: Fragment() {

    private var listener: ListenerFragmentJourney? = null

    interface ListenerFragmentJourney {
        fun onClickJourney()
        fun onJourneyCreated()
    }

    private lateinit var btnJourney: ToggleButton

    private lateinit var tvGpsUnable: TextView
    private lateinit var tvMsgErr: TextView
    private lateinit var containerJourneyInfo: LinearLayout
    private lateinit var tvSendFile: TextView
    private lateinit var tvNumberLoc: TextView
    private lateinit var tvStartJourney: TextView
    private lateinit var tvChronometer: Chronometer

    private val serviceReceiver: ServiceReceiver = ServiceReceiver()

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
        tvChronometer = view.findViewById(R.id.fj_chronometer)

        requestLocationSettingsEnable()
        listener?.onJourneyCreated()
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ListenerFragmentJourney
        if (listener == null) {
            throw ClassCastException("$targetFragment must implement ListenerFragmentJourney")
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(serviceReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY)
        )
        LocalBroadcastManager.getInstance(context).registerReceiver(serviceReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS)
        )
    }

    override fun onDetach() {
        LocalBroadcastManager.getInstance(activity!!.applicationContext).unregisterReceiver(serviceReceiver)
        super.onDetach()
    }

    private fun requestLocationSettingsEnable(){
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builderSettingsLocation: LocationSettingsRequest.Builder  = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val task = LocationServices.getSettingsClient(activity!!.applicationContext).checkLocationSettings(builderSettingsLocation.build())

        task.addOnSuccessListener { response ->
            val mainActivity = activity as MainActivity
            mainActivity.mainService?.setServiceInformationsStates(response.locationSettingsStates.isGpsPresent, response.locationSettingsStates.isGpsUsable)
            btnJourney.isEnabled = true
            gpsUsable()
        }
        task.addOnFailureListener { e ->
            btnJourney.isEnabled = false
            gpsUnusable()
        }
    }

    fun updateChronometer(state: Boolean, startTime: Long) {
        if(state) {
            tvChronometer.base = startTime
            tvChronometer.start()
        }
        else{
            tvChronometer.base = SystemClock.elapsedRealtime()
            tvChronometer.stop()
        }
    }

    private fun gpsUnusable(){
        tvGpsUnable.text = "Désactivé"
        tvGpsUnable.setTextColor(Color.RED )
        tvMsgErr.visibility = View.VISIBLE
    }
    private fun gpsUsable(){
        tvGpsUnable.text = "Activé"
        tvGpsUnable.setTextColor(Color.GREEN)
        tvMsgErr.visibility = View.INVISIBLE
    }

    fun serviceInformationsToScreen(serviceInfos: ServiceInformations) {
        if(serviceInfos.isGpsUsable){
            btnJourney.isEnabled = true
            gpsUsable()
        }
        else{
            gpsUnusable()
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

    private inner class ServiceReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                // LOCATION
                MainService.ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS -> {
                    val serviceInfos = intent.getParcelableExtra<ServiceInformations>(
                        MainService.EXTRA_INFORMATIONS
                    )

                    if (serviceInfos != null) {
                        serviceInformationsToScreen(serviceInfos)
                    }
                }
                // JOURNEY
                MainService.ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY -> {
                    val journeyInfo = intent.getParcelableExtra<Journey>(
                        MainService.EXTRA_JOURNEY
                    )

                    if (journeyInfo != null) {
                        journeyInformationsToScreen(journeyInfo)
                    }
                }
            }
        }
    }
}