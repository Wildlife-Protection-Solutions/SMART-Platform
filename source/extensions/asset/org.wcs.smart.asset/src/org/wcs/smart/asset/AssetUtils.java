/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Utilties for the asset plugin
 * 
 * @author Emily
 *
 */
public class AssetUtils {

	/**
	 * Formats the given time in seconds into days and hours.  (2 days 6.54 hours)
	 * @param timeInSeconds
	 * @return
	 */
	public static String formatTime(double timeInSeconds) {
		int days = (int) Math.floor( timeInSeconds / 86_400.0 );
		
		double remainder = timeInSeconds - days * 86_400.0;
		double hours = remainder / 3_600.0;
		
		if (timeInSeconds == 0) return "0 days";
		return MessageFormat.format("{0} days {1,number,#.##} hours", days, hours);
	}
	
	
	/**
	 * Reconfirms the curernt users password.
	 * @param shell
	 * @param title
	 * @param message
	 * @return
	 */
	public static boolean confirmPassword(Shell shell, String title, String message) {
		InputDialog confirm = new InputDialog(shell, title, message, "", null){ //$NON-NLS-1$
					
			@Override
			protected void okPressed() {
				if (!HibernateManager.validatePassword(getText().getText(), SmartDB.getCurrentEmployee())){
					setErrorMessage("Invalid Password");
				}else{
					setReturnCode(OK);
					close();
				}
			}
			
			@Override
			protected int getInputTextStyle() {
				return super.getInputTextStyle() | SWT.PASSWORD;
			}
		};
		if (confirm.open() != Window.OK){
			return false;
		}
		return true;
	}
}
