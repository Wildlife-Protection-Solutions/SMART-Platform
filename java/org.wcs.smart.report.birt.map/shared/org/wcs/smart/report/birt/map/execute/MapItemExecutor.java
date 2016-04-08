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

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IDataQueryDefinition;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.content.IForeignContent;
import org.eclipse.birt.report.engine.extension.IBaseResultSet;
import org.eclipse.birt.report.engine.extension.IExecutorContext;
import org.eclipse.birt.report.engine.extension.IReportItemExecutor;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.ui.WorkbenchException;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.ca.SmartStyle;
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
		content.setRawValue(executeQuery());
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
		
	}

	protected MapConfiguration executeQuery(  ) throws EngineException, BirtException{
		
		MapConfiguration configuration = new MapConfiguration();
		DesignElementHandle elementHandle = modelHandle.getContainer();

		IDataQueryDefinition[] query = context.getQueries( modelHandle );
		for (int i = 0; i < query.length; i ++){
			MapQueryDefinition def = (MapQueryDefinition) query[i];
			IBaseResultSet qresult = context.executeQuery( null, def.getWrapper(), elementHandle );
			configuration.addQuery(qresult,def.getInfo());
			
			if (def.getInfo().getMapStyle() == null){
				//get map style
				LayerItem layer = def.getLayerItem();
				if (layer.getLayerStyles() == null || layer.getLayerStyles().isEmpty()){
					//no style is provided; so lets try to load the default style
					if (layer.getHandle().getDataSet() instanceof OdaDataSetHandle){// &&
						try{
							Session session =  (Session)context.getAppContext().get(SmartReportRunner.SESSION_PARAM);
							String queryText = ((OdaDataSetHandle)layer.getHandle().getDataSet()).getQueryText();
							String xid = ((OdaDataSetHandle)layer.getHandle().getDataSet()).getExtensionID();
							StyleBlackboard queryStyle = BirtStyleManager.INSTANCE.getStyle(xid, queryText, session);
							
							StyleBlackboard ss = processSmartStyle(queryStyle, session);
							if (ss != null){
								def.getInfo().setMapStyleBlackboard(ss);
							}else{
								def.getInfo().setMapStyleBlackboard(queryStyle);	
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
