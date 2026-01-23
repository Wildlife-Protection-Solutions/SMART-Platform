/*
 * Copyright (C) 2016 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.util;

/**
 * UTF8 output stream and removes invalid XML file characters.
 * 
 * This should be used as input to the marshaller to ensure all non-supported
 * xml characters are removed.
 *  
 * try(BufferedWriter fw= Files.newBufferedWriter(xmlFile.toAbsolutePath());
 * 	SanitizingXmlFileWriter writer = new SanitizingXmlFileWriter(fw)){
 * 		marshaller.marshal(element, writer);
 * }
 * 
 */
/*
 * I have applied this to the Profile entity and record exports ONLY.  Other xml updates
 * can be updated as errors arise.
 */
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

public class SanitizingXmlFileWriter extends FilterWriter {

    public SanitizingXmlFileWriter(Writer out) {
        super(out);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        String cleaned = stripInvalidXMLChars(new String(cbuf, off, len));
        out.write(cleaned);
    }

    @Override
    public void write(int c) throws IOException {
        if (isValidXmlChar((char) c)) {
            out.write(c);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        String cleaned = stripInvalidXMLChars(str.substring(off, off + len));
        out.write(cleaned);
    }

    private static boolean isValidXmlChar(char c) {
        return c == 0x9 || c == 0xA || c == 0xD ||
               (c >= 0x20 && c <= 0xD7FF) ||
               (c >= 0xE000 && c <= 0xFFFD);
    }

    public static String stripInvalidXMLChars(String input) {
        StringBuilder out = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (isValidXmlChar(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }    
}