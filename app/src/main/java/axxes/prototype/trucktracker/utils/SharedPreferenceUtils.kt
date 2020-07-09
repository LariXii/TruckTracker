package axxes.prototype.trucktracker.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

internal class SharedPreferenceUtils {
    companion object {
        const val PREFERENCE_KEY = "com.example.android.while_in_use_location.PREFERENCE_KEY"
        //Clé pour stocker l'état du service
        const val KEY_FOREGROUND_ENABLED = "tracking_foreground_location"
        //Clé pour savoir si l'application s'est terminé avec un crash
        const val KEY_APPLICATION_CRASHED = "application_crashed"
        //Clé du nom de l'utilisateur
        const val KEY_USER_NAME = "user_name"
        //Clé du type de roue
        const val KEY_TYRE_TYPE = "tyre_type"
        //Clé de l'essieu du tracteur
        const val KEY_TRACTOR_AXLES = "tractor_axles"
        //Clé de l'essieu de la remorque
        const val KEY_TRAILER_AXLES = "trailer_axles"
        //Clé du nom du device
        const val KEY_DEVICE_NAME = "device_name"
        //Clé de l'addresse du device
        const val KEY_DEVICE_ADDRESS = "device_addr"

        /**
         * Returns true if requesting location updates, otherwise returns false.
         *
         * @param context The [Context].
         */
        fun getLocationTrackingPref(context: Context): Boolean =
            context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).getBoolean(KEY_FOREGROUND_ENABLED, false)

        /**
         * Stores the location updates state in SharedPreferences.
         * @param requestingLocationUpdates The location updates state.
         */
        fun saveLocationTrackingPref(context: Context, requestingLocationUpdates: Boolean){
            val editor = context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit()
            editor.putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates)
            editor.apply()
        }
        fun saveStringPreference(context: Context, key: String, value: String?){
            val editor = context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit()
            editor.putString(key, value)
            editor.apply()
        }

        fun eraseStringPreference(context: Context, key: String){
            val editor = context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit()
            editor.remove(key)
            editor.apply()
        }
    }
}