package io.hammock.enex2md

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream

/**
 *
 * @author Mark Hofmann (mark@mark-hofmann.de)
 */
class RecoIndexParser {

    private var recoIndex = RecoIndex()

    fun parse(xmlData: String): RecoIndex {
        val defaultHandler = object : DefaultHandler() {
            var currentElement = false
            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                currentElement = true
                when (qName) {
                    "recoIndex" -> {
                        for (i in 0..attributes.length) {
                            val attrName = attributes.getQName(i)
                            val attrValue = attributes.getValue(i)
                            when (attrName) {
                                "objID" -> recoIndex.objID = attrValue
                                "docType" -> recoIndex.docType = attrValue
                                "objType" -> recoIndex.objType = attrValue
                                "lang" -> recoIndex.lang = attrValue
                                "objWidth" -> recoIndex.objWidth = attrValue.toInt()
                                "objHeight" -> recoIndex.objHeight = attrValue.toInt()
                            }
                        }
                    }
                }
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                currentElement = false
            }
        }
        val istream = ByteArrayInputStream(xmlData.toByteArray())
        ParserFactory.getParser().parse(istream, defaultHandler)
        istream.close()
        return recoIndex
    }
}
