/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.navigation.ui;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.render.IViewportModel;
import org.locationtech.udig.project.render.displayAdapter.IMapDisplayListener;
import org.locationtech.udig.project.render.displayAdapter.MapDisplayEvent;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseWheelEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseWheelListener;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.NavigationLayer;
import org.wcs.smart.cybertracker.model.NavigationTarget;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.udig.EditPointTool;
import org.wcs.smart.udig.IEditPointAction;
import org.wcs.smart.udig.IMapEditManager;
import org.wcs.smart.udig.IMapEditManager.EditPoint;
import org.wcs.smart.udig.SetBasemapTool;
import org.wcs.smart.udig.UndoTool;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapInfoAreaComposite;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.tool.PanTool;
import org.wcs.smart.ui.map.tool.ZoomExtentTool;
import org.wcs.smart.ui.map.tool.ZoomInTool;
import org.wcs.smart.ui.map.tool.ZoomOutTool;
import org.wcs.smart.ui.map.tool.ZoomTool;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Dialog for editing/viewing navigation layers.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class NavigationLayerDialog extends SmartStyledDialog implements MapPart, ITargetEditor{
	
	private static final String COLOR_KEY = "COLOR"; //$NON-NLS-1$
	
	private static final String UUID_PROPERTY = "uuid"; //$NON-NLS-1$
	private static final String SELECTED_PROPERTY = "selected";  //$NON-NLS-1$
	private static final String GEOM_PROPERTY = "the_geom";  //$NON-NLS-1$
	
	private static final String GEOMTYPE_FUNCTION_NAME = "geometryType";  //$NON-NLS-1$

	private FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
	
	private NavigationLayer nav;
	
	private Text txtName;
	private TableViewer tblTargets;
	
	private List<NavigationTarget> targets = new ArrayList<>();
	private FeatureStore<SimpleFeatureType, SimpleFeature> fstore;
	private MapViewer mapViewer;
	private Layer targetLayer;
	private MapToolComposite tools ;
	
	private Label pointColorLabel;
	private Label lineColorLabel;
	private Spinner txtLineWidth;
	private Spinner txtPointSize;
	private ComboViewer cmbMarkerStyle;
	private ComboViewer cmbLineStyle;
	
	private List<Control> pointControls;
	private List<Control> lineControls;
	
	
	private static enum MarkerStyle{
		CIRCLE("circle"), //$NON-NLS-1$
		CROSS("cross"), //$NON-NLS-1$
		DIAMOND("diamond"), //$NON-NLS-1$
		SQUARE("square"), //$NON-NLS-1$
		TRIANGLE("triangle"), //$NON-NLS-1$
		X("x"); //$NON-NLS-1$
		
		String key;
		
		MarkerStyle(String key){
			this.key = key;
		}
		
		public String getName() {
			switch(this) {
			case CIRCLE: return Messages.NavigationLayerDialog_circlestyle;
			case CROSS: return Messages.NavigationLayerDialog_crossstyle;
			case DIAMOND: return Messages.NavigationLayerDialog_diamondstyle;
			case SQUARE: return Messages.NavigationLayerDialog_squarestyle;
			case TRIANGLE: return Messages.NavigationLayerDialog_trianglestyle;
			case X: return Messages.NavigationLayerDialog_xstyle;
			}
			return ""; //$NON-NLS-1$
		}
		public static MarkerStyle parse(String item) {
			for (MarkerStyle ms : MarkerStyle.values()) {
				if (ms.key.equals(item)) return ms;
			}
			return MarkerStyle.CIRCLE;
		}
	}
	private static enum LineStyle{
		SOLID("solid"), //$NON-NLS-1$
		DASH("dash"), //$NON-NLS-1$
		DASHDOT("dashdot"), //$NON-NLS-1$
		DASHDOTDOT("dashdotdot"), //$NON-NLS-1$
		DOT("dot"); //$NON-NLS-1$
		
		
		String key;
		
		LineStyle(String key){
			this.key = key;
		}
		public String getName() {
			switch(this) {
			case DASH: return Messages.NavigationLayerDialog_dashstyle;
			case SOLID: return Messages.NavigationLayerDialog_solidstyle;
			case DASHDOT: return Messages.NavigationLayerDialog_dashdotstyle;
			case DOT: return Messages.NavigationLayerDialog_dotstyle;
			case DASHDOTDOT: return Messages.NavigationLayerDialog_dashdotdotstyle;
			}
			return ""; //$NON-NLS-1$
		}
		public float[] getDashArray() {
			switch(this) {
			case DASH: return new float[] {5};
			case DASHDOT: return new float[] {5, 3, 1, 3};
			case DASHDOTDOT: return new float[] {5, 3, 1, 3, 1, 3};
			case DOT: return new float[] {1,3};
			}
			return null;
		}
		public static LineStyle parse(String item) {
			for (LineStyle ms : LineStyle.values()) {
				if (ms.key.equals(item)) return ms;
			}
			return LineStyle.SOLID;
		}
	}
	private Job refreshJob = new Job(Messages.NavigationLayerDialog_refreshmapjob){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (getShell() != null && !getShell().isDisposed() && mapViewer != null) mapViewer.getMap().getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
	};
	
	private Job refreshTargetsJob = new Job(Messages.NavigationLayerDialog_refreshmapjob){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (getShell() != null && !getShell().isDisposed() && mapViewer != null) 
				mapViewer.getMap().getRenderManager().refresh(targetLayer, null);
			return Status.OK_STATUS;
		}
	};
	
	public NavigationLayerDialog(Shell parentShell, NavigationLayer nav) {
		super(parentShell);
		this.nav = nav;
		targets.addAll(nav.getTargetsAsJson());
	}
	
	@Override
	public Point getInitialSize() {
		return new Point(900, 800);
	}
	
	@Override
	public void okPressed() {
		nav.setName(txtName.getText());
		for (NavigationTarget t : targets) {
			if (t.isLine()) {
				t.setStyle(getColor(lineColorLabel), txtLineWidth.getSelection(), ((LineStyle)cmbLineStyle.getStructuredSelection().getFirstElement()).key);
			}else if (t.isPoint()) {
				t.setStyle(getColor(pointColorLabel), txtPointSize.getSelection(), ((MarkerStyle)cmbMarkerStyle.getStructuredSelection().getFirstElement()).key);
			}
		}
		nav.setTargets(targets);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				nav.setLastModifiedBy(SmartDB.getCurrentEmployee());
				nav.setLastModifiedDate(LocalDate.now());
				session.saveOrUpdate(nav);
				session.getTransaction().commit();
			}catch (Exception ex) {
				try {
					session.getTransaction().rollback();
				}catch (Exception ex2) {
					CyberTrackerPlugIn.log(ex.getMessage(), ex2);
				}
				CyberTrackerPlugIn.displayError(Messages.NavigationLayerDialog_ErrorTitle, Messages.NavigationLayerDialog_SaveError + ex.getMessage(), ex);
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
		
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btnSave = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btnSave.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);		
	}
	
	private void validate() {
		boolean hasline = false;
		boolean haspoint = false;
		for (NavigationTarget t : targets) {
			if (t.isLine()) hasline = true;
			if (t.isPoint()) haspoint = true;
		}
		
		for (Control c : pointControls) c.setEnabled(haspoint);
		for (Control c : lineControls) c.setEnabled(hasline);
		
		targetLayer.getStyleBlackboard().put(SLDContent.ID, getTargetStyle());
		targetLayer.refresh(null);
		
		validate(true);
	}
	private void validate(boolean dirty) {
		if (dirty) getButton(IDialogConstants.OK_ID).setEnabled(true);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth = 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartUiUtils.createHeaderLabel(parent, Messages.NavigationLayerDialog_NavLayerHeader);
		
		Composite detailsComp = new Composite(parent, SWT.NONE);
		detailsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		detailsComp.setLayout(new GridLayout(3, false));
		((GridLayout)detailsComp.getLayout()).marginWidth = 0;
		((GridLayout)detailsComp.getLayout()).marginHeight = 0;
		
		Label l = new Label(detailsComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_NameField);
		
		txtName = new Text(detailsComp, SWT.BORDER);
		txtName.setText(nav.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtName.addListener(SWT.Modify, e->validate());
		
		Composite infoComp = new Composite(detailsComp, SWT.NONE);
		infoComp.setLayout(new GridLayout(2, false));
		((GridLayout)infoComp.getLayout()).marginWidth = 0;
		((GridLayout)infoComp.getLayout()).marginHeight = 0;
		infoComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		l = new Label(infoComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_CreatedDateField);
		
		l = new Label(infoComp, SWT.NONE);
		l.setText((nav.getCreatedDate() == null) ? "" : DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(nav.getCreatedDate())); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(infoComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_ModifiedDateField);
		
		l = new Label(infoComp, SWT.NONE);
		l.setText((nav.getLastModifiedDate() == null) ? "" : DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(nav.getLastModifiedDate())); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(infoComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_ModifiedByField);
		
		l = new Label(infoComp, SWT.NONE);
		l.setText(nav.getLastModifiedBy() == null ? "" : SmartLabelProvider.getShortLabel( nav.getLastModifiedBy())); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		SmartUiUtils.createHeaderLabel(parent, Messages.NavigationLayerDialog_TargetsHeader);
		
		SashForm targetsComp = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);
		targetsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		targetsComp.setLayout(new GridLayout(2, false));
		
		Composite listPart = new Composite(targetsComp, SWT.NONE);
		listPart.setLayout(new GridLayout());
		listPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		((GridLayout)listPart.getLayout()).marginWidth = 0;
		((GridLayout)listPart.getLayout()).marginHeight = 0;
		
		Composite ttable = new Composite(listPart, SWT.NONE);
		ttable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ttable.setLayout(new TableColumnLayout());
		
		tblTargets = new TableViewer(ttable, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tblTargets) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		tblTargets.addSelectionChangedListener(e->updateSelection(false));
		tblTargets.getControl().addListener(SWT.FocusOut, e->updateSelection(true));
		tblTargets.setContentProvider(ArrayContentProvider.getInstance());
		TableViewerEditor.create(tblTargets, actSupport, ColumnViewerEditor.DEFAULT);
		TableViewerColumn c = new TableViewerColumn(tblTargets,SWT.NONE);
		
		TableColumn tc = c.getColumn();
		((TableColumnLayout)ttable.getLayout()).setColumnData(tc, new ColumnWeightData(100));
		c.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((NavigationTarget)element).getId();
			}
		});
		c.setEditingSupport(new EditingSupport(c.getViewer()) {
			TextCellEditor editor = new TextCellEditor(tblTargets.getTable());
			@Override
			protected void setValue(Object element, Object value) {
				((NavigationTarget)element).setId((String)value);	
				tblTargets.refresh(element);
				validate();
			}
			
			@Override
			protected Object getValue(Object element) {
				return ((NavigationTarget)element).getId();
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		addDragDropSupport();
		tblTargets.setInput(targets);
		
		Menu targetMenu = new Menu(tblTargets.getControl());
		
		MenuItem miRename = new MenuItem(targetMenu, SWT.PUSH);
		miRename.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miRename.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miRename.addListener(SWT.Selection, e->{
			NavigationTarget t = (NavigationTarget) tblTargets.getStructuredSelection().getFirstElement();
			if (t == null) return;
			
			tblTargets.editElement(t, 0);
		});
		
		new MenuItem(targetMenu, SWT.SEPARATOR);
		
		MenuItem miDelete = new MenuItem(targetMenu, SWT.PUSH);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.addListener(SWT.Selection, e->deleteTargets());
		
		new MenuItem(targetMenu, SWT.SEPARATOR);
		
		MenuItem miUp = new MenuItem(targetMenu, SWT.PUSH);
		miUp.setText(Messages.NavigationLayerDialog_MoveUpTool);
		miUp.addListener(SWT.Selection, e->moveTarget(1));
		
		MenuItem miDown = new MenuItem(targetMenu, SWT.PUSH);
		miDown.setText(Messages.NavigationLayerDialog_MoveDownTool);
		miDown.addListener(SWT.Selection, e->moveTarget(-1));
		
		tblTargets.getControl().setMenu(targetMenu);
				
		
		SmartUiUtils.createHeaderLabel(listPart, Messages.NavigationLayerDialog_StylesHeader);
		
		Composite styleComp = new Composite(listPart, SWT.NONE);
		styleComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		styleComp.setLayout(new GridLayout(2, false));
		((GridLayout)styleComp.getLayout()).marginWidth = 0;
		((GridLayout)styleComp.getLayout()).marginHeight = 0;
		
		java.awt.Color pointColor = new java.awt.Color(255, 0, 0);
		int pointsize = 6;
		java.awt.Color lineColor = new java.awt.Color(255, 0, 0);
		int linesize = 1;
		boolean hasline = false;
		boolean haspoint = false;
		MarkerStyle defaultms = MarkerStyle.CIRCLE;
		LineStyle defaultls = LineStyle.SOLID;
		for (NavigationTarget t : targets) {
			if (t.isLine()) {
				hasline = true;
				lineColor = new java.awt.Color(Integer.parseInt(t.getColor(), 16));
				linesize = t.getSize();
				defaultls = LineStyle.parse(t.getStyle());
			}else if (t.isPoint()) {
				haspoint = true;
				pointColor = new java.awt.Color(Integer.parseInt(t.getColor(), 16));
				pointsize = t.getSize();
				defaultms = MarkerStyle.parse(t.getStyle());
			}
			if (hasline && haspoint) break;
		}
		
		
		Composite pntComp = new Composite(styleComp, SWT.NONE);
		pntComp.setLayout(new GridLayout(2, false));
		pntComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)pntComp.getLayout()).marginWidth = 0;
		((GridLayout)pntComp.getLayout()).marginHeight = 0;
		pointControls = new ArrayList<>();
		
		Composite c2 = SmartUiUtils.createSubHeaderLabel(pntComp, Messages.NavigationLayerDialog_PointHeader);
		c2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		l = new Label(pntComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_colorlabel);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		pointControls.add(l);
		
		pointColorLabel = new Label(pntComp, SWT.NONE);
		pointColorLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		((GridData)pointColorLabel.getLayoutData()).widthHint = 30;
		pointColorLabel.addListener(SWT.Dispose, e->disposeColor(pointColorLabel));		
		WidgetElement.setCSSClass(pointColorLabel, "customcolor"); //$NON-NLS-1$
		pointColorLabel.addListener(SWT.Paint, e->{
			if (pointColorLabel.getData(COLOR_KEY) != null) e.gc.drawRectangle(0, 0, pointColorLabel.getBounds().width-1, pointColorLabel.getBounds().height-1);
		});
		pointControls.add(pointColorLabel);
		
		Color temp = new Color(getShell().getDisplay(), pointColor.getRed(), pointColor.getGreen(), pointColor.getBlue());
		pointColorLabel.setData(COLOR_KEY,temp);
		pointColorLabel.setBackground(temp);
		Listener changeColor = e->{
			ColorDialog cd = new ColorDialog(getShell());
			cd.setRGB(pointColorLabel.getBackground().getRGB());
			cd.setText(Messages.CyberTrackerPropertiesComposite_ColorSelectionDialogTitle);
			RGB rgb = cd.open();
			if (rgb == null) return;
			
			disposeColor(pointColorLabel);
			Color newColor = new Color(getShell().getDisplay(), rgb);
			pointColorLabel.setData(COLOR_KEY, newColor);
			pointColorLabel.setBackground(newColor);
			validate();
		};
		
		pointColorLabel.addListener(SWT.MouseDoubleClick, changeColor);
		
		l = new Label(pntComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_sizelabel);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		pointControls.add(l);
		
		txtPointSize = new Spinner(pntComp, SWT.BORDER);
		txtPointSize.setMinimum(0);
		txtPointSize.setMaximum(50);
		txtPointSize.setSelection(pointsize);
		txtPointSize.addListener(SWT.Selection,  e->validate());
		pointControls.add(txtPointSize);
		
		((GridData)pointColorLabel.getLayoutData()).widthHint = txtPointSize.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		((GridData)pointColorLabel.getLayoutData()).heightHint = txtPointSize.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		
		l = new Label(pntComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_stylelabel);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		pointControls.add(l);
		
		cmbMarkerStyle = new ComboViewer(pntComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbMarkerStyle.setContentProvider(ArrayContentProvider.getInstance());
		cmbMarkerStyle.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((MarkerStyle)element).getName();
			}
		});
		cmbMarkerStyle.setInput(MarkerStyle.values());
		cmbMarkerStyle.setSelection(new StructuredSelection(defaultms));
		cmbMarkerStyle.addSelectionChangedListener(e->validate());
		pointControls.add(cmbMarkerStyle.getControl());
		
		
		Composite lineComp = new Composite(styleComp, SWT.NONE);
		lineComp.setLayout(new GridLayout(2, false));
		lineComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)lineComp.getLayout()).marginWidth = 0;
		((GridLayout)lineComp.getLayout()).marginHeight = 0;
		lineControls = new ArrayList<>();
		
		c2 = SmartUiUtils.createSubHeaderLabel(lineComp, Messages.NavigationLayerDialog_LinesHeader);
		c2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		l = new Label(lineComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_colorlabel);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		lineControls.add(l);
		
		lineColorLabel = new Label(lineComp, SWT.NONE);
		lineControls.add(lineColorLabel);
		lineColorLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		((GridData)lineColorLabel.getLayoutData()).widthHint = 30;
		((GridData)lineColorLabel.getLayoutData()).heightHint = 20;
		lineColorLabel.addListener(SWT.Dispose, e->disposeColor(lineColorLabel));		
		WidgetElement.setCSSClass(lineColorLabel, "customcolor"); //$NON-NLS-1$
		lineColorLabel.addListener(SWT.Paint, e->{
			if (lineColorLabel.getData(COLOR_KEY) != null) e.gc.drawRectangle(0, 0, lineColorLabel.getBounds().width-1, lineColorLabel.getBounds().height-1);
		});
		temp = new Color(getShell().getDisplay(), lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue());
		lineColorLabel.setData(COLOR_KEY,temp);
		lineColorLabel.setBackground(temp);

		changeColor = e->{
			ColorDialog cd = new ColorDialog(getShell());
			cd.setRGB(lineColorLabel.getBackground().getRGB());
			cd.setText(Messages.CyberTrackerPropertiesComposite_ColorSelectionDialogTitle);
			RGB rgb = cd.open();
			if (rgb == null) return;
			
			disposeColor(lineColorLabel);
			Color newColor = new Color(getShell().getDisplay(), rgb);
			lineColorLabel.setData(COLOR_KEY, newColor);
			lineColorLabel.setBackground(newColor);
			validate();
		};
		
		lineColorLabel.addListener(SWT.MouseDoubleClick, changeColor);
		
		l = new Label(lineComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_widthlabel);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		lineControls.add(l);
		
		txtLineWidth = new Spinner(lineComp, SWT.BORDER);
		txtLineWidth.setMinimum(0);
		txtLineWidth.setMaximum(20);
		txtLineWidth.setSelection(linesize);
		txtLineWidth.addListener(SWT.Selection, e->validate());
		lineControls.add(txtLineWidth);

		((GridData)lineColorLabel.getLayoutData()).widthHint = txtLineWidth.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		((GridData)lineColorLabel.getLayoutData()).heightHint = txtLineWidth.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		
		l = new Label(lineComp, SWT.NONE);
		l.setText(Messages.NavigationLayerDialog_stylelabel);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		lineControls.add(l);
		
		cmbLineStyle = new ComboViewer(lineComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbLineStyle.setContentProvider(ArrayContentProvider.getInstance());
		cmbLineStyle.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((LineStyle)element).getName();
			}
		});
		cmbLineStyle.setInput(LineStyle.values());
		cmbLineStyle.setSelection(new StructuredSelection(defaultls));
		cmbLineStyle.addSelectionChangedListener(e->validate());
		lineControls.add(cmbLineStyle.getControl());
		
		for (Control ctr : pointControls) ctr.setEnabled(haspoint);
		for (Control ctr : lineControls) ctr.setEnabled(hasline);
		
		Composite mapArea = new Composite(targetsComp, SWT.NONE);
		mapArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mapArea.setLayout(new GridLayout(2, false));
		((GridLayout)mapArea.getLayout()).marginWidth = 0;
		((GridLayout)mapArea.getLayout()).marginHeight = 0;
		
		mapViewer = new MapViewer(mapArea,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Map map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName(Messages.NavigationLayerDialog_MapName);
		
		mapViewer.setMap(map);
		
		//set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(GeometryUtils.SMART_CRS);

		ApplicationGIS.getToolManager().setCurrentEditor(this);
		String[] thisTools = new String[] {
				SetBasemapTool.ID,
				ZoomExtentTool.ID,
				PanTool.ID,
				ZoomTool.ID,
				ZoomInTool.ID,
				ZoomOutTool.ID,
				AddPointTool.ID,
				AddLineTool.ID,
				EditPointTool.ID,
				UndoTool.ID,
				ImportTool.ID};
		
		tools = new MapToolComposite(thisTools);
		tools.createComposite(mapArea);
		tools.getTool(UndoTool.ID).setEnabled(false);
		
		MapInfoAreaComposite i = new MapInfoAreaComposite(mapArea, SWT.NONE, mapViewer) ;
		i.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		tools.selectTool(PanTool.ID);

		map.getBlackboard().put(ITargetEditor.ID, this);
		getMap().getBlackboard().put(IMapEditManager.BLACKBOARD_KEY, editManager);
		
		final LoadDefaultLayersJob defaultLayer = new LoadDefaultLayersJob(mapViewer.getMap());
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		defaultLayer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (getShell() == null || getShell().isDisposed() || mapViewer == null) return;
				
				mapViewer.getMap().sendCommandSync(new ZoomExtentCommand());
				mapViewer.getMap().getRenderManager().refresh(null);
			}
		});
		defaultLayer.schedule();
		
		//if I am disposed before finished cancel job
		getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				defaultLayer.cancel();
			}
		});
		mapViewer.getViewport().addPaneListener(new IMapDisplayListener() {
			@Override
			public void sizeChanged(MapDisplayEvent event) {
				refreshJob.cancel();
				refreshJob.schedule(600);
			}
		});
		mapViewer.getViewport().addMouseWheelListener(new MapMouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MapMouseWheelEvent e) {
				refreshJob.cancel();
				refreshJob.schedule(600);
			}
		});
		
		targetsComp.setWeights(new int[] {3,7});
	
		try {
			createNavigationLayer();
		} catch (Exception e1) {
			CyberTrackerPlugIn.log(e1.getMessage(), e1);
		}
		
		getShell().setText(MessageFormat.format(Messages.NavigationLayerDialog_ShellTitle, nav.getName()));
		return parent;
	}

	private void disposeColor(Label l) {
		Color c = (Color) l.getData(COLOR_KEY);
		if (c != null) {
			c.dispose();
			l.setData(COLOR_KEY, null);
		}
		
	}
	
	@Override
	public Map getMap() {
		return mapViewer.getMap();
	}

	@Override
	public void openContextMenu() {
	}

	@Override
	public void setFont(Control textArea) {
	}

	@Override
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider) {
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

	private void refreshTargets() {
		tblTargets.refresh();
		refreshTargetsJob.schedule(100);
		validate();
	}
	
	private void addDragDropSupport() {
		DragSource dragSource = new DragSource(tblTargets.getControl(), DND.DROP_MOVE);
		dragSource.setTransfer(LocalSelectionTransfer.getTransfer());
		dragSource.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				event.doit = !tblTargets.getStructuredSelection().isEmpty();
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					LocalSelectionTransfer.getTransfer().setSelection(tblTargets.getStructuredSelection());
				}
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(null);
			}
		});
		
		DropTarget dropTarget = new DropTarget(tblTargets.getControl(), DND.DROP_MOVE);
		dropTarget.setTransfer(LocalSelectionTransfer.getTransfer());
		dropTarget.addDropListener(new ViewerDropAdapter(tblTargets) {
			@Override
			public void dragEnter(DropTargetEvent event) {
				// make sure drag was triggered from current tableViewer
				if (event.widget instanceof DropTarget) {
					boolean isSameViewer = tblTargets.getControl().equals(((DropTarget) event.widget).getControl());
					if (isSameViewer) {
						event.detail = DND.DROP_MOVE;
						setSelectionFeedbackEnabled(false);
						super.dragEnter(event);
						return;
					}
				}
				event.detail = DND.DROP_NONE;
			}

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				return true;
			}

			@Override
			public boolean performDrop(Object target) {
				int location = determineLocation(getCurrentEvent());
				int targetindex = targets.indexOf(getCurrentTarget());
				int sourceindex = targets.indexOf(getSelectedElement());
				if (location == LOCATION_BEFORE) {
					targetindex --;
				} else if (location == LOCATION_AFTER) {
					
				}else {
					return false;
				}
				if (targetindex < sourceindex) targetindex++;
				if (targetindex < 0) targetindex = 0;
				
				targets.remove(getSelectedElement());
				targets.add(targetindex, (NavigationTarget)getSelectedElement());
				refreshTargets();
				return true;
			}

			private Object getSelectedElement() {
				return ((IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection()).getFirstElement();
			}
		});
	}
	
	
	private List<NavigationTarget> getTargetSelection(){
		List<NavigationTarget> tts = new ArrayList<>();
		for (Iterator<?> iterator = tblTargets.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object t = iterator.next();
			if (t instanceof NavigationTarget)  tts.add((NavigationTarget)t);
		}
		return tts;
	}
	
	private void updateGeometry(NavigationTarget target, Geometry newGeometry) {
		
		try {
			doLayerAction(()->
			fstore.modifyFeatures(fstore.getSchema().getGeometryDescriptor().getName(), 
					newGeometry, ff.equals(ff.property(UUID_PROPERTY), ff.literal(target.getUuid()))));
			target.setGeometry(newGeometry);
		}catch (Exception ex) {
			CyberTrackerPlugIn.displayError(Messages.NavigationLayerDialog_ErrorTitle, Messages.NavigationLayerDialog_TargetUpdateError + ex.getMessage(), ex);
		}
		refreshTargets();
	}
	
	private void deleteVertex(NavigationTarget target, int index) {
		if (!target.isLine()) return;
		
		Coordinate[] c = ((LineString)target.getGeometry()).getCoordinates();
		Coordinate[] newc = new Coordinate[c.length-1];
		int j = 0;
		for (int i = 0; i < c.length; i ++) {
			if (i == index) continue;
			newc[j++] = c[i];
		}
		updateGeometry(target, GeometryFactoryProvider.getFactory().createLineString(newc));
	}
	
	private void updateSelection(boolean clear) {
		
		try {
			doLayerAction(()->{
				fstore.modifyFeatures(fstore.getSchema().getDescriptor(SELECTED_PROPERTY).getName(), Boolean.FALSE, Filter.INCLUDE);
				if (!clear) {
					List<NavigationTarget> selected = getTargetSelection();
					List<Filter> ors = new ArrayList<>();
					for (NavigationTarget t : selected) {
						Filter filter = ff.equals(ff.property(UUID_PROPERTY), ff.literal(t.getUuid()));
						ors.add(filter);
					}
					fstore.modifyFeatures(fstore.getSchema().getDescriptor(SELECTED_PROPERTY).getName(), Boolean.TRUE, ff.or(ors));
				}
			});
		}catch (Exception ex) {
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
		}
		
		getMap().getRenderManager().refresh(targetLayer, null);
	}
	
	private void moveTarget(int dir) {
		List<NavigationTarget> tomove = getTargetSelection();
		if (dir < 0) Collections.reverse(tomove);
		for(NavigationTarget t : tomove) {
			int i = targets.indexOf(t);
			targets.remove(t);
			i -= dir;
			if (i < 0) i = 0;
			if ( i > targets.size()) i = targets.size();
			targets.add(i, t);
		}
		refreshTargets();
	}
	
	private void deleteTargets(NavigationTarget t) {
		try {
			Filter filter = ff.equals(ff.property(UUID_PROPERTY), ff.literal(t.getUuid()));
			doLayerAction(()->fstore.removeFeatures(filter));
			targets.remove(t);
		}catch (Exception ex) {
			CyberTrackerPlugIn.displayError(Messages.NavigationLayerDialog_ErrorTitle, Messages.NavigationLayerDialog_TargetDeleteError + ex.getMessage(), ex);
		}
		
		refreshTargets();
	}
	
	private void deleteTargets() {
		List<NavigationTarget> tts = getTargetSelection();
		List<Filter> filters = new ArrayList<>();
		
		for (NavigationTarget t : tts) filters.add( ff.equals(ff.property(UUID_PROPERTY), ff.literal(t.getUuid())) );
		
		try {
			Filter filter = ff.or(filters);
			doLayerAction(()->fstore.removeFeatures(filter));
			targets.removeAll(tts);
		}catch (Exception ex) {
			CyberTrackerPlugIn.displayError(Messages.NavigationLayerDialog_21, Messages.NavigationLayerDialog_TargetDeleteError + ex.getMessage(), ex);
		}
		refreshTargets();
	}
	
	private String getColor(Label l) {
		Color t = (Color)l.getData(COLOR_KEY);
		return Integer.toHexString( new java.awt.Color(t.getRed(), t.getGreen(), t.getBlue()).getRGB() ).substring(2);
	}

	@Override
	public void addLinearTarget(LineString ls) {
		NavigationTarget newTarget = new NavigationTarget(MessageFormat.format(Messages.NavigationLayerDialog_DefaultTargetName, targets.size()), ls);
		targets.add(newTarget);
		addTargetToFeatureStore(newTarget);
	}
	
	@Override
	public void addTargets(List<NavigationTarget> targets) {
		List<NavigationTarget> newTargets = new ArrayList<>();
		int cnt = this.targets.size();
		for (NavigationTarget ls : targets) {
			
			String id = ls.getId() != null ? ls.getId() : MessageFormat.format(Messages.NavigationLayerDialog_DefaultTargetName, cnt++);
			if (ls.getGeometry() instanceof org.locationtech.jts.geom.Point) {
				NavigationTarget newTarget = new NavigationTarget(id, (org.locationtech.jts.geom.Point)ls.getGeometry());
				newTargets.add(newTarget);
			}else if (ls.getGeometry() instanceof LineString) {
				NavigationTarget newTarget = new NavigationTarget(id, (LineString)ls.getGeometry());
				newTargets.add(newTarget);
			}
		}
		this.targets.addAll(newTargets);
		
		List<SimpleFeature> features = new ArrayList<>();
		for (NavigationTarget newTarget : newTargets) {
			SimpleFeature f = toFeature(newTarget);
			features.add(f);
		}			
		try {
			doLayerAction(()->fstore.addFeatures(DataUtilities.collection(features)));
		}catch (Exception ex) {
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
		}
		Display.getDefault().syncExec(()->refreshTargets());

	}
	
	@Override
	public void addPointTarget(Coordinate c) {
		NavigationTarget newTarget = new NavigationTarget(MessageFormat.format(Messages.NavigationLayerDialog_DefaultTargetName, targets.size()), GeometryFactoryProvider.getFactory().createPoint(c));
		targets.add(newTarget);
		addTargetToFeatureStore(newTarget);
	}

	private void addTargetToFeatureStore(NavigationTarget newTarget) {
		SimpleFeature f = toFeature(newTarget);
		try {
			doLayerAction(()->fstore.addFeatures(DataUtilities.collection(f)));
		}catch (Exception ex) {
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
		}
		refreshTargets();
	}
	
	public void doLayerAction(ILayerAction function) throws Exception{
		try {
			function.doAction();
		}catch (ConcurrentModificationException ex) {
			function.doAction();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void createNavigationLayer() throws SchemaException, IOException {
		
		String spec = GEOM_PROPERTY + ":Geometry:srid=4326,name:String," + UUID_PROPERTY + ":String," + SELECTED_PROPERTY + ":Boolean"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		SimpleFeatureType type =  DataUtilities.createType("NavigationTarget", spec); //$NON-NLS-1$
		
		IGeoResource r = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(type);
		fstore = r.resolve(FeatureStore.class, null);
		
		DefaultFeatureCollection  collection = new DefaultFeatureCollection ("navlayer", type); //$NON-NLS-1$
		for (NavigationTarget t : targets) {
			collection.add(toFeature(t));
		}
		fstore.addFeatures(collection);
		
		AddLayersCommand cmd = new AddLayersCommand(Collections.singleton(r)) {
			 public void run( IProgressMonitor monitor ) throws Exception {
				 super.run(monitor);
				 
				 Layer l = getLayers().get(0);
				 targetLayer = l;
				 Display.getDefault().asyncExec(()->{
					 targetLayer.getStyleBlackboard().put(SLDContent.ID, getTargetStyle());	 
				 });
				 
				 
			 }
			
		};
		getMap().sendCommandASync(cmd);
	}
	
	
	private SimpleFeature toFeature(NavigationTarget t) {
		Object data[] = new Object[4];
		data[0] = t.getGeometry();
		data[1] = t.getId();
		data[2] = t.getUuid();
		data[3] = false;
		SimpleFeature feature = SimpleFeatureBuilder.build((SimpleFeatureType)fstore.getSchema(), data, t.getUuid());
		return feature;
	}
	
	private Style getTargetStyle() {
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

		StyleBuilder sb = new StyleBuilder(sf);
		Style style = sf.createStyle();
		
		Filter fnotselected = ff.equals(ff.property(SELECTED_PROPERTY), ff.literal(Boolean.FALSE));
		Filter fselected = ff.equals(ff.property(SELECTED_PROPERTY), ff.literal(Boolean.TRUE));
		Filter fls = ff.equals(ff.function(GEOMTYPE_FUNCTION_NAME, ff.property(GEOM_PROPERTY)), ff.literal("LineString")); //$NON-NLS-1$
		Filter fpoint = ff.equals(ff.function(GEOMTYPE_FUNCTION_NAME, ff.property(GEOM_PROPERTY)), ff.literal("Point")); //$NON-NLS-1$
		Rule rr;
		org.geotools.styling.FeatureTypeStyle fts;

		//--- Linear Style ---
		LineStyle ll = (LineStyle) cmbLineStyle.getStructuredSelection().getFirstElement();
		Color c = (Color) lineColorLabel.getData(COLOR_KEY);
		Stroke linestroke = sb.createStroke(
				new java.awt.Color(c.getRed(),c.getGreen(),c.getBlue()),
				txtLineWidth.getSelection(),
				ll.getDashArray());
		LineSymbolizer lines = sb.createLineSymbolizer(linestroke);
		
		rr = sb.createRule(new Symbolizer[] {lines});
		rr.setFilter(ff.and(fnotselected, fls));
		fts = sf.createFeatureTypeStyle();
    	fts.setName("Line Target Style"); //$NON-NLS-1$
    	fts.rules().add(rr);
    	style.featureTypeStyles().add(fts);

		//--- Point Style ---
		c = (Color) pointColorLabel.getData(COLOR_KEY);

		MarkerStyle ms = (MarkerStyle) cmbMarkerStyle.getStructuredSelection().getFirstElement();
		Stroke pointstroke = sb.createStroke(new java.awt.Color(0,0,0), 0);
		Fill pointfill = sb.createFill(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue()));
		Mark pointmark = sb.createMark(sb.literalExpression(ms.key), pointfill, pointstroke);
		Graphic pointgraphic = sb.createGraphic(null,  pointmark,  null);
		pointgraphic.setSize(sb.literalExpression(txtPointSize.getSelection()));
        PointSymbolizer endpoint = sb.createPointSymbolizer(pointgraphic);
        rr = sb.createRule(new Symbolizer[] {endpoint});
		rr.setFilter(ff.and(fnotselected, fpoint));
		fts = sf.createFeatureTypeStyle();
    	fts.setName("Point Target Style"); //$NON-NLS-1$
    	fts.rules().add(rr);
    	style.featureTypeStyles().add(fts);
    	
		//--- Selected Point Style ---
		pointfill = sb.createFill(new java.awt.Color(255,255,100));
		pointmark = sb.createMark(sb.literalExpression(ms.key), pointfill, pointstroke); 
		pointgraphic = sb.createGraphic(null,  pointmark,  null);
		pointgraphic.setSize(sb.literalExpression(txtPointSize.getSelection()));
        endpoint = sb.createPointSymbolizer(pointgraphic);
		rr = sb.createRule(new Symbolizer[] {endpoint});
		rr.setFilter(ff.and(fselected, fpoint));
		fts = sf.createFeatureTypeStyle();
    	fts.setName("Selected Point Target Style"); //$NON-NLS-1$
    	fts.rules().add(rr);
		style.featureTypeStyles().add(fts);
		
		//--- Linear Selected Style ---
		linestroke = sb.createStroke(new java.awt.Color(255,255,100),2);
		lines = sb.createLineSymbolizer(linestroke);
		rr = sb.createRule(new Symbolizer[] {lines});
		rr.setFilter(ff.and(fselected, fls));
		fts = sf.createFeatureTypeStyle();
    	fts.setName("Selected Line Target Style"); //$NON-NLS-1$
    	fts.rules().add(rr);
    	style.featureTypeStyles().add(fts);
		
		
		return style;
		
	}
	
	private IEditPointAction deleteVertexAction = new IEditPointAction() {
		@Override
		public String getText() {
			return Messages.NavigationLayerDialog_RemoveVertexActionName;
		}

		@Override
		public Image getImage() {
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON);
		}

		@Override
		public void run(EditPoint point) {
			NavigationTarget t = (NavigationTarget)((Object[])point.getFeature())[0];
			int index = (int) ((Object[])point.getFeature())[1];
			NavigationLayerDialog.this.deleteVertex( t, index);
		}
	};
	
	private IEditPointAction deleteTargetAction = new IEditPointAction() {
		@Override
		public String getText() {
			return Messages.NavigationLayerDialog_DeleteTargetActionName;
		}

		@Override
		public Image getImage() {
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON);
		}

		@Override
		public void run(EditPoint point) {
			Object x = point.getFeature();
			NavigationTarget t = null;
			if (x instanceof NavigationTarget) {
				t = (NavigationTarget)x;
			}else if ( x instanceof Object[] ) {
				t = (NavigationTarget) ((Object[])x)[0];
			}
			NavigationLayerDialog.this.deleteTargets( t );
		}
	};
	
	private IMapEditManager editManager = new IMapEditManager() {

		private Stack<Object[]> undo = new Stack<>();
		
		@Override
		public EditPoint findFeature(int x, int y, IViewportModel vm) {
			int xll = x - 5;
			int yll = y - 5;
			int xur = x + 5;
			int yur = y + 5;
			
			Coordinate world = vm.pixelToWorld(x, y);
			Coordinate worldll = vm.pixelToWorld(xll, yll);
			Coordinate worldur = vm.pixelToWorld(xur, yur);
			
			try {
				ReferencedEnvelope env = new ReferencedEnvelope(worldll.x, worldur.x, worldll.y, worldur.y, vm.getCRS());
				env = ReprojectUtils.reproject(env,  SmartDB.DATABASE_CRS);
				
				org.locationtech.jts.geom.Point db = GeometryFactoryProvider.getFactory().createPoint(ReprojectUtils.reproject(world.x, world.y, vm.getCRS(), SmartDB.DATABASE_CRS));
				NavigationTarget closest = null;
				double distance = Double.POSITIVE_INFINITY;
				Coordinate cc = null;
				for (NavigationTarget t : targets) {
					if (!t.getGeometry().getEnvelopeInternal().intersects(env)) continue;
					if (t.isPoint()) {
						org.locationtech.jts.geom.Point p = (org.locationtech.jts.geom.Point)t.getGeometry();
						if (p.distance(db) < distance) {
							distance = p.distance(db);
							closest = t;
							cc = p.getCoordinate();
						}
					}else if (t.isLine()) {
						org.locationtech.jts.geom.LineString ls = (org.locationtech.jts.geom.LineString)t.getGeometry();
						if (ls.distance(db) < distance) {
							distance = ls.distance(db);
							closest = t;
							cc = DistanceOp.nearestPoints(ls, db)[0];
						}
						
					}
				}
				if (closest == null) return null;
				
				Coordinate px = ReprojectUtils.reproject(cc.getX(),cc.getY(), SmartDB.DATABASE_CRS, vm.getCRS());
				java.awt.Point pnt = vm.worldToPixel(px);
				if (pnt.distance(x, y) > 5) return null;
					
				if (closest.isPoint()) {
					EditPoint ep = new EditPoint(vm.worldToPixel(px), closest, closest.getId());
					ep.addAction(deleteTargetAction);
					return ep;
				}
				if (closest.isLine()) {
					//are we at a vertex of just a point along the line?
					org.locationtech.jts.geom.LineString ls = (org.locationtech.jts.geom.LineString)closest.getGeometry();
					Coordinate nearest = null;
					distance = 0;
					int index = 0;
					for (int i = 0; i <ls.getCoordinates().length; i ++) {
						Coordinate c = ls.getCoordinates()[i];
						if (nearest == null || c.distance(cc) < distance) {
							nearest = c;
							distance = c.distance(cc);
							index = i;
						}
					}
					if (env.contains(nearest)) {
						//edit
						EditPoint ep = new EditPoint(vm.worldToPixel(px), new Object[] {closest,index, false}, closest.getId());
						ep.addAction(deleteVertexAction);
						ep.addAction(deleteTargetAction);
						return ep;

					}else {
						//insert
						int insertat = index;
						if (index == 0) {
							insertat = 1;
						}else if (index == ls.getCoordinates().length - 1) {
							insertat = index;
						}else {
							LineSegment l1 = new LineSegment(ls.getCoordinates()[index-1], ls.getCoordinates()[index]);
							LineSegment l2 = new LineSegment(ls.getCoordinates()[index], ls.getCoordinates()[index+1]);
							if (l1.distance(cc) < l2.distance(cc)) {
								insertat = index;
							}else {
								insertat = index+1;
							}
						}
						EditPoint ep = new EditPoint(vm.worldToPixel(px), new Object[] {closest, insertat, true}, closest.getId());
						ep.addAction(deleteTargetAction);
						return ep;
					}
					
				}
				
			}catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}

		@Override
		public void moveFeature(Object feature, int x, int y, IViewportModel vm) {
			Coordinate crspx = vm.pixelToWorld(x, y);
			// convert to lat/long
			if (!CRS.equalsIgnoreMetadata(vm.getCRS(), SmartDB.DATABASE_CRS)) {
				try {
					crspx = ReprojectUtils.reproject(crspx.x, crspx.y, vm.getCRS(), SmartDB.DATABASE_CRS);
				} catch (Exception ex) {
					CyberTrackerPlugIn.log(ex.getMessage(), ex);
					return;
				}
			}
			
			NavigationTarget t = null;
			int index = -1;
			boolean add = false;
			if (feature instanceof NavigationTarget) {
				 t = (NavigationTarget)feature;
			}else {
				t = (NavigationTarget) ((Object[])feature)[0];
				index = (int)((Object[])feature)[1];
				add = (boolean)((Object[])feature)[2];
			}
			
			undo.push(new Object[] {t,  GeometryFactoryProvider.getFactory().createGeometry(t.getGeometry())});
			if (t.isPoint()) {
				updateGeometry(t, GeometryFactoryProvider.getFactory().createPoint(crspx));
			}else if (t.isLine()) {
				Coordinate[] crs = ((LineString)t.getGeometry()).getCoordinates();
				if (!add) {
					crs[index] = crspx;
				}else {
					Coordinate[] ccc = new Coordinate[crs.length+1];
					int j = 0;
					for (int i = 0; i < crs.length; i ++) {
						if (i == index) ccc[j++] = crspx;
						ccc[j++] = crs[i];
					}
					crs = ccc;
				}
				updateGeometry(t, GeometryFactoryProvider.getFactory().createLineString(crs));		
			}
			tools.getTool(UndoTool.ID).setEnabled(editManager.canUndo());
		}

		@Override
		public void undo() {
			Object[] item = undo.pop();
			Display.getDefault().syncExec(()->{
				NavigationTarget t = (NavigationTarget)item[0];
				Geometry g = (Geometry)item[1]; 
				updateGeometry(t, g);
				tools.getTool(UndoTool.ID).setEnabled(editManager.canUndo());
			});			
		}

		@Override
		public boolean canUndo() {
			return !undo.isEmpty();
		}
		
	
	};
	
	public interface ILayerAction{
		public void doAction() throws Exception;
	}
}
