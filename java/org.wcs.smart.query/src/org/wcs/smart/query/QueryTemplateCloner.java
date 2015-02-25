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
package org.wcs.smart.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.udig.style.SmartLayerStyle;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Template cloner that copies query data 
 * from the template to the new conservation area.
 * <p>Data copied includes shared query folders.  This does not
 * clone user folders OR any queries. Plugins that
 * implement Query type must create
 * there own cloner for cloning the query types
 * they provide.</p>
 * 
 * 
 * @author Emily
 *
 */
public class QueryTemplateCloner implements
		IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.QueryTemplateCloner_ProgressQuery, 1);
		try{
			//	need to clone: shared query folders
			monitor.subTask(Messages.QueryTemplateCloner_ProgressCopyFolders);
			cloneFolders(engine);		
		}finally{
			monitor.done();
		}
	}
	
	
	private void cloneFolders(ConservationAreaClonerEngine engine){
		@SuppressWarnings("unchecked")
		List<QueryFolder> queryFolder = engine.getSession().createCriteria(QueryFolder.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.add(Restrictions.isNull("employee")) //$NON-NLS-1$
				.add(Restrictions.isNull("parentFolder")).list(); //$NON-NLS-1$
		for (QueryFolder q : queryFolder){
			processQueryFolder(q, null, engine);
		}
		engine.getSession().flush();
	}

	private QueryFolder processQueryFolder(QueryFolder templateFolder, QueryFolder newParent, ConservationAreaClonerEngine engine){
		QueryFolder clone = new QueryFolder();
		engine.copyLabels(templateFolder, clone);
		clone.setConservationArea(engine.getNewCa());
		clone.setEmployee(null);
		clone.setParentFolder(newParent);
		clone.setRootFolder(false);
		engine.getSession().save(clone);
		engine.addConservationItemMapping(templateFolder, clone);
			
		for (QueryFolder kid : templateFolder.getChildren()){
			QueryFolder clonedKid = processQueryFolder(kid, clone, engine);
			if (clone.getChildren() == null){
				clone.setChildren(new ArrayList<QueryFolder>());
			}
			clone.getChildren().add(clonedKid);
		}
		return clone;
	}
	
	public static String updateStyleString(ConservationAreaClonerEngine engine, String queryString){
		if (queryString == null || queryString.length() == 0) return queryString;
		
		try{
			Map<String, StyleBlackboard> blackboardMap = StyleManager.INSTANCE.fromStringMap(queryString);
			for (StyleBlackboard blackboard : blackboardMap.values()){
				if (blackboard.contains(SmartLayerStyle.STYLE_ID)){
					byte[] uuid = (byte[])blackboard.get(SmartLayerStyle.STYLE_ID);
					if (uuid != null && uuid.length == 16){
						UuidItem newStyleUuid = engine.getNewConservationItem(uuid);
						if (newStyleUuid == null){
							blackboard.remove(SmartLayerStyle.STYLE_ID);
						}else{
							blackboard.put(SmartLayerStyle.STYLE_ID, newStyleUuid.getUuid());
						}
					}else{
						blackboard.remove(SmartLayerStyle.STYLE_ID);
					}
				}
			}
			return StyleManager.INSTANCE.asString(blackboardMap);
		}catch (Exception ex){
			QueryPlugIn.log(ex.getMessage(), ex);
		}
		return queryString;
	}
}
