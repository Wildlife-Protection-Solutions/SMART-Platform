/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetManager;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypoint.State;
import org.wcs.smart.asset.model.AssetWaypointAttachment;
import org.wcs.smart.asset.model.AssetWaypointMapping;
import org.wcs.smart.asset.ui.AttachmentTable;
import org.wcs.smart.asset.ui.DataDisplaySettings;
import org.wcs.smart.asset.ui.EditWaypointDialog;
import org.wcs.smart.asset.ui.SettingsShell;
import org.wcs.smart.asset.ui.views.data.DataImporterView;
import org.wcs.smart.asset.ui.views.data.StationAssetSelectionDialog;
import org.wcs.smart.asset.ui.views.data.StationAssetSelectionDialog.Type;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.ui.AttachmentPropertiesDialog;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Page for reviewing results
 * 
 * @author Emily
 *
 */
public abstract class AssetDataPanel {

	private static final String COLOR = "COLOR"; //$NON-NLS-1$

	public static final String ICON_SIZE_PREF = DataImporterView.ID + "org.wcs.smart.asset.ui.data.AssetDataPanel.iconsize"; //$NON-NLS-1$

	private FormToolkit toolkit;
	private IEclipseContext context;
	
	private List<UUID> waypointsToDisplay;
	
	private int startIndex = 0;
	
	private ScrolledComposite scroll;
	private Composite details;
	
	private int lastWpSelection;
	private List<RowItem> rows;
	
	private DataDisplaySettings displaySettings;
	
	Color headerColor = null;
	Color selectionColor = null;
	Color mouseOverColor = null;
	Color validatedColor = null;
	
	private boolean isEdit = false;
	private boolean isEditable = false;
	
	private boolean hideOnValidate = true;
	private boolean canValidate = true;
	
	private Asset asset;
	private UUID scrollTo = null;
	
	public AssetDataPanel(FormToolkit toolkit, boolean isEditable, boolean editState, boolean hideOnValidation, boolean canValidate, IEclipseContext context) {
		this.toolkit = toolkit;
		this.isEdit = editState;
		this.isEditable = isEditable;
		this.context = context;
		this.hideOnValidate = hideOnValidation;
		this.canValidate = canValidate;
		displaySettings = DataDisplaySettings.getSettings();
	}
	
	/**
	 * If this data panel is specific to an asset, set that asset here.  Waypoints with attachments from
	 * multiple assets will display the asset name for attachments that are not directly associated
	 * with this asset.
	 * @param asset
	 */
	public void setAsset(Asset asset) {
		this.asset = asset;
	}
	
	public boolean isEdit() {
		return this.isEdit;
	}
	
	public void setEditable(boolean isEditable) {
		this.isEdit = isEditable;
		loadPage.schedule();
	}
	
