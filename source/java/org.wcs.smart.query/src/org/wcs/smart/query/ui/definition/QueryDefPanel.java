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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.QuerySourceProvider;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;

/**
 * Manages the set of definition panels associated with a query type.
 * 
 * @author Emily
 *
 */
public class QueryDefPanel {

	private Composite main;
	private IQueryType queryType;
	private List<IDefinitionPanel> dropPanels;
	
	private String currentPanel;
	private QueryDefView parentView;
	
	/**
	 * New definition panel
	 * @param queryType query type
	 * @param parentView parent view
	 */
	public QueryDefPanel(IQueryType queryType, QueryDefView parentView){
		this.parentView = parentView;
		this.queryType = queryType;
		dropPanels = new ArrayList<IDefinitionPanel>();
	}
	
	/**
	 * Gets all definition panels.
	 * @return
	 */
	public List<IDefinitionPanel> getDefinitionPanels(){
		return this.dropPanels;
	}
	
	/**
	 * Disposes of all panels and composite
	 */
	public void dispose(){
		for (IDefinitionPanel p : dropPanels){
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
		for (IDefinitionPanel p : dropPanels){
			p.clear();
		}
	}
	
	/**
	 * Adds the drop item to the appropriate panel
	 * @param dropItem drop item to add
	 * @param itemPanelId the item panel that sourced the drop item
	 */
	public void addItem(DropItem dropItem, String itemPanelId){
		for (IDefinitionPanel p : dropPanels){
			String itemId = QueryTypeManager.getInstance().getQueryItemPanel(queryType, p.getId());
			if (itemId != null && itemId.equals(itemPanelId)){
				p.addItem(dropItem);
			}
		}
		parentView.validate();
	}
	
	/**
	 * Init the items in each definition panel
	 * @param q
	 */
	public void initItems(QueryProxy q){
		for (IDefinitionPanel p : dropPanels){
			p.initItems(q);
		}
	}
	
	/**
	 * Save the items in each definition panel
	 * @param q
	 */
	public void saveItems(QueryProxy q){
		for (IDefinitionPanel p : dropPanels){
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
		String[] panelIds = QueryTypeManager.getInstance()
				.getQueryDefinitionPanelIds(queryType);
		if (panelIds == null || panelIds.length == 0) {
			main.setLayout(new GridLayout(1, false));
			Label l = new Label(parent, SWT.NONE);
			l.setText(MessageFormat.format(
					Messages.QueryDefPanel_QueryTypeNotSupported,
					new Object[] { queryType.getGuiName() }));
		} else if (panelIds.length == 1) {
			IDefinitionPanel pnl = DefinitionPanelManager.getInstance()
					.createDefinitionPanel(panelIds[0]);

			Composite c = pnl.createComposite(main);
			dropPanels.add(pnl);
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			setQueryDefinitionPanel(pnl.getId());
			currentPanel = pnl.getId();

			pnl.initItems(parentView.getQueryProxy());
		} else {
			final TabFolder tf = new TabFolder(main, SWT.NONE);
			tf.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			for (int i = 0; i < panelIds.length; i++) {
				TabItem item = new TabItem(tf, SWT.NONE);

				IDefinitionPanel pnl = DefinitionPanelManager.getInstance()
						.createDefinitionPanel(panelIds[i]);
				if (pnl != null) {
					item.setText(pnl.getGuiName());
					Composite comp = pnl.createComposite(tf);
					item.setControl(comp);
					item.setData(pnl);

					pnl.initItems(parentView.getQueryProxy());
					dropPanels.add(pnl);
				}

			}
			tf.pack();
			tf.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					TabItem[] sel = tf.getSelection();
					if (sel.length > 0) {
						setQueryDefinitionPanel(((IDefinitionPanel) sel[0]
								.getData()).getId());
					}
				}
			});
			setQueryDefinitionPanel(((IDefinitionPanel) tf.getItem(0).getData())
					.getId());

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
		
		ISourceProviderService service = (ISourceProviderService)parentView.getSite().getService(ISourceProviderService.class);
		final QuerySourceProvider provider = (QuerySourceProvider) service.getSourceProvider(QuerySourceProvider.DEFINITION_PANEL_ID);
		provider.setQueryDefinitionPanelId(panelId, queryType);
	}
	
	/**
	 * Finds a given panel id
	 * @param panelId
	 * @return
	 */
	public IDefinitionPanel findQueryDefinitionPanel(String panelId){
		for (IDefinitionPanel pnl : dropPanels){
			if (pnl.getId().equals(panelId)){
				return pnl;
			}
		}
		return null;
	}
}
