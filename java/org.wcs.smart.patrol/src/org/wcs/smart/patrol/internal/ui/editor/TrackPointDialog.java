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
package org.wcs.smart.patrol.internal.ui.editor;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Track;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Dialog for display tack points for a track.
 * <p>The track points are displayed in a simple table.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class TrackPointDialog extends TitleAreaDialog {

	private Track track;
	
	/**
	 * @param parentShell parent shell
	 * @param t the track to display
	 */
	public TrackPointDialog(Shell parentShell, Track t) {
		super(parentShell);
		this.track = t;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		super.setMessage(Messages.TrackPointDialog_DialogMessage);
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableViewer trackviewer = new TableViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = Math.min(trackviewer.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).y, 400);
		trackviewer.getTable().setLayoutData(gd);
		trackviewer.getTable().setLinesVisible(true);
		trackviewer.getTable().setHeaderVisible(true);
		trackviewer.setContentProvider(ArrayContentProvider.getInstance());

		//--X
		TableViewerColumn column = new TableViewerColumn(trackviewer,SWT.NONE);
		column.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element) {
				if (element instanceof Coordinate){
					return String.valueOf(((Coordinate)element).x);
				}
				return ""; //$NON-NLS-1$
			}
		});
		column.getColumn().setText(Messages.TrackPointDialog_XColumnName);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(false);
		column.getColumn().setWidth(200);
		
		//-Y
		column = new TableViewerColumn(trackviewer,SWT.NONE);
		column.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element) {
				if (element instanceof Coordinate){
					return String.valueOf(((Coordinate)element).y);
				}
				return ""; //$NON-NLS-1$
			}
		});
		column.getColumn().setText(Messages.TrackPointDialog_YColumnName);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(false);
		column.getColumn().setWidth(200);
		
		//time
		column = new TableViewerColumn(trackviewer,SWT.NONE);
		column.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element) {
				if (element instanceof Coordinate){
					Date d = new Date( (long) ((Coordinate)element).z );
					DateFormat df = DateFormat.getDateTimeInstance();
					df.setTimeZone(Track.ZTIMEZONE);
					return df.format(d);
				}
				return ""; //$NON-NLS-1$
			}
		});
		column.getColumn().setText(Messages.TrackPointDialog_ZColumnName);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(false);
		column.getColumn().setWidth(200);
		
		trackviewer.setInput(track.getLineString().getCoordinates());
		
		getShell().setText(Messages.TrackPointDialog_DialogTitle);
		
		return main;
		
	}
	
	
	/** Only OK button is displayed
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}
	
	/**
	 * Dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}

}
