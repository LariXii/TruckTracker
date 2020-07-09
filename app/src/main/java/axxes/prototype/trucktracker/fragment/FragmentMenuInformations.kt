package axxes.prototype.trucktracker.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import axxes.prototype.trucktracker.R
import axxes.prototype.trucktracker.manager.DSRCAttributManager
import axxes.prototype.trucktracker.manager.DSRCManager
import axxes.prototype.trucktracker.model.DSRCAttribut

class FragmentMenuInformations: Fragment() {

    private lateinit var etNom: EditText
    private lateinit var etRoue: EditText
    private lateinit var etEssTractor: EditText
    private lateinit var etEssTrailer: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_menu_informations, container, false)
        /*
        R.id.fmf_btn_del
        R.id.fmf_btn_save
        R.id.fmf_btn_valider
         */

        R.id.fmf_et_nom
        R.id.fmf_et_roue
        R.id.fmf_et_ess_tracteur
        R.id.fmf_et_ess_remorque

        return v
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    fun setValuesMenu(array: List<DSRCAttribut>){
        for(attr in array){
            Log.d("MenuInformations","Attribut : ${attr.attrName}\nData : ${attr.data?.let {
                DSRCManager.toHexString(
                    it
                )
            }}")
        }
    }
}