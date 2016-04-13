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
package org.wcs.smart.report.birt.map.execute;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IDataQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.content.IForeignContent;
import org.eclipse.birt.report.engine.extension.IBaseResultSet;
import org.eclipse.birt.report.engine.extension.IExecutorContext;
import org.eclipse.birt.report.engine.extension.IReportItemExecutor;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.style.sld.SLDContent;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.query.common.GriddedQueryResultSetMetadata;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;
import org.wcs.smart.report.birt.map.item.LayerItem;
import org.wcs.smart.report.execute.SmartReportRunner;
import org.wcs.smart.udig.style.SmartLayerStyle;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Executor for SMART Birt Map.  Ensure the queries used in the map are
 * executed.
 * 
 * @author Emily
 *
 */
public class MapItemExecutor implements IReportItemExecutor{

	private ExtendedItemHandle modelHandle;
	private IExecutorContext context;
	private IReportItemExecutor parent;
	
	private List<File> cleanUp;
	
	@Override
	public void setModelObject(Object handle) {
		if ( handle instanceof ExtendedItemHandle ){
			this.modelHandle = (ExtendedItemHandle)handle;
		}
	}

	@Override
	public void setContext(IExecutorContext context) {
		this.context = context;
	}

	@Override
	public void setParent(IReportItemExecutor parent) {
		this.parent = parent;
		
	}

	@Override
	public IReportItemExecutor getParent() {
		return this.parent;
	}

	@Override
	public Object getModelObject() {
		return this.modelHandle;
	}

	@Override
	public IExecutorContext getContext() {
		return this.context;
	}

	@Override
	public IContent execute() throws BirtException {
		IForeignContent content = context.getReportContent().createForeignContent();
		content.setRawType(IForeignContent.EXTERNAL_TYPE);
		try {
			content.setRawValue(executeQuery());
		} catch (BirtException e) {
			throw e;
		}catch (Throwable ex){
			throw new BirtException("org.wcs.smart.report.birt.map", "Error executing SMART Query",  //$NON-NLS-1$ //$NON-NLS-2$
					(ResourceBundle)null, ex);
		}
		return content;
	}

	@Override
	public IBaseResultSet[] getQueryResults() {
		return null;
	}

	@Override
	public IContent getContent() {
		return null;
	}

	@Override
	public boolean hasNextChild() throws BirtException {
		return false;
	}

	@Override
	public IReportItemExecutor getNextChild() throws BirtException {
		return null;
	}

	@Override
	public void close() throws BirtException {
		//delete all temporary raster files
		for (File f : cleanUp){
			try{
				f.delete();
			}catch (Throwable t){
				Logger.getLogger(MapItemExecutor.class.getName()).log(Level.WARNING, t.getMessage(), t);
			}
		}
	}

	protected MapConfiguration executeQuery(  ) throws Exception{
		cleanUp = new ArrayList<File>();
		MapConfiguration configuration = new MapConfiguration();
		DesignElementHandle elementHandle = modelHandle.getContainer();

		IDataQueryDefinition[] query = context.getQueries( modelHandle );
		for (int i = 0; i < query.length; i ++){
			MapQueryDefinition def = (MapQueryDefinition) query[i];
			IBaseResultSet qresult = context.executeQuery( null, def.getWrapper(), elementHandle );
			configuration.addQuery(qresult,def.getInfo());

			String queryText = ((OdaDataSetHandle)def.getLayerItem().getHandle().getDataSet()).getQueryText();

			double minValue = 0;
			double maxValue = 0;
			//create raster results file
			if (def.getInfo().getLayerType() == LayerType.RASTER){
				SmartConnection connection = (SmartConnection) context.getAppContext().get(SmartConnection.class.getCanonicalName());
				IQuery tmp = connection.newQuery(((OdaDataSetHandle)def.getLayerItem().getHandle().getDataSet()).getExtensionID());
				tmp.prepare(queryText);
				IResultSetMetaData md = tmp.getMetaData();
				if (md instanceof GriddedQueryResultSetMetadata){
					//build the raster
					GriddedQueryResultSetMetadata gmd = (GriddedQueryResultSetMetadata)md;
					BirtRasterBuilder builder = new BirtRasterBuilder(gmd.getCoordinateReferenceSystem(), 
							gmd.getOrigin(), gmd.getCellSize(), gmd.getXColumn(), 
							gmd.getYColumn(), gmd.getValueColumn());
					builder.buildRaster((IQueryResults)qresult.getQueryResults());
					def.getInfo().setRasterFile(builder.getFileImage());
					cleanUp.addAll(builder.getAllFiles());
					
					minValue = builder.getMinValue();
					maxValue = builder.getMaxValue();
				}
			}
			
			//Configure layer styles
			if (def.getInfo().getMapStyle() == null){
				LayerItem layer = def.getLayerItem();
				if (layer.getLayerStyles() == null || layer.getLayerStyles().isEmpty()){
					//no style is provided; so lets try to load the default style
					if (layer.getHandle().getDataSet() instanceof OdaDataSetHandle){// &&
						try{
							Session session =  (Session)context.getAppContext().get(SmartReportRunner.SESSION_PARAM);
							
							String xid = ((OdaDataSetHandle)layer.getHandle().getDataSet()).getExtensionID();
							StyleBlackboard queryStyle = BirtStyleManager.INSTANCE.getStyle(xid, queryText, session);
							if (queryStyle != null){
								StyleBlackboard ss = processSmartStyle(queryStyle, session);
								if (ss != null){
									def.getInfo().setMapStyleBlackboard(ss);
								}else{
									def.getInfo().setMapStyleBlackboard(queryStyle);	
								}
							}else{
								//not query style provider; use a default
								if (layer.getLayerType() == LayerType.RASTER){
									def.getInfo().setMapStyleBlackboard(createDefaultRasterStyle(minValue, maxValue));
								}
							}
						}catch (Exception ex){
							Logger.getLogger(MapItemExecutor.class.getName()).log(Level.WARNING, "Error loading layer style for report map.", ex); //$NON-NLS-1$
						}
					}
				}				
			}
		}		
		return configuration;
	}

