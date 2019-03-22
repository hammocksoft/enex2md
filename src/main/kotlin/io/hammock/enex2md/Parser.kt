package io.hammock.enex2md

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.text.SimpleDateFormat
import java.util.Base64
import kotlin.math.roundToInt

/**
 *
 * @author Mark Hofmann (mark@mark-hofmann.de)
 */
class Parser {
    companion object {
        private const val MAX_IMAGE_WIDTH: Int = 600
    }

    private var noteData = HashMap<String, String>()
    private var mediaMap = HashMap<String, RecoIndex>()

    private var outputDirectory: File? = null
    private var resourceFile: File? = null
    private var resourceWriter: BufferedWriter? = null
    //    private var resourceFileName: String? = null
    private var resourceMimeType: String? = null
    private var objectId: String? = null

    private var noteCreated: Long = 0
    private var noteUpdated: Long = 0

    fun parse(enexFilename: String) {
        val defaultHandler = object : DefaultHandler() {
            var currentValue = StringBuilder()
            var currentElement = false
            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                currentElement = true
                currentValue = StringBuilder()
                when (qName) {
                    "data" -> createResource()
                    "note" -> {
                        noteData = HashMap()
                        mediaMap = HashMap()
                    }
                }
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                currentElement = false
                val valueString = currentValue.toString()
                when (qName) {
                    "title" -> noteData["title"] = valueString
                    "content" -> addContent(valueString)
                    "data" -> closeResource()
                    "resource" -> convertResourceData(isHtml())
                    "file-name" -> {
                        println("fileName: $valueString")
                        mediaMap[objectId!!]?.fileName = valueString
                    }
                    "mime" -> resourceMimeType = valueString
                    // <width>3024</width>
                    // <height>4032</height>
                    // <duration>0</duration>
                    "recognition" -> {
//                        println("RECO: $valueString")
                        val recoIndex = RecoIndexParser().parse(valueString)
                        println(recoIndex)
                        objectId = recoIndex.objID!!
                        mediaMap[recoIndex.objID!!] = recoIndex
                    }
                    "note" -> saveNote()
                    "created" -> noteCreated = getTimeInMillis(valueString)
                    "updated" -> noteUpdated = getTimeInMillis(valueString)
                    "source" -> {
                        if (valueString == "web.clip") {
                            noteData["html"] = "true"
                        }
                    }
                    // TODO: add as metadata in top of document? add switch "include-metadata"
                    // <note-attributes>
                    //   <latitude>52.48939824910649</latitude>
                    //   <longitude>13.39185053806454</longitude>
                    //   <altitude>43</altitude>
                    //   <author>kibatonic</author>
                    //   <reminder-order>0</reminder-order>
                    // </note-attributes>
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (currentElement) {
                    val data = String(ch, start, length)
                    if (resourceWriter != null) {
                        resourceWriter!!.append(data)
                    } else {
                        currentValue.append(data)
                    }
                }
            }
        }

        val enexFile = File(enexFilename)
        val dirName = enexFile.nameWithoutExtension
        outputDirectory = File(enexFile.parentFile, dirName)
        outputDirectory!!.mkdirs()
        val istream = BufferedReader(InputStreamReader(FileInputStream(enexFilename), "UTF-8"))
        ParserFactory.getParser().parse(InputSource(istream), defaultHandler)
        istream.close()
    }

    private fun isHtml(): Boolean {
        return noteData["html"] != null
    }

    private fun getTimeInMillis(valueString: String) =
        SimpleDateFormat("YYYYMMDD'T'HHmmss'Z'").parse(valueString).time

    private fun createResource() {
        val tmpFile = File.createTempFile("enex2md", ".tmp", outputDirectory)
        println("writing resource to ${tmpFile.absolutePath}")
        resourceFile = tmpFile
        resourceWriter = BufferedWriter(FileWriter(tmpFile))
    }

    private fun closeResource() {
        resourceWriter?.close()
        resourceWriter = null
    }

    private fun convertResourceData(html: Boolean) {
        val decoder = Base64.getMimeDecoder()
        val is1 = decoder.wrap(BufferedInputStream(FileInputStream(resourceFile)))
        val fileName = resourceFile!!.nameWithoutExtension + getFileExtension(resourceMimeType)
        val attachmentsDirectory = File(outputDirectory, getNoteFileName(noteData["title"]!!, html) + ".attachments")
        attachmentsDirectory.mkdirs()
        val outFile = File(attachmentsDirectory, fileName)
        val out = BufferedOutputStream(FileOutputStream(outFile))
        is1.copyTo(out)
        is1.close()
        out.close()
        mediaMap[objectId!!]?.fileName = fileName
        resourceFile?.delete()
        resourceFile = null
    }

    private fun addContent(value: String) {
        val enNoteStart = value.indexOf("<en-note")
        val enNoteEnd = value.indexOf("</en-note>")
        val enNote = value.substring(enNoteStart, enNoteEnd)
        val content = enNote.substring(enNote.indexOf('>') + 1)
        noteData["content"] = content
    }

    private fun saveNote() {
        val title = noteData["title"]!!
        val c = noteData["content"]!!
        val isHtml = noteData["html"] != null
        val content = if (!isHtml) {
            println("Now parsing: $noteData[\"title\"]")
            var markDown =
                try {
                    NoteParser().parse("<div>$c</div>")
                } catch (e: Exception) {
                    e.printStackTrace()
                    "error parsing html: " + e.message
                }

            mediaMap.entries.forEach {
                var width = it.value.objWidth
                var height = it.value.objHeight.toDouble()
                if (width > MAX_IMAGE_WIDTH) {
                    val factor: Double = MAX_IMAGE_WIDTH.toDouble() / width
                    width = MAX_IMAGE_WIDTH
                    height *= factor
                }
                markDown = markDown.replace(it.key + "_width", width.toString())
                markDown = markDown.replace(it.key + "_height", height.roundToInt().toString())
                val fileName = it.value.fileName
                if (fileName != null) {
                    markDown = markDown.replace(it.key, fileName)
                } else {
                    println("fileName for key: $it.key is null!")
                }
            }
            markDown
        } else {
            "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\"/>\n" +
                "    <title>${noteData["title"]}</title>\n" +
                "</head>\n" +
                "<body>\n" + c + "</body></html>"
        }

        println(
            "title: $title\n" +
                "content: \n$content\n"
        )
        val file = File(outputDirectory, getNoteFileName(title, isHtml))
        file.writeText(content)

        val path = file.toPath()
        val attributes = Files.getFileAttributeView(path, BasicFileAttributeView::class.java)
        val lastUpdate = FileTime.fromMillis(noteUpdated)
        // NOTE: setting the create time on MacOS and some other Unix file systems fails silently
        attributes.setTimes(lastUpdate, lastUpdate, FileTime.fromMillis(noteCreated))
    }

    private fun getFileExtension(mimeType: String?): String {
        return when (mimeType) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "application/pdf" -> ".pdf"
            else -> ".unknown"
        }
    }

    private fun getNoteFileName(title: String, html: Boolean): String {
        val ext = if (html) "html" else "md"
        return "$title.$ext"
            .replace('/', ' ')
            .replace('\\', ' ')
            .replace("  ", " ")
            .replace("  ", " ")
    }
}
