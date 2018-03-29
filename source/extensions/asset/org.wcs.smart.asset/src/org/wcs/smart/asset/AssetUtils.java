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
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

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
		
		if (timeInSeconds == 0) return Messages.AssetUtils_noDays;
		return MessageFormat.format(Messages.AssetUtils_DayHourLabel, days, hours);
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
					setErrorMessage(Messages.AssetUtils_InvalidPasswordError);
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
	
	
	/**
	 * Returns true if the obs matches one of the observations
	 * in the all array.  Matches requires the category to be the same
	 * and all the attributes to be identical.
	 * @param obs
	 * @param all
	 * @return
	 */
	public static boolean containsObservation(WaypointObservation obs, List<WaypointObservation> all) {
		for (WaypointObservation wo : all) {
			if (!wo.getCategory().equals(obs.getCategory())) continue;
			
			if (wo.getAttributes().size() != obs.getAttributes().size()) continue;
			
			boolean ok = true;
			for (WaypointObservationAttribute a : wo.getAttributes()) {
				WaypointObservationAttribute matching = null;
				for (WaypointObservationAttribute aa : obs.getAttributes()) {
					if (aa.getAttribute().equals(a.getAttribute())){
						matching = aa;
						break;
					}
				}
				if (matching == null) {
					ok = false;
					break;
				}
				switch(a.getAttribute().getType()) {
					case BOOLEAN:
					case NUMERIC:
						ok = Objects.equals(a.getNumberValue(), matching.getNumberValue());
						break;
					case DATE:
					case TEXT:
						ok = Objects.equals(a.getStringValue(), matching.getStringValue());
						break;
					case LIST:
						ok = Objects.equals(a.getAttributeListItem(), matching.getAttributeListItem());
						break;
					case TREE:
						ok = Objects.equals(a.getAttributeTreeNode(), matching.getAttributeTreeNode());
						break;				
				}
				if (!ok) break;
			}
			if (ok) return true;
		}
		return false;
	}
}
