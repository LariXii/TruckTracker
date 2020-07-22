package axxes.prototype.trucktracker.manager

import android.content.Context
import axxes.prototype.trucktracker.utils.MyFileUtils
import axxes.prototype.trucktracker.model.Event
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

/**
 * This classe is used to create event file and manage his writting content
 */
class EventManager(_context: Context, _obuId: Int, _manufacturerId: Int, _contractProviderId: Int) {
    private val applicationContext = _context

    private lateinit var fileStream: FileOutputStream
    private lateinit var name: String
    private lateinit var fileDateTime: LocalDateTime

    //ID row
    private var c1: Int = 0
    //OBU ID
    private val c2: Int = _obuId
    //MANUFACTURER ID
    private val c3: Int = _manufacturerId
    //CONTRACT PROVIDER ID
    private val c4: Int = _contractProviderId

    fun openFile(): String{
        //Set the date of writing file
        fileDateTime = LocalDateTime.now()
        //Give an temporary name to the file
        name = "${MyFileUtils.TYPE_EVNT}.${MyFileUtils.TMP_EXT}"
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
        name = MyFileUtils.nameFile(fileContent,fileDateTime,MyFileUtils.TYPE_EVNT,MyFileUtils.CSV_EXT)
        //Rename the file with the new name
        file.renameTo(File(applicationContext.filesDir, name))
        return name

    }
    fun writeEvent(evnt: Event){
        val sep = MyFileUtils.SEP
        val eof = MyFileUtils.EOF
        //ProxyTime
        val c11 = null

        fileStream.write(("$c1$sep" +
                "$c2$sep" +
                "$c3$sep" +
                "$c4$sep" +
                "$evnt$sep" +
                "${c11?:""}$sep" +
                eof
                ).toByteArray())
        c1++
    }
}