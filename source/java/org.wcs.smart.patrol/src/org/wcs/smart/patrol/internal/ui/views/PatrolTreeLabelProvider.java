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
package org.wcs.smart.patrol.internal.ui.views;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.IFolder;
import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.common.folder.NoneFolder;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.DateGroupBy.Type;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.ui.PatrolEditorInput;

/**
 * Label provider for patrol list tree viewer 
 * 
 * @author Emily
 *
 */
public class PatrolTreeLabelProvider extends ColumnLabelProvider {
	
	private Font boldFont;
	private IconCache iconCache;
	public PatrolTreeLabelProvider(){
		FontData fd = Display.getDefault().getActiveShell().getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(Display.getCurrent(), fd);
		iconCache = new IconCache(null, IconManager.Size.ICON);
	}
	
	@Override
	public void dispose(){
		boldFont.dispose();
		iconCache.dispose();
	}
	@Override
	public Image getImage(Object element){
		if (element instanceof PatrolEditorInput pi){
			return iconCache.getImage(pi.getType());			
		}else if (element instanceof PatrolType pt){
			return iconCache.getImage(pt);
		}else if (element instanceof PatrolTransportType ptt){
			return iconCache.getImage(ptt);
		}else if (element instanceof PatrolMandate){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MANDATE_ICON);
		}else if (element instanceof Team){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_TEAM_ICON);
		}else if (element instanceof Station){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STATION_ICON);
		
		}else if (element instanceof DateGroupBy){
			if (((DateGroupBy)element).getType() == Type.MONTH){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.MONTH_ICON);
			}else if (((DateGroupBy)element).getType() == Type.YEAR){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.YEAR_ICON);
			}
		}else if (element instanceof IFolder) {
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_FOLDER_ICON);
		}
		return super.getImage(element);
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof PatrolEditorInput){
			return ((PatrolEditorInput)element).getPatrolId() 
					+ "  [" + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format( ((PatrolEditorInput)element).getStartDate())  //$NON-NLS-1$
					+ " - " + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format( ((PatrolEditorInput)element).getEndDate())  //$NON-NLS-1$
					+ " ]"; //$NON-NLS-1$ 
		}else if (element instanceof NamedItem){
			return ((NamedItem) element).getName();		
		}else if (element instanceof DateGroupBy){
			return ((DateGroupBy)element).getLabel();
		}else if (element == NoneFolder.INSTANCE) {
			return Messages.PatrolListView_NoneFolder_Name;
		}
		return super.getText(element);
	}
	
	@Override
	public Color getBackground(Object element) {
		return null;
	}

	@Override
	public Color getForeground(Object element) {
		if (!(element instanceof PatrolEditorInput)){
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
		}
		return null;
	}
	
	@Override
	public Font getFont(Object element) {
		if (!(element instanceof PatrolEditorInput)){
			return boldFont;
		}
		return null;
	}
}
