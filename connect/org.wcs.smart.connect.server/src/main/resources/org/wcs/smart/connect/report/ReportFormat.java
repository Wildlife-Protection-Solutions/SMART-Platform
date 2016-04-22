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

/**
 * Supported report output formats.
 * @author Emily
 *
 */
public enum ReportFormat {

	HTML("html", "HTML", null, null),
	PDF("pdf", "PDF", "org.eclipse.birt.report.engine.emitter.pdf", "application/pdf"),
	DOC("doc", "Word Document (.doc)", "org.eclipse.birt.report.engine.emitter.word", "application/doc"),
	ODT("odt", "Open Document (.odf)", "org.eclipse.birt.report.engine.emitter.odt", "application/odt");
	
	private String typeKey;
	private String guiName;
	private String emitterId;
	private String responseType;
	
	ReportFormat(String typeKey, String guiName, String emitterId, String responseType){
		this.typeKey = typeKey;
		this.guiName = guiName;
		this.emitterId = emitterId;
		this.responseType = responseType;
	}
	
	public String getResponseType(){
		return this.responseType;
	}
	public String getTypeKey(){
		return this.typeKey;
	}
	
	public String getGuiName(){
		return this.guiName;
	}
	
	public String getEmitterId(){
		return this.emitterId;
	}
}
