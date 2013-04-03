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
package org.wcs.smart.report.birt.map;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.mapgraphic.MapGraphic;
import net.refractions.udig.project.IMap;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.ChangeCRSCommand;
import net.refractions.udig.project.internal.impl.MapImpl;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.ApplicationGIS.DrawMapParameter;
import net.refractions.udig.style.sld.SLDContent;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.engine.extension.IRowSet;
import org.eclipse.birt.report.engine.extension.ReportItemPresentationBase;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.map.udig.QueryServiceFactory;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.ObservationQuery;
import org.wcs.smart.query.model.PatrolQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.parser.PatrolQueryOptions.DATE_FILTER_OP;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.report.SmartReportParameters;
import org.wcs.smart.report.birt.map.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * For rendering map in a report.
 * 
 * @author Emily
 *
 */
public class SmartMapPresentationImpl extends ReportItemPresentationBase {

	private SmartMapItem mapItem;

	/**
	 * @see org.eclipse.birt.report.engine.extension.ReportItemPresentationBase#setModelObject(org.eclipse.birt.report.model.api.ExtendedItemHandle)
	 */
	public void setModelObject(ExtendedItemHandle modelHandle) {
		super.setModelObject(modelHandle);
		try {
			mapItem = (SmartMapItem) modelHandle.getReportItem();
		} catch (ExtendedElementException e) {
			SmartMapItemPlugIn.displayLog(Messages.SmartMapPresentationImpl_ErrorCreatingMap + e.getMessage(), e);
		}
	}

	/**
	 * @see org.eclipse.birt.report.engine.extension.ReportItemPresentationBase#getOutputType()
	 */
	public int getOutputType() {
		return OUTPUT_AS_IMAGE;
	}

