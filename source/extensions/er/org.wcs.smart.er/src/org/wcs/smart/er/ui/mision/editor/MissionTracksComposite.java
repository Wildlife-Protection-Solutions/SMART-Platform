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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolItem;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.TextSymbolizer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.selection.SelectCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.map.samplingunit.SamplingUnitGeoResource;
import org.wcs.smart.er.map.samplingunit.SamplingUnitService;
import org.wcs.smart.er.map.samplingunit.SamplingUnitServiceExtension;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.ui.mision.importwp.MissionImportGpsDataWizard;
import org.wcs.smart.er.ui.mision.udig.MissionDataSource;
import org.wcs.smart.er.ui.mision.udig.MissionGeoResource;
import org.wcs.smart.er.ui.mision.udig.MissionService;
import org.wcs.smart.er.ui.mision.udig.MissionServiceExtension;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.ui.SmartWizardDialog;
import org.wcs.smart.ui.map.TracksComposite;
import org.wcs.smart.ui.map.tool.SplitTrackTool;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;
import org.wcs.smart.util.UuidUtils;

/**
 * Composite to display list of mission tracks.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionTracksComposite extends TracksComposite {

	private FilterFactory ff = CommonFactoryFinder.getFilterFactory();
	
	private MissionTrackEditDialog dialog;
	
	private List<Layer> trackLayers = new ArrayList<Layer>();
	
	private MissionService missionService = null;
	private SamplingUnitService suService = null;
	
	private List<MissionTrack> toDelete = new ArrayList<MissionTrack>();

	public MissionTracksComposite(Composite parent, MissionTrackEditDialog dialog) {
		super(parent);
		this.dialog = dialog;
		createControls();
		updateInput();
	}

	private List<MissionTrack> buildTrackInput() {
		List<MissionTrack> tblInput = new ArrayList<MissionTrack>();
		tblInput.addAll(dialog.getMissionDay().getTracks());
		
		//sort tracks based on start time
		Collections.sort(tblInput, new Comparator<MissionTrack>() {
			@Override
			public int compare(MissionTrack o1, MissionTrack o2) {
				try{
					double z1 = o1.getLineString().getCoordinateN(0).getZ();
					double z2 = o2.getLineString().getCoordinateN(0).getZ();
					return Double.compare(z1, z2);
				}catch (Exception ex){
					return 0;
				}
			}
		});
		return tblInput;
	}

	
	public void updateInput() {
		getTrackViewer().setInput(buildTrackInput());
	}

	protected String getMapName() {
		return Messages.TracksComposite_MapName;
	}
	
	protected void updateMapSelection(){
		super.updateMapSelection();

		if (trackLayers == null) return;
		//update map language
		IStructuredSelection selection = (IStructuredSelection) getTrackViewer().getSelection();
		
		List<Filter> allFilters = new ArrayList<Filter>();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object su = (Object) iterator.next();
			if (su instanceof MissionTrack){
				MissionTrack mt = (MissionTrack)su;
				allFilters.add(ff.equals(ff.property("fid"),  //$NON-NLS-1$
					ff.literal(UuidUtils.uuidToString(mt.getUuid())))); 
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
	
	private boolean confirmChanges(){
		if (dialog.isChanged()){
			if (!MessageDialog.openQuestion(getShell(),
					Messages.TracksComposite_ImportTitle,
					Messages.TracksComposite_SaveChangesMessage)){
				return false;
			}
			if (!dialog.saveChanges()){
				return false;
			}
			
		}
		return true;
	}
	
	protected void importTracks() {
		if (!confirmChanges()) return;
		final ImportGpsDataWizard wizard = new MissionImportGpsDataWizard(dialog.getMissionDay(), GPSDataImport.ImportType.TRACK);
		wizard.setDateOption(dialog.getMissionDay().getDate());
		WizardDialog dialog = new SmartWizardDialog(getShell(), wizard);
		dialog.open();
		refresh(false);		
	}
	
	protected void mergeTrack() {
		IStructuredSelection sel = (IStructuredSelection) getTrackViewer().getSelection();
		List<MissionTrack> tracksToMerge = new ArrayList<MissionTrack>();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof MissionTrack){
				tracksToMerge.add((MissionTrack) item);
			}
		}
		if (tracksToMerge.size() < 2){
			return ;
		}
		
		//sort tracks based on start time
		Collections.sort(tracksToMerge, new Comparator<MissionTrack>() {
			@Override
			public int compare(MissionTrack o1, MissionTrack o2) {
				try{
					double z1 = o1.getLineString().getCoordinateN(0).getZ();
					double z2 = o2.getLineString().getCoordinateN(0).getZ();
					return Double.compare(z1, z2);
				}catch (Exception ex){
					return 0;
				}
				
			}
		});
		
		//get all coordinates; if start and end coordinates match
		//then only add once
		try{
			MissionTrack main = tracksToMerge.get(0);
			List<Coordinate> ls = new ArrayList<Coordinate>();
			for (Coordinate c : main.getLineString().getCoordinates()){
				ls.add(c);
			}
			for (int i = 1; i < tracksToMerge.size(); i ++){
				toDelete.add(tracksToMerge.get(i));
				dialog.getMissionDay().getTracks().remove(tracksToMerge.get(i));
				Coordinate[] cs = tracksToMerge.get(i).getLineString().getCoordinates();
				if (!ls.get(ls.size()-1).equals2D(cs[0])){
					ls.add(cs[0]);
				}
				for (int j = 1; j < cs.length;j ++){
					ls.add(cs[j]);
				}
			}
			
			
			LineString newLs = GeometryFactoryProvider.getFactory().createLineString(ls.toArray(new Coordinate[ls.size()]));
			main.setLineString(newLs);
		
			refresh(true);
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(Messages.TracksComposite_CouldNotParseLinestring, ex);
		}
	}
	
	protected void splitTrack(ToolItem splitToolItem) {
		if (!confirmChanges()) return;

		final SplitTrackTool spTool = (SplitTrackTool) ApplicationGIS.getToolManager().findTool(SplitTrackTool.ID);
		if (spTool != null){

			spTool.setFinishCommand(new SplitTrackTool.FinishCommand() {
				
				@Override
				public void onFinish(List<Coordinate> points) {
					if (MissionTracksComposite.this.isDisposed())
						return;
					if (points == null){
						//tool has been de-activated
						clearMessage();
						splitToolItem.setSelection(false);
						selectLastTool();
						return;
					}
					IStructuredSelection sel = (IStructuredSelection) getTrackViewer().getSelection();
					MissionTrack trackToSplit = null;
					for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
						Object item = (Object) iterator.next();
						if (item instanceof MissionTrack){
							trackToSplit = (MissionTrack) item;
						}
					}
					
					if (trackToSplit != null){
						LineString ls1 = null;
						try{
							ls1 = trackToSplit.getLineString();
						}catch (Exception ex){
							setError(Messages.TracksComposite_CouldNotParseLinestring);
							return;
						}
						
						LineString ls2 = GeometryFactoryProvider.getFactory().createLineString(new Coordinate[]{points.get(0), points.get(1)});
						
						Point intersection = null;
						Geometry g = ls1.intersection(ls2);
						if (g == null){
							setError(Messages.TracksComposite_NoIntersection1);
							return;
						}else if (g instanceof Point){
							intersection = (Point)g;
						}else if (g instanceof MultiPoint){
							intersection = (Point)((MultiPoint)g).getGeometryN(0);
						}
						if (intersection == null){
							setError(Messages.TracksComposite_NoIntersection2);
							return;
						}
						LineString[] newLs = GeometryUtils.splitSimple(ls1, new Coordinate(intersection.getX(), intersection.getY()));

						if (newLs == null || newLs.length != 2){
							setError(Messages.TracksComposite_CouldNotSplit);
							return;
						}
						trackToSplit.setLineString(newLs[0]);
						MissionTrack newTrack = new MissionTrack();
						newTrack.setId(trackToSplit.getId());
						newTrack.setLineString(newLs[1]);
						newTrack.setMissionDay(trackToSplit.getMissionDay());
						newTrack.setSamplingUnit(trackToSplit.getSamplingUnit());
						
						trackToSplit.getMissionDay().getTracks().add(newTrack);
						
						refresh(true);
						
						//de-active
						selectLastTool();
					}
					
					
				}
			});
			setInfo(Messages.TracksComposite_SplitInformation);
			ApplicationGIS.getToolManager().getToolAction(SplitTrackTool.ID, SplitTrackTool.CATEGORY_ID).run();	
		}
	}
	
	private void refresh(boolean fire) {
		clearMessage();
		updateInput();
		if (fire){
			fireChangeListeners();
		}
		getMapViewer().getMap().getRenderManager().refresh(null);
		if (missionService != null){
			try {
				missionService.refresh(dialog.getMissionDay().getMission(), null);
			} catch (IOException e) {
				setError(Messages.TracksComposite_MapError + e.getMessage());
				EcologicalRecordsPlugIn.log(e.getMessage(), e);
			}
		}
	}
	
	protected void deleteTrack(){
		IStructuredSelection sel = (IStructuredSelection) getTrackViewer().getSelection();
		List<MissionTrack> toDelete = new ArrayList<MissionTrack>();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof MissionTrack){
				toDelete.add((MissionTrack)item);
			}
		}
		
		if (toDelete.size() == 0){
			return ;
		}
		
		
		if (!MessageDialog.openQuestion(getShell(), Messages.TracksComposite_DeleteTitle, MessageFormat.format(Messages.TracksComposite_DeleteMessage, new Object[]{toDelete.size()}))){
			return;
		}
		
		//delete the track and remove any waypoint references to it.
		for (MissionTrack mt : toDelete){
			dialog.getMissionDay().getTracks().remove(mt);
		}
		this.toDelete.addAll(toDelete);
		refresh(true);
	}
	
	public List<MissionTrack> getTracksToDelete(){
		return this.toDelete;
	}
	
	public void clearTracksToDelete(){
		toDelete.clear();
	}

	protected void editTrack() {
		if (!confirmChanges()) return;
		
		IStructuredSelection sel = (IStructuredSelection) getTrackViewer().getSelection();
		if (sel != null && !sel.isEmpty()) {
			MissionTrack track = (MissionTrack) sel.getFirstElement();
			try{
				MissionTrackPointDialog tpd = new MissionTrackPointDialog(getShell(), track);
				tpd.open();
			}catch (Exception ex){
				EcologicalRecordsPlugIn.displayLog(Messages.TracksComposite_CouldNotParseLinestring, ex);
			}
			ApplicationGIS.getToolManager().setCurrentEditor(this);
			selectLastTool();
			refresh(false);
		}
	}

	protected void zoomTrack() {
		IStructuredSelection sel = (IStructuredSelection) getTrackViewer().getSelection();
		Envelope env = null;
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			MissionTrack track = (MissionTrack) iterator.next();
			try{
				if (env == null){
					env = track.getLineString().getEnvelopeInternal();
				}else{
					env.expandToInclude(track.getLineString().getEnvelopeInternal());
				}
			}catch (Exception ex){
				EcologicalRecordsPlugIn.displayLog(Messages.TracksComposite_CouldNotParseLinestring, ex);
			}
			
		}
		if (env != null){
			SetViewportBBoxCommand bbox = new SetViewportBBoxCommand(env, GeometryUtils.SMART_CRS);
			getMap().sendCommandASync(bbox);
		}
	}

	/**
	 * Cell editor for track id
	 */
	private class IdTableEditor extends EditingSupport {
		private CellEditor editor;
		
		IdTableEditor(TableViewer viewer) {
			super(viewer);
			this.editor = new TextCellEditor(viewer.getTable());	
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element instanceof MissionTrack) {
				MissionTrack t = (MissionTrack) element;
				if (!t.getId().equals(value)){
					String svalue = ((String) value).trim();
					if (!SmartUtils.isSimpleString(svalue, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, MissionTrack.MAX_ID_LENGTH, 1)){
						MessageDialog.openError(getShell(), Messages.TracksComposite_ErrorDialogTitle, MessageFormat.format(Messages.TracksComposite_InvalidId, new Object[]{MissionTrack.MAX_ID_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
					}else{
						t.setId(svalue);
						refresh(true);
					}
				}
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
		private SamplingUnitCellEditor editor;
		
		SuTableEditor(TableViewer viewer) {
			super(viewer);
			this.editor = new SamplingUnitCellEditor(viewer.getTable(), true);	
			editor.setInput(dialog.getMissionDay());
		}

		@Override
		protected void setValue(Object element, Object value) {
			
			if (element instanceof MissionTrack) {
				boolean changed = false;
				MissionTrack t = (MissionTrack) element;
				if (value == null){
					if (t.getSamplingUnit() != null){
						changed = true;
						t.setSamplingUnit(null);
					}
				}else{
					Object value2 = editor.getSamplingUnit((Integer)value);
					if (value2 != null && value2 instanceof SamplingUnit){
						if ((t.getSamplingUnit() != null && !t.getSamplingUnit().equals(value2)) || 
								t.getSamplingUnit() == null){
							t.setSamplingUnit((SamplingUnit) value2);
												changed = true;
						}
					}else{
						if (t.getSamplingUnit() != null){
							t.setSamplingUnit(null);
							changed = true;
						}					
					}
				}
				if (changed){
					refresh(true);
				}
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
	
	private Style buildTrackStyle(){
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		
		Stroke otherDays = sf.createStroke(ff.literal("#0080FF"),  //$NON-NLS-1$
				ff.literal(1.0), 
				null, null, null, 
				new float[]{15,1},
				null, null, null);
		
		Stroke toDay = sf.createStroke(ff.literal("#0080FF"), ff.literal(3.0)); //$NON-NLS-1$

		Filter dateFilter = ff.equals(ff.property("date"), ff.literal(dialog.getMissionDay().getDate())); //$NON-NLS-1$
		LineSymbolizer otherSym = sf.createLineSymbolizer();
		otherSym.setStroke(otherDays);
		
		LineSymbolizer todaySym = sf.createLineSymbolizer();
		todaySym.setStroke(toDay);
		
		Rule otherRule = sf.createRule();
		otherRule.symbolizers().add(otherSym);
		otherRule.setElseFilter(true);
		otherRule.setFilter(dateFilter);
		otherRule.setName(Messages.TracksComposite_OtherDayRule);
		
		Rule todayRule = sf.createRule();
		todayRule.symbolizers().add(todaySym);
		todayRule.setElseFilter(false);
		todayRule.setFilter(dateFilter);
		todayRule.setName(Messages.TracksComposite_CurrentDayRule);

		FeatureTypeStyle fts = sf.createFeatureTypeStyle();
		fts.rules().add(todayRule);
		fts.rules().add(otherRule);
		
		Style style = sf.createStyle();
		style.featureTypeStyles().add(fts);

		return style;
	}
	
	private TextSymbolizer createTextSymbolizer(){
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		
		TextSymbolizer sym = sf.createTextSymbolizer();
		sym.setLabel(ff.property("id")); //$NON-NLS-1$
		
		return sym;
	}

	@Override
	protected void createTableViewerColumns(TableViewer trackTableViewer, TableColumnLayout layout) {
		final TableViewerColumn columnId = new TableViewerColumn(trackTableViewer, SWT.NONE);
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
		columnId.setEditingSupport(new IdTableEditor(trackTableViewer));
		layout.setColumnData(columnId.getColumn(), new ColumnWeightData(50));
		
		final TableViewerColumn colSu = new TableViewerColumn(trackTableViewer, SWT.NONE);
		colSu.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MissionTrack) {
					MissionTrack track = (MissionTrack)element;
					if (track.getSamplingUnit() == null){
						return Messages.TracksComposite_NoneOption;
					}else{
						return track.getSamplingUnit().getId();
					}
				}
				return super.getText(element);
			}
		});
		colSu.getColumn().setText(Messages.TracksComposite_SuLabel);
		colSu.getColumn().setResizable(true);
		colSu.getColumn().setMoveable(false);
		layout.setColumnData(colSu.getColumn(), new ColumnWeightData(50));	
		colSu.setEditingSupport(new SuTableEditor(trackTableViewer));
	}

	@Override
	protected void addLayers(MapViewer viewer) {
		//add track layer
		try{
			List<IResolve> resolves = CatalogPlugin.getDefault().getLocalCatalog().find(MissionServiceExtension.createURUL(dialog.getMissionDay().getMission()), null);
			for (IResolve r : resolves){
				IService service = r.resolve(IService.class, null);
				if (service != null && service instanceof MissionService){
					missionService = (MissionService) service;		
					break;
				}
			}

			if (missionService != null){
				List<IGeoResource> newLayers = new ArrayList<IGeoResource>();

				@SuppressWarnings("unchecked")
				List<IGeoResource> layers = (List<IGeoResource>) missionService.resources(null);
				for (IGeoResource layer : layers) {
					if (((MissionGeoResource) layer).getType() == MissionDataSource.MISSIONTRACK_TYPE) {
						newLayers.add(layer);
					}
				}
				missionService.refresh(dialog.getMissionDay().getMission(), null);
				AddLayersCommand command = new AddLayersCommand(newLayers, 0){
					@Override
					public void run( IProgressMonitor monitor ) throws Exception {
						super.run(monitor);
						MissionTracksComposite.this.trackLayers = getLayers();
						
						//add track style
						for (Layer trackLayer : MissionTracksComposite.this.trackLayers){
							trackLayer.getStyleBlackboard().put(SLDContent.ID, MissionTracksComposite.this.buildTrackStyle());
						}
					}
				};
				getMapViewer().getMap().sendCommandASync(command);
			}
			
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(Messages.TracksComposite_MapConfigurationError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
		
		//add sampling unit layers
		try{
			List<IResolve> resolves = CatalogPlugin.getDefault().getLocalCatalog().find(SamplingUnitServiceExtension.createURL(dialog.getMissionDay().getMission().getSurvey().getSurveyDesign().getUuid()), null);
			for (IResolve r : resolves){
				IService service = r.resolve(IService.class, null);
				if (service != null && service instanceof SamplingUnitService){
					suService = (SamplingUnitService) service;		
					break;
				}
			}

			if (suService != null){
				@SuppressWarnings("unchecked")
				List<IGeoResource> layers = (List<IGeoResource>) suService.resources(null);	
				List<IGeoResource> tmp = new ArrayList<IGeoResource>();
				for (IGeoResource r : layers){
					if (r instanceof SamplingUnitGeoResource){
						String type = ((SamplingUnitGeoResource)r).getDataType();
						if (type.equals(SamplingUnit.GeometryType.PLOT.name()) ||
								type.equals(SamplingUnit.GeometryType.TRANSECT.name())){
							tmp.add(r);
						}
					}		
				}
				AddLayersCommand command = new AddLayersCommand(tmp, 0){
					@Override
					public void run( IProgressMonitor monitor ) throws Exception {
						super.run(monitor);
						
						//add track style
						for (Layer trackLayer : getLayers()){
							Style sd = (Style) trackLayer.getStyleBlackboard().get(SLDContent.ID);
							if (sd == null) continue;
							try{
								sd.featureTypeStyles().get(0).rules().get(0).symbolizers().add(createTextSymbolizer());
							}catch (Exception ex){
								
							}
						}
					}
				};
				getMapViewer().getMap().sendCommandASync(command);
			}
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(Messages.TracksComposite_MapConfigurationError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
	}
}
