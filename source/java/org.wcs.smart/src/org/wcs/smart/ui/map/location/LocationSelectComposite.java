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
package org.wcs.smart.ui.map.location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;
import net.refractions.udig.project.render.ViewportModelEvent.EventType;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.tool.Tool;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.map.location.SmartPointLabelProvider.ICrsProvider;
import org.wcs.smart.ui.map.location.tool.IMapPointSelectionListener;
import org.wcs.smart.ui.map.location.tool.SelectionTool;
import org.wcs.smart.ui.properties.DialogConstants;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Composite to select certain points on the map.
 * Used to save location for some events.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class LocationSelectComposite<T extends ISmartPoint> extends SashForm implements IMapPointSelectionListener, ICrsProvider, ISmartPointDataProvider {

	private static final int MAP_MIN_WIDTH = 280;
	
	private ControlDecoration decoration;
	private TableViewer pointsListViewer;

	private WritableList points = new WritableList();

	private Text xCoordText;
	private Text yCoordText;

	private Button addButton;
	private Button removeButton;
	
	private MapComposite mapComposite;

	private List<ILocationPointsChangeListener> pointsChangeListeners = new ArrayList<ILocationPointsChangeListener>();
	
	/**
	 * @param parent
	 * @param style
	 */
	public LocationSelectComposite(Composite parent, int style) {
		super(parent, SWT.HORIZONTAL | style);
		createControls();
		setWeights(new int[] {1, 2});
	}

	private void createControls(){
//		setLayout(new GridLayout(2, false));
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setBackground(getDisplay().getSystemColor(SWT.COLOR_GRAY));

		//========points part========
		Composite pointsComposite = new Composite(this, SWT.NONE);
		pointsComposite.setLayout(new GridLayout(1, false));
		pointsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		Label pointsLabel = new Label(pointsComposite, SWT.NONE);
		pointsLabel.setText(Messages.LocationSelectComposite_Points_Label);
		decoration = new ControlDecoration(pointsLabel, SWT.RIGHT);
		decoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		decoration.setShowHover(true);
		decoration.hide();

		pointsListViewer = new TableViewer(pointsComposite, SWT.MULTI | SWT.BORDER);
		pointsListViewer.setContentProvider(new ObservableListContentProvider());
		pointsListViewer.setLabelProvider(createLabelProvider());
		pointsListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)pointsListViewer.getControl().getLayoutData()).widthHint = 120;
		pointsListViewer.setInput(this.points);
		pointsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateMapConposite();				
				removeButton.setEnabled(!pointsListViewer.getSelection().isEmpty());
			}
		});

		//========point coordinates manual input part========
		Composite coordsComposite = new Composite(pointsComposite, SWT.NONE);
		coordsComposite.setLayout(new GridLayout(2, false));
		coordsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label xLabel = new Label(coordsComposite, SWT.NONE);
		xLabel.setText(Messages.LocationSelectComposite_X_Label);
		xLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		xCoordText = new Text(coordsComposite, SWT.BORDER | SWT.LEFT);
		xCoordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		xCoordText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateAddButtonState();
			}
		});

		Label yLabel = new Label(coordsComposite, SWT.NONE);
		yLabel.setText(Messages.LocationSelectComposite_Y_Label);
		yLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		yCoordText = new Text(coordsComposite, SWT.BORDER | SWT.LEFT);
		yCoordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		yCoordText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateAddButtonState();
			}
		});
		
		//========buttons part part========
		Composite buttonsComposite = new Composite(pointsComposite, SWT.NONE);
		buttonsComposite.setLayout(new GridLayout(2, false));
		buttonsComposite.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));

		addButton = new Button(buttonsComposite, SWT.PUSH);
		addButton.setText(DialogConstants.ADD_BUTTON_TEXT);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		addButton.setEnabled(false);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Double x = convertToDouble(xCoordText.getText(), Messages.LocationSelectComposite_X_Invalid_Error);
				Double y = convertToDouble(yCoordText.getText(), Messages.LocationSelectComposite_Y_Invalid_Error);
				if (x != null && y != null) {
					Point p = convertToDBCrs(x, y);
					if (p != null) {
						handleAddPoint(p.getX(), p.getY());
					}
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
				if (!sel.isEmpty()) {
					for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
						points.remove(iterator.next());
					}
					updateMapConposite();
					fireLocationPointsChangeListeners();
				}
				pointsListViewer.refresh();
			}
		});

		//========map part========
		ScrolledComposite mapScrollCmp = new ScrolledComposite(this, SWT.H_SCROLL);
		mapComposite = new MapComposite(mapScrollCmp, SWT.NONE);
		mapComposite.setDataProvider(this);
		mapScrollCmp.setContent(mapComposite);
		mapScrollCmp.setExpandVertical(true);
		mapScrollCmp.setExpandHorizontal(true);
		mapScrollCmp.setMinWidth(MAP_MIN_WIDTH);
		
		mapComposite.getMap().getViewportModelInternal().addViewportModelListener(new IViewportModelListener() {
			@Override
			public void changed(ViewportModelEvent event) {
				if (EventType.CRS.equals(event.getType())) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							pointsListViewer.refresh(true);
						}
					});
				}
					
			}
		});

		//========register required listeners========
		Tool selectionTool = ApplicationGIS.getToolManager().findTool(SelectionTool.ID);
		if (selectionTool != null) {
			((SelectionTool)selectionTool).addListener(this);
		}

		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				//we need to properly remove listener added to selection tool
				//when this component was created
				Tool tool = ApplicationGIS.getToolManager().findTool(SelectionTool.ID);
				if (tool != null) {
					((SelectionTool)tool).removeListener(LocationSelectComposite.this);
				}
			}
		});
	}	

	@Override
	public void pointSelected(double x, double y) {
		handleAddPoint(x, y);
	}

	private Double convertToDouble(String value, String errorMessage) {
		try {
			Double result = Double.valueOf(value);
			return result;
		} catch (NumberFormatException e) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), errorMessage, null);
		}
		return null;

	}

	protected IBaseLabelProvider createLabelProvider() {
		return new SmartPointLabelProvider(this);
	}

	protected void handleAddPoint(double x, double y) {
		ISmartPoint point = createNewPoint();
		point.setX(x);
		point.setY(y);
		points.add(point);
		updateMapConposite();
		fireLocationPointsChangeListeners();
	}

	private Point convertToDBCrs(double x, double y) {
		try {
			CoordinateReferenceSystem sourceCrs = getCurrentCrs();
			Point point = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x, y));
			return (Point) JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));
		} catch (Exception e) {
			SmartPlugIn.displayLog(null, Messages.LocationSelectComposite_PointConversion_Error, e);
		}
		return null;
		
	}

	@Override
	public CoordinateReferenceSystem getCurrentCrs() {
		return mapComposite.getMap().getViewportModelInternal().getCRS();
	}
	
	protected abstract ISmartPoint createNewPoint();

	@SuppressWarnings("unchecked")
	@Override
	public List<T> getPoints() {
		return Collections.unmodifiableList(points);
	}

	@Override
	public boolean isSelected(ISmartPoint point) {
		StructuredSelection selection = (StructuredSelection) pointsListViewer.getSelection();
		for (Iterator<?> i = selection.iterator(); i.hasNext();) {
			Object obj = (Object) i.next();
			if (point.equals(obj)) {
				return true;
			}
		}
		return false;
	}
	
	public void setPoints(List<? extends T> pointList) {
		points.clear();
		points.addAll(pointList);
		updateMapConposite();
		fireLocationPointsChangeListeners();
	}

	private void updateAddButtonState() {
		String x = xCoordText.getText();
		String y = yCoordText.getText();
		addButton.setEnabled(x != null && !x.isEmpty() && y != null && !y.isEmpty());
	}
	
	private void updateMapConposite() {
		mapComposite.updatePointsLayer();
	}
	
	public ControlDecoration getDecoration() {
		return decoration;
	}

	public void addLocationPointsChangeListener(ILocationPointsChangeListener listener) {
		pointsChangeListeners.add(listener);
	}

	public void removeLocationPointsChangeListener(ILocationPointsChangeListener listener) {
		pointsChangeListeners.remove(listener);
	}
	
	protected void fireLocationPointsChangeListeners() {
		for (ILocationPointsChangeListener listener : pointsChangeListeners) {
			listener.locationPointsChanged();
		}
	}

}
