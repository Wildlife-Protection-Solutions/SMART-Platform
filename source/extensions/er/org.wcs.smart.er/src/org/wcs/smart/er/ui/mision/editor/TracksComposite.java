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
package org.wcs.smart.er.ui.mision.editor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IResolve;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.selection.SelectCommand;
import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.viewers.MapViewer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.map.samplingunit.SamplingUnitGeoResource;
import org.wcs.smart.er.map.samplingunit.SamplingUnitService;
import org.wcs.smart.er.map.samplingunit.SamplingUnitServiceExtension;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.ui.ISurveyListener;
import org.wcs.smart.er.ui.mision.importwp.MissionImportGpsDataWizard;
import org.wcs.smart.er.ui.mision.udig.MissionDataSource;
import org.wcs.smart.er.ui.mision.udig.MissionGeoResource;
import org.wcs.smart.er.ui.mision.udig.MissionService;
import org.wcs.smart.er.ui.mision.udig.MissionServiceExtension;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.common.importwp.GPSDataImport;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.udig.SetBasemapTool;
import org.wcs.smart.ui.map.LayerListComposite;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapInfoAreaComposite;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.tool.ClearSelectionTool;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite to display list of tracks.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class TracksComposite extends Composite implements MapPart{

	private List<ISurveyListener> changeListeners = new ArrayList<ISurveyListener>();
	private FilterFactory ff = CommonFactoryFinder.getFilterFactory();
	
	private TableViewer trackViewer;
	private MissionTrackEditDialog dialog;
	private MapViewer mapViewer;
	private List<Layer> trackLayers = null;
	
	private MissionService missionService = null;
	private SamplingUnitService suService = null;
	
	public TracksComposite(Composite parent, MissionTrackEditDialog dialog) {
		super(parent, SWT.NONE);
		this.dialog = dialog;
		createControls();
		updateInput();
	}
	
	private List<MissionTrack> buildTrackInput(Mission m) {
		List<MissionTrack> tblInput = new ArrayList<MissionTrack>();
		Date date = dialog.getDate();
		for (MissionTrack t : m.getTracks()) {
			if (SmartUtils.isSameDate(t.getDate(), date)) {
				tblInput.add(t);
			}
		}
		return tblInput;
	}
	
	public void updateInput() {
		trackViewer.setInput(buildTrackInput(dialog.getMission()).toArray());
	}

	private void createControls() {
		setLayout(new GridLayout());
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm sash = new SashForm(this, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftArea = new Composite(sash, SWT.NONE);
		leftArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		leftArea.setLayout(gl);
		
		TabFolder tabFolder = new TabFolder(leftArea, SWT.TOP);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TabItem  tracksTabItem = new TabItem(tabFolder, SWT.NONE);
		tracksTabItem.setText("Tracks");
		
		Composite tableCompOuter = new Composite(tabFolder, SWT.NONE);
		gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		tableCompOuter.setLayout(gl);
		tableCompOuter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//========links========
		ToolBar bar = new ToolBar(tableCompOuter, SWT.HORIZONTAL);
		
		ToolItem importItem = new ToolItem(bar, SWT.PUSH);
		importItem.setText(Messages.TracksComposite_Import);
		importItem.setToolTipText("import tracks");
		importItem.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.IMPORT_TRACK_ICON));
		importItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importTracks();	
			}
		});
		
		ToolItem editItem = new ToolItem(bar, SWT.PUSH);
		editItem.setText(Messages.TracksComposite_Edit);
		editItem.setToolTipText("edit track points");
		editItem.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.EDIT_TRACK_ICON));
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editTrack();	
			}
		});
		
		ToolItem splitItem = new ToolItem(bar, SWT.PUSH);
		splitItem.setText(Messages.TracksComposite_Split);
		splitItem.setToolTipText("split track into multiple segments");
		splitItem.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SPLIT_TRACK_ICON));
		splitItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				splitTrack();
			}
		});
		
		Composite tableComp = new Composite(tableCompOuter, SWT.BORDER);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tracksTabItem.setControl(tableCompOuter);
		
		TableColumnLayout layout = new TableColumnLayout();
		tableComp.setLayout(layout);
		
		trackViewer = new TableViewer(tableComp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION );
