/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.report;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;

/**
 * Supported report output formats.
 * @author Emily
 *
 */
public enum ReportFormat {

	HTML("html", "ReportFormat_HTMLOutType", null, null), //$NON-NLS-1$ //$NON-NLS-2$
	PDF("pdf", "ReportFormat_PdfOutType", "org.eclipse.birt.report.engine.emitter.pdf", "application/pdf"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	DOC("doc", "ReportFormat_WordOutType", "org.eclipse.birt.report.engine.emitter.word", "application/msword"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	ODT("odt", "ReportFormat_OdfOutType", "org.eclipse.birt.report.engine.emitter.odt", "application/odt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	
	private String typeKey;
	private String guiNameKey;
	private String emitterId;
	private String responseType;
	
	ReportFormat(String typeKey, String guiNameKey, String emitterId, String responseType){
		this.typeKey = typeKey;
		this.guiNameKey = guiNameKey;
		this.emitterId = emitterId;
		this.responseType = responseType;
	}
	
	public String getResponseType(){
		return this.responseType;
	}
	public String getTypeKey(){
		return this.typeKey;
	}
	
	public String getGuiName(Locale l){
		return Messages.getString(guiNameKey, l);
	}
	
	public String getEmitterId(){
		return this.emitterId;
	}
	
	public String getContentDisposition(String filename){
		if (this == DOC){
			 return "attachment;filename=" + filename + ".doc"; //$NON-NLS-1$ //$NON-NLS-2$
		}else if (this == ODT){
			return "attachment;filename=" + filename + ".odt"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}
}