	private StyleBlackboard createDefaultRasterStyle(double minValue, double maxValue){
		String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //$NON-NLS-1$
				+ "<styleEntry type=\"SLDStyle\" version=\"1.0\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;" //$NON-NLS-1$
				+ "&lt;sld:StyledLayerDescriptor xmlns=&quot;http://www.opengis.net/sld&quot; xmlns:sld=&quot;http://www.opengis.net/sld&quot; xmlns:ogc=&quot;http://www.opengis.net/ogc&quot; xmlns:gml=&quot;http://www.opengis.net/gml&quot; version=&quot;1.0.0&quot;&gt;" //$NON-NLS-1$
				+ "&lt;sld:UserLayer&gt; " //$NON-NLS-1$
				+ "&lt;sld:LayerFeatureConstraints&gt; " //$NON-NLS-1$
				+ "&lt;sld:FeatureTypeConstraint/&gt; " //$NON-NLS-1$
				+ "&lt;/sld:LayerFeatureConstraints&gt; " //$NON-NLS-1$
				+ "&lt;sld:UserStyle&gt; " //$NON-NLS-1$
				+ "&lt;sld:Name&gt;000051&lt;/sld:Name&gt; " //$NON-NLS-1$
				+ "&lt;sld:Title/&gt; &lt;sld:FeatureTypeStyle&gt; " //$NON-NLS-1$
				+ "&lt;sld:Name&gt;name&lt;/sld:Name&gt; &lt;sld:Rule&gt; " //$NON-NLS-1$
				+ "&lt;sld:RasterSymbolizer&gt; &lt;sld:Geometry&gt; " //$NON-NLS-1$
				+ "&lt;ogc:PropertyName&gt;grid&lt;/ogc:PropertyName&gt; " //$NON-NLS-1$
				+ "&lt;/sld:Geometry&gt; &lt;sld:ColorMap&gt; " //$NON-NLS-1$
				+ "&lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;0.0&quot; quantity=&quot;-9999&quot; label=&quot;-no data-&quot;/&gt; "; //$NON-NLS-1$
		sld +="&lt;sld:ColorMapEntry color=&quot;#FFECEC&quot; opacity=&quot;0.8&quot; quantity=&quot;" + minValue; //$NON-NLS-1$
		sld += "&quot;/&gt; &lt;sld:ColorMapEntry color=&quot;#FF0000&quot; opacity=&quot;0.8&quot; quantity=&quot;" //$NON-NLS-1$
				+ maxValue
				+ "&quot;/&gt; &lt;/sld:ColorMap&gt; &lt;/sld:RasterSymbolizer&gt; &lt;/sld:Rule&gt; &lt;/sld:FeatureTypeStyle&gt; &lt;/sld:UserStyle&gt; &lt;/sld:UserLayer&gt;&lt;/sld:StyledLayerDescriptor&gt;</styleEntry>"; //$NON-NLS-1$
		try {
			XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
			SLDContent c = new SLDContent();
			Style style = (Style)c.load(memento);
			
			StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
			sb.put(SLDContent.ID, style);
			return sb;
		} catch (Exception ex) {
			Logger.getLogger(MapItemExecutor.class.getName()).log(Level.FINE, "Could not create raster default style.", ex); //$NON-NLS-1$
			return null;
		}
	}
	private StyleBlackboard processSmartStyle(StyleBlackboard blackboard, Session session) throws WorkbenchException, IOException{
		//check for SMART saved style
		final UUID styleUuid = (UUID) blackboard.get(SmartLayerStyle.STYLE_ID);
		if (styleUuid != null) {
			//load from database			
			SmartStyle ss = (SmartStyle) session.get(SmartStyle.class, styleUuid);
			if (ss != null){
				return StyleManager.INSTANCE.fromString(ss.getStyleString());
			}
		}
		return null;
	}
}
