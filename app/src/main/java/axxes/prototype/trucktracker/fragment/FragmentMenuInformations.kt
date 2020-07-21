package axxes.prototype.trucktracker.fragment

import android.app.Dialog
import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import axxes.prototype.trucktracker.MainActivity
import axxes.prototype.trucktracker.R
import axxes.prototype.trucktracker.manager.DSRCAttributManager
import axxes.prototype.trucktracker.manager.DSRCManager
import axxes.prototype.trucktracker.model.DSRCAttribut
import axxes.prototype.trucktracker.model.DSRCAttributEID1
import axxes.prototype.trucktracker.service.MainService

class FragmentMenuInformations: Fragment() {
    var listener: ListenerFragmentMenuInformations? = null

    interface ListenerFragmentMenuInformations{
        fun onClickValide()
        fun onClickSave(listAttribut: List<DSRCAttribut>)
        fun onGetAttributesMenu(listAttribut: MutableList<DSRCAttribut>)
    }

    private lateinit var appContext: Context

    private lateinit var btnValide: Button
    private lateinit var btnSave: ImageButton

    private lateinit var etRoue: EditText
    private lateinit var etEssTractor: EditText
    private lateinit var etEssTrailer: EditText

    private lateinit var listEditText: List<EditText>
    private lateinit var attributeVehicleAxles: DSRCAttribut
    private lateinit var dsrcManager: DSRCManager
    private val serviceBroadcastReceiver: ServiceBroadcastReceiver = ServiceBroadcastReceiver()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_menu_informations, container, false)
        dsrcManager = DSRCManager()

        btnSave = v.findViewById(R.id.fmf_btn_save)
        btnSave.setOnClickListener {
            val listValues: List<Int> =
                listOf(
                    0x00
                    , etRoue.text.toString().toInt()
                    , etEssTrailer.text.toString().toInt()
                    , etEssTractor.text.toString().toInt()
                )
            Log.d("InformationsMenu","Valeurs avant envoie : $listValues")
            attributeVehicleAxles.data = dsrcManager.writeVehicleAxles(listValues)

            listener?.onClickSave(listOf(attributeVehicleAxles))
        }

        btnValide = v.findViewById(R.id.fmf_btn_valider)
        btnValide.setOnClickListener {
            listener?.onClickValide()
        }

        etRoue = v.findViewById(R.id.fmf_et_roue)
        etEssTractor = v.findViewById(R.id.fmf_et_ess_tracteur)
        etEssTrailer = v.findViewById(R.id.fmf_et_ess_remorque)

        etRoue.showSoftInputOnFocus = false
        etRoue.setOnClickListener {
            numberPickerCustom("Numero type de roue",1,3, etRoue)
        }
        etEssTractor.showSoftInputOnFocus = false
        etEssTractor.setOnClickListener {
            numberPickerCustom("Nombre d'essieu du camion",1,7, etEssTractor)
        }

        etEssTrailer.showSoftInputOnFocus = false
        etEssTrailer.setOnClickListener {
            numberPickerCustom("Nombre d'essieu de remorque",1,7, etEssTrailer)
        }

        listEditText = listOf(etRoue, etEssTractor, etEssTrailer)

        val listAttribut: MutableList<DSRCAttribut> = mutableListOf()
        attributeVehicleAxles = DSRCAttributManager.finAttribut(1, 19)!!
        listAttribut.add(attributeVehicleAxles)

        listener?.onGetAttributesMenu(listAttribut)

        return v
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ListenerFragmentMenuInformations
        if (listener == null) {
            throw ClassCastException("$context must implement ListenerFragmentMenuInformations")
        }
        appContext = context
        LocalBroadcastManager.getInstance(context).registerReceiver(serviceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS)
        )
        LocalBroadcastManager.getInstance(context).registerReceiver(serviceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS_SAVED)
        )
    }

    override fun onDetach() {
        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(serviceBroadcastReceiver)
        super.onDetach()
    }

    private fun numberPickerCustom(title: String, minVal: Int, maxVal: Int, editText: EditText) {
        val d = AlertDialog.Builder(appContext)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_number_picker, null)
        d.setTitle(title)
        d.setView(dialogView)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.dnb_number_picker)
        numberPicker.maxValue = maxVal
        numberPicker.minValue = minVal
        numberPicker.wrapSelectorWheel = false
        numberPicker.setOnValueChangedListener { _, _, _ -> println("onValueChange: ") }
        d.setPositiveButton("Valider") { _, _ ->
            editText.setText(numberPicker.value.toString())
        }
        val alertDialog = d.create()
        alertDialog.show()
    }

    fun setValuesMenu(array: List<DSRCAttribut>){
        for(attr in array){
            Log.d("FragmentMenuInformations","Attributs : ${attr.attrName} ::: size : ${attr.data?.size}\nData : ${DSRCManager.toHexString(attr.data!!)}")
            when(attr.attrName){
                DSRCAttributEID1.VEHICLE_AXLES.attribut.attrName -> {
                    attributeVehicleAxles.data = attr.data
                    val listValues = dsrcManager.readVehicleAxles(attr.data!!)
                    etRoue.setText(listValues[1].toString())
                    etEssTrailer.setText(listValues[2].toString())
                    etEssTractor.setText(listValues[3].toString())
                }
            }
        }
    }

    private inner class ServiceBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                MainService.ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS -> {
                    val values = intent.getParcelableArrayExtra(MainService.EXTRA_MENU_INFORMATIONS)
                    if(values != null){
                        setValuesMenu(values.toList() as List<DSRCAttribut>)
                    }
                }

                MainService.ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS_SAVED -> {
                    val values = intent.getIntArrayExtra(MainService.EXTRA_RETURN_CODE)
                    if(values != null){
                        if(values.sum() == 0)
                            Toast.makeText(appContext, "Sauvegarde des informations réussie !", Toast.LENGTH_LONG).show()
                        else
                            Toast.makeText(appContext, "Erreur lors de la sauvegarde des informations !", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

}