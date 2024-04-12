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
package org.wcs.smart.intelligence.xml;

import javax.xml.datatype.DatatypeConfigurationException;

import org.wcs.smart.ca.Label;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceAttachment;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.intelligence.xml.model.IntelligenceType;
import org.wcs.smart.intelligence.xml.model.LabelType;
import org.wcs.smart.intelligence.xml.model.PointType;
import org.wcs.smart.util.SmartUtils;

/**
 * Class responsible for converting intelligence object to XML.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceToXmlConverter {

	public static IntelligenceType toXml(Intelligence i) throws DatatypeConfigurationException {
		IntelligenceType xml = new IntelligenceType();
		
		/* names */
		for(Label label : i.getNames()) {
			xml.getName().add(toXmlLabel(label));
		}

		/* dates */
		xml.setReceivedDate(SmartUtils.toXmlDate(i.getReceivedDate()));
		xml.setFromDate(SmartUtils.toXmlDate(i.getFromDate()));
		xml.setToDate(SmartUtils.toXmlDate(i.getToDate()));

		/* source */
		if (i.getSource() != null){
			xml.setSource(i.getSource().getKeyId());
		}

		/* description */
		xml.setDescription(i.getDescription());

		/* points */
		for(IntelligencePoint p : i.getPoints()) {
			xml.getPoints().add(toXmlPoint(p));
		}

		/* attachments */
		for(IntelligenceAttachment a : i.getAttachments()) {
			xml.getAttachments().add(a.getFilename());
		}
		
		return xml;
	}

	private static LabelType toXmlLabel(Label label){
		if (label == null) {
			return null;
		}
		LabelType lbl = new LabelType();
		lbl.setLanguageCode(label.getLanguage().getCode());
		lbl.setValue(label.getValue());
		return lbl;
	}

	private static PointType toXmlPoint(IntelligencePoint p) {
		if (p == null) {
			return null;
		}
		PointType pt = new PointType();
		pt.setX(p.getX());
		pt.setY(p.getY());
		return pt;
	}
	
}