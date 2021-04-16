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
package org.wcs.smart.asset.query.ui.itempanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;
import org.wcs.smart.query.common.ui.itempanel.ItemTreeNodeContentProvider;
import org.wcs.smart.query.common.ui.itempanel.ItemTreeNodeTree;
import org.wcs.smart.query.common.ui.itempanel.OperatorsTreeNode;
import org.wcs.smart.query.ui.itempanel.AbstractQueryItemPanel;

/**
 * Panel displaying default filter items for filtering
 * query results.
 * 
 * @author Emily
 *
 */
public class AssetDeploymentItemPanel extends AbstractQueryItemPanel {
	
	public static final String ID = "org.wcs.smart.query.asset.deployment.itempanel"; //$NON-NLS-1$
	
	private Composite main = null;
	private TreeViewer filterTreeViewer;
	
	private @Inject IEventBroker eventBroker;

	
	public AssetDeploymentItemPanel() {
		
	}

	protected Composite createPanel(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		main.setLayout(gl);
		
		List<IItemTreeNode> nodes = new ArrayList<IItemTreeNode>();
		nodes.add(new AssetFilterTreeItem());
		nodes.add(new OperatorsTreeNode());
		
		ItemTreeNodeTree tree = new ItemTreeNodeTree(main, SWT.NONE, nodes);
		filterTreeViewer = tree.getTreeViewer();

		filterTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addItem();
			}
		});
		filterTreeViewer.setAutoExpandLevel(2);
		filterTreeViewer.setInput(LOADING_TEXT);

		createAddButton(filterTreeViewer, main);
		
		refreshPanel();
		
		EventHandler handler = event->refreshPanel();
		eventBroker.subscribe(AssetEvents.ASSET_ALL, handler);
		eventBroker.subscribe(AssetEvents.ASSETSTATION_ALL, handler);
		eventBroker.subscribe(AssetEvents.ASSETSTATIONLOCATION_ALL, handler);
		eventBroker.subscribe(AssetEvents.ASSETTYPE_ALL, handler);
		eventBroker.subscribe(AssetEvents.ASSETSTATIONLOCATION_CONFIG_MODIFIED, handler);
		
		
		return main;
	}
	

	@Override
	protected void addItem(){
		addQueryItem( ItemTreeNodeContentProvider.unwrapSelection((IStructuredSelection) filterTreeViewer.getSelection()));		
	}
	
	@Override
	public void refreshPanel(){
		if (filterTreeViewer != null){
			filterTreeViewer.setInput(LOADING_TEXT);
			filterTreeViewer.refresh();
			refreshJob.cancel();
			refreshJob.schedule();
		}
	}

	private Job refreshJob = new Job(Messages.QueryFilterPanel_RefreshTree_JobTitle) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final HashMap<Object, Object> input = new HashMap<Object, Object> ();

			List<Operator> ops = new ArrayList<Operator>();
			ops.add(Operator.NOT);
			ops.add(Operator.BRACKETS);
			
			input.put(OperatorsTreeNode.KEY, ops);
			
			if (!SmartDB.isMultipleAnalysis()){
				
				AssetFilterContentProvider.AssetFilterInput ainput = new AssetFilterContentProvider.AssetFilterInput();
				
				try(Session session = HibernateManager.openSession()){
					ainput.assets = QueryFactory.buildQuery(session, Asset.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
							new Object[] {"isRetired", false}).list(); //$NON-NLS-1$
					
					ainput.assets.forEach(a->a.getAssetType().getName());
					
					ainput.stations = QueryFactory.buildQuery(session, AssetStation.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
					ainput.stations.forEach(s->s.getLocations().forEach(l->l.getId()));
					
					ainput.assetAttributes = session.createQuery("FROM AssetAttribute a WHERE a in (SELECT id.attribute FROM AssetTypeAttribute WHERE id.attribute.conservationArea = :ca and id.attribute.type != :type)", AssetAttribute.class) //$NON-NLS-1$
							.setParameter("type",  AssetAttribute.AttributeType.POSITION) //$NON-NLS-1$
							.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
							.list();
					
					ainput.stationAttributes = session.createQuery("FROM AssetAttribute a WHERE a in (SELECT id.attribute FROM AssetStationAttribute WHERE id.attribute.conservationArea = :ca and id.attribute.type != :type)", AssetAttribute.class) //$NON-NLS-1$
							.setParameter("type",  AssetAttribute.AttributeType.POSITION) //$NON-NLS-1$
							.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
							.list();
					
					ainput.stationLocationAttributes = session.createQuery("FROM AssetAttribute a WHERE a in (SELECT id.attribute FROM AssetStationLocationAttribute WHERE id.attribute.conservationArea = :ca and id.attribute.type != :type)", AssetAttribute.class) //$NON-NLS-1$
							.setParameter("type",  AssetAttribute.AttributeType.POSITION) //$NON-NLS-1$
							.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
							.list();
					
					ainput.deploymentAttributes = session.createQuery("FROM AssetAttribute a WHERE a in (SELECT id.attribute FROM AssetTypeDeploymentAttribute WHERE id.attribute.conservationArea = :ca and id.attribute.type != :type)", AssetAttribute.class) //$NON-NLS-1$
							.setParameter("type",  AssetAttribute.AttributeType.POSITION) //$NON-NLS-1$
							.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
							.list();
				}
				
				input.put(AssetFilterTreeItem.KEY, ainput);
			}

			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					filterTreeViewer.setInput(input);
					filterTreeViewer.refresh();
				}
				
			});
			return Status.OK_STATUS;
		}

	};

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public Composite getComposite(Composite parent) {
		if (main == null){
			main = createPanel(parent);
		}
		return main;
	}
}
