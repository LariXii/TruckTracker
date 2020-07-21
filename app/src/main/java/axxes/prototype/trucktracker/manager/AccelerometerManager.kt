package axxes.prototype.trucktracker.manager

import android.content.Context
import axxes.prototype.trucktracker.utils.MyFileUtils
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

class AccelerometerManager(_context: Context, _obuId: Int) {
    private val applicationContext = _context

    private lateinit var fileStream: FileOutputStream
    private lateinit var name: String
    private lateinit var fileDateTime: LocalDateTime

    private val obuID = _obuId

    fun openFile(): String{
        //Set the date of writing file
        fileDateTime = LocalDateTime.now()
        //Give an temporary name to the file
        name = "${MyFileUtils.TYPE_ACCEL}.${MyFileUtils.TMP_EXT}"
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
        name = MyFileUtils.nameFile(fileContent,fileDateTime, MyFileUtils.TYPE_ACCEL, MyFileUtils.CSV_EXT)
        //Rename the file with the new name
        file.renameTo(File(applicationContext.filesDir, name))
        return name
    }
    fun writeAcceleration(time: Long, x: Float, y: Float, z: Float){
        val sep = MyFileUtils.SEP
        val eof = MyFileUtils.EOF
        fileStream.write(("$obuID$sep" +
                "$time$sep" +
                "$x$sep" +
                "$y$sep" +
                "$z$sep" +
                eof
                ).toByteArray())

    }
}