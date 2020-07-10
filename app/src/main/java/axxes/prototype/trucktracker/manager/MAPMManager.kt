package axxes.prototype.trucktracker.manager

import android.content.Context
import axxes.prototype.trucktracker.model.Localisation
import axxes.prototype.trucktracker.utils.MyFileUtils
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

/**
 * This classe is used to create event file and manage his writting content
 */
class MAPMManager(_context: Context, _obuId: Int, _manufacturerId: Int, _contractProviderId: Int, _tyreType: Int, _trailerAxles: Int, _tractorAxles: Int) {
    private val applicationContext = _context

    private lateinit var fileStream: FileOutputStream
    private lateinit var name: String
    private lateinit var fileDateTime: LocalDateTime

    //Identifiant de l'équipement
    private val c1: Int = _obuId
    //identifiant du frabricant
    private val c2: Int = _manufacturerId
    //Identifiant du fournisseur de contrat
    private val c3: Int = _contractProviderId
    //Identifiant du type de pneu
    private val c4: Int = _tyreType
    //Nombre d'essieu de remorque
    private val c5: Int = _trailerAxles
    //Nombre d'essieu du tracteur
    private val c6: Int = _tractorAxles
    //Poids maximal que le camion peut supporter
    private val c16: String = ""
    //Code du pays
    private val c17: String = "BE"
    //Identifiant du fournisseur du domaine de péage
    private val c18: Int = 16383

    fun openFile(): String{
        //Set the date of writing file
        fileDateTime = LocalDateTime.now()
        //Give an temporary name to the file
        name = "${MyFileUtils.TYPE_MAPM}.${MyFileUtils.TMP_EXT}"
        File(applicationContext.filesDir, name)
        //Open file stream
        fileStream = applicationContext.openFileOutput(name, Context.MODE_PRIVATE)
        return name
    }
    fun closeFile(): String{
        fileStream.close()
        //Get the file
        val file = File(applicationContext.filesDir,name)
        //Convert file content to Bytes
        val fileContent = file.readBytes()
        //Generate name for the file
        name = MyFileUtils.nameFile(fileContent,fileDateTime,MyFileUtils.TYPE_MAPM,MyFileUtils.CSV_EXT)
        //Rename the file with the new name
        file.renameTo(File(applicationContext.filesDir, name))
        return name
    }
    fun writeLocation(loc: Localisation){
        val sep = MyFileUtils.SEP
        val eof = MyFileUtils.EOF
        fileStream.write(("$c1$sep" +
                    "$c2$sep" +
                    "$c3$sep" +
                    "$c4$sep" +
                    "$c5$sep" +
                    "$c6$sep" +
                    "$loc$sep" +
                    "$c16$sep" +
                    "$c17$sep" +
                    "$c18$sep" +
                    eof
                    ).toByteArray())

    }
}