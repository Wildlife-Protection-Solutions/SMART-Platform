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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
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
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.selection.SelectCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.geotools.PatrolDataSource;
import org.wcs.smart.patrol.internal.ui.importwp.PatrolImportGpsDataWizard;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.TrackPart;
import org.wcs.smart.patrol.udig.catalog.PatrolGeoResource;
import org.wcs.smart.patrol.udig.catalog.PatrolService;
import org.wcs.smart.patrol.udig.catalog.PatrolServiceExtension;
import org.wcs.smart.patrol.ui.PatrolTrackPointDialog;
import org.wcs.smart.ui.map.TracksComposite;
import org.wcs.smart.ui.map.tool.SplitTrackTool;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;

/**
 * Composite to display list of mission tracks.
 * 
 * @author elitvin
 * @since 6.0.0
 */
public class PartolTracksComposite extends TracksComposite {
	
	private List<Layer> trackLayers = new ArrayList<Layer>();

	private PatrolService patrolService = null;
	
	private PatrolLegDay patrolLegDay;
	private boolean canEdit;

	public PartolTracksComposite(Composite parent, PatrolLegDay patrolLegDay, boolean canEdit) {
		super(parent);
		this.patrolLegDay = patrolLegDay;
		this.canEdit = canEdit;
		createControls();
		updateInput();
	}

	public void updateInput() {
		getTrackViewer().setInput(buildTrackInput());
	}

	private List<TrackPart> buildTrackInput() {
		try {
			Track track = patrolLegDay.getTrack();
			if (track != null) {
				List<TrackPart> tblInput = new ArrayList<>(track.getTrackParts());
				//sort tracks based on start time
				Collections.sort(tblInput, new Comparator<TrackPart>() {
					@Override
					public int compare(TrackPart tp1, TrackPart tp2) {
						try{
							double z1 = tp1.getLineString().getCoordinateN(0).z;
							double z2 = tp2.getLineString().getCoordinateN(0).z;
							return Double.compare(z1, z2);
						}catch (Exception ex){
							return 0;
						}
					}
				});
				return tblInput;
			}
		} catch (ParseException e) {
			SmartPlugIn.displayLog("Unable to parse geometry assiciated with a track.", e);
		}
		return new ArrayList<>();
	}
	
	@Override
	protected void createTableViewerColumns(TableViewer trackTableViewer, TableColumnLayout layout) {
		final TableViewerColumn columnId = new TableViewerColumn(trackTableViewer, SWT.NONE);
		columnId.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (getTrackViewer().getInput() instanceof List) {
					List<?> input = (List<?>) getTrackViewer().getInput();
					return "Part " + (input.indexOf(element) + 1);
				}
				return super.getText(element);
			}
		});
		columnId.getColumn().setText("Track Part");
		columnId.getColumn().setResizable(true);
		columnId.getColumn().setMoveable(false);
		layout.setColumnData(columnId.getColumn(), new ColumnWeightData(50));
	}

	@Override
	protected void addLayers(MapViewer viewer) {
		//add track layer
		try{
			List<IResolve> resolves = CatalogPlugin.getDefault().getLocalCatalog().find(PatrolServiceExtension.createURL(patrolLegDay.getPatrolLeg().getPatrol()), null);
			for (IResolve r : resolves){
				IService service = r.resolve(IService.class, null);
				if (service != null && service instanceof PatrolService){
					patrolService = (PatrolService) service;		
					break;
				}
			}

			if (patrolService != null){
				List<IGeoResource> newLayers = new ArrayList<IGeoResource>();

				@SuppressWarnings("unchecked")
				List<IGeoResource> layers = (List<IGeoResource>) patrolService.resources(null);
				for (IGeoResource layer : layers) {
					if (((PatrolGeoResource) layer).getType() == PatrolDataSource.TRACK_PART_TYPE) {
						newLayers.add(layer);
					}
				}
				patrolService.refresh(patrolLegDay.getPatrolLeg().getPatrol(), null);
				AddLayersCommand command = new AddLayersCommand(newLayers, 0){
					@Override
					public void run( IProgressMonitor monitor ) throws Exception {
						super.run(monitor);
						PartolTracksComposite.this.trackLayers = getLayers();
						
						//add track style
						for (Layer trackLayer : PartolTracksComposite.this.trackLayers){
							trackLayer.getStyleBlackboard().put(SLDContent.ID, PartolTracksComposite.this.buildTrackStyle());
						}
					}
				};
				getMapViewer().getMap().sendCommandASync(command);
			}
			
		}catch (Exception ex){
			SmartPlugIn.displayLog("Error configuring map." + "\n\n" + ex.getMessage(), ex);
		}
	}

	protected void updateMapSelection(){
		super.updateMapSelection();

		if (trackLayers == null) return;
		//update map language
		IStructuredSelection selection = (IStructuredSelection) getTrackViewer().getSelection();
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		
		List<Filter> allFilters = new ArrayList<Filter>();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object sel = iterator.next();
			if (sel instanceof TrackPart){
				TrackPart tp = (TrackPart)sel;
				allFilters.add(ff.equals(ff.property("uid"),  //$NON-NLS-1$
					ff.literal(tp.getUid())));
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
	
	private void refresh(boolean fire) {
		clearMessage();
		updateInput();
		if (fire){
			fireChangeListeners();
		}
		getMapViewer().getMap().getRenderManager().refresh(null);
		if (patrolService != null){
			try {
				patrolService.refresh(patrolLegDay.getPatrolLeg().getPatrol(), null);
			} catch (IOException e) {
				setError("TracksComposite_MapError" + e.getMessage());
				SmartPlugIn.log(e.getMessage(), e);
			}
		}
	}
	
	@Override
	protected void importTracks() {
		final PatrolImportGpsDataWizard wizard = new PatrolImportGpsDataWizard(patrolLegDay, GPSDataImport.ImportType.TRACK);		

		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.setTaskName("LOAD_WIZARD_PROGRESS_MSG");
					WizardDialog dialog = new WizardDialog(getShell(), wizard);
					monitor.setTaskName("Messages.MissionDayComposite_DisplayingWizard");
					dialog.open();
					refresh(false);
				}
			});
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog("Messages.PatrolLegDayInputComposite_ErrorImportTracksWizard" + ex.getLocalizedMessage(), ex);
		}
	}

	@Override
	protected void mergeTrack() {
		IStructuredSelection sel = (IStructuredSelection) getTrackViewer().getSelection();
		List<LineString> tracksToMerge = new ArrayList<>();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof TrackPart){
				tracksToMerge.add(((TrackPart) item).getLineString());
			}
		}
		if (tracksToMerge.size() < 2){
			return ;
		}
		
		//sort tracks based on start time
		Collections.sort(tracksToMerge, new Comparator<LineString>() {
			@Override
			public int compare(LineString o1, LineString o2) {
				try{
					double z1 = o1.getCoordinateN(0).z;
					double z2 = o2.getCoordinateN(0).z;
					return Double.compare(z1, z2);
				}catch (Exception ex){
					return 0;
				}
				
			}
		});
		
		//get all coordinates; if start and end coordinates match
		//then only add once
		try{
			List<LineString> newLsList = new ArrayList<>(patrolLegDay.getTrack().getLineStrings());
			newLsList.removeAll(tracksToMerge);
			List<Coordinate> ls = new ArrayList<Coordinate>();
			for (LineString mergedTrack : tracksToMerge) {
				Coordinate[] cs = mergedTrack.getCoordinates();
				if (ls.isEmpty() || !ls.get(ls.size()-1).equals2D(cs[0])){
					ls.add(cs[0]);
				}
				for (int j = 1; j < cs.length;j ++){
					ls.add(cs[j]);
				}
				
			}
			
			LineString newLs = GeometryFactoryProvider.getFactory().createLineString(ls.toArray(new Coordinate[ls.size()]));
			newLsList.add(newLs);
			patrolLegDay.getTrack().setLineStrings(newLsList);
		
			refresh(true);
		}catch (Exception ex){
			SmartPlugIn.displayLog("Could not parse linestring.", ex);
		}
	}

	@Override
	protected void splitTrack(ToolItem splitToolItem) {
		final SplitTrackTool spTool = (SplitTrackTool) ApplicationGIS.getToolManager().findTool(SplitTrackTool.ID);
		if (spTool != null){

			spTool.setFinishCommand(new SplitTrackTool.FinishCommand() {
				
				@Override
				public void onFinish(List<Coordinate> points) {
					if (PartolTracksComposite.this.isDisposed())
						return;
					if (points == null){
						//tool has been de-activated
						clearMessage();
						splitToolItem.setSelection(false);
						selectLastTool();
						return;
					}
					IStructuredSelection sel = (IStructuredSelection) getTrackViewer().getSelection();
					LineString trackToSplit = null;
					for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
						Object item = (Object) iterator.next();
						if (item instanceof TrackPart){
							trackToSplit = ((TrackPart) item).getLineString();
						}
					}
					
					if (trackToSplit != null){
						LineString ls1 = null;
						try{
							ls1 = trackToSplit;
						}catch (Exception ex){
							setError("Could not parse linestring.");
							return;
						}
						
						LineString ls2 = GeometryFactoryProvider.getFactory().createLineString(new Coordinate[]{points.get(0), points.get(1)});
						
						Point intersection = null;
						Geometry g = ls1.intersection(ls2);
						if (g == null){
							setError("Line does not intersect track.");
							return;
						}else if (g instanceof Point){
							intersection = (Point)g;
						}else if (g instanceof MultiPoint){
							intersection = (Point)((MultiPoint)g).getGeometryN(0);
						}
						if (intersection == null){
							setError("No intersection point found.");
							return;
						}
						LineString[] newLs = GeometryUtils.splitSimple(ls1, new Coordinate(intersection.getX(), intersection.getY()));

						if (newLs == null || newLs.length != 2){
							setError("Could not split tracks.");
							return;
						}

						try {
							List<LineString> newLsList = new ArrayList<>(patrolLegDay.getTrack().getLineStrings());
							newLsList.remove(trackToSplit);
							for (LineString nls : newLs) {
								newLsList.add(nls);
							}
							patrolLegDay.getTrack().setLineStrings(newLsList);
						} catch (ParseException e) {
							setError("Failed to assign new tracks after split.");
							return;
						}
						
						refresh(true);
						
						//de-active
						selectLastTool();
					}
					
					
				}
			});
			setInfo("Draw a line across the selected track where you want it to split.");
			ApplicationGIS.getToolManager().getToolAction(SplitTrackTool.ID, SplitTrackTool.CATEGORY_ID).run();	
		}
	}

	@Override
	protected void deleteTrack() {
		IStructuredSelection sel = (IStructuredSelection) getTrackViewer().getSelection();
		List<LineString> toDelete = new ArrayList<LineString>();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof TrackPart){
				toDelete.add(((TrackPart)item).getLineString());
			}
		}
		
		if (toDelete.size() == 0){
			return;
		}
		
		if (!MessageDialog.openQuestion(getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected tracks?  This cannot be undone.", new Object[]{toDelete.size()}))){
			return;
		}
		
		//delete the track and remove any waypoint references to it.
		List<LineString> lsList;
		try {
			lsList = new ArrayList<>(patrolLegDay.getTrack().getLineStrings());
			for (LineString ls : toDelete){
				lsList.remove(ls);
			}
			patrolLegDay.getTrack().setLineStrings(lsList);
		} catch (ParseException e) {
			SmartPlugIn.displayLog(e.getMessage(), e);
		}
		refresh(true);
	}

	@Override
	protected void editTrack() {
		IStructuredSelection sel = (IStructuredSelection) getTrackViewer().getSelection();
		if (sel != null && !sel.isEmpty()) {
			LineString ls = ((TrackPart) sel.getFirstElement()).getLineString();
			try{
				int index = patrolLegDay.getTrack().getLineStrings().indexOf(ls);
				if (index >= 0) {
					PatrolTrackPointDialog tpd = new PatrolTrackPointDialog(getShell(), patrolLegDay.getTrack(), index, canEdit);
					tpd.open();
				} else {
					SmartPlugIn.displayLog("Selected track part do not belong to current track.", null);
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog("Messages.TracksComposite_CouldNotParseLinestring", ex);
			}
			ApplicationGIS.getToolManager().setCurrentEditor(this);
			selectLastTool();
			refresh(false);
		}
	}

	@Override
	protected void zoomTrack() {
		// TODO ZZZZZZ implement
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

		Filter dayFilter = ff.equals(ff.property("day"), ff.literal(patrolLegDay.getDate())); //$NON-NLS-1$
		Filter legFilter = ff.equals(ff.property("leg"), ff.literal(patrolLegDay.getPatrolLeg().getId())); //$NON-NLS-1$
		Filter filter = ff.and(dayFilter, legFilter);
		LineSymbolizer otherSym = sf.createLineSymbolizer();
		otherSym.setStroke(otherDays);
		
		LineSymbolizer todaySym = sf.createLineSymbolizer();
		todaySym.setStroke(toDay);
		
		Rule otherRule = sf.createRule();
		otherRule.symbolizers().add(otherSym);
		otherRule.setIsElseFilter(true);
		otherRule.setFilter(filter);
		otherRule.setName("Other Days Legs");
		
		Rule todayRule = sf.createRule();
		todayRule.symbolizers().add(todaySym);
		todayRule.setIsElseFilter(false);
		todayRule.setFilter(filter);
		todayRule.setName("Current Day Leg");

		FeatureTypeStyle fts = sf.createFeatureTypeStyle();
		fts.rules().add(todayRule);
		fts.rules().add(otherRule);
		
		Style style = sf.createStyle();
		style.featureTypeStyles().add(fts);

		return style;
	}
	
}
