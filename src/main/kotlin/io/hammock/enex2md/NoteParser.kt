package io.hammock.enex2md

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.util.Stack

/**
 *
 * @author Mark Hofmann (mark@mark-hofmann.de)
 */
class NoteParser(private val defaultFontSize: Int = 12) {

    private val note = StringBuilder()

    private var lists = Stack<ListInfo>()
    private var listLevel = 0

    private var tables = Stack<TableInfo>()
    private var attributesStack = ArrayList<Attrs>()
    private var previousTag: String? = null
    private var currentStartTag: String? = null
    private var currentEndTag: String? = null
    private var currentElement = false
    private var valueStack = Stack<StringBuilder>()
    private var lineBreaks: Int = 0

    fun parse(xmlData: String): String {

        val defaultHandler = object : DefaultHandler() {
            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                currentElement = true
                valueStack.push(StringBuilder())
                attributesStack.add(Attrs(attributes))
                currentStartTag = qName
                when (qName) {
                    "h1" -> addHeadline(1)
                    "h2" -> addHeadline(2)
                    "h3" -> addHeadline(3)
                    "h4" -> addHeadline(4)
                    "h5" -> addHeadline(5)
                    "ul" -> {
                        listLevel++
                        lists.push(ListInfo(false, 0))
                    }
                    "ol" -> {
                        listLevel++
                        lists.push(ListInfo(true, 0))
                    }
                    "table" -> {
                        if (tables.empty()) {
                            note.append('\n')
                        }
                        tables.push(TableInfo(0))
                    }
                    "td" -> addColumn(attributes)
                    "b" -> bold = true
                    "u" -> underline = true
                    "i" -> italics = true
//                    "span" -> startSpan(attributes)
                    "en-media" -> {
                        addLineBreak()
                        val hash = attributes.getValue("hash")
                        if (attributes.getValue("type").startsWith("image")) {
                            note.append("<img src=\"").append(hash).append("\" alt=\"").append(hash)
                                .append("\" width=\"").append(hash).append("_width")
                                .append("\" height=\"").append(hash).append("_height").append("\" /><br />")
                        } else {
                            note.append("![").append(hash).append("](").append(hash).append(" \"")
                                .append(hash).append("\")")
                        }
                        addLineBreak()
                    }
                    "div" -> {
                        if (currentEndTag == "div" && previousTag != "div" && listLevel == 0) {
                            addLineBreak()
                        }
                    }
                    "li" -> addListItem()
                    "p", "font", "tbody", "col", "colgroup", "tr", "span", "a", "br", "code" -> {
                        // NOOP
                    }
                    else -> {
                        println("start tag not supported: $qName")
                    }
                }
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                currentElement = false
                currentEndTag = qName
                val currentValue = valueStack.peek()
                when (qName) {
                    "h1", "h2", "h3", "h4", "h5" -> {
                        note.append(readCurrentValue())
                        note.append("\n\n")
                    }
                    "a" -> {
                        val href = attributesStack.last().getValue("href")
                        if (currentValue.isEmpty()) {
                            note.append("<$href>")
                        } else {
                            note.append("[$currentValue]($href)")
                        }
                    }
                    "ul", "ol" -> {
                        lists.pop()
                        listLevel--
                        if (listLevel == 0) {
                            addLineBreak()
                        }
                    }
                    "code" -> {
                        addText()
                    }
                    "font" -> {
                        println("font: $currentValue")
                    }
                    "b" -> {
                        addText()
                        bold = false
                    }
                    "u" -> {
                        addText()
                        underline = false
                    }
                    "i" -> {
                        addText()
                        italics = false
                    }
                    "li" -> {
                        addText()
                        if (note.length > 1 && !(note[note.length - 1] == '\n')) {
                            note.append('\n')
                        }
                    }
                    "span" -> {
                        addText()
                    }
                    "div" -> {
                        addText()
                        val tableLevel = tables.size
                        if (previousTag == "div" || previousTag == "a") {
                            addBr()
                        } else {
                            note.append(' ')
                        }
                        if (tableLevel == 0 && previousTag != "span") {
                            // don't add div line-breaks inside ul/ol constructs
                            addLineBreak()
//                            if (listLevel == 0) {
//                                if (tableLevel == 0 && (previousTag == "div" || previousTag == "span")) {
//                                    addLineBreak()
//                                }
//                            }
//                        } else {
//                            if (currentEndTag != "div") {
//                                addBr()
//                            } else {
//                                note.append(' ')
//                            }
////                            note.append(' ')
                        }
                    }
                    "table" -> {
                        tables.pop()
                    }
                    "td" -> addText()
                    "tr" -> endRow()
                    "br" -> {
//                        addText()
//                        addBr()
                        lineBreaks++
                    }
                    "p", "col", "colgroup", "tbody", "en-media" -> {
                        // NOOP
                        if (!currentValue.isEmpty()) {
                            println("ignored tag '$qName' has a value: $currentValue")
                        }
                        addText()
                    }
                    else -> {
                        println("end tag not supported: $qName , value: $currentValue")
                        addText()
                    }
                }
                previousTag = qName
                attributesStack.removeAt(attributesStack.size - 1)
                valueStack.pop()
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (!currentElement) {
                    valueStack.peek().append(' ')
                }
                val string = String(ch, start, length)
                valueStack.peek().append(string)
            }
        }
        val istream = ByteArrayInputStream(xmlData.toByteArray())
        ParserFactory.getParser().parse(istream, defaultHandler)
        istream.close()
        return note.toString()
    }

    private fun addListItem() {
        if (!lists.empty()) {
            val currentList = lists.peek()
            if (listLevel == 1 && currentList.counter == 0) {
                addLineBreak()
            }
            val prefix = if (currentList.ordered) {
                "${currentList.counter + 1}. "
            } else {
                "* "
            }
            currentList.counter++
            note.append(addPadding(prefix, (listLevel - 1) * 3))
        }
    }

    private fun addColumn(attributes: Attributes) {
        val rowspan = attributes.getValue("rowspan")
        val currentTable = tables.peek()
        if (rowspan != null) {
            currentTable.rowSpan = rowspan.toInt()
            currentTable.rowSpanStart = true
            currentTable.rowSpanColumn = currentTable.columnIndex
        }
        if (currentTable.columnIndex > 0) {
            note.append(" |")
            if (currentTable.rowSpanColumn == currentTable.columnIndex && currentTable.rowSpan-- > 1) {
                if (currentTable.rowSpanStart) {
                    // ignore the column that started the rowspan
                    currentTable.rowSpanStart = false
                } else {
//                    currentTable.rowSpan--
                    note.append('|')
                }
            }
            note.append(' ')
        }
        currentTable.columnIndex++
    }

    private fun addHeadline(level: Int) {
        val currentValue = valueStack.peek()
        if (!currentValue.isBlank()) {
            note.append("\n\n")
            for (i in 0..level) {
                note.append("#")
            }
            note.append(' ')
        }
    }

    private class Attr(val name: String?, val value: String?)

    private class Attrs(attributes: Attributes) {
        val list = ArrayList<Attr>()

        init {
            for (i in 0..attributes.length) {
                list.add(Attr(attributes.getQName(i), attributes.getValue(i)))
            }
        }

        fun getValue(name: String): String? {
            list.forEach { if (name == it.name) return it.value }
            return null
        }
    }

    private fun getAttribute(style: String, name: String, defaultValue: String): String {
        return if (style.contains(name)) {
            val valueStart = style.indexOf(name) + name.length + 2
            var valueEnd = style.indexOf(';', valueStart)
            if (valueEnd == -1) {
                valueEnd = style.indexOf('\"', valueStart)
            }
            val value = style.substring(valueStart, valueEnd).trim()
//            println("key: $name, valueStart: $valueStart, valueEnd:$valueEnd, value:$value\nstyle:$style")
            if (value.isEmpty()) {
                defaultValue
            } else {
                value
            }
        } else {
            defaultValue
        }
    }

    class TextAttributes {
        var bold: Boolean? = null
        var underline: Boolean? = null
        var strikeThrough: Boolean? = null
        var fontSize: Int? = null
        var fontFamily: String = ""
        var italics: Boolean? = null
    }

    private fun getTextAttributes(): TextAttributes {
        val textAttributes = TextAttributes()
        attributesStack.forEach { attributes ->
            val style = attributes.getValue("style")
            if (!style.isNullOrEmpty()) {
                addStyleAttributes(style, textAttributes)
            }
        }
        if (textAttributes.italics == null && italics) {
            textAttributes.italics = true
        }
        if (textAttributes.bold == null && bold) {
            textAttributes.bold = true
        }
        if (textAttributes.underline == null && underline) {
            textAttributes.underline = true
        }
        return textAttributes
    }

    private fun addStyleAttributes(style: String, textAttributes: TextAttributes) {
        val fontSizeValue =
            getAttribute(style, "font-size", defaultFontSize.toString()).trim().removeSuffix("px")
        val fontSize =
            if (fontSizeValue.isEmpty()) defaultFontSize else fontSizeValue.toIntOrNull() ?: defaultFontSize
        val fontFamily = getAttribute(style, "font-family", "")
        val bold = getAttribute(style, "font-weight", "") == "bold"
        val underline = getAttribute(style, "text-decoration", "").contains("underline")
        if (textAttributes.bold == null && bold) {
            textAttributes.bold = true
        }
        if (textAttributes.underline == null && underline) {
            textAttributes.underline = true
        }
        if (textAttributes.fontSize == null && fontSize != defaultFontSize) {
            textAttributes.fontSize = fontSize
        }
        if (textAttributes.fontFamily.isEmpty() && !fontFamily.isEmpty()) {
            textAttributes.fontFamily = fontFamily
        }
    }

    var italics: Boolean = false
    var bold: Boolean = false
    var underline: Boolean = false
    private fun addText() {
        if (valueStack.isEmpty() || valueStack.peek().isEmpty()) {
            return
        }
        val textAttributes = getTextAttributes()
        addFontStyle(textAttributes)
        note.append(readCurrentValue())
        addFontStyle2(textAttributes)
        if (lineBreaks > 0) {
//            if (previousTag == "div" && tables.empty()) {
            for (i in 0..lineBreaks) {
                addBr(true)
            }
//            }
            lineBreaks = 0
        }
    }

    private fun addBr(force: Boolean = false) {
        if (force || !note.endsWith("<br />")) {
            note.append("<br />")
            if (lineBreaks > 0) {
                lineBreaks--
            }
        }
    }

    private var previousFontStyle: String? = null

    // TODO: don't add linebreaks the same way inside code block as outside
    private var insideCodeBlock = false

    private fun addFontStyle(textAttributes: NoteParser.TextAttributes) {
        if (textAttributes.fontFamily.toLowerCase().contains("mono")) {
            // merge code blocks
            if (previousFontStyle == "code") {
                val newNote = note.removeRange(note.length - 5 until note.length)
                note.clear().append(newNote)
            } else {
                note.append("\n```\n")
            }
            previousFontStyle = "code"
        } else {
            previousFontStyle = null
        }
        if (textAttributes.italics == true && textAttributes.bold == true) {
            note.append("***")
        } else if (textAttributes.italics == true) {
            note.append("_")
        } else if (textAttributes.bold == true) {
            note.append("**")
        }
        if (textAttributes.underline == true) {
            note.append("<u>")
        }
        if (textAttributes.strikeThrough == true) {
            note.append("~~")
        }
    }

    private fun addFontStyle2(textAttributes: NoteParser.TextAttributes) {
        if (textAttributes.strikeThrough == true) {
            note.append("~~")
        }
        if (textAttributes.underline == true) {
            note.append("</u>")
        }
        if (textAttributes.italics == true && textAttributes.bold == true) {
            note.append("***")
        } else if (textAttributes.italics == true) {
            note.append("_")
        } else if (textAttributes.bold == true) {
            note.append("**")
        }
        if (textAttributes.fontFamily.toLowerCase().contains("mono")) {
            note.append("\n```\n")
        }
    }

    private fun readCurrentValue(): String {
        val currentValue = valueStack.peek()
        val s = currentValue.toString()
        currentValue.clear()
        return s
    }

    private fun endRow() {
        val currentTable = tables.peek()
        note.append('\n')
        if (currentTable.rowIndex == 0) {
            note.append("--- ")
            for (i in 0 until currentTable.columnIndex - 1) {
                note.append("| --- ")
            }
            note.append('\n')
        }
        currentTable.rowIndex++
        currentTable.columnIndex = 0
    }

    private fun addLineBreak() {
        if (note.length > 2 && !(note[note.length - 1] == '\n' && note[note.length - 2] == '\n')) {
            note.append('\n')
        }
    }

    private fun addPadding(s: String, count: Int): String {
        val sb = StringBuilder(s.length + count)
        for (i in 1..count) {
            sb.append(' ')
        }
        sb.append(s)
        return sb.toString()
    }

    private class ListInfo(var ordered: Boolean = false, var counter: Int = 0)
    private class TableInfo(
        var rowIndex: Int = 0,
        var columnIndex: Int = 0,
        var rowSpanColumn: Int = 0,
        var rowSpanStart: Boolean = false,
        var rowSpan: Int = 0
    )
}