	public Control createControl(Composite parent) {
		headerColor = new Color(parent.getDisplay(), 230, 230, 230);
		selectionColor = SmartUtils.getListSelectedColor(parent.getDisplay());
		mouseOverColor = SmartUtils.getListHighlightColor(parent.getDisplay());
		validatedColor = new Color(parent.getDisplay(), 255,255,225);
		
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		main.addListener(SWT.Dispose, e->{
			selectionColor.dispose();
			mouseOverColor.dispose();
			headerColor.dispose();
			validatedColor.dispose();
		});
		
		details = toolkit.createComposite(main, SWT.BORDER);
		details.setLayout(new GridLayout());
		details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)details.getLayout()).marginWidth = 0;
		((GridLayout)details.getLayout()).marginHeight = 0;
		
		if (waypointsToDisplay == null) {
			refresh();
		}else {
			loadPage.schedule();
		}
		return main;
	}
	
	void resizeScroll() {
		int width = scroll.getBounds().width;
		if (rows != null) rows.forEach(a->a.resize(width, displaySettings.getIconSize()));
		scroll.setMinSize(scroll.getChildren()[0].computeSize(scroll.getBounds().width - scroll.getVerticalBar().getSize().x, SWT.DEFAULT));
	}
	
	private void createHeaderMenu(Control control) {
		if (!isEdit) return;
		if (control.getMenu() != null && !control.getMenu().isDisposed()) return;
		Menu mnuHeader = new Menu(details);
		
		MenuItem mnuOk = null; 
		if (canValidate) {
			mnuOk = new MenuItem(mnuHeader, SWT.PUSH);
			mnuOk.setText(Messages.AssetDataPanel_ValidateSelectionMnu);
			mnuOk.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_VALIDATE));
			mnuOk.addListener(SWT.Selection, e->{
				List<AssetWaypointMapping> toClear = rows.stream().filter(i->i.isSelected).map(i->i.waypoint).collect(Collectors.toList());
				markValidated(toClear);
			});
			
			new MenuItem(mnuHeader, SWT.SEPARATOR);
	
			MenuItem mnuOkAll = new MenuItem(mnuHeader, SWT.PUSH);
			mnuOkAll.setText(Messages.AssetDataPanel_ValidateAllMnu);
			mnuOkAll.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_VALIDATE_ALL));
			mnuOkAll.addListener(SWT.Selection, e->{
				List<AssetWaypointMapping> toClear = rows.stream().map(i->i.waypoint).collect(Collectors.toList());
				markValidated(toClear);
				refresh();
			});
		
			new MenuItem(mnuHeader, SWT.SEPARATOR);
		}
		
		MenuItem mnuMerge = new MenuItem(mnuHeader, SWT.PUSH);
		mnuMerge.setText(Messages.AssetDataPanel_MergeMnu);
		mnuMerge.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_MERGE));
		mnuMerge.addListener(SWT.Selection, e->{
			List<AssetWaypointMapping> toMerge = rows.stream().filter(i->i.isSelected).map(i->i.waypoint).collect(Collectors.toList());
			if (mergeItems(toMerge)) refresh();
		});
		
		MenuItem mnuDelete = new MenuItem(mnuHeader, SWT.PUSH);
		mnuDelete.setText(Messages.AssetDataPanel_DeleteMnu);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->{
			List<AssetWaypointMapping> toClear = rows.stream().filter(i->i.isSelected).map(i->i.waypoint).collect(Collectors.toList());
			deleteWaypoints(toClear);
		});
		
		MenuItem fmnuOk = mnuOk;
		mnuHeader.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				long cntNotValidated = rows.stream().filter(i->i.isSelected && i.waypoint.hasDirty()).collect(Collectors.counting());
				long cntSelected = rows.stream().filter(i->i.isSelected).collect(Collectors.counting());
				
				if (fmnuOk != null) fmnuOk.setEnabled(cntNotValidated != 0);
				mnuMerge.setEnabled(cntSelected > 1 );
				mnuDelete.setEnabled(cntSelected != 0);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		control.setMenu(mnuHeader);
	}
	
	private void createWidgetPanel(List<AssetWaypointMapping> waypoints) {
		if (details == null || details.isDisposed()) return;
		
		for (Control c : details.getChildren()) c.dispose();
		rows = new ArrayList<>();
		
		if (waypointsToDisplay.size() == 0) {
			toolkit.createLabel(details, Messages.AssetDataPanel_NoDataLabel);
			details.layout();
			return;
		}
		
		createPageControl(details, true, true);
		
		scroll = new ScrolledComposite(details, SWT.V_SCROLL );
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(scroll);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		
		Composite scrollDetails = toolkit.createComposite(scroll, SWT.NONE);
		scrollDetails.setLayout(new GridLayout());
		((GridLayout)scrollDetails.getLayout()).marginWidth = 0;
		((GridLayout)scrollDetails.getLayout()).marginHeight = 0;
		
		scroll.setContent(scrollDetails);
		resizeScroll();
		scroll.addListener(SWT.Resize, e->{
			resizeScroll();
		});
		
		Control showControl = null;
		for (AssetWaypointMapping aw : waypoints) {
			RowItem item = new RowItem(aw);
			Control c = item.createControl(scrollDetails);
			if (scrollTo != null && scrollTo.equals(aw.getWaypoint().getUuid())) {
				showControl = c;
			}
			rows.add(item);
		}
		createPageControl(scrollDetails, false, false);
		details.layout();
		resizeScroll();
		
		if (showControl != null) {
			scroll.showControl(showControl);
		}
		scrollTo = null;
	}
	
	private AssetWaypointMapping newIncident(AssetWaypointMapping aw, List<WaypointAttachment> attachments) {
		if (!isEdit) return null;
		if (attachments.size() == 0) return null;
		
		if (!MessageDialog.openQuestion(details.getShell(), Messages.AssetDataPanel_NewIncidentTitle, MessageFormat.format(Messages.AssetDataPanel_NewIncidentMsg, attachments.size()))) return null;
	
		Waypoint dtWaypoint = new Waypoint();
		dtWaypoint.setDateTime(aw.getWaypoint().getDateTime());
		dtWaypoint.setComment(aw.getWaypoint().getComment());
		
		AssetWaypoint tmpwp = new AssetWaypoint();
		tmpwp.setIncidentLength(aw.getAssetLinks().iterator().next().getIncidentLength());
		AssetWaypointMapping temp = new AssetWaypointMapping(dtWaypoint, Collections.singletonList(tmpwp));
				
		EditWaypointDialog dialog = new EditWaypointDialog(details.getShell(), temp, false, true);
		if (dialog.open() != Window.OK) return null;
		
		
		AssetWaypointMapping clonedMapping = null;
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor(false))){
			session.beginTransaction();
			try {
				//clone waypoint
				Waypoint cloneWp = aw.getWaypoint().clone(session);
				cloneWp.setAttachments(new ArrayList<>());
				cloneWp.setDateTime(dtWaypoint.getDateTime());
				cloneWp.setComment(dtWaypoint.getComment());
				
				//clone attachments and associate deployments
				Map<AssetDeployment, AssetWaypoint> newDeployments = new HashMap<>(); 
				for (WaypointAttachment sp : attachments) {
					WaypointAttachment att = new WaypointAttachment();
					// copy file to temp location so it won't be deleted out from under us
					Path tmpLocation = Files.createTempFile("smart_" + System.nanoTime(), ""); //$NON-NLS-1$ //$NON-NLS-2$
					tmpLocation.toAbsolutePath().toFile().deleteOnExit();
					sp.computeFileLocation(session);
					Files.copy(sp.getAttachmentFile(), tmpLocation);
					att.setCopyFromLocation(tmpLocation);
					att.setFilename(sp.getFilename());
					att.setWaypoint(cloneWp);
					cloneWp.getAttachments().add(att);
					
					for (AssetWaypoint oldlink : aw.getAssetLinks()) {
						for (AssetWaypointAttachment attachlink : oldlink.getAttachments()) {
							if (attachlink.getWaypointAttachment().equals(sp)) {
								AssetWaypoint newlink = newDeployments.get(oldlink.getAssetDeployment());
								if (newlink == null) {
									newlink = new AssetWaypoint();
									newlink.setAssetDeployment(oldlink.getAssetDeployment());
									newlink.setState(State.DIRTY);
									newlink.setAttachments(new HashSet<>());
									newlink.setWaypoint(cloneWp);
									newlink.setIncidentLength(tmpwp.getIncidentLength());
									newDeployments.put(oldlink.getAssetDeployment(), newlink);
								}
								
								AssetWaypointAttachment newattachlink = new AssetWaypointAttachment();
								newattachlink.setWaypointAttachment(att);
								newattachlink.setAssetWaypoint(newlink);
								newlink.getAttachments().add(newattachlink);
								
								oldlink.getAttachments().remove(attachlink);
								break;
							}
						}
					}
					
				}
				aw.getAssetLinks().forEach(a->session.saveOrUpdate(a));
				aw.getWaypoint().getAttachments().removeAll(attachments);
				for(WaypointAttachment wa : attachments) session.delete(wa);
				
				session.save(cloneWp);
				if (newDeployments.isEmpty()) {
					throw new Exception(Messages.AssetDataPanel_NoAssetsMsg);
				}
				for (AssetWaypoint newWp : newDeployments.values()) session.save(newWp);
				
				List<AssetWaypoint> toDelete = new ArrayList<>();
				for (AssetWaypoint oldlink : aw.getAssetLinks()) {
					if (oldlink.getAttachments().isEmpty()) {
						session.delete(oldlink);
						toDelete.add(oldlink);
					}
				}
				aw.getAssetLinks().removeAll(toDelete);
				if (aw.getAssetLinks().isEmpty()) {
					throw new Exception(Messages.AssetDataPanel_NoAssetsMsg2);
				}

				clonedMapping = new AssetWaypointMapping(cloneWp, newDeployments.values());
				
				//validate asset deployments to ensure they include the waypoint date/time
				validateAndExtend(cloneWp, newDeployments.values(), session);
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				AssetPlugIn.displayLog(Messages.AssetDataPanel_SaveError + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return null;
			}
		}
		fireEvent();
		return clonedMapping; 
	}
	
	private void fireEvent() {
		context.get(IEventBroker.class).post(AssetEvents.ASSETDATA, null);
	}
	
	private void createPageControl(Composite parent, boolean includeSettings, boolean includeRefresh) {
		int pageSize = displaySettings.getPageSize().getPageSize();
		int from = startIndex;
		if (waypointsToDisplay.isEmpty()) from = -1;
		int to = Math.min(startIndex + pageSize,  waypointsToDisplay.size());
		
		int cols = 1;
		if (includeSettings || includeRefresh) cols ++;
		if (!(from <= 0 && to == waypointsToDisplay.size())) cols += 3;
				
		Composite part = toolkit.createComposite(parent);
		part.setLayout(new GridLayout(cols, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		toolkit.createLabel(part, MessageFormat.format(Messages.AssetDataPanel_DisplayingLabel, (from + 1), to, waypointsToDisplay.size()));
		if (!(from <= 0 && to == waypointsToDisplay.size())) {
			Hyperlink prev = toolkit.createHyperlink(part, "<", SWT.NONE); //$NON-NLS-1$
			if(from == 0) prev.setEnabled(false);
			prev.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					startIndex = startIndex - pageSize;
					if (startIndex < 0) startIndex = 0;
					loadPage.schedule();
				}
			});
			Hyperlink more = toolkit.createHyperlink(part, "...",  SWT.NONE); //$NON-NLS-1$
			more.addListener(SWT.MouseDown, e->{
				Shell shell = new Shell(more.getShell(), SWT.NO_TRIM | SWT.ON_TOP );
				shell.setLayout(new GridLayout());
				((GridLayout)shell.getLayout()).marginWidth = 0;
				((GridLayout)shell.getLayout()).marginHeight = 0;
				
				Composite c = new Composite(shell, SWT.BORDER);
				c.setLayout(new GridLayout());
				c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				c.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
				((GridLayout)c.getLayout()).marginHeight = 1;
				((GridLayout)c.getLayout()).marginWidth = 1;
				((GridLayout)c.getLayout()).verticalSpacing = 1;
				
				Label l = new Label(c, SWT.NONE);
				l.setText(Messages.AssetDataPanel_FirstLabel);
				l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
				l.addListener(SWT.MouseEnter, evt->l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
				l.addListener(SWT.MouseExit, evt->l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
				l.addListener(SWT.MouseUp, evt->{startIndex = 0; loadPage.schedule();shell.close();shell.dispose();});
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				Label l2 = new Label(c, SWT.NONE);
				l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
				l2.setText(Messages.AssetDataPanel_LastLabel);
				l2.addListener(SWT.MouseEnter, evt->l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
				l2.addListener(SWT.MouseExit, evt->l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
				l2.addListener(SWT.MouseUp, evt->{
					startIndex = (int)((waypointsToDisplay.size() / pageSize) * pageSize); 
					loadPage.schedule();
					shell.close();
					shell.dispose();
				});
				l2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				ScrolledComposite src = new ScrolledComposite(c,  SWT.V_SCROLL | SWT.READ_ONLY);
				src.setExpandHorizontal(true);
				src.setExpandVertical(true);
				src.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				src.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	
				Composite core = new Composite(src, SWT.NONE);
				core.setLayout(new GridLayout());
				core.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
				((GridLayout)core.getLayout()).marginWidth = 0;
				((GridLayout)core.getLayout()).marginHeight = 0;
				((GridLayout)core.getLayout()).verticalSpacing = 1;
				src.setContent(core);
				for (int i = 0; i < waypointsToDisplay.size(); i += pageSize) {
					final int ii = i;
					Label l3 = new Label(core, SWT.NONE);
					l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
					l3.setText(MessageFormat.format("{0}-{1}", (i+1), Math.min(i+pageSize, waypointsToDisplay.size()))); //$NON-NLS-1$
					l3.addListener(SWT.MouseEnter, evt->l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
					l3.addListener(SWT.MouseExit, evt->l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
					l3.addListener(SWT.MouseUp, evt->{startIndex = ii; loadPage.schedule();shell.close();shell.dispose();});
					l3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}
				src.setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				
				shell.pack();
				shell.setSize(150,  150);
				shell.layout(true);
				more.getParent().getParent().layout(true);
				Point p2 = more.toDisplay(more.getLocation());
				shell.setLocation(p2.x-150, p2.y + more.getSize().y);
				shell.open();
				shell.addListener(SWT.Deactivate, evt->{shell.dispose();});
				
			});
			
			Hyperlink next = toolkit.createHyperlink(part, ">", SWT.NONE); //$NON-NLS-1$
			if (to == waypointsToDisplay.size()) next.setEnabled(false);
			next.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					startIndex = startIndex + pageSize;
					if (startIndex > waypointsToDisplay.size()) startIndex = waypointsToDisplay.size();
					loadPage.schedule();
				}
			});
		}
		
		if (includeSettings || includeRefresh) {
			ToolBar tb = new ToolBar(part, SWT.FLAT);
			tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			toolkit.adapt(tb);
			
			if (isEditable) {
				//enable/disable editing button based on user permission
				ToolItem itemEdit = new ToolItem(tb, SWT.CHECK);
				itemEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
				itemEdit.setToolTipText(Messages.AssetDataPanel_editmodetooltip);
				itemEdit.setSelection(isEdit());
				itemEdit.addListener(SWT.Selection, e->setEditable(itemEdit.getSelection()));
				
			}
			if (includeRefresh) {	
				ToolItem itemRefresh = new ToolItem(tb, SWT.PUSH);
				itemRefresh.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
				itemRefresh.setToolTipText(Messages.AssetDataPanel_reloadtooltip);
				itemRefresh.addListener(SWT.Selection, e->refresh());		
			}
			if (includeSettings) {
				ToolItem itemSettings = new ToolItem(tb, SWT.PUSH);
				itemSettings.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_SETTINGS));
				itemSettings.setToolTipText(Messages.AssetDataPanel_configuretooltip);
				itemSettings.addListener(SWT.Selection, e->{
					SettingsShell settingDialog = new SettingsShell(tb.getDisplay());
					settingDialog.show(tb);
					settingDialog.getShell().addListener(SWT.Dispose, evt->{
						DataDisplaySettings newSettings = settingDialog.getSettings();
						boolean updateIcon = false;
						if (newSettings.getIconSize() != this.displaySettings.getIconSize()) {
							this.displaySettings.setIconSize(newSettings.getIconSize());
							updateIcon = true;
						}
						if (newSettings.getPageSize() != this.displaySettings.getPageSize()) {
							this.displaySettings.setPageSize(newSettings.getPageSize());
							refresh();
							updateIcon = false;
						}
						if (updateIcon) {
							for (RowItem i : rows) {
								i.tt.setThumbnailSize(this.displaySettings.getIconSize().getSize());
							}
							resizeScroll();
						}
						
					});
				});
			}
		}
	}
	
	private Job loadPage = new Job(Messages.AssetDataPanel_loadwpJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<AssetWaypointMapping> waypoints = new ArrayList<>();
			final int pageSize = displaySettings.getPageSize().getPageSize();
			try(Session session = HibernateManager.openSession()){
				for(int i = startIndex; i < Math.min(startIndex + pageSize,  waypointsToDisplay.size()); i ++) {
					
					Waypoint wp = session.get(Waypoint.class, waypointsToDisplay.get(i));
					if (wp == null) {
						//deleted - need to add deleted item
						waypoints.add(new AssetWaypointMapping(null, Collections.emptyList()));						
						continue;
					}
					if (wp.getLastModifiedBy() != null) SmartLabelProvider.getShortLabel(wp.getLastModifiedBy());
					List<AssetWaypoint> aws = session.createQuery("FROM AssetWaypoint WHERE id.waypoint.uuid = :uuid", AssetWaypoint.class) //$NON-NLS-1$
							.setParameter("uuid", waypointsToDisplay.get(i)) //$NON-NLS-1$
							.list();
							
					
					if (wp.getAttachments() != null) {
						wp.getAttachments().forEach(a->{try {
							a.computeFileLocation(session);
						} catch (Exception e) {
							e.printStackTrace();
						}});
					}
					if (wp.getAllObservations() != null) {
						wp.getAllObservations().forEach(e->{
							e.getCategory().getFullCategoryName();	
							if (e.getAttachments() != null) e.getAttachments().forEach(a->{try {
								a.computeFileLocation(session);
							} catch (Exception e1) {
								e1.printStackTrace();
							}});
							if (e.getAttributes() != null) {
								e.getAttributes().forEach(a->{
									a.getAttribute().getName();
									a.getAttributeValueAsString(Locale.getDefault());
								});
							}
						} );
					}
					aws.forEach(aw->{
						aw.getAssetDeployment().getAsset().getId();
						aw.getAssetDeployment().getAsset().getAssetType();
						aw.getAssetDeployment().getStationLocation().getId();
						aw.getAssetDeployment().getStationLocation().getStation().getId();
						aw.getAttachments().forEach(a->a.getWaypointAttachment());
					});
					waypoints.add(new AssetWaypointMapping(wp, aws));
				}
			}
			Display.getDefault().syncExec(()->{createWidgetPanel(waypoints);});
			return Status.OK_STATUS;
		}
	};
		
	public void refresh() {
		Display.getDefault().syncExec(()->{
			if (details == null || details.isDisposed()) return;
			for (Control c : details.getChildren()) c.dispose();
			toolkit.createLabel(details, DialogConstants.LOADING_TEXT);
			details.layout(true);
		});
		
		loadWaypoints();
	}
	/**
	 * Should schedule a job that loads all the waypoints 
	 * to display.  Should call set waypoints to configure the waypoints.
	 */
	public abstract void loadWaypoints();
	
	/**
	 * Sets the list of waypoints to display and updates the
	 * current page
	 * @param waypointUuids
	 */
	public void setWaypoints(List<UUID> waypointUuids) {
		this.waypointsToDisplay = waypointUuids;
		loadPage.schedule();
	}
	
	/**
	 * Scroll in the results table to the given waypoint
	 * uuid. 
	 * @param waypointUuid
	 * @return
	 */
	public boolean scrollToWaypoint (UUID waypointUuid) {
		if (waypointsToDisplay == null) return false;
		final int index = waypointsToDisplay.indexOf(waypointUuid);
		if (index < 0) return false;
		
		int pageSize = displaySettings.getPageSize().getPageSize();
		startIndex = index / pageSize;
		
		Job j = new Job("background zoom") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					loadPage.join();
				} catch (InterruptedException e) {
				}
				scrollTo = waypointUuid;
				loadPage.schedule();
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
		
		return true;
	}
	
	private void markValidated(Collection<AssetWaypointMapping> tovalidate) {
		if (!isEdit) return;
		try (Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (AssetWaypointMapping aw : tovalidate) {
					aw.getAssetLinks().forEach(assetWaypoint ->{
						AssetWaypoint aww = session.get(AssetWaypoint.class, assetWaypoint.getUuid());
						if (aww != null) {
							aww.setState(AssetWaypoint.State.OK);
							assetWaypoint.setState(AssetWaypoint.State.OK);
						}
					});
				}
				session.getTransaction().commit();
			}catch(Exception ex){
				AssetPlugIn.displayLog(Messages.AssetDataPanel_saveerror + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		for (RowItem i : rows) {
			if (tovalidate.contains(i.waypoint)) {
				if (hideOnValidate) i.hideAndDisable(Messages.AssetDataPanel_ValidatedLabel);
			}
		}
		resizeScroll();
	}
	
	private void editWaypoint(AssetWaypointMapping toedit) {
		//modify the waypoint datetime and/or ID
		if (!isEdit) return;
		if (toedit == null) return;
		EditWaypointDialog dialog = new EditWaypointDialog(details.getShell(), toedit);
		if (dialog.open() == EditWaypointDialog.OK) {
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try {
					validateAndExtend(toedit.getWaypoint(), toedit.getAssetLinks(), s);
					s.saveOrUpdate(toedit.getWaypoint());
					for (AssetWaypoint aw : toedit.getAssetLinks()) {
						s.saveOrUpdate(aw);
					}
					s.getTransaction().commit();
				}catch(Exception ex) {
					AssetPlugIn.displayLog(Messages.AssetDataPanel_WpSaveError + ex.getMessage(), ex);
					s.getTransaction().rollback();
					return;
				}
			}
			for (RowItem i : rows) {
				if (i.waypoint.equals(toedit)) {
					i.populateHeaderLabel();
					i.refreshComment();
				}
			}
			fireEvent();
			resizeScroll();
		}
	}
	
	private void deleteWaypoints(Collection<AssetWaypointMapping> todelete) {
		if (!isEdit) return;
		if (todelete.isEmpty()) return;
		if (todelete.size() == 1) {
			if (!MessageDialog.openQuestion(details.getShell(), Messages.AssetDataPanel_DeleteTitle, Messages.AssetDataPanel_DeleteMsg1)) return;
		}else {
			if (!MessageDialog.openQuestion(details.getShell(), Messages.AssetDataPanel_28, MessageFormat.format(Messages.AssetDataPanel_DeleteMsg2, todelete.size()))) return;
		}
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				for (AssetWaypointMapping aw : todelete) {
					//delete waypoint links
					aw.getAssetLinks().forEach(assetWaypoint->{
						UUID deploymentUuid = assetWaypoint.getAssetDeployment().getUuid();
						
						assetWaypoint = session.get(AssetWaypoint.class, assetWaypoint.getUuid());
						assetWaypoint.getAttachments().forEach(a->{
							session.delete(a);
						});
						assetWaypoint.getAttachments().clear();
						
						session.delete(assetWaypoint);
						assetWaypoint.getAssetDeployment().getAssetWaypoints().remove(assetWaypoint);
						assetWaypoint.setAssetDeployment(null);
						
						AssetDeployment d = (AssetDeployment)session.get(AssetDeployment.class, deploymentUuid);
						//delete deployment if there are no more images in it
						if (d.getAssetWaypoints().size() == 0) {
							session.delete(d);	
						}
						session.flush();
						
					});
					
					//delete waypoint
					session.delete(session.get(Waypoint.class, aw.getWaypoint().getUuid()));
					session.flush();
				}
				session.getTransaction().commit();
			}catch(Exception ex){
				AssetPlugIn.displayLog(Messages.AssetDataPanel_WpDeleteError2 +ex.getMessage(), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		
		for (RowItem i : rows) {
			if (todelete.contains(i.waypoint)) {
				i.hideAndDisable(Messages.AssetDataPanel_DeletedLabel);
			}
		}
		resizeScroll();
		fireEvent();
	}
	
	/**
	 * Extends all the deployments in the assetlinks collection
	 * to include the date/time provided by the waypoint.  Will throw
	 * an exception of a deployment cannot be extended
	 * 
	 * @param wp
	 * @param assetlinks
	 */
	private void validateAndExtend(Waypoint wp, Collection<AssetWaypoint> assetlinks, Session session) throws Exception{
		Long wpTime = wp.getDateTime().getTime();
		for (AssetWaypoint fromaw : assetlinks) {
			
			boolean check = false;
			Long deploymentStart = fromaw.getAssetDeployment().getStartDate().getTime();
			Long deploymentEnd = null;
			if (fromaw.getAssetDeployment().getEndDate() != null) {
				deploymentEnd = fromaw.getAssetDeployment().getEndDate().getTime();
			}
			
			//we need to extend the asset deployment 
			if (deploymentStart > wpTime) {
				deploymentStart = wpTime;
				fromaw.getAssetDeployment().setStartDate(new Date(deploymentStart));
				session.saveOrUpdate(fromaw.getAssetDeployment());
				check = true;
			}
			if (deploymentEnd != null && deploymentEnd < wpTime) {
				deploymentEnd = wpTime;
				fromaw.getAssetDeployment().setEndDate(new Date(deploymentEnd));
				session.saveOrUpdate(fromaw.getAssetDeployment());
				check = true;
			}
			if (check) {
				//get all deployments for the asset and make sure they don't override
				//if we cannot extend the asset deployment we need to throw an exception
				List<AssetDeployment> allDeployments = QueryFactory.buildQuery(session, AssetDeployment.class, new Object[] {"asset", fromaw.getAssetDeployment().getAsset()}).list(); //$NON-NLS-1$
				if (AssetManager.INSTANCE.overlaps(fromaw.getAssetDeployment(), allDeployments)) {
					throw new Exception(MessageFormat.format(Messages.AssetDataPanel_MergeError, fromaw.getAssetDeployment().getAsset().getId()));
				}
			}
		}
	}
	private boolean mergeItems(List<AssetWaypointMapping> tomerge) {
		if (!isEdit) return false;
		if (tomerge.isEmpty() || tomerge.size() < 2) return false;
		int inlength = 0;
		//To merge the station MUST be the same
		Set<AssetStation> stns = new HashSet<>();
		for (AssetWaypointMapping mapping : tomerge) {
			for (AssetWaypoint aw : mapping.getAssetLinks()) {
				stns.add(aw.getAssetDeployment().getStationLocation().getStation());
				inlength = aw.getIncidentLength();
			}	
		}
		if (stns.size() != 1) {
			MessageDialog.openError(details.getShell(), Messages.AssetDataPanel_MergeTitle, Messages.AssetDataPanel_MergeMsg);
			return false;
		}
		
			
		if (!MessageDialog.openQuestion(details.getShell(), Messages.AssetDataPanel_MergeTitle, MessageFormat.format(Messages.AssetDataPanel_MergeConfigrm, tomerge.size()))) return false;
		
		Waypoint dtWaypoint = new Waypoint();
		dtWaypoint.setDateTime(tomerge.get(0).getWaypoint().getDateTime());
		StringBuilder sb= new StringBuilder();
		tomerge.forEach(e->{
			if (e.getWaypoint().getComment() !=  null) {
				if (sb.length() > 0) sb.append("\n"); //$NON-NLS-1$
				sb.append(e.getWaypoint().getComment());
			}
		});
		dtWaypoint.setComment(sb.toString());
		
		AssetWaypoint tmpwp = new AssetWaypoint();
		tmpwp.setIncidentLength(inlength);
		AssetWaypointMapping tmpmap = new AssetWaypointMapping(dtWaypoint,Collections.singletonList(tmpwp));
		EditWaypointDialog dialog = new EditWaypointDialog(details.getShell(), tmpmap, false, true);
		if (dialog.open() != Window.OK) return false;
		
		AssetWaypointMapping core = tomerge.get(0);
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor(false))){
			session.beginTransaction();
			try {
				List<AssetWaypointAttachment> toSave = new ArrayList<>();
				core.getWaypoint().setDateTime(dtWaypoint.getDateTime());
				core.getWaypoint().setComment(dtWaypoint.getComment());
				session.saveOrUpdate(core.getWaypoint());
				
				//validate and extend all deployments associated with the core
				validateAndExtend(core.getWaypoint(), core.getAssetLinks(), session);

				for (int i = 1; i < tomerge.size(); i ++) {
				
					AssetWaypointMapping from = tomerge.get(i);

					//update asset deployment times so we include the waypoint date/time
					validateAndExtend(core.getWaypoint(), from.getAssetLinks(), session);

					//copy over attachments
					for (AssetWaypoint fromaw : from.getAssetLinks()) {
						
						AssetWaypoint toAssetWaypoint = null;
						for (AssetWaypoint aw : core.getAssetLinks()) {
							if (aw.getAssetDeployment().equals(fromaw.getAssetDeployment())) {
								toAssetWaypoint = aw;
							}
						}
						if (toAssetWaypoint == null) {
							toAssetWaypoint = new AssetWaypoint();
							toAssetWaypoint.setAssetDeployment(fromaw.getAssetDeployment());
							toAssetWaypoint.setAttachments(new HashSet<>());
							toAssetWaypoint.setState(State.DIRTY);
							toAssetWaypoint.setWaypoint(core.getWaypoint());
							core.getAssetLinks().add(toAssetWaypoint);
						}
						session.saveOrUpdate(toAssetWaypoint);

						
						if (fromaw.getAttachments() != null) {
							for (AssetWaypointAttachment fromattachment : fromaw.getAttachments()) {
								
								//create a new waypoint
								WaypointAttachment source = fromattachment.getWaypointAttachment();
								
								WaypointAttachment att = new WaypointAttachment();
								// copy file to temp location so it won't be deleted out from under us
								Path tmpLocation = Files.createTempFile("smart_" + System.nanoTime(), ""); //$NON-NLS-1$ //$NON-NLS-2$
								tmpLocation.toAbsolutePath().toFile().deleteOnExit();
								source.computeFileLocation(session);
								Files.copy(source.getAttachmentFile(), tmpLocation);
								att.setCopyFromLocation(tmpLocation);
								att.setFilename(source.getFilename());
								att.setWaypoint(core.getWaypoint());
								core.getWaypoint().getAttachments().add(att);
								
								AssetWaypointAttachment newAttachment = new AssetWaypointAttachment();
								newAttachment.setAssetWaypoint(toAssetWaypoint);
								newAttachment.setWaypointAttachment(att);
								toAssetWaypoint.getAttachments().add(newAttachment);
								
							}
						}
					}
					//copy over observations
					if (from.getWaypoint().getObservationGroups() != null) {
						
						if (core.getWaypoint().getObservationGroups() == null) core.getWaypoint().setObservationGroups(new ArrayList<>());
						
						
						
						for (WaypointObservationGroup fromgroup : from.getWaypoint().getObservationGroups()) {
							
							WaypointObservationGroup g = null;
							if (from.getWaypoint().getObservationGroups().size() == 1 && core.getWaypoint().getObservationGroups().size() < 2) {
								//copy everything into existing group
								if (core.getWaypoint().getObservationGroups().isEmpty()) {
									g = new WaypointObservationGroup();
									g.setObservations(new ArrayList<>());
									g.setWaypoint(core.getWaypoint());
									core.getWaypoint().getObservationGroups().add(g);
								}else {
									g = core.getWaypoint().getObservationGroups().get(0);
								}
								
							}else {
								//make a new group
								g = new WaypointObservationGroup();
								g.setWaypoint(core.getWaypoint());
								g.setObservations( new ArrayList<>() );
								core.getWaypoint().getObservationGroups().add(g);
							}
							for (WaypointObservation wo : fromgroup.getObservations()) {
								if (!AssetUtils.containsObservation(wo, g.getObservations())) {
									WaypointObservation woClone = wo.clone(session);
									g.getObservations().add(woClone);
									woClone.setObservationGroup(g);
								}
							}
						}
						
						List<WaypointObservationGroup> empty = new ArrayList<>();
						for (WaypointObservationGroup g : core.getWaypoint().getObservationGroups()) {
							if (g.getObservations().isEmpty()) empty.add(g);
						}
						core.getWaypoint().getObservationGroups().removeAll(empty);
						
					}

					for (AssetWaypoint aw : from.getAssetLinks()) {
						aw.getAttachments().forEach(a->session.delete(a));
						aw.getAttachments().clear();
						session.delete(aw);
					}
					session.delete(from.getWaypoint());
					
				}
				for (AssetWaypoint aw : core.getAssetLinks()) {
					aw.setIncidentLength(tmpwp.getIncidentLength());
				}
				session.saveOrUpdate(core.getWaypoint());
				toSave.forEach(a->session.save(a));
				session.getTransaction().commit();
			}catch(Exception ex){
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetDataPanel_MergeSaveError +ex.getMessage(), ex);
			}
		}
		return true;
	}
	
	
	private void processMouseClickEvent(RowItem item, Event event) {
		int index = rows.indexOf(item);
		if ((event.stateMask & SWT.CTRL) != 0){
			item.setSelected(!item.isSelected);
		}else if ((event.stateMask & SWT.SHIFT) != 0){
			boolean newSelection = !item.isSelected;
			//clearSelection();
			
			int from = lastWpSelection;
			int to = index;
			if (index < lastWpSelection){
				from = index;
				to = lastWpSelection;
			}
			
			for (int i = from; i <= to; i ++){
				if (i == index){
					rows.get(i).setSelected(true);
				}else{
					rows.get(i).setSelected(newSelection);		
				}
			}
			
		}else{					
			boolean newSelection = item.isSelected;
			if (event.button == 1) {
				newSelection = !newSelection;
			}else {
				newSelection = true;
			}
			if (event.button != 3 || !item.isSelected) {
				rows.forEach(e->e.setSelected(false));
				item.setSelected(newSelection);
			}
		}	
		List<Waypoint> selection = rows.stream().filter(e->e.isSelected).map(e->{ return e.waypoint.getWaypoint(); }).collect(Collectors.toList());
		context.get(ESelectionService.class).setSelection(new StructuredSelection(selection));
		lastWpSelection = index;
		
	}
	
	static void colorControl(Control control, Color color) {
		forEachChild(control, e->{
			if (e.isDisposed()) return false;
			Boolean c = (Boolean) e.getData(COLOR);
			if (c == null || c) {
				e.setBackground(color);
				e.redraw();
				return true;
			}
			return false;
		});
	}
	
	static void forEachChild( Control control, Predicate<Control> consumer) {
		if (!consumer.test(control)) return;
		if (!(control instanceof Composite)) return;
		List<Control> kids = new ArrayList<>();
		for (Control c : ((Composite)control).getChildren()) kids.add(c);
		while(!kids.isEmpty()) {
			Control c = kids.remove(0);
			if (!consumer.test(c)) continue;
			if (c instanceof Composite) {
				for (Control c3 : ((Composite)c).getChildren()) {
					kids.add(c3);
				}
			}
		}
	}
	
	private boolean removeAttachments(AssetWaypointMapping waypoint, List<ISmartAttachment> toRemove) {
		if (!isEdit) return false;
		if (toRemove.isEmpty()) return false;
		if (!MessageDialog.openQuestion(details.getShell(), Messages.AssetDataPanel_RemoveAttachmentsTitle, MessageFormat.format(Messages.AssetDataPanel_RemoveAttachmentsMessage, toRemove.size()))) return false;
		
		boolean fireEvents = false;
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				List<AssetWaypoint> assetLinksToRemove = new ArrayList<AssetWaypoint>();
				for (ISmartAttachment attachment : toRemove) {
					//remove asset waypoint attachment link
					
					for (AssetWaypoint aw : waypoint.getAssetLinks()) {
						List<AssetWaypointAttachment> attachments = new ArrayList<>();
						for (AssetWaypointAttachment a : aw.getAttachments()){
							if (a.getWaypointAttachment().equals(attachment)) {
								attachments.add(a);
							}
						}
						attachments.forEach(a->{
							session.delete(a);
							aw.getAttachments().remove(a);
						});
						
						//if no more attachment links we should remove the asset waypoint
						if (aw.getAttachments().isEmpty()) {
							assetLinksToRemove.add(aw);
						}

					}
					waypoint.getWaypoint().getAttachments().remove(attachment);
					session.delete(attachment);
				}
				session.flush();
				
				//if associated deployment has no more waypoints we should remove that as well
				//if no more attachment links we should remove the asset waypoint
				for (AssetWaypoint a : assetLinksToRemove) {
					fireEvents = true;
					session.delete(a);
					session.flush();
					AssetDeployment d = session.get(AssetDeployment.class, a.getAssetDeployment().getUuid());
					d.getAssetWaypoints().remove(a);
					a.setAssetDeployment(null);
					if (d.getAssetWaypoints().size() == 0) {
						session.delete(d);
						session.flush();
					}
				}
				waypoint.getAssetLinks().removeAll(assetLinksToRemove);
				if (waypoint.getAssetLinks().isEmpty()) throw new IllegalStateException(Messages.AssetDataPanel_RemoveAttachmentsError1);
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetDataPanel_RemoveAttachmentsError2 + ex.getMessage(), ex);
			}
			if (fireEvents) fireEvent();
		}
		return true;
	}
	
	private List<ISmartAttachment> importAttachment(AssetWaypointMapping waypoint) {
		if (!isEdit) return null;
		FileDialog fd = new FileDialog(details.getShell(), SWT.OPEN | SWT.MULTI);
		if (fd.open() == null) return null;
		
		StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(details.getShell(), Type.ASSET);
		if (dialog.open() != Window.OK) return null;
		
		//get asest to link to
		Asset asset = dialog.getSelectedAsset();
		
		boolean fireEvent = false;
		//find - link to deployment might be new one if selected asset not already associated with incident
		AssetWaypoint assetWaypointLink = null;
		for (AssetWaypoint t : waypoint.getAssetLinks()) {
			//link to existing deployment for asset; don't care about location
			if (t.getAssetDeployment().getAsset().equals(asset)) {
				assetWaypointLink = t;
				break;
			}
		}
		if (assetWaypointLink == null) {
			fireEvent = true;
			// find deployment at location and create new asset link
			Set<AssetStation> options = waypoint.getAssetLinks().stream().map(aw->aw.getAssetDeployment().getStationLocation().getStation()).collect(Collectors.toSet());
			if (options.size() != 1) {
				throw new IllegalStateException(Messages.AssetDataPanel_TooManyStations);
			}
			AssetStation station = options.iterator().next();
			try(Session session = HibernateManager.openSession()){
				station = session.get(AssetStation.class, station.getUuid());
				station.getLocations().forEach(l->l.getId());
			}
			AssetStationLocation location = null;
			if (station.getLocations().size() == 1) {
				location = station.getLocations().get(0);
			}else {
				//prompt for location; must be at given station
				dialog = new StationAssetSelectionDialog(details.getShell(), Type.LOCATION, station);
				if (dialog.open() != Window.OK) return null;
				location = dialog.getSelectedLocation();
			}
			
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try {
					AssetDeployment d = FileProcessor.findAssetDeployment(waypoint.getWaypoint(), asset, location, session, Locale.getDefault());
					assetWaypointLink = new AssetWaypoint();
					assetWaypointLink.setAssetDeployment(d);
					assetWaypointLink.setWaypoint(waypoint.getWaypoint());
					assetWaypointLink.setState(State.DIRTY);
					assetWaypointLink.setAttachments(new HashSet<>());
					assetWaypointLink.setIncidentLength(waypoint.getAssetLinks().iterator().next().getIncidentLength());
					session.getTransaction().commit();
				}catch (Exception ex) {
					assetWaypointLink = null;
					session.getTransaction().rollback();
					AssetPlugIn.displayLog(Messages.AssetDataPanel_ImportAttachmentSaveError + ex.getMessage(), ex);
				}
			}
		}
		
		if (assetWaypointLink== null) return null;
		List<ISmartAttachment> toAdd = new ArrayList<>();
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				if (assetWaypointLink.getUuid() == null) {
					if (assetWaypointLink.getAssetDeployment().getUuid() == null) {
						session.save(assetWaypointLink.getAssetDeployment());
					}
					session.save(assetWaypointLink);
					waypoint.getAssetLinks().add(assetWaypointLink);
				}
				
				String path = fd.getFilterPath();
				for (String s : fd.getFileNames()) {
					Path p = Paths.get(path, s);

					//create new waypoint attachment
					WaypointAttachment wa = new WaypointAttachment();
					wa.setCopyFromLocation(p);
					wa.setWaypoint(waypoint.getWaypoint());
					wa.setFilename(s);
					toAdd.add(wa);

					//add to waypoint
					waypoint.getWaypoint().getAttachments().add(wa);
					session.save(wa);
					wa.computeFileLocation(session);

					//create new attachment/deployment link
					AssetWaypointAttachment assetAttachmentLink = new AssetWaypointAttachment();
					assetAttachmentLink.setWaypointAttachment(wa);
					assetAttachmentLink.setAssetWaypoint(assetWaypointLink);
					assetWaypointLink.getAttachments().add(assetAttachmentLink);
					
					session.saveOrUpdate(assetWaypointLink);
					
					session.flush();
				}
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetDataPanel_ImportAttachmentSaveError + ex.getMessage(), ex);
			}
		}
		
		if (fireEvent) fireEvent();
		return toAdd;
	}
	
	
	private class RowItem{
		
		private AssetWaypointMapping waypoint;
		private AttachmentTable tt;
		
		private boolean isSelected = false;
		private boolean isMouseOver = false;
		
		private Composite item;
		private Composite header;
		private Composite wppart ;
		
		private Label headerLabel;
		
		private Color bgColor = null;
		private Listener clickListener = null;
		
		private Label waypointComment;
		
		private Composite imageTableComposite;
		
		public RowItem(AssetWaypointMapping waypoint) {
			this.waypoint = waypoint;
		}
		
		public void refreshComment() {
			waypointComment.setText(MessageFormat.format(Messages.AssetDataPanel_WpCommentLabel, waypoint.getWaypoint().getComment() == null ? "" : waypoint.getWaypoint().getComment())); //$NON-NLS-1$
			waypointComment.setVisible(waypoint.getWaypoint().getComment() != null);
		}
		
		public void hideAndDisable(String msg) {
			for (Control c : item.getChildren()) c.dispose();
			
			this.isMouseOver = false;
			this.isSelected = false;
			
			header = toolkit.createComposite(item, SWT.BORDER);
			header.setLayout(new GridLayout(2, false));
			header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
			header.setBackground(validatedColor);
			
			headerLabel = toolkit.createLabel(header, ""); //$NON-NLS-1$
			populateHeaderLabel();
			headerLabel.setText(msg + " " + headerLabel.getText()); //$NON-NLS-1$
			headerLabel.setBackground(header.getBackground());
			headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			if (clickListener != null) {
				item.removeListener(SWT.MouseUp, clickListener);
				item.removeListener(SWT.MouseEnter, clickListener);
				item.removeListener(SWT.MouseExit, clickListener);
			}
		}
		
		public void setSelected(boolean isSelected) {
			if (this.isSelected == isSelected) return;
			
			this.isSelected = isSelected;
			if (isSelected) {
				colorControl(header, selectionColor);
				colorControl(wppart, mouseOverColor);
			}else {
				colorControl(header, headerColor);
				colorControl(wppart, bgColor);
			}
			item.redraw();
		}
		
		public void populateHeaderLabel() {
			if (waypoint.getWaypoint() == null) return ;
			
			StringBuilder sb = new StringBuilder();
			sb.append(DateFormat.getDateTimeInstance().format(waypoint.getWaypoint().getDateTime()));
			sb.append(" ("); //$NON-NLS-1$
			sb.append(AssetUtils.formatTimeHours(waypoint.getAssetLinks().iterator().next().getIncidentLength()));
			sb.append(")"); //$NON-NLS-1$
			
			Optional<AssetStation> station = waypoint.getAssetLinks().stream().map(e->e.getAssetDeployment().getStationLocation().getStation()).findFirst();
			if (station.isPresent()) {
				sb.append("      "); //$NON-NLS-1$
				sb.append(station.get().getId());
			}
			
			Set<AssetStationLocation> locations= waypoint.getAssetLinks().stream().map(e->e.getAssetDeployment().getStationLocation()).collect(Collectors.toSet());
			sb.append("      "); //$NON-NLS-1$
			for (AssetStationLocation l : locations) {
				sb.append(l.getId());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			
			Set<Asset> assets = waypoint.getAssetLinks().stream().map(e->e.getAssetDeployment().getAsset()).collect(Collectors.toSet());
			sb.append("      "); //$NON-NLS-1$
			for (Asset a : assets) {
				sb.append(a.getId());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			headerLabel.setText(sb.toString());
			headerLabel.getParent().layout();
		}
		public Control createControl(Composite parent) {
			item = toolkit.createComposite(parent, SWT.NONE);
			item.setLayout(new GridLayout());
			item.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
			((GridLayout)item.getLayout()).marginWidth = 2;
			((GridLayout)item.getLayout()).marginHeight = 2;
			((GridLayout)item.getLayout()).verticalSpacing = 0;
			bgColor = item.getBackground();
			
			if (waypoint.getWaypoint() == null) {
				hideAndDisable(Messages.AssetDataPanel_DeletedLabel);
				return item;
			}
			if (!waypoint.hasDirty() && hideOnValidate) {
				hideAndDisable(Messages.AssetDataPanel_ValidatedLabel);
				return item;
			}
			header = toolkit.createComposite(item, SWT.NONE);
			header.setLayout(new GridLayout(2, false));
			header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
			header.setBackground(headerColor);
			
			headerLabel = toolkit.createLabel(header, ""); //$NON-NLS-1$
			populateHeaderLabel();
			
			headerLabel.setBackground(header.getBackground());
			headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			if (isEdit) {
				ToolBar tb = new ToolBar(header, SWT.FLAT);
				
				ToolItem itemEdit = new ToolItem(tb, SWT.PUSH);
				itemEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
				itemEdit.setToolTipText(Messages.AssetDataPanel_editwptooltip);
				itemEdit.addListener(SWT.Selection, e->{
					editWaypoint(waypoint);
				});
				
				ToolItem itemDelete = new ToolItem(tb, SWT.PUSH);
				itemDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
				itemDelete.setToolTipText(Messages.AssetDataPanel_deletewptooltip);
				itemDelete.addListener(SWT.Selection, e->{
					deleteWaypoints(Collections.singleton(waypoint));
					
				});
				
				if (waypoint.hasDirty() && canValidate) {
					ToolItem itemCh = new ToolItem(tb, SWT.PUSH);
					itemCh.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_VALIDATE));
					itemCh.setToolTipText(Messages.AssetDataPanel_verfitywptooltip);
					itemCh.addListener(SWT.Selection, e->{
						markValidated(Collections.singleton(waypoint));
					});
				}
			}
			
			wppart = toolkit.createComposite(item, SWT.NONE);
			wppart.setLayout(new GridLayout(2, false));
			wppart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true ));
			
			((GridLayout)wppart.getLayout()).marginWidth = 0;
			((GridLayout)wppart.getLayout()).marginHeight = 0;
			
			
			if (AssetDataPanel.this.asset == null) {
				List<ISmartAttachment> assetFiles = new ArrayList<>();
				List<ISmartAttachment> otherFiles = new ArrayList<>();
				
				if (waypoint.getWaypoint().getAttachments() != null) {
					for (AssetWaypoint aw : waypoint.getAssetLinks()) {
						for (AssetWaypointAttachment a : aw.getAttachments()) {
							if (aw.getAssetDeployment().getAsset().equals(asset)) {
								assetFiles.add(a.getWaypointAttachment());
							}else {
								otherFiles.add(a.getWaypointAttachment());
							}
						}
					}
				}
				if (waypoint.getWaypoint().getAllObservations() != null) {
					waypoint.getWaypoint().getAllObservations().forEach(o->{
						if (o.getAttachments() != null) {
							o.getAttachments().forEach(a->otherFiles.add(a));
						}
					});
				}
				
				List<ISmartAttachment> allFiles = new ArrayList<>();
				allFiles.addAll(assetFiles);
				allFiles.addAll(otherFiles);
				tt = new AttachmentTable(wppart, toolkit, getAttachmentTableMenu(), allFiles, displaySettings.getIconSize().getSize());
				tt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				((GridData)tt.getLayoutData()).widthHint = displaySettings.getIconSize().getSize()*2+20;
			}else {
				
				imageTableComposite = toolkit.createComposite(wppart);
				imageTableComposite.setLayout(new GridLayout());
				imageTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				((GridLayout)imageTableComposite.getLayout()).marginWidth = 0;
				((GridLayout)imageTableComposite.getLayout()).marginHeight = 0;
				
				createImageTable(false);
			}
								
			Composite spacer = toolkit.createComposite(wppart);
			spacer.setLayout(new GridLayout());
			spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridLayout)spacer.getLayout()).marginWidth = 0;
			((GridLayout)spacer.getLayout()).marginHeight = 3;
			
			Composite detailsPart = toolkit.createComposite(spacer, SWT.NONE);
			detailsPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			detailsPart.setLayout(new GridLayout());
			((GridLayout)detailsPart.getLayout()).verticalSpacing = 1;
			((GridLayout)detailsPart.getLayout()).marginWidth = 1;
			((GridLayout)detailsPart.getLayout()).marginHeight = 1;
			detailsPart.setData(COLOR, false);
			
			Composite attributes = toolkit.createComposite(detailsPart, SWT.NONE);
			attributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			attributes.setLayout(new GridLayout());
			((GridLayout)detailsPart.getLayout()).verticalSpacing = 1;
			((GridLayout)attributes.getLayout()).marginWidth = 0;
			((GridLayout)attributes.getLayout()).marginHeight = 0;

			new WaypointAttributeTable(attributes, toolkit, waypoint.getWaypoint(), AssetDataPanel.this);
			
			waypointComment = toolkit.createLabel(detailsPart, "", SWT.WRAP); //$NON-NLS-1$
			refreshComment();
			waypointComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)waypointComment.getLayoutData()).widthHint = detailsPart.getBounds().width;
			
			String lastModified = null;
			if (waypoint.getWaypoint().getLastModifiedBy() != null) {
				lastModified = MessageFormat.format(Messages.AssetDataPanel_LastUpdated1, SmartLabelProvider.getShortLabel(waypoint.getWaypoint().getLastModifiedBy()), DateFormat.getDateTimeInstance().format(waypoint.getWaypoint().getLastModified()));
			}else {
				lastModified = MessageFormat.format(Messages.AssetDataPanel_LastUpdated2, DateFormat.getDateTimeInstance().format(waypoint.getWaypoint().getLastModified()));
			}
			Label lastModifiedLabel = toolkit.createLabel(spacer, lastModified);
			lastModifiedLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false));
			
			if (isEdit) {
				clickListener = e->{
					switch(e.type) {
					case SWT.MouseUp:
						processMouseClickEvent(RowItem.this, e);
					case SWT.MouseEnter:
						this.isMouseOver = true;
						if (!isSelected) colorControl(header, mouseOverColor);
						item.redraw();
						break;
					case SWT.MouseExit:
						this.isMouseOver = false;
						if (isSelected) colorControl(header, selectionColor);
						else colorControl(header, headerColor);
						item.redraw();
						break;
					}
				};
				
				item.addPaintListener(e->{
					if (!isMouseOver && !isSelected) return;
					
					if (isMouseOver) {
						e.gc.setLineWidth(2);
						e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
					}else if (isSelected) {
						e.gc.setLineWidth(2);
						e.gc.setForeground(selectionColor);
					}	
					e.gc.drawRectangle(0, 0, item.getBounds().width,item.getBounds().height);
					
				});	
				
				forEachChild(header, e->{
					e.addListener(SWT.MouseUp, clickListener);
					e.addListener(SWT.MouseEnter, clickListener);
					e.addListener(SWT.MouseExit, clickListener);
					return true;
				});
				
				item.addListener(SWT.MouseUp, clickListener);
				item.addListener(SWT.MouseEnter, clickListener);
				item.addListener(SWT.MouseExit, clickListener);
				
				headerLabel.addListener(SWT.MouseDown, e->{
					if (e.button == 3) createHeaderMenu(headerLabel);
				});
			}
			return item;
		}
		
		private void createImageTable(boolean includeAll) {
			
			
			for (Control c : imageTableComposite.getChildren()) c.dispose();
			
			List<ISmartAttachment> assetFiles = new ArrayList<>();
			List<ISmartAttachment> otherFiles = new ArrayList<>();
			
			HashMap<ISmartAttachment,String> otherAssets = new HashMap<>();
			if (waypoint.getWaypoint().getAttachments() != null) {
				for (AssetWaypoint aw : waypoint.getAssetLinks()) {
					for (AssetWaypointAttachment a : aw.getAttachments()) {
						if (aw.getAssetDeployment().getAsset().equals(asset)) {
							assetFiles.add(a.getWaypointAttachment());
						}else {
							otherFiles.add(a.getWaypointAttachment());
							otherAssets.put(a.getWaypointAttachment(), aw.getAssetDeployment().getAsset().getId());
						}
					}
				}
			}
			if (waypoint.getWaypoint().getAllObservations() != null) {
				waypoint.getWaypoint().getAllObservations().forEach(o->{
					if (o.getAttachments() != null) {
						o.getAttachments().forEach(a->otherFiles.add(a));
					}
				});
			}
			
			List<ISmartAttachment> allFiles = new ArrayList<>();
			allFiles.addAll(assetFiles);
			if (includeAll) allFiles.addAll(otherFiles);
			
			tt = new AttachmentTable(imageTableComposite, toolkit, getAttachmentTableMenu(), allFiles, displaySettings.getIconSize().getSize()) {
				@Override
				protected String getWatermark(ISmartAttachment file) {
					if (otherFiles.contains(file)) {
						return otherAssets.get(file);
					}
					return null;
				}
			};
			tt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			((GridData)tt.getLayoutData()).widthHint = displaySettings.getIconSize().getSize() *2 + 20;
			
			if (!includeAll && !otherFiles.isEmpty()) {
				Hyperlink lnk = toolkit.createHyperlink(imageTableComposite, Messages.AssetDataPanel_ShowOtherImages, SWT.NONE);
				lnk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				((GridData)lnk.getLayoutData()).widthHint = 100;
				lnk.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						createImageTable(true);
						
						resizeScroll();
						item.layout(true,true);
					}
				});
			}else if (includeAll && !otherFiles.isEmpty()) {
				Hyperlink lnk = toolkit.createHyperlink(imageTableComposite, Messages.AssetDataPanel_HideOtherImages, SWT.NONE);
				lnk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				((GridData)lnk.getLayoutData()).widthHint = 100;
				lnk.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						createImageTable(false);
						
						resizeScroll();
						item.layout(true,true);
					}
				});
			}
		}
		
		
		public void resize(int totalWidth, DataDisplaySettings.IconSize iconSize) {
			if (tt == null || tt.isDisposed()) return;
			int w1 = iconSize.getSize() * 2 + 20;
			int w2 = (totalWidth * 1) / 2;
			int w3 = tt.getAttachments().size()  * ( iconSize.getSize() + 5 );
			
			int setting = w3;
			
			if (w3 < w1 && w3 < w2) {
				((GridData)tt.getLayoutData()).widthHint = w3;
			}else {
				int numItems = (int)Math.floor(w2 / (iconSize.getSize() + 5 ));
				numItems = Math.min(waypoint.getWaypoint().getAttachments() == null || waypoint.getWaypoint().getAttachments().isEmpty()? 1 : waypoint.getWaypoint().getAttachments().size(), numItems);
				setting = Math.min(numItems * (iconSize.getSize() + 5 ), w2);
				
			}
			if (setting == 0) setting = 50;
			((GridData)tt.getLayoutData()).widthHint = setting;
			tt.getParent().layout(true);
		}
		
		public AttachmentTable.IMenuCreator getAttachmentTableMenu(  ){
			
			return new AttachmentTable.IMenuCreator() {
				MenuItem removeImg = null;
				MenuItem createIncident = null;
				
				@SuppressWarnings("unchecked")
				@Override
				public Menu createMenu(AttachmentTable parent) {
					Menu mnu = new Menu(parent);
					
					if (isEdit) {
						createIncident = new MenuItem(mnu, SWT.PUSH);
						createIncident.setText(Messages.AssetDataPanel_ExtractNewIncidentMenu);
						createIncident.addListener(SWT.Selection, e->{
							if (!isEdit) return;
							IStructuredSelection items = tt.getSelection();
							List<WaypointAttachment> toMove = new ArrayList<>();
							for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
								Object attachment = iterator.next();
								if (attachment instanceof WaypointAttachment) toMove.add((WaypointAttachment) attachment);
							}
								
							AssetWaypointMapping newWaypoint = newIncident(waypoint, toMove);
							if (newWaypoint == null) return;
							//refresh entire view as we now have a new incident
							refresh();
						});
						new MenuItem(mnu, SWT.SEPARATOR);
						
						removeImg = new MenuItem(mnu, SWT.PUSH);
						removeImg.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						removeImg.setText(Messages.AssetDataPanel_RemoveAttachmentMenu);
						removeImg.addListener(SWT.Selection, e->{
							if (!isEdit) return;
							IStructuredSelection items = tt.getSelection();
							List<ISmartAttachment> toRemove = new ArrayList<>();
							for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
								Object attachment = iterator.next();
								if (attachment instanceof ISmartAttachment) toRemove.add((ISmartAttachment) attachment);
							}
							if (removeAttachments(RowItem.this.waypoint, toRemove)) {
								tt.getAttachments().removeAll(toRemove);
								populateHeaderLabel();
								tt.refresh();
								AssetDataPanel.this.resizeScroll();
								
							}
							
						});					
						MenuItem addAttachment = new MenuItem(mnu, SWT.PUSH);
						addAttachment.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
						addAttachment.setText(Messages.AssetDataPanel_ImportAttachmentMenu);
						addAttachment.addListener(SWT.Selection, e->{
							List<ISmartAttachment> addedItems = importAttachment(RowItem.this.waypoint);
							if (addedItems != null) {
								populateHeaderLabel();
								((List<ISmartAttachment>)tt.getAttachments()).addAll(addedItems);
								tt.refresh();
								AssetDataPanel.this.resizeScroll();
							}
							
						});
						new MenuItem(mnu, SWT.SEPARATOR);
					}
					
					MenuItem properties = new MenuItem(mnu, SWT.PUSH);
					properties.setText(Messages.AssetDataPanel_PropertiesMenu);
					properties.addListener(SWT.Selection, e->{
						Object x = tt.getSelection().getFirstElement();
						if (x instanceof WaypointAttachment) {
							AttachmentPropertiesDialog dialog = new AttachmentPropertiesDialog(parent.getShell(), (WaypointAttachment)x) {
								protected List<Entry> findAdditionalDetails(ISmartAttachment attachment) {
									for (AssetWaypoint aw : waypoint.getAssetLinks()) {
										for (AssetWaypointAttachment attach: aw.getAttachments()) {
											if (attach.getWaypointAttachment().equals(x)) {
												List<AttachmentPropertiesDialog.Entry> addedItems = new ArrayList<>();
												addedItems.add(new AttachmentPropertiesDialog.Entry(Messages.AssetDataPanel_AssetsOption, aw.getAssetDeployment().getAsset().getId()));
												addedItems.add(new AttachmentPropertiesDialog.Entry(Messages.AssetDataPanel_StationsOption, aw.getAssetDeployment().getStationLocation().getStation().getId()));
												addedItems.add(new AttachmentPropertiesDialog.Entry(Messages.AssetDataPanel_LocationsOption, aw.getAssetDeployment().getStationLocation().getId()));
												return addedItems;
											}
										}
									}
									return Collections.emptyList();
								}
							};
							dialog.open();

						}
					});
					
					mnu.addMenuListener(new MenuListener() {
						
						@Override
						public void menuShown(MenuEvent e) {
							boolean hasSelection = !tt.getSelection().isEmpty();
							properties.setEnabled(hasSelection);
							if (removeImg != null) removeImg.setEnabled(hasSelection);
							if (createIncident != null) createIncident.setEnabled(hasSelection);
						}
						
						@Override
						public void menuHidden(MenuEvent e) {
						}
					});
					return mnu;
				}
			};
		}
	}

}
