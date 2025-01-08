/*
 * Copyright (C) 2018 Wildlife Conservation Society
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
package org.wcs.smart.imageprocessor;

import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * Image processing option
 * @author Emily
 *
 */
public class ProcessingItem {

	/**
	 * Processing Status
	 */
	public enum Status{
		OK,
		WARNING,
		ERROR,
		PROCESSING
		
	}
	
	private ISmartAttachment file;
	private Status status;
	private String message;
	
	public ProcessingItem(ISmartAttachment file) {
		this.file = file;
	}
	
	public Status getStatus() {
		return this.status;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public ISmartAttachment getAttachment() {
		return this.file;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
}
