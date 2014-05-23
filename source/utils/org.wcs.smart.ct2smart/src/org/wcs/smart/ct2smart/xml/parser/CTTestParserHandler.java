/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.ct2smart.xml.parser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CTTestParserHandler extends DefaultHandler {

	private static final String E	= "E"; //$NON-NLS-1$
	private static final String A	= "A"; //$NON-NLS-1$
	private static final String S	= "S"; //$NON-NLS-1$
	private static final String T	= "T"; //$NON-NLS-1$

	private boolean isS = false;
	private int ei, en, sai, san, sav, tai, tan, tav;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		if (E.equals(qName)) {
			ei = Math.max(ei, attributes.getValue(TagE.I).length());
			en = Math.max(en, attributes.getValue(TagE.N).length());

		} else if (S.equals(qName)) {
			isS = true;

		} else if (T.equals(qName)) {
			isS = false;

		} else if (A.equals(qName)) {
			if (isS) {
				sai = Math.max(sai, attributes.getValue(TagA.I).length());
				san = Math.max(san, attributes.getValue(TagA.N).length());
				sav = Math.max(sav, attributes.getValue(TagA.V).length());
				
			} else {
				tai = Math.max(tai, attributes.getValue(TagA.I).length());
				tan = Math.max(tan, attributes.getValue(TagA.N).length());
				tav = Math.max(tav, attributes.getValue(TagA.V).length());
			}
		}
		
	}

	public void printResult() {
		System.out.println(ei);
		System.out.println(en);
		
		System.out.println(sai);
		System.out.println(san);
		System.out.println(sav);

		System.out.println(tai);
		System.out.println(tan);
		System.out.println(tav);
	}
	
}