//		trackViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		trackViewer.setContentProvider(ArrayContentProvider.getInstance());
		trackViewer.getTable().setHeaderVisible(true);
		trackViewer.getTable().setLinesVisible(true);
		trackViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateMapSelection();	
			}
		});
		final TableViewerColumn columnId = new TableViewerColumn(trackViewer, SWT.NONE);
		columnId.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MissionTrack) {
					MissionTrack track = (MissionTrack)element;
					return track.getId();
				}
				return super.getText(element);
			}
		});
		columnId.getColumn().setText(Messages.TracksComposite_TrackId);
		columnId.getColumn().setResizable(true);
		columnId.getColumn().setMoveable(false);
		columnId.setEditingSupport(new IdTableEditor(trackViewer));
		layout.setColumnData(columnId.getColumn(), new ColumnWeightData(50));
		
		final TableViewerColumn colSu = new TableViewerColumn(trackViewer, SWT.NONE);
		colSu.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MissionTrack) {
					MissionTrack track = (MissionTrack)element;
					if (track.getSamplingUnit() == null){
						return "None";
					}else{
						return track.getSamplingUnit().getId();
					}
				}
				return super.getText(element);
			}
		});
		colSu.getColumn().setText("Sampling Unit");
		colSu.getColumn().setResizable(true);
		colSu.getColumn().setMoveable(false);
		layout.setColumnData(colSu.getColumn(), new ColumnWeightData(50));	
		colSu.setEditingSupport(new SuTableEditor(trackViewer));
		
		TabItem  layerListTabItem = new TabItem(tabFolder, SWT.NONE);
		layerListTabItem.setText("Map Layers");
		
		Composite layersTab = new Composite(tabFolder, SWT.NONE);
		gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		layersTab.setLayout(gl);
		LayerListComposite lv = new LayerListComposite(layersTab);
		lv.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		layerListTabItem.setControl(layersTab);
		
		//========map part========
		Composite mapPart = new Composite(sash, SWT.NONE);
		gl = new GridLayout(2, false);
		gl.marginWidth = gl.marginHeight = 0;
		mapPart.setLayout(gl);

		setupMap(mapPart);
		
		sash.setWeights(new int[]{30,70});
		
		lv.setMap(mapViewer.getMap());
		
		//add track layer
		try{
			List<IResolve> resolves = CatalogPlugin.getDefault().getLocalCatalog().find(MissionServiceExtension.createURUL(dialog.getMission()), null);
			for (IResolve r : resolves){
				IService service = r.resolve(IService.class, null);
				if (service != null && service instanceof MissionService){
					missionService = (MissionService) service;		
					break;
				}
			}

			if (missionService != null){
				List<IGeoResource> newLayers = new ArrayList<IGeoResource>();
				List<IGeoResource> layers = (List<IGeoResource>) missionService.resources(null);
				for (IGeoResource layer : layers) {
					if (((MissionGeoResource) layer).getType() == MissionDataSource.MISSIONTRACK_TYPE) {
						newLayers.add(layer);
					}
				}
				missionService.refresh(dialog.getMission(), null);
				AddLayersCommand command = new AddLayersCommand(newLayers, 0);
				mapViewer.getMap().sendCommandSync(command);
				trackLayers = command.getLayers();
			}
			
		}catch (Exception ex){
			ex.printStackTrace();
			//TODO:
		}
		
		//add sampling unit layers
		try{
			List<IResolve> resolves = CatalogPlugin.getDefault().getLocalCatalog().find(SamplingUnitServiceExtension.createURL(dialog.getMission().getSurvey().getSurveyDesign().getUuid()), null);
			for (IResolve r : resolves){
				IService service = r.resolve(IService.class, null);
				if (service != null && service instanceof SamplingUnitService){
					suService = (SamplingUnitService) service;		
					break;
				}
			}

			if (suService != null){
				List<IGeoResource> layers = (List<IGeoResource>) suService.resources(null);	
				List<IGeoResource> tmp = new ArrayList<IGeoResource>();
				for (IGeoResource r : layers){
					if (r instanceof SamplingUnitGeoResource){
						String type = ((SamplingUnitGeoResource)r).getDataType();
						if (type.equals(SamplingUnit.SamplingUnitType.PLOT.name()) ||
								type.equals(SamplingUnit.SamplingUnitType.TRANSECT.name())){
							tmp.add(r);
						}
					}		
				}
				AddLayersCommand command = new AddLayersCommand(tmp, 0);
				mapViewer.getMap().sendCommandASync(command);
			}
		}catch (Exception ex){
			ex.printStackTrace();
			//TODO:
		}
	}

	
	private void setupMap(Composite parent){
		Composite mapComp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = gl.marginHeight = 0;
		mapComp.setLayout(gl);
		mapComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
		mapViewer = new MapViewer(mapComp, SWT.NONE);
		mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
		Map map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName("Mission Tracks");
		mapViewer.setMap(map);
		
		//set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(SmartDB.DATABASE_CRS);
		
		final LoadDefaultLayersJob defaultLayer = new LoadDefaultLayersJob(mapViewer.getMap(),
				true, null);
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		defaultLayer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (mapViewer == null || TracksComposite.this.isDisposed()){
					return;
				}
				mapViewer.getMap().getRenderManager().refresh(null);
				
			}
		});
		defaultLayer.schedule();
			
			
		ApplicationGIS.getToolManager().setCurrentEditor(this);
		String[] thisTools = new String[] {
				SetBasemapTool.ID,
				MapToolComposite.UDIG_ZOOM_EXTENT_ID,
				MapToolComposite.UDIG_PAN_ID,
				MapToolComposite.UDIG_ZOOM_ID,
				MapToolComposite.UDIG_ZOOM_IN_ID,
				MapToolComposite.UDIG_ZOOM_OUT_ID,
				ClearSelectionTool.ID};

		MapToolComposite tools = new MapToolComposite(thisTools);
		tools.createComposite(mapComp);
		MapInfoAreaComposite infoComp = new MapInfoAreaComposite(mapComp, SWT.NONE, mapViewer) ;
		infoComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));	
	}
	
	private void updateMapSelection(){
		//update map language
		IStructuredSelection selection = (IStructuredSelection) trackViewer.getSelection();
		List<Filter> allFilters = new ArrayList<Filter>();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object su = (Object) iterator.next();
			if (su instanceof MissionTrack){
				MissionTrack mt = (MissionTrack)su;
				allFilters.add(ff.equals(ff.property("fid"),  //$NON-NLS-1$
					ff.literal(SmartUtils.encodeHex(mt.getUuid())))); 
			}
		}
		for (Layer l : trackLayers){
			if (allFilters.size() == 0){
				SelectCommand sc = new SelectCommand(l, Filter.EXCLUDE);
				getMap().sendCommandASync(sc);
			}else{
				SelectCommand sc = new SelectCommand(l, ff.or(allFilters));
				getMap().sendCommandASync(sc);
			}
		}
	}
	public void addChangeListener(ISurveyListener listener){
		changeListeners.add(listener);
	}
	
	protected void fireChangeListeners() {
		for (ISurveyListener listener: changeListeners) {
			listener.compositeModified();
		}
	}
	
	protected void importTracks() {
		final ImportGpsDataWizard wizard = new MissionImportGpsDataWizard(dialog.getMission(), GPSDataImport.ImportType.TRACK);
		wizard.setDateOption(dialog.getDate());
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.setTaskName(Messages.MissionDayComposite_LoadingWizard);
					WizardDialog dialog = new WizardDialog(getShell(), wizard);

					if (dialog != null) {
						monitor.setTaskName(Messages.MissionDayComposite_DisplayingWizard);
						dialog.open();
					}
				}
			});
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog(Messages.MissionDayComposite_ImportWizardError + ex.getLocalizedMessage(), ex);
		}
	}
	
	protected void splitTrack() {
		// TODO Auto-generated method stub
		
	}

	protected void editTrack() {
		IStructuredSelection sel = (IStructuredSelection) trackViewer.getSelection();
		if (sel != null && !sel.isEmpty()) {
			MissionTrack track = (MissionTrack) sel.getFirstElement();
			MissionTrackPointDialog tpd = new MissionTrackPointDialog(Display.getCurrent().getActiveShell(), track);
			tpd.open();
			
			ApplicationGIS.getToolManager().setCurrentEditor(this);
		}
	}

	/**
	 * Cell editor for track id
	 */
	private class IdTableEditor extends EditingSupport {
		private TableViewer viewer;

		private CellEditor editor;
		
		IdTableEditor(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
			this.editor = new TextCellEditor(viewer.getTable());	
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element instanceof MissionTrack) {
				MissionTrack t = (MissionTrack) element;
				t.setId((String)value);
				viewer.refresh();
				fireChangeListeners();
			}
		}

		@Override
		protected Object getValue(Object element) {
			if (element instanceof MissionTrack) {
				MissionTrack t = (MissionTrack) element;
				return t.getId() != null ? t.getId() : ""; //$NON-NLS-1$
			}
			return ""; //$NON-NLS-1$
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}

	/**
	 * Cell editor for track id
	 */
	private class SuTableEditor extends EditingSupport {
		private TableViewer viewer;

		private SamplingUnitCellEditor editor;
		
		SuTableEditor(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
			this.editor = new SamplingUnitCellEditor(trackViewer.getTable(), true);	
			editor.setInput(dialog.getMission(), dialog.getDate());
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element instanceof MissionTrack) {
				MissionTrack t = (MissionTrack) element;
				if (value == null){
					t.setSamplingUnit(null);
					t.setType(TrackType.TRACK);
				}else{
					Object value2 = editor.getSamplingUnit((Integer)value);
					if (value2 != null && value2 instanceof SamplingUnit){
						t.setSamplingUnit((SamplingUnit) value2);
						t.setType(TrackType.RECON);
					}else{
						t.setSamplingUnit(null);
						t.setType(TrackType.TRACK);
					}
				}
				viewer.refresh();
				fireChangeListeners();
			}
		}

		@Override
		protected Object getValue(Object element) {
			if (element instanceof MissionTrack) {
				MissionTrack t = (MissionTrack) element;
				if (t.getSamplingUnit() != null){
					return editor.getIndex(t.getSamplingUnit());
				}
				return 0;
			}
			return 0; 
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}
	
	@Override
	public Map getMap() {
		return mapViewer.getMap();
	}

	@Override
	public void openContextMenu() {
		mapViewer.openContextMenu();
	}

	@Override
	public void setFont(Control textArea) {
		mapViewer.setFont(textArea);
	}

	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapViewer.setSelectionProvider(selectionProvider);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

	
}
