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
package org.wcs.smart.connect.ui.server.configure;

import org.wcs.smart.connect.model.ConnectServer;

/**
 * Label provider for server options.
 * 
 * @author Emily
 *
 */
public enum ServerOptionLabelProvider {
	INSTANCE;

	/**
	 * The gui label for the server options.
	 * 
	 * @param option
	 * @return
	 */
	public String getOptionLabel(ConnectServer.Option option){
		switch(option){
		case MAX_PROCESSING_WAIT_TIME:
			return "Server Processing Wait Time (sec)";
		case MAX_RETRY_DOWNLOAD:
			return "Download Retry Count";
		case MAX_RETRY_UPLOAD:
			return "Upload Retry Count";
		case RETY_WAIT_TIME:
			return "Retry Wait Time (milli sec)";
		default:
			break;
		}
		return "ERROR";
	}
	
	/**
	 * The gui tooltip for the server options.
	 * 
	 * @param option
	 * @return
	 */
	public String getOptionTooltip(ConnectServer.Option option){
		switch(option){
		case MAX_PROCESSING_WAIT_TIME:
			return "The length of time to wait for processing to finish on server before failing.";
		case MAX_RETRY_DOWNLOAD:
			return "The number of times to retry downloading files before failing.";
		case MAX_RETRY_UPLOAD:
			return "The number of times to rety uploading files before failing"; 
		case RETY_WAIT_TIME:
			return "The number of milli seconds to wait between retrying";
			
		default:
			break;
		
		}
		return "ERROR";
	}
	
	/**
	 * The option from the given server as a String for display to the user.
	 * This function will convert the units as required to match the information
	 * provided in the getOptionLabel function.
	 * 
	 * @param option
	 * @param server
	 * @return
	 */
	public String getValueInDisplayUnits(ConnectServer.Option option, ConnectServer server){
		String value = server.getOption(option);
		if (value == null){
			value = option.getDefaultValueAsString();
		}
		if (option == ConnectServer.Option.MAX_PROCESSING_WAIT_TIME){
			value = String.valueOf(Long.parseLong(value) / 1000);
		}
		return value;
	}
}
