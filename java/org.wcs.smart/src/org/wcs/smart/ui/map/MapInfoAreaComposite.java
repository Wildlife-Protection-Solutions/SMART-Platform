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
package org.wcs.smart.ui.map;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.commands.ChangeCRSCommand;
import org.locationtech.udig.project.render.IViewportModelListener;
import org.locationtech.udig.project.render.ViewportModelEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * MapInfoAreaComposite
 * 
 * @author Emily Gouge, Graham Davis (Refractions Research, Inc.)
 * @author elitvin
 * @since 1.0.0
 */
public class MapInfoAreaComposite extends Composite {

	private Label lblCoordinates;
	private MapViewer mapViewer;
	
	/**
	 * @param parent
	 * @param style
	 */
	public MapInfoAreaComposite(Composite parent, int style, MapViewer mapViewer) {
		super(parent, style);
		this.mapViewer = mapViewer;
		createControls();
	}

	protected void createControls(){
		Assert.isNotNull(getMapViewer(), "MapViewer cannot be null"); //$NON-NLS-1$
		Assert.isNotNull(getMap(), "Map cannot be null"); //$NON-NLS-1$
        GridLayout gl = new GridLayout(5, false);
        gl.marginTop = gl.marginBottom = gl.marginHeight= 0;
        this.setLayout(gl);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        lblCoordinates = new Label(this, SWT.NONE);
        lblCoordinates.setText(SmartMapEditorPart.COORDINATE_LABEL);
        lblCoordinates.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        lblCoordinates.setAlignment(SWT.RIGHT);
        
        Label lblSeparator = new Label(this, SWT.SEPARATOR | SWT.VERTICAL);
        GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        lblSeparator.setLayoutData(gd);
        
        ScaleRatioComposite scale = new ScaleRatioComposite(this, getMap(), true);
        scale.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        
        
        lblSeparator = new Label(this, SWT.SEPARATOR | SWT.VERTICAL);
        gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        lblSeparator.setLayoutData(gd);
        
        final Button lblSRID = new Button(this, SWT.NONE);
        lblSRID.setText(getMap().getViewportModel().getCRS().getName().getCode());
        lblSRID.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
        lblSRID.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ProjectionDialog pd = new ProjectionDialog(getShell(), mapViewer.getMap().getViewportModel().getCRS());
				if (pd.open() == IDialogConstants.OK_ID){
					try{
						ChangeCRSCommand command = new ChangeCRSCommand(
								ReprojectUtils.stringToCrs(pd.getSelection().getDefinition()));
						getMap().sendCommandASync(command);
					}catch (Exception ex){
						SmartPlugIn.displayLog(SmartMapEditorPart.ERROR_SETTING_MAP_PROJECTION + ex.getLocalizedMessage(), ex);
					}	
				}
			}
		});
        
        getMap().getViewportModel().addViewportModelListener(new IViewportModelListener() {
			@Override
			public void changed(ViewportModelEvent event) {
				if(event.getType() == ViewportModelEvent.EventType.CRS){
					Display.getDefault().asyncExec(new Runnable(){
						@Override
						public void run() {
							if (lblSRID != null && !lblSRID.isDisposed()) {
								lblSRID.setText(getMap().getViewportModel().getCRS().getName().getCode());
								lblSRID.getParent().layout();
							}
						}});
				}
				
			}
		});
      
        
        getMapViewer().getViewport().addMouseMotionListener(new MapMouseMotionListener() {
			
			@Override
			public void mouseMoved(MapMouseEvent event) {
				event.getPoint();
				Coordinate c = getMap().getViewportModelInternal().pixelToWorld(event.x, event.y);
				lblCoordinates.setText(format(c.x) + SmartMapEditorPart.COORDINATE_XYSEPARATOR + format(c.y));
				//see CursorPosition Tool
			}
			
			private String format(double d){
				 DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
		         format.setMaximumFractionDigits(4);
		         format.setMinimumIntegerDigits(1);
		         format.setGroupingUsed(false);
		         String string = format.format(d);
		         return string;
			}
			@Override
			public void mouseHovered(MapMouseEvent event) {
			}
			
			@Override
			public void mouseDragged(MapMouseEvent event) {
			}
		});  		
		
	}

	protected MapViewer getMapViewer() {
		return mapViewer;
	}
	
	protected Map getMap() {
		return mapViewer.getMap();
	}
}
