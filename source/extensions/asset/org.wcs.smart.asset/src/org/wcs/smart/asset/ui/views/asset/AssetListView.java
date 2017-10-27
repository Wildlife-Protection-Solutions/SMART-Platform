/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.asset;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.ui.AssetLabelProvider;
import org.wcs.smart.asset.ui.handler.NewAssetHandler;
import org.wcs.smart.asset.ui.handler.OpenAssetHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;


public class AssetListView {
	public static final String ID = "org.wcs.smart.asset.ui.view.assets"; //$NON-NLS-1$
	

	@Inject
	private IEclipseContext context;
	
	private Composite assetComposite;
	private Composite stationComposite;
	
	private TreeViewer lstAssets;
	private TreeViewer lstStations;
	private FormToolkit toolkit;
	
	public AssetListView() {
		super();
	}
	
	
	@PostConstruct
	public void createPartControl(final Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite header = toolkit.createComposite(main);
		header.setLayout(new GridLayout(2, false));
		
		Composite content = toolkit.createComposite(main, SWT.BORDER);
		content.setLayout(new StackLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Hyperlink hlAssets = toolkit.createHyperlink(header, "Assets", SWT.NONE);
		hlAssets.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				//TODO: remove me 
				if (assetComposite != null) { assetComposite.dispose(); assetComposite = null; }
				
				if (assetComposite == null) assetComposite = createAssetsPanel(content);
				((StackLayout)content.getLayout()).topControl = assetComposite;
				content.layout(true);
			}
		});
		Hyperlink hlStations = toolkit.createHyperlink(header, "Stations", SWT.NONE);
		hlStations.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				//TODO: remove me
				if (stationComposite != null) { stationComposite.dispose(); stationComposite = null; }
				
				if (stationComposite == null) stationComposite = createStationsPanel(content);
				((StackLayout)content.getLayout()).topControl = stationComposite;
				content.layout(true);
			}
		});
		
		//create asset panel
		if (assetComposite == null) assetComposite = createAssetsPanel(content);
		((StackLayout)content.getLayout()).topControl = assetComposite;
		content.layout(true);
	}

	private Composite createAssetsPanel(Composite parent) {
		Composite assetPanel = toolkit.createComposite(parent);
		assetPanel.setLayout(new GridLayout());
		toolkit.createLabel(assetPanel, "ASSEt PANEL");

		lstAssets = new TreeViewer(assetPanel, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		toolkit.adapt(lstAssets.getControl(), true, true);
		lstAssets.setLabelProvider(new AssetLabelProvider());
		lstAssets.setContentProvider(new AssetTypeContentProvider());
		lstAssets.setInput(new String[] {DialogConstants.LOADING_TEXT});
		lstAssets.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstAssets.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object x = ((IStructuredSelection)lstAssets.getSelection()).getFirstElement();
				if (x instanceof Asset) {
					(new OpenAssetHandler()).openAsset((Asset)x);
				}
				
			}
		});
		createAssetMenu(lstAssets.getControl());
		
		loadAssets();
		
		return assetPanel;
		
	}
	private Composite createStationsPanel(Composite parent) {
		Composite stationsPanel = toolkit.createComposite(parent);
		stationsPanel.setLayout(new GridLayout());
		toolkit.createLabel(stationsPanel, "STATION PANEL");
		return stationsPanel;
	}
	
	private void refreshView() {
		//TODO:
		
		
	}
	
	private void createAssetMenu(Control control) {
		Menu mnu = new Menu(control);
		control.setMenu(mnu);
		
		MenuItem addAsset = new MenuItem(mnu, SWT.CASCADE);
		addAsset.setText("New Asset...");
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem openAsset = new MenuItem(mnu, SWT.CASCADE);
		openAsset.setText("Open");
		openAsset.addListener(SWT.Selection, e->{
			IStructuredSelection selection = (IStructuredSelection)lstAssets.getSelection();
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				Object type = (Object) iterator.next();
				if (type instanceof Asset) (new OpenAssetHandler()).openAsset((Asset)type);
				
			}
		});
		
		Menu addAssetMenu = new Menu(addAsset);
		addAsset.setMenu(addAssetMenu);
		new MenuItem(addAssetMenu, SWT.SEPARATOR);
		
		MenuItem otherAssetType = new MenuItem(addAssetMenu, SWT.PUSH);
		otherAssetType.setText("Other...");
		otherAssetType.addListener(SWT.Selection, evt->(new NewAssetHandler()).execute());
		
		addAssetMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				String newTypes = ConfigurationScope.INSTANCE.getNode(NewAssetHandler.NEW_TYPE_OPTIONS_NODE).get(NewAssetHandler.NEW_TYPE_OPTIONS_KEY, null);
				if (newTypes == null || newTypes.isEmpty()) return;
				String[] bits = newTypes.split(NewAssetHandler.OPTION_SEP);
				List<AssetType> menuItems = new ArrayList<>();
				try(Session session = HibernateManager.openSession()){
					for (int i = 0; i < bits.length; i ++) {
						String uuid = bits[i];
						try {
							UUID assetTypeUuid = UuidUtils.stringToUuid(uuid);
							if (assetTypeUuid != null) {
								AssetType assetType = session.get(AssetType.class, assetTypeUuid);
								if (assetType != null) {
									assetType.getName();
									menuItems.add(assetType);
								}
							}
							
						}catch (Exception ex) {
							
						}
					}
				}
				for (MenuItem mi : addAssetMenu.getItems()) {
					if (mi.getData() != null) mi.dispose();
				}
				for (int i = menuItems.size() - 1; i >= 0; i --) {
					MenuItem addAssetType = new MenuItem(addAssetMenu, SWT.PUSH, 0);
					addAssetType.setText(menuItems.get(i).getName());
					addAssetType.setData(menuItems.get(i).getUuid());
					final UUID assetTypeUuid = menuItems.get(i).getUuid();
					addAssetType.addListener(SWT.Selection, evt->(new NewAssetHandler()).execute(assetTypeUuid));
				}
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
	}
	
	@Optional
	@Inject
	private void dbModified(@UIEventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		refreshView();
	}
	

	@Optional
	@Inject
	public void assetsModified(@UIEventTopic(AssetEvents.ASSET_ALL) Object payLoad) {
		loadAssets(250);
	}
	
	@Focus
	public void setFocus() {
	}

	@PreDestroy
	public void dispose() {
		toolkit.dispose();
	}
	
	public static class AssetListViewWrapper extends DIViewPart<AssetListView>{
		public AssetListViewWrapper() {
			super(AssetListView.class);
		}
	}

	private void loadAssets() {
		loadAssets(0);
	}
	private void loadAssets(int delay) {
		loadAssetType.cancel();
		loadAssetType.setSystem(true);
		loadAssetType.schedule(delay);
	}

	private Job loadAssetType = new Job("loading assets") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Asset> assets = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				assets.addAll(QueryFactory.buildQuery(session, Asset.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()} 
//TODO: add this back with filter						new Object[] {"isRetired", false})
						).list());	
				assets.forEach(a->{a.getAssetType().getUuid().equals(null); a.getAssetType().getName();});
			}
			if(monitor.isCanceled()) return Status.CANCEL_STATUS;
			
			Display.getDefault().syncExec(()->{
				if (lstAssets != null && !lstAssets.getControl().isDisposed()){
					lstAssets.setInput(assets);
					lstAssets.expandAll();
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	private class AssetTypeContentProvider implements ITreeContentProvider{

		private HashMap<AssetType, List<Asset>> mapping = new HashMap<>();
		
		@Override
		public Object[] getElements(Object inputElement) {
			List<AssetType> types = new ArrayList<>(mapping.keySet());
			types.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			return types.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Asset) return null;
			if (parentElement instanceof AssetType) return mapping.get((AssetType)parentElement).toArray();
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof AssetType) return null;
			if (element instanceof Asset) return ((Asset)element).getAssetType();
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof AssetType) return true;
			return false;
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			mapping = new HashMap<>();
			if (newInput instanceof List<?>) {
				((List<?>)newInput).forEach(item->{
					if (item instanceof Asset) {
						Asset e = (Asset)item;
					
						List<Asset> assets = mapping.get(e.getAssetType());
						if (assets == null) {
							assets = new ArrayList<>();
							mapping.put(e.getAssetType(), assets);
						}
						assets.add(e);
					}
				});
				for (List<Asset> items : mapping.values()) {
					items.sort((a,b)->Collator.getInstance().compare(a.getId(), b.getId()));
				}
			}
		}
	}

}