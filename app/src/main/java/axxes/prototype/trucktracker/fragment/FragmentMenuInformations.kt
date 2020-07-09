package axxes.prototype.trucktracker.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import axxes.prototype.trucktracker.R
import axxes.prototype.trucktracker.manager.DSRCAttributManager
import axxes.prototype.trucktracker.manager.DSRCManager
import axxes.prototype.trucktracker.model.DSRCAttribut

class FragmentMenuInformations: Fragment() {
    var listener: ListenerFragmentMenuInformations? = null

    interface ListenerFragmentMenuInformations{
        fun onClickValide()
        fun onGetAttributesMenu(listAttribut: MutableList<DSRCAttribut>)
    }

    private lateinit var btnValide: Button

    private lateinit var etNom: EditText
    private lateinit var etRoue: EditText
    private lateinit var etEssTractor: EditText
    private lateinit var etEssTrailer: EditText

    private lateinit var listEditText: List<EditText>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_menu_informations, container, false)
        /*
        R.id.fmf_btn_del
        R.id.fmf_btn_save
        */

        btnValide = v.findViewById(R.id.fmf_btn_valider)
        btnValide.setOnClickListener {
            listener?.onClickValide()
        }

        etNom = v.findViewById(R.id.fmf_et_nom)
        etRoue = v.findViewById(R.id.fmf_et_roue)
        etEssTractor = v.findViewById(R.id.fmf_et_ess_tracteur)
        etEssTrailer = v.findViewById(R.id.fmf_et_ess_remorque)

        listEditText = listOf(etNom, etRoue, etEssTractor, etEssTrailer)

        val listAttribut: MutableList<DSRCAttribut> = mutableListOf()
        var attribut = DSRCAttributManager.finAttribut(1, 4)
        attribut?.let{
            listAttribut.add(it)
            etRoue.tag = it.attrId
        }
        attribut = DSRCAttributManager.finAttribut(1, 19)
        attribut?.let{
            listAttribut.add(it)
            etEssTractor.tag = it.attrId
        }
        attribut = DSRCAttributManager.finAttribut(1, 24)
        attribut?.let{
            listAttribut.add(it)
            etEssTrailer.tag = it.attrId
        }

        listener?.onGetAttributesMenu(listAttribut)

        return v
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ListenerFragmentMenuInformations
        if (listener == null) {
            throw ClassCastException("$context must implement ListenerFragmentMenuInformations")
        }
    }

    fun setValuesMenu(array: List<DSRCAttribut>){
        for(attr in array){
            Log.d("MenuInformations","Attribut : ${attr.attrName}\nData : ${attr.data?.let {
                DSRCManager.toHexString(
                    it
                )
            }}")
            for(et in listEditText){
                if(et.tag == attr.attrId)
                    attr.data?.let {et.setText(DSRCManager.toHexString(it))}
            }

        }
    }
}