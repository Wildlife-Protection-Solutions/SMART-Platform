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

import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;

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
	public String getOptionLabel(ConnectServerOption.ConnectionOption option){
		switch(option){
		case MAX_PROCESSING_WAIT_TIME:
			return Messages.ServerOptionLabelProvider_ServerWaitTimeLabel;
		case MAX_RETRY_DOWNLOAD:
			return Messages.ServerOptionLabelProvider_DownloadRetryCountLabel;
		case MAX_RETRY_UPLOAD:
			return Messages.ServerOptionLabelProvider_UploadRetryCountLabel;
		case RETY_WAIT_TIME:
			return Messages.ServerOptionLabelProvider_WaitTimeLabel;
		case SYNC_AUTOMATICALLY:
			return Messages.ServerOptionLabelProvider_CheckLabel;
		case SYNC_PROMPT_PASSWORD:
			return Messages.ServerOptionLabelProvider_PromptLabel;
		case SYNC_DOWNLOAD:
			return Messages.ServerOptionLabelProvider_AutoDownloadLabel;
		case SYNC_AUTO_UPLOAD:
			return Messages.ServerOptionLabelProvider_AutoUploadLabel;
		case DOWNLOAD_ON_STARTUP:
			return Messages.ServerOptionLabelProvider_StartupDownloadLabel;
		case UPLOAD_ON_STARTUP:
			return Messages.ServerOptionLabelProvider_StartuUploadLabel;
		case DOWNLOAD_ON_SHUTDOWN:
			return Messages.ServerOptionLabelProvider_ShutdownDownloadLabel;
		case UPLOAD_ON_SHUTDOWN:
			return Messages.ServerOptionLabelProvider_ShutdownUploadLabel;
		case PACKAGE_PROMPT:
			return Messages.ServerOptionLabelProvider_PackagePrompt;
		case PACKAGE_PROMPT_SIZE:
			return Messages.ServerOptionLabelProvider_MegaBytesLabel;
		default:
			break;
		}
		return "ERROR"; //$NON-NLS-1$
	}

	/**
	 * The gui tooltip for the server options.
	 * 
	 * @param option
	 * @return
	 */
	public String getOptionTooltip(ConnectServerOption.ConnectionOption option){
		switch(option){
		case MAX_PROCESSING_WAIT_TIME:
			return Messages.ServerOptionLabelProvider_ServerWaitTimeTooltip;
		case MAX_RETRY_DOWNLOAD:
			return Messages.ServerOptionLabelProvider_DownloadRetryTooltip;
		case MAX_RETRY_UPLOAD:
			return Messages.ServerOptionLabelProvider_UploadRetryTooltip; 
		case RETY_WAIT_TIME:
			return Messages.ServerOptionLabelProvider_WaitTimeTooltip;
		case SYNC_AUTOMATICALLY:
			return Messages.ServerOptionLabelProvider_AutoTooltip;
		case SYNC_PROMPT_PASSWORD:
			return Messages.ServerOptionLabelProvider_PromptTooltip;
		case SYNC_DOWNLOAD:
			return Messages.ServerOptionLabelProvider_AutoDownloadTooltip;
		case SYNC_AUTO_UPLOAD:
			return Messages.ServerOptionLabelProvider_AutoUploadTooltip;
		case DOWNLOAD_ON_STARTUP:
			return Messages.ServerOptionLabelProvider_StartupDownloadTooltip;
		case UPLOAD_ON_STARTUP:
			return Messages.ServerOptionLabelProvider_StartupUploadTooltip;
		case DOWNLOAD_ON_SHUTDOWN:
			return Messages.ServerOptionLabelProvider_ShutdownDownloadTooltip;
		case UPLOAD_ON_SHUTDOWN:
			return Messages.ServerOptionLabelProvider_ShutdownUploadTooltip;
		case PACKAGE_PROMPT:
			return Messages.ServerOptionLabelProvider_PackagePromptTooltip;
		case PACKAGE_PROMPT_SIZE:
			return Messages.ServerOptionLabelProvider_SizeTooltip;
		default:
			break;
		}
		return "ERROR"; //$NON-NLS-1$
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
	public String getValueInDisplayUnits(ConnectServerOption.ConnectionOption option, ConnectServer server){
		String value = null;
		if (server.getOptions() == null || server.getOptions().get(option.name()) == null){
			value = option.getDefaultValueAsString();
		}else{
			value = server.getOptions().get(option.name()).getValue();
		}
		
		if (option == ConnectServerOption.ConnectionOption.MAX_PROCESSING_WAIT_TIME){
			value = String.valueOf(Long.parseLong(value) / 1000);
		}
		return value;
	}
}
