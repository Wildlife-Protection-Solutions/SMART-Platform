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
package org.wcs.smart.intelligence.ui.wizard.location;

import java.util.Iterator;
import java.util.List;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.ui.viewers.MapViewer;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.model.ISmartPoint;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Composite to select certain points on the map.
 * Used to save location for some events.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class LocationSelectComposite<T extends ISmartPoint> extends Composite {

	private TableViewer pointsListViewer;
	private MapViewer mapViewer;

	private WritableList points = new WritableList();
	
	private Text xCoordText;
	private Text yCoordText;
	
	private Button addButton;
	private Button removeButton;
	
	/**
	 * @param parent
	 * @param style
	 */
	public LocationSelectComposite(Composite parent, int style) {
		super(parent, style);
		createControls();
	}

	private void createControls(){
		setLayout(new GridLayout(2, false));
		
		//========points part========
		Composite pointsComposite = new Composite(this, SWT.NONE);
        pointsComposite.setLayout(new GridLayout(1, false));
        pointsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
//        pointsComposite.setBackground(new Color(null, 0, 255, 0));
		
		Label label = new Label(pointsComposite, SWT.NONE);
		label.setText("Points:");
		
		pointsListViewer = new TableViewer(pointsComposite, SWT.MULTI | SWT.BORDER);
		pointsListViewer.setContentProvider(new ObservableListContentProvider());
		pointsListViewer.setLabelProvider(createLabelProvider());
		pointsListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		pointsListViewer.setInput(this.points);
		pointsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				removeButton.setEnabled(!pointsListViewer.getSelection().isEmpty());
			}
		});
		

		//========point coordinates manual input part========
		Composite coordsComposite = new Composite(pointsComposite, SWT.NONE);
		coordsComposite.setLayout(new GridLayout(2, false));
		coordsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      
        Label xLabel = new Label(coordsComposite, SWT.NONE);
        xLabel.setText("X:");
        xLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        xCoordText = new Text(coordsComposite, SWT.BORDER | SWT.LEFT);

        Label yLabel = new Label(coordsComposite, SWT.NONE);
        yLabel.setText("Y:");
        yLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        yCoordText = new Text(coordsComposite, SWT.BORDER | SWT.LEFT);
  
        //========buttons part part========
		Composite buttonsComposite = new Composite(pointsComposite, SWT.NONE);
		buttonsComposite.setLayout(new GridLayout(2, false));
		buttonsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		addButton = new Button(buttonsComposite, SWT.PUSH);
        addButton.setText(DialogConstants.ADD_BUTTON_TEXT);
        addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Double x = convertToDouble(xCoordText.getText(), "X coordinate is invalid.");
				Double y = convertToDouble(yCoordText.getText(), "Y coordinate is invalid.");
				if (x != null && y != null) {
					handleAddPoint(x, y);
				}
			}
		});
 
		removeButton = new Button(buttonsComposite, SWT.PUSH);
        removeButton.setText(DialogConstants.DELETE_BUTTON_TEXT);
        removeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        removeButton.setEnabled(false);
        removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) pointsListViewer.getSelection();
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					points.remove(iterator.next());
				}
				pointsListViewer.refresh();
			}
		});
        
        
		//========map part========
        Composite mapComposite = new Composite(this, SWT.NONE);
        mapComposite.setLayout(new GridLayout(1, false));
        mapComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        mapComposite.setBackground(new Color(null, 255, 0, 0));

        mapViewer = new MapViewer(mapComposite,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
        mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        final Map map = (Map) ProjectFactory.eINSTANCE.createMap();
        map.setName("Smart Map");
        mapViewer.setMap(map);
        //set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(SmartDB.DATABASE_CRS);
 
		LoadDefaultLayersJob layer = new LoadDefaultLayersJob(map, true, null);
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		layer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				map.getRenderManager().refresh(null);
			}
		});
		layer.schedule();
		
	}	

	private Double convertToDouble(String value, String errorMessage) {
		try {
			Double result = Double.valueOf(value);
			return result;
		} catch (NumberFormatException e) {
			IntelligencePlugIn.displayLog(errorMessage, null);
		}
		return null;
		
	}
	
	protected IBaseLabelProvider createLabelProvider() {
		return new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ISmartPoint) {
					ISmartPoint p = (ISmartPoint) element;
					return p.getX() + ", " +p.getY();
				}
				return super.getText(element);
			}
		};
	}

	protected void handleAddPoint(double x, double y) {
		ISmartPoint point = createNewPoint();
		point.setX(x);
		point.setY(y);
		points.add(point);
	}
	
	protected abstract ISmartPoint createNewPoint();

	@SuppressWarnings("unchecked")
	public List<T> getPoints() {
		return points;
	}
}
