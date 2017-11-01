package org.wcs.smart.asset;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public class AssetUtils {

	public static final void main (String[] args) {
	
		int days = 3;
		int hours = 14;
		int minutes = 4;
		double seconds = 2.3;
		
		double millisecond = seconds * 1000 + minutes * 60 * 1000 + hours * 60 * 60 * 1000 + days * 24 * 60 * 60 * 1000;
		System.out.println(formatTime(millisecond));
	}
	
	
	public static String formatTime(double timeInSeconds) {
		int days = (int) Math.floor( timeInSeconds / 86_400.0 );
		double remainder = timeInSeconds - days * 86_400.0;
		
		double hours = remainder / 3_600.0;
		
//		int hours = (int)Math.floor( remainder / 3_600_000.0 );
//		remainder = remainder - hours * 3_600_000;
//				
//		int minutes =  (int)Math.floor( remainder / 60_000.0);
//		remainder = remainder - minutes* 60_000;
//		
//		int seconds =  (int)Math.floor( remainder / 1000.0);
		if (timeInSeconds == 0) return "0 days";
//		return MessageFormat.format("{0} days {1}:{2}:{3}", days, hours, minutes, seconds);
		return MessageFormat.format("{0} days {1,number,#.##} hours", days, hours);
	}
	
	
	public static boolean confirmPassword(Shell shell, String title, String message) {
		InputDialog confirm = new InputDialog(shell, title, message, "", null){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					
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
