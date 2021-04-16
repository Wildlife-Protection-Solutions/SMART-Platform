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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.QuerySourceProvider;
import org.wcs.smart.query.ui.model.IQueryDefinitionPanel;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;

/**
 * Manages the set of definition panels associated with a query type.
 * 
 * @author Emily
 *
 */
public class QueryDefPanel {

	private Composite main;
	private IQueryType queryType;
	private List<IQueryDefinitionPanel> dropPanels;
	
	private String currentPanel;
	private QueryDefView parentView;
	
	private CTabFolder tabFolder ;
	
	/**
	 * New definition panel
	 * @param queryType query type
	 * @param parentView parent view
	 */
	public QueryDefPanel(IQueryType queryType, QueryDefView parentView){
		this.parentView = parentView;
		this.queryType = queryType;
		dropPanels = new ArrayList<IQueryDefinitionPanel>();
	}
	
	/**
	 * Gets all definition panels.
	 * @return
	 */
	public List<IQueryDefinitionPanel> getDefinitionPanels(){
		return this.dropPanels;
	}
	
	/**
	 * Disposes of all panels and composite
	 */
	public void dispose(){
		for (IQueryDefinitionPanel p : dropPanels){
			p.dispose();
		}
		if(main != null){
			main.dispose();
		}
	}
	
	/**
	 * Clears all panel
	 */
	public void clear(){
		for (IQueryDefinitionPanel p : dropPanels){
			p.clear();
		}
	}
	
	/**
	 * Adds the drop item to the appropriate panel
	 * @param dropItem drop item to add
	 * @param itemPanelId the item panel that created the drop item
	 */
	public void addItem(DropItem dropItem, String itemPanelId){
		IQueryDefinitionPanel panel = null;
		if (dropPanels.size() == 1){
			panel = dropPanels.get(0);
		}else if (dropPanels.size() > 1){
			panel = (IQueryDefinitionPanel) tabFolder.getSelection().getData();
		}
		if (panel != null){
			String itemId = QueryTypeManager.INSTANCE.getQueryItemPanel(queryType, panel.getId());
			if (itemId != null && itemId.equals(itemPanelId)){
				panel.addItem(dropItem);
			}
		}
		
		parentView.validate();
	}
	
	/**
	 * Init the items in each definition panel
	 * @param q
	 */
	public void initItems(QueryProxy q) throws Exception{
		for (IQueryDefinitionPanel p : dropPanels){
			p.initItems(q);
		}
	}
	
	/**
	 * Save the items in each definition panel
	 * @param q
	 */
	public void saveItems(QueryProxy q){
		for (IQueryDefinitionPanel p : dropPanels){
			p.saveItems(q);
		}
	}
	/**
	 * 
	 * @param parent
	 * @return the composite that contains all the definition panels
	 */
	public Composite getComposite(Composite parent){
		if (main == null){
			main = createComposite(parent);
		}
		return main;
	}
	
	private Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = gl.marginWidth = 0;
		main.setLayout(gl);
		String[] panelIds = QueryTypeManager.INSTANCE.getQueryDefinitionPanelIds(queryType);
		if (panelIds == null || panelIds.length == 0) {
			main.setLayout(new GridLayout(1, false));
			Label l = new Label(parent, SWT.NONE);
			l.setText(MessageFormat.format(
					Messages.QueryDefPanel_QueryTypeNotSupported,
					new Object[] { queryType.getGuiName() }));
		} else if (panelIds.length == 1) {
			IQueryDefinitionPanel pnl = parentView.getPart().getContext().get(DefinitionPanelManager.class).createDefinitionPanel(panelIds[0]);

			Composite c = pnl.createComposite(main);
			dropPanels.add(pnl);
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			setQueryDefinitionPanel(pnl.getId());
			currentPanel = pnl.getId();

			try{
				pnl.initItems(parentView.getQueryProxy());
			}catch (Exception ex){
				QueryPlugIn.displayLog(ex.getMessage(), ex);
			}
		} else {
			tabFolder = new CTabFolder(main, SWT.NONE);
			tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			for (int i = 0; i < panelIds.length; i++) {
				CTabItem item = new CTabItem(tabFolder, SWT.NONE);

				IQueryDefinitionPanel pnl = parentView.getPart().getContext().get(DefinitionPanelManager.class).createDefinitionPanel(panelIds[i]);
				if (pnl != null) {
					item.setText(pnl.getGuiName());
					Composite comp = pnl.createComposite(tabFolder);
					item.setControl(comp);
					item.setData(pnl);

					try{
						pnl.initItems(parentView.getQueryProxy());
					}catch (Exception ex){
						QueryPlugIn.displayLog(ex.getMessage(), ex);
					}
					dropPanels.add(pnl);
				}

			}
			tabFolder.pack();
			tabFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					CTabItem sel = tabFolder.getSelection();
					if (sel != null) {
						setQueryDefinitionPanel(((IQueryDefinitionPanel)sel.getData()).getId());
					}
				}
			});
			tabFolder.setSelection(0);
			setQueryDefinitionPanel(((IQueryDefinitionPanel) tabFolder.getItem(0).getData()).getId());

		}
		parent.layout(true);
		return main;
	}
	/**
	 * Called when the panel is made visible
	 */
	public void madeVisible(){
		setQueryDefinitionPanel(currentPanel);
	}
	
	/**
	 * Selects the given definition panel it
	 * @param panelId
	 */
	public void setQueryDefinitionPanel(String panelId){
		currentPanel = panelId;
		
		ISourceProviderService service = (ISourceProviderService)parentView.getPart().getContext().get(ISourceProviderService.class);
		final QuerySourceProvider provider = (QuerySourceProvider) service.getSourceProvider(QuerySourceProvider.DEFINITION_PANEL_ID);
		provider.setQueryDefinitionPanelId(panelId, queryType);
	}
	
	/**
	 * Finds a given panel id
	 * @param panelId
	 * @return
	 */
	public IQueryDefinitionPanel findQueryDefinitionPanel(String panelId){
		for (IQueryDefinitionPanel pnl : dropPanels){
			if (pnl.getId().equals(panelId)){
				return pnl;
			}
		}
		return null;
	}
}
