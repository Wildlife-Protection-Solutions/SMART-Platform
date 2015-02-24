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
package org.wcs.smart.query.ui.definition;

import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.ui.itempanel.IQueryItemPanel;
import org.wcs.smart.query.ui.model.IDefinitionPanel;

/**
 * Query definition panel manager.  Loads and creates
 * query definition panels and associated query item panels.
 * <p>
 * Each query type is associated with a set of definition panels
 * which combine to define the entire query.  For each query type
 * each definition panel is associated with one item panel which
 * provides the drop items which can be added to the definition panel.</p>
 * 
 * 
 * @author Emily
 *
 */
public class DefinitionPanelManager {

	private HashMap<String, IQueryItemPanel> filterPanels = new HashMap<String, IQueryItemPanel>();
	private IEclipseContext localContext;

	private DefinitionPanelManager(){
		localContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		localContext.set(DefinitionPanelManager.class, this);
	}
	
	/**
	 * Creates the definition panel manager for the current
	 * eclipse context; should only be called once
	 * @return
	 */
	public static void createInstance(){
		new DefinitionPanelManager();
	}

	/**
	 * Determines if the given panel id is valid for the current
	 * system configuration 
	 * 
	 * @param panelId the definition panel
	 * @return <code>true</code> if panel applicable, <code>false</code> otherwise
	 */
	public static boolean isValid(String panelId){
		String isValidAttribute = "isSingleCa"; //$NON-NLS-1$
		if (SmartDB.isMultipleAnalysis()){
			isValidAttribute ="isMultiCa"; //$NON-NLS-1$
		}
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IDefinitionPanel.PANEL_EXTENSION_ID);
		for (IConfigurationElement e : config) {
			String id = e.getAttribute("id"); //$NON-NLS-1$
			if (id.equals(panelId)){
				return Boolean.valueOf(e.getAttribute(isValidAttribute));
			}
		}
		return false;
			 			
	}
	
	/**
	 * Creates a new definition panel with the given panel id.
	 * 
	 * @param panelId
	 * @return
	 */
	public IDefinitionPanel createDefinitionPanel(String panelId){
		String isValidAttribute = "isSingleCa"; //$NON-NLS-1$
		if (SmartDB.isMultipleAnalysis()){
			isValidAttribute ="isMultiCa"; //$NON-NLS-1$
		}
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IDefinitionPanel.PANEL_EXTENSION_ID);
		for (IConfigurationElement e : config) {
			String id = e.getAttribute("id"); //$NON-NLS-1$
			Boolean isValid = Boolean.valueOf(e.getAttribute(isValidAttribute));
			 			            
			if (isValid && id.equals(panelId)){
				try {
					IDefinitionPanel pnl = (IDefinitionPanel)e.createExecutableExtension("panel"); //$NON-NLS-1$
					ContextInjectionFactory.inject(pnl, localContext);
					return pnl;
				} catch (CoreException ex) {
					QueryPlugIn.log(MessageFormat.format(Messages.DefinitionPanelManager_QueryEditingPanelError, new Object[]{panelId}), ex);
				}
			}
		}
		return null;
	}

	/**
	 * Find the query item panel that is associated with the given definition panel id
	 * for the given query type.
	 * 
	 * @param definitionPanelId
	 * @param queryType
	 * @return
	 */
	public IQueryItemPanel getQueryItemPanel(String definitionPanelId, IQueryType queryType){
		String filterPanelId = QueryTypeManager.getInstance().getQueryItemPanel(queryType, definitionPanelId);
		if (filterPanelId == null){
			return null;
		}
		if (filterPanels.get(filterPanelId) == null){
			filterPanels.put(filterPanelId, createQueryItemPanel(filterPanelId));
		}
		return filterPanels.get(filterPanelId);
	}
	
	/*
	 * Creates a query item panel
	 */
	private IQueryItemPanel createQueryItemPanel(String panelId){
		
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IQueryItemPanel.EXTENSION_ID);
		for (IConfigurationElement e : config) {
			String id = e.getAttribute("id"); //$NON-NLS-1$
			if (id.equals(panelId)){
				try {
					IQueryItemPanel pnl = (IQueryItemPanel)e.createExecutableExtension("panel"); //$NON-NLS-1$
					ContextInjectionFactory.inject(pnl, localContext);
					return pnl;
				} catch (CoreException ex) {
					QueryPlugIn.log(MessageFormat.format(Messages.DefinitionPanelManager_QueryItemPanelError, new Object[]{panelId}), ex);
				}
			}
		}
		return null;
	}
}