	/**
	 * @see org.eclipse.birt.report.engine.extension.ReportItemPresentationBase#onRowSets(org.eclipse.birt.report.engine.extension.IRowSet[])
	 */
	public Object onRowSets(IRowSet[] rowSets) throws BirtException {
		if (mapItem == null) {
			return null;
		}

		int iwidth = BirtMapUtils.getWidthInPx(modelHandle, dpi);
		int iheight = BirtMapUtils.getHeightInPx(modelHandle, dpi);
		
		try {
			// gdavis - ARGB won't output proper background color for non-alpha
			// supporting
			// image types like jpg. Since the resulting image contains no
			// alpha, RGB works
			// fine for all formats.
			BufferedImage image = new BufferedImage(iwidth, iheight,
					BufferedImage.TYPE_INT_ARGB); // .TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			IMap renderedMap = null;

			String basemap = mapItem.getBasemapName();
			List<GeoSmart> layers = new ArrayList<GeoSmart>();
			
			BasemapDefinition def = null;
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try {
				byte[] uuid = null;
				try{
					uuid = SmartUtils.decodeHex(basemap);
				}catch (Exception ex){
					//eatme
				}
				if (uuid != null){
					def = HibernateManager.getBasemapDefinition(session,uuid);
				}

				List<String> mapqueries = mapItem.getLayers();
				List<String> mapnames = mapItem.getLayerNames();
				List<String> mapstyles = mapItem.getLayerStyles();

				if (mapqueries != null){
					for (int i = 0; i < mapqueries.size(); i++) {
						byte[] quuid = SmartUtils.decodeHex(mapqueries.get(i).split(":")[1]); //$NON-NLS-1$
						QueryType qtype = QueryType.valueOf(mapqueries.get(i).split(":")[0]); //$NON-NLS-1$
						
						Query q = QueryHibernateManager.getInstance().findQuery(session,quuid, qtype);
						GeoSmart layer = new GeoSmart();
						layer.name = mapnames.get(i);
						layer.dbQuery = q;
						layer.style = mapstyles.get(i);
						layers.add(layer);
					}
				}
			} finally {
				session.getTransaction().commit();
				session.close();
			}

			renderedMap = ProjectFactory.eINSTANCE.createMap();

			if (def != null) {
				MapSettings.getInstance(def).applyTo((Map) renderedMap);
			}
			
			// create date filter
			DateFilter dateFilter = new DateFilter(
					DateFilter.DATE_FIELD_OP.WAYPOINT, DATE_FILTER_OP.CUSTOM,
					(Date) context.getParameterValue(SmartReportParameters.PARAM_START_DATE_KEY),
					(Date) context.getParameterValue(SmartReportParameters.PARAM_END_DATE_KEY));
			List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
			for (GeoSmart layer : layers) {
				IService qs = QueryServiceFactory.generateQueryService(layer.dbQuery);
				if (qs != null) {
					boolean add = true;
					layer.service = qs;
					if (Query.class.isAssignableFrom( layer.dbQuery.getClass() )){
						((Query)layer.dbQuery).setDateFilter(dateFilter);
					}
					if (layer.dbQuery instanceof ObservationQuery) {
						((ObservationQuery) layer.dbQuery).setDateFilter(dateFilter);
						((ObservationQuery) layer.dbQuery)
								.getQueryResults(new NullProgressMonitor());
					} else if (layer.dbQuery instanceof PatrolQuery) {
						((PatrolQuery) layer.dbQuery).setDateFilter(dateFilter);
						((PatrolQuery) layer.dbQuery)
								.getQueryResults(new NullProgressMonitor());
					} else if (layer.dbQuery instanceof GriddedQuery ){
						((GriddedQuery)layer.dbQuery).setDateFilter(dateFilter);
						Collection<GridResultItem> data = ((GriddedQuery) layer.dbQuery).getQueryResults(new NullProgressMonitor());
						if (data.size() <= 0){
							add = false;
						}
					}
					if (add){
						List<? extends IGeoResource> resources = qs.resources(null);
						if (resources.size() > 0){
							layer.georesource = resources.get(0);
							toAdd.add(layer.georesource);
						}
					}
					
					
				}
			}
			
			AddLayersCommand cmd = new AddLayersCommand(toAdd);
			renderedMap.executeSyncWithoutUndo(cmd);
			for (Layer l : cmd.getLayers()) {
				//setup name and style
				for (GeoSmart smrt : layers){
					if (smrt.georesource != null && smrt.georesource.equals(l.getGeoResource())){
						l.setName(smrt.name);
						if (smrt.style != null){
							Object st = BirtMapUtils.mementoToStyle(smrt.style);
							if (st != null) {
								l.getStyleBlackboard().put(SLDContent.ID, st);
							}
						}
					}
				}
			}

			ReferencedEnvelope bounds = null;
			if (mapItem.getMapBounds() == null){
				bounds = renderedMap.getBounds(new NullProgressMonitor());
			}else{
				bounds = mapItem.getMapBounds();
			}
			
			/* reorder layers so mapgraphic layers are at the top */
			List<Layer> maplayers = ((MapImpl)renderedMap).getLayersInternal();
			List<Layer> orderedLayers = new ArrayList<Layer>();
			int cnt = 0;
			for (Layer l : maplayers){
				if (l.getGeoResource().canResolve(MapGraphic.class)){
					orderedLayers.add(l);
				}else{
					orderedLayers.add(cnt,l);
					cnt++;
				}
			}
			((MapImpl) renderedMap).getContextModel().eSetDeliver(false);
			((MapImpl) renderedMap).getLayersInternal().clear();
			((MapImpl) renderedMap).getLayersInternal().addAll(0,orderedLayers);
			((MapImpl) renderedMap).getContextModel().eSetDeliver(true);
		    
			renderedMap.sendCommandSync(new ChangeCRSCommand(bounds.getCoordinateReferenceSystem()));
			renderedMap.sendCommandSync(new SetViewportBBoxCommand(bounds));
			try {
				DrawMapParameter drawMapParameter = new DrawMapParameter(g,
						new java.awt.Dimension(iwidth, iheight), renderedMap,
						dpi, new NullProgressMonitor());

				renderedMap = ApplicationGIS.drawMap(drawMapParameter);
			} finally {
				g.dispose();
			}

			NullProgressMonitor monitor = new NullProgressMonitor();
			for (GeoSmart layer: layers) {
				if (layer.georesource != null){
					layer.georesource.dispose(monitor);
				}
				layer.service.dispose(monitor);
				CatalogPlugin.getDefault().getLocalCatalog().remove(layer.service);
			}

			ImageIO.setUseCache(false);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
			ImageIO.write(image, "png", ios); //$NON-NLS-1$
			ios.flush();
			ios.close();
			ByteArrayInputStream bis = new ByteArrayInputStream(
					baos.toByteArray());
			return bis;
		} catch (Exception ex) {
			SmartMapItemPlugIn.displayLog(Messages.SmartMapPresentationImpl_ErrorRenderingMap + ex.getMessage(), ex);
			return null;
		}
	}
	
	
	//structure for aggregating smart
	class GeoSmart{
		String name;
		Query dbQuery;
		IGeoResource georesource;
		Layer layer;
		IService service;
		String style;
		
	}
}