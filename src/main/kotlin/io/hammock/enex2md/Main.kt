package io.hammock.enex2md

import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

/**
 *
 * @author Mark Hofmann (mark@mark-hofmann.de)
 */
@Suppress("UnusedMainParameter")
fun main(args: Array<String>) {
//    com.sun.org.apache.xml.internal.security.Init.init();
    // either export by name, or all enex files in the current directory
//    Parser().parse("/Users/mark/Downloads/Masterplan.enex")
//    Parser().parse("/Users/mark/Downloads/htmlnote.enex")
//    Parser().parse("/Users/mark/Downloads/Thai Thai Shop.enex")
//    Parser().parse("/Users/mark/Downloads/Umstellung auf Linux.enex")
//    Parser().parse("/Users/mark/Downloads/HOWTOs.enex")
//    Parser().parse("/Users/mark/Downloads/TODO 2019.enex")
    Parser().parse("/Users/mark/Downloads/privat.enex")
//    Parser().parse("/Users/mark/Downloads/MyNotes.enex")
//    Parser().parse("/Users/mark/Downloads/Impfausweis.enex")
//    Parser().parse("/Users/mark/Downloads/Eigentumswohnungen.enex")
}

object ParserFactory {
    fun getParser(): SAXParser {
        val parserFactory: SAXParserFactory = SAXParserFactory.newInstance()
        parserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return parserFactory.newSAXParser()
    }
}

