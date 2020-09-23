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
package org.wcs.smart.asset.ui.views.asset;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetSecurityManager;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.data.inout.deployment.DeploymentFromXml;
import org.wcs.smart.asset.data.inout.deployment.DeploymentToXml;
import org.wcs.smart.asset.engine.StatisticsEngine;
import org.wcs.smart.asset.engine.StatisticsEngine.Statistic;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetDeploymentDisruption;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.asset.ui.handler.OpenStationHandler;
import org.wcs.smart.asset.ui.handler.OpenStationLocationHandler;
import org.wcs.smart.asset.ui.views.station.StationEditorInput;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Asset deployment page for asset editor
 * @author Emily
 *
 */
public class AssetDeploymentPage {

	private static final String LABEL_VALUE = "VALUE"; //$NON-NLS-1$

	@Inject
	private IEclipseContext parentContext;
	
	private TableViewer tblDeployments;
	
	private Map<IAssetSummary, Label> assetSummaryValues;

	private List<AssetDeploymentWrapper> allDeployments;
	
	private AssetEditor parentEditor;

	private ScrolledComposite scrollDetails;
	private Composite detailsPane;
	private List<AssetTypeDeploymentAttribute> allDeploymentAttributes;
	
	private FormToolkit toolkit;
	
	public AssetDeploymentPage(AssetEditor parent) {
		this.parentEditor = parent;
	}
	
	public Composite createDeploymentsSection(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		panel.addListener(SWT.Dispose, e->computeDeploymentStats.cancel());
		
		Composite summaryPanel = toolkit.createComposite(panel, SWT.BORDER);
		summaryPanel.setLayout(new GridLayout(2, false));
		summaryPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = toolkit.createLabel(summaryPanel, Messages.AssetDeploymentPage_SummarySectionName);
		FontData fd = l.getFont().getFontData()[0];
		fd.setHeight(fd.getHeight() + 1);
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.setFont(boldFont);
		l.addListener(SWT.Dispose, e->boldFont.dispose());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		IAssetSummary[] summaryValues = new IAssetSummary[] {ActiveTimeInFieldAssetSummary.INSTANCE, TimeInFieldAssetSummary.INSTANCE, IncidentAssetSummary.INSTANCE};
		assetSummaryValues = new HashMap<>();
		for (IAssetSummary s : summaryValues) {
			toolkit.createLabel(summaryPanel, s.getSummaryName());
			
			Label sv = toolkit.createLabel(summaryPanel, ""); //$NON-NLS-1$
			sv.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			sv.setData(s);
			
			assetSummaryValues.put(s,  sv);
		}
		
		Composite historyPanel = toolkit.createComposite(panel, SWT.BORDER);
		historyPanel.setLayout(new GridLayout());
		historyPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)historyPanel.getLayout()).marginWidth = 0;
		((GridLayout)historyPanel.getLayout()).marginHeight = 0;
		
		Composite headerPanel = toolkit.createComposite(historyPanel, SWT.NONE);
		headerPanel.setLayout(new GridLayout(2, false));
		headerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerPanel.getLayout()).marginHeight = 0;
		((GridLayout)headerPanel.getLayout()).marginTop = 5;

		
		l = toolkit.createLabel(headerPanel, Messages.AssetDeploymentPage_SecitonName);
		fd = l.getFont().getFontData()[0];
		fd.setHeight(fd.getHeight() + 1);
		fd.setStyle(SWT.BOLD);
		Font boldFont1 = new Font(l.getDisplay(), fd);
		l.setFont(boldFont1);
		l.addListener(SWT.Dispose, e->boldFont1.dispose());
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		ToolItem itemDelete = null;
		ToolItem itemEdit = null;
		ToolItem itemExport = null;
		ToolItem itemImport = null;
		
		if (canEdit()) {
			ToolBar toolbar = new ToolBar(headerPanel, SWT.FLAT);
			toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			
			itemImport = new ToolItem(toolbar, SWT.PUSH);
			itemImport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
			itemImport.setToolTipText(Messages.AssetDeploymentPage_importTooltip);
			itemImport.addListener(SWT.Selection, e->importDeployments());
			
			itemExport = new ToolItem(toolbar, SWT.PUSH);
			itemExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
			itemExport.setToolTipText(Messages.AssetDeploymentPage_exportTooltip);
			itemExport.addListener(SWT.Selection, e->exportSelectedDeployments());
			itemExport.setEnabled(false);
			
			itemDelete = new ToolItem(toolbar, SWT.PUSH);
			itemDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			itemDelete.setToolTipText(Messages.AssetDeploymentPage_deleteTooltip);
			itemDelete.addListener(SWT.Selection, e->deleteSelectedDeployments());
			itemDelete.setEnabled(false);
			
			itemEdit = new ToolItem(toolbar, SWT.PUSH);
			itemEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			itemEdit.setToolTipText(Messages.AssetDeploymentPage_edittooltip);
			itemEdit.addListener(SWT.Selection, e->editSelectedDeployments());
			itemEdit.setEnabled(false);
			
			ToolItem itemAdd = new ToolItem(toolbar, SWT.PUSH);
			itemAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			itemAdd.setToolTipText(Messages.AssetDeploymentPage_createTooltip);
			itemAdd.addListener(SWT.Selection, e->addDeployment());
			
		}
		
		SashForm bodyPanel = new SashForm(historyPanel, SWT.HORIZONTAL);
		bodyPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite temp = new Composite(bodyPanel, SWT.BORDER);
		temp.setLayout(new GridLayout());
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		
		tblDeployments = new TableViewer(temp, SWT.FULL_SELECTION | SWT.MULTI );
		tblDeployments.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblDeployments.setContentProvider(ArrayContentProvider.getInstance());
		tblDeployments.getTable().setHeaderVisible(true);
		tblDeployments.getTable().setLinesVisible(true);
		
		tblDeployments.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = tblDeployments.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				int colIndex = cell.getColumnIndex();
				if (colIndex == AssetDeploymentTableColumn.FixedColumn.STATION.ordinal()){
					AssetDeployment d = ((AssetDeploymentWrapper) cell.getElement()).getDeployment();
					
					IEclipseContext ctx = parentContext.createChild();
					ctx.set(OpenStationHandler.STATION_PARAM, new StationEditorInput(d.getStationLocation().getStation().getUuid(), d.getStationLocation().getStation().getId()));
					ContextInjectionFactory.invoke(new OpenStationHandler(), Execute.class, ctx);
					
				}else if (colIndex == AssetDeploymentTableColumn.FixedColumn.LOCATION.ordinal()){
					AssetDeployment d = ((AssetDeploymentWrapper) cell.getElement()).getDeployment();
					(new OpenStationLocationHandler()).openStationLocation(d.getStationLocation());
				}else {
					if (!parentEditor.getAsset().getIsRetired()) editSelectedDeployments();
				}
			}
					
		});
		
		final ToolItem fitemExport = itemExport;
		tblDeployments.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fitemExport.setEnabled(!tblDeployments.getSelection().isEmpty());
			}
		});
		
		if (canEdit()) {
			final ToolItem fitemDelete = itemDelete;
			final ToolItem fitemEdit = itemEdit;

			tblDeployments.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					fitemDelete.setEnabled(!tblDeployments.getSelection().isEmpty());
					fitemEdit.setEnabled(!tblDeployments.getSelection().isEmpty());
				}
			});
			
			Menu mnuDeployments = new Menu(tblDeployments.getControl());
			
			MenuItem mnuAddDisruption = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuAddDisruption.setText(Messages.AssetDeploymentPage_NewDistruptionLbl);
			mnuAddDisruption.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			mnuAddDisruption.addListener(SWT.Selection, e-> addDisruption());
			
			MenuItem mnuAdd = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuAdd.setText(Messages.AssetDeploymentPage_NewDeploymentLbl);
			mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			mnuAdd.addListener(SWT.Selection, e-> addDeployment());
			
			new MenuItem(mnuDeployments, SWT.SEPARATOR);

			MenuItem mnuImport = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuImport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
			mnuImport.addListener(SWT.Selection, e-> importDeployments());
			mnuImport.setText(DialogConstants.IMPORT_BUTTON_TEXT);
			
			MenuItem mnuExport = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
			mnuExport.addListener(SWT.Selection, e-> exportSelectedDeployments());
			mnuExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
			
			new MenuItem(mnuDeployments, SWT.SEPARATOR);
			
			MenuItem mnuEdit = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			mnuEdit.addListener(SWT.Selection, e-> editSelectedDeployments());
			mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
			
			MenuItem mnuDelete = new MenuItem(mnuDeployments, SWT.PUSH);
			mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			mnuDelete.addListener(SWT.Selection, e->deleteSelectedDeployments());
			mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			
			tblDeployments.getControl().setMenu(mnuDeployments);
			
			mnuDeployments.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					mnuExport.setEnabled(!tblDeployments.getSelection().isEmpty());
					mnuEdit.setEnabled(!tblDeployments.getSelection().isEmpty());
					mnuDelete.setEnabled(!tblDeployments.getSelection().isEmpty());
				}
				
				@Override
				public void menuHidden(MenuEvent e) { }
			});
		}
		
		Composite detailsPaneOuter = new Composite(bodyPanel, SWT.BORDER);
		detailsPaneOuter.setLayout(new GridLayout());
		detailsPaneOuter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		//((GridData)detailsPaneOuter.getLayoutData()).widthHint = 200;
		bodyPanel.setWeights(new int[] {70,30});
		
		scrollDetails = new ScrolledComposite(detailsPaneOuter, SWT.V_SCROLL | SWT.H_SCROLL);
		toolkit.adapt(scrollDetails);
		
		scrollDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrollDetails.setExpandHorizontal(true);
		scrollDetails.setExpandVertical(true);
		detailsPane = toolkit.createComposite(scrollDetails, SWT.NONE);
		scrollDetails.setContent(detailsPane);
		
		detailsPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblDeployments.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)tblDeployments.getSelection()).getFirstElement();
				if (x instanceof AssetDeploymentWrapper) {
					updateDetailsPane(((AssetDeploymentWrapper)x).getDeployment());
				}
			}
		});
		
		detailsPane.addListener(SWT.Resize, e->{
			resizeLabels();
		});
					
		initializePanel(parentEditor.getAsset());
		refreshSummaryStatistics();
		
		return panel;
	}
	
	private void resizeLabels() {
		List<Label> items = new ArrayList<>();
		List<Control> tempitem = new ArrayList<>();
		tempitem.add(detailsPane);
		while(!tempitem.isEmpty()) {
			Control c = tempitem.remove(0);
			if (c instanceof Label && c.getData(LABEL_VALUE) != null) {
				items.add((Label)c);
			}else if (c instanceof Composite) {
				for (Control k : ((Composite)c).getChildren()) tempitem.add(k);
			}
		}
		
		for (Label lbl : items) {
			lbl.setText(""); //$NON-NLS-1$
		}
		detailsPane.layout(true);
		
		for (Label lbl : items) {
			int w = lbl.getParent().getBounds().width - 17;
			lbl.setText((String)lbl.getData(LABEL_VALUE));
			int h = lbl.computeSize(w, SWT.DEFAULT).y;
			lbl.setBounds(0, 0, w, h);
			lbl.getParent().pack();
			lbl.getParent().layout(true);
		}
		detailsPane.layout(true);
	}
	
	private boolean canEdit() {
		return (!parentEditor.getAsset().getIsRetired() && AssetSecurityManager.INSTANCE.canImportData()) ;
	}
	
	private void updateDetailsPane(AssetDeployment deployment) {
		if (detailsPane == null) return;
		
		for (Control c : detailsPane.getChildren()) c.dispose();
		
		detailsPane.setLayout(new GridLayout(2, false));
		((GridLayout)detailsPane.getLayout()).marginWidth = 0;
		((GridLayout)detailsPane.getLayout()).marginHeight = 0;
		
		Composite p = SmartUiUtils.createHeaderLabel(detailsPane, MessageFormat.format("{0} ({1})", deployment.getStationLocation().getId(), deployment.getStationLocation().getStation().getId())); //$NON-NLS-1$
//		Label l = toolkit.createLabel(detailsPane, MessageFormat.format("{0} ({1})", deployment.getStationLocation().getId(), deployment.getStationLocation().getStation().getId())); //$NON-NLS-1$
		p.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridLayout)p.getLayout()).marginHeight = 8;
		
		Composite dateDetails = toolkit.createComposite(detailsPane);
		dateDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		dateDetails.setLayout(new GridLayout(3, false));
		((GridLayout)dateDetails.getLayout()).marginWidth = 0;
		((GridLayout)dateDetails.getLayout()).marginHeight = 0;
		
		Label l = toolkit.createLabel(dateDetails, 
				deployment.getStartDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + "\n" + deployment.getStartDate().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)));  //$NON-NLS-1$
					
		l = toolkit.createLabel(dateDetails, "   -   "); //$NON-NLS-1$
		if (deployment.getEndDate() == null) {
			l = toolkit.createLabel(dateDetails, Messages.AssetDeploymentPage_CurrentDateLabel);
		}else {
			l = toolkit.createLabel(dateDetails,
					deployment.getEndDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + "\n" + deployment.getEndDate().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)));  //$NON-NLS-1$
		}

		l = toolkit.createLabel(detailsPane, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		if (allDeploymentAttributes != null) {
			for (AssetTypeDeploymentAttribute a : allDeploymentAttributes) {
				l = toolkit.createLabel(detailsPane, a.getAttribute().getName() + ":"); //$NON-NLS-1$
				l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
				AssetDeploymentAttributeValue value = null;
				for (AssetDeploymentAttributeValue v : deployment.getAttributeValues()) {
					if (v.getAttribute().equals(a.getAttribute())) {
						value = v;
						break;
					}
				}
				if (value != null) {
					Composite t = toolkit.createComposite(detailsPane);
					t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					
					Label l2 = toolkit.createLabel(t, "", SWT.WRAP); //$NON-NLS-1$
					final String str = value.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
					l2.setData(LABEL_VALUE, str);
					l2.setText(str);
	
				}else {
					toolkit.createLabel(detailsPane, ""); //$NON-NLS-1$
				}
			}
		}
		
		p = SmartUiUtils.createHeaderLabel(detailsPane, Messages.AssetDeploymentPage_DisruptionsSection);
		if (canEdit()) {
			((GridLayout)p.getLayout()).numColumns = ((GridLayout)p.getLayout()).numColumns + 1;
		
			ToolBar tb = new ToolBar(p, SWT.FLAT);
			tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			ToolItem addItem = new ToolItem(tb, SWT.PUSH);
			addItem.setToolTipText(Messages.AssetDeploymentPage_addDisruptionTooltip);
			addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			addItem.addListener(SWT.Selection, e->addDisruption());
		}
		

		p.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));

		l = toolkit.createLabel(detailsPane, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		List<AssetDeploymentDisruption> temp = new ArrayList<>();
		temp.addAll(deployment.getDisruptions());
		temp.sort((a,b)->-1*a.getStartDate().compareTo(b.getEndDate()));
		
		for (AssetDeploymentDisruption d : temp) {
			
			int col = canEdit() ? 4 : 3;
			dateDetails = toolkit.createComposite(detailsPane);
			dateDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			dateDetails.setLayout(new GridLayout(col, false));
			((GridLayout)dateDetails.getLayout()).marginWidth = 0;
			((GridLayout)dateDetails.getLayout()).marginHeight = 0;
			
			l = toolkit.createLabel(dateDetails, d.getStartDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + "\n" + d.getStartDate().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)));  //$NON-NLS-1$
			l = toolkit.createLabel(dateDetails, "   -   "); //$NON-NLS-1$
			l = toolkit.createLabel(dateDetails, d.getEndDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + "\n" + d.getEndDate().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)));  //$NON-NLS-1$
						
			if (canEdit()) {
				ToolBar tb = new ToolBar(dateDetails, SWT.FLAT);
				tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
				
				ToolItem editItem = new ToolItem(tb, SWT.PUSH);
				editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
				editItem.setToolTipText(Messages.AssetDeploymentPage_editDisruptionTooltip);
				editItem.addListener(SWT.Selection, e->{
					editDisruption(d);
				});
				
				ToolItem deleteItem = new ToolItem(tb, SWT.PUSH);
				deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
				deleteItem.setToolTipText(Messages.AssetDeploymentPage_deleteDisruptionTooltip);
				deleteItem.addListener(SWT.Selection, e->{
					deleteDisruption(d);
				});
			}
			
			Composite parent = toolkit.createComposite(dateDetails);
			parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, col, 1));
			
			l = toolkit.createLabel(parent, d.getComment(), SWT.WRAP);
			l.setData(LABEL_VALUE, d.getComment());

			l = toolkit.createLabel(detailsPane, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, col, 1));
			
		}
		
		detailsPane.layout(true);
		
		scrollDetails.setMinSize(detailsPane.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		resizeLabels();
	}
	
	
	public void initializePanel(Asset asset) {
		loadHistoryDataJob.setSystem(true);
		loadHistoryDataJob.schedule();
		refreshSummaryStatistics();
	}
	
	public void refreshSummaryStatistics() {
//		refreshSummaryStatsJob.setSystem(true);
		refreshSummaryStatsJob.schedule();
		computeDeploymentStats.schedule();
	}
	
	private void addDeployment() {
		AssetDeployment newDeployment = new AssetDeployment();
		newDeployment.setAsset(parentEditor.getAsset());
		newDeployment.setStartDate(LocalDateTime.now());
		newDeployment.setAssetWaypoints(new ArrayList<>());
		newDeployment.setDisruptions(new ArrayList<>());
		AssetDeploymentDialog dialog = new AssetDeploymentDialog(parentEditor.getSite().getShell(), newDeployment, allDeployments);
		ContextInjectionFactory.inject(dialog, parentContext);
		if (dialog.open() != Window.OK) return;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(newDeployment);
				parentEditor.getAsset().computeStatus(session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetDeploymentPage_SaveError + ex.getMessage(), ex);
			}
		}
		
		allDeployments.add(new AssetDeploymentWrapper(newDeployment));
		sortDeployments();
		tblDeployments.refresh();
		parentEditor.fireAssetModified(false);
		parentEditor.deploymentModified(Collections.singletonList(newDeployment), AssetEvents.ASSETDEPLOYMENT_NEW);
	}
	
	private void importDeployments() {
			
		FileDialog dir = new FileDialog(detailsPane.getShell(), SWT.OPEN | SWT.MULTI);
		dir.setText(Messages.AssetDeploymentPage_DirDialogTitle);
		dir.setFilterNames(new String[] {Messages.AssetDeploymentPage_zipFiles, Messages.AssetDeploymentPage_AllFiles});
		dir.setFilterExtensions(new String[] {"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		
		String path = dir.open();
		if (path == null) return;
		
		Path directory = Paths.get(dir.getFilterPath());
		List<Path> files = new ArrayList<>();
		for (String s : dir.getFileNames()) {
			files.add(directory.resolve(s));
		}
		
		List<AssetDeployment> deployments = new ArrayList<>();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(detailsPane.getShell());
		final Shell parent = detailsPane.getShell();
		try {
			pmd.run(true,  true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor sub = SubMonitor.convert(monitor);
					sub.beginTask(Messages.AssetDeploymentPage_importingTask, files.size());
					
					for (Path item : files) {
						
						try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
					
							DeploymentFromXml engine = new DeploymentFromXml();
							AssetDeployment in = engine.importDeployment(item, session, parent, parentContext.get(IEventBroker.class), sub.split(1));
							if (in != null) deployments.add(in);
						

						}catch(OperationCanceledException ex) {
							return;
						}catch (Exception ex) {
							AssetPlugIn.displayLog(MessageFormat.format(Messages.AssetDeploymentPage_ImportError,  item.getFileName().toString()),ex);
						}
						
					}
				}
			});
		}catch (Exception ex) {
			AssetPlugIn.displayLog(ex.getMessage(), ex);
		}

		MessageDialog.openInformation(detailsPane.getShell(), Messages.AssetDeploymentPage_SaveOkTitle,
				MessageFormat.format(Messages.AssetDeploymentPage_SaveOkMessage, deployments.size(), files.size()));

		try(Session session = HibernateManager.openSession()){
			parentEditor.getAsset().computeStatus(session);
		}
		
		for (AssetDeployment d : deployments) {
			allDeployments.add(new AssetDeploymentWrapper(d));
		}
		
		sortDeployments();
		tblDeployments.refresh();
		parentEditor.fireAssetModified(false);
		parentEditor.reloadDataPage();
	}
	
	private void exportSelectedDeployments() {
		if (tblDeployments == null) return;
	
		List<AssetDeploymentWrapper> toExport = new ArrayList<>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblDeployments.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof AssetDeploymentWrapper) {
				toExport.add((AssetDeploymentWrapper)x);			
			}
		}
		if (toExport.isEmpty()) return;
		
		
		DirectoryDialog dir = new DirectoryDialog(detailsPane.getShell());
		String path = dir.open();
		if (path == null) return;
		
		Path outputDirectory  = Paths.get(path);
		SmartUtils.createDirectory(outputDirectory);
		
		SimpleDateFormat dformat = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(detailsPane.getShell());
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor sub = SubMonitor.convert(monitor, toExport.size());
					sub.beginTask(Messages.AssetDeploymentPage_ExportingTask, toExport.size());
					int cnt = 0;
					try(Session session = HibernateManager.openSession()){
					
						for (AssetDeploymentWrapper deploy : toExport) {
							
							try {
								AssetDeployment d = session.get(AssetDeployment.class, deploy.getDeployment().getUuid());
								
								String fname = URLUtils.cleanFilename( d.getAsset().getId() + "_" + dformat.format(d.getStartDate()) + "-" + dformat.format(d.getEndDate() == null ? new Date() : d.getEndDate()) ); //$NON-NLS-1$ //$NON-NLS-2$
								fname += ".zip"; //$NON-NLS-1$
								
								Path outFile = outputDirectory.resolve(fname);
								
								DeploymentToXml engine = new DeploymentToXml();
								try {
									engine.writeDeployment(d, session, outFile, sub.split(1));
									cnt++;
								}catch(OperationCanceledException ex) {
									throw ex;
								}catch (Exception ex) {
									AssetPlugIn.displayLog(Messages.AssetDeploymentPage_ExportError + ex.getMessage(), ex);
								}
								if(sub.isCanceled()) throw new OperationCanceledException();
							}catch(OperationCanceledException ex) {
								break;
							}
						}
					}

					final int fcnt = cnt;
					detailsPane.getDisplay().asyncExec(()->{
						MessageDialog.openInformation(detailsPane.getShell(), Messages.AssetDeploymentPage_ExportOkTitle, 
							MessageFormat.format(Messages.AssetDeploymentPage_ExportOkMessage, fcnt, toExport.size(), outputDirectory.toString()));
					});
					
				}
			});
		}catch (Exception ex) {
			AssetPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
	
	private void deleteSelectedDeployments() {
		if (tblDeployments == null) return;
		List<AssetDeploymentWrapper> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblDeployments.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof AssetDeploymentWrapper) {
				toDelete.add((AssetDeploymentWrapper)x);			
			}
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(parentEditor.getSite().getShell(), Messages.AssetDeploymentPage_DeleteTitle, 
				MessageFormat.format(Messages.AssetDeploymentPage_DeleteConfirm, toDelete.size()))){
			return;
		}
		
		//confirm password
		if (!AssetUtils.confirmPassword(parentEditor.getSite().getShell(), Messages.AssetDeploymentPage_ConfirmPasswordTitle, Messages.AssetDeploymentPage_ConfirmPasswordMsg)) {
			return;
		}
		
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				for (AssetDeploymentWrapper deploy : toDelete) {
					//delete deployment & all associated waypoints/observations
					AssetDeployment d = session.get(AssetDeployment.class, deploy.getDeployment().getUuid());
					List<AssetWaypoint> dd = new ArrayList<>(d.getAssetWaypoints());
					d.getAssetWaypoints().clear();
					
					for (AssetWaypoint aw : dd) {
						session.delete(aw);
					}
					session.flush();
					
					//delete any waypoints not associated with asset waypoint
					try (ScrollableResults scroll = session.createQuery("FROM Waypoint ww WHERE source = :source and ww not in (SELECT waypoint FROM AssetWaypoint)").setParameter("source", AssetWaypointSource.KEY).scroll()){ //$NON-NLS-1$ //$NON-NLS-2$
						while(scroll.next()) {
							Waypoint wp = (Waypoint)scroll.get(0);
							session.delete(wp);
						}
					}
					
					session.flush();
					session.delete(d);
				}
				parentEditor.getAsset().computeStatus(session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetDeploymentPage_SaveError + ex.getMessage(), ex);
			}
		}
		allDeployments.removeAll(toDelete);
		tblDeployments.refresh();
		parentEditor.fireAssetModified(false);
		parentEditor.deploymentModified(toDelete.stream().map(e->e.getDeployment()).collect(Collectors.toList()), AssetEvents.ASSETDEPLOYMENT_DELETE);
		parentEditor.reloadDataPage();
	}
	
	private void editSelectedDeployments() {
		if (tblDeployments == null) return;
		Object toEdit = ((IStructuredSelection)tblDeployments.getSelection()).getFirstElement();
		if (toEdit == null) return;
		if (!(toEdit instanceof AssetDeploymentWrapper)) return;
		
		AssetDeployment toUpdate = ((AssetDeploymentWrapper)toEdit).getDeployment();
		AssetDeploymentDialog dialog = new AssetDeploymentDialog(parentEditor.getSite().getShell(), toUpdate, allDeployments);
		ContextInjectionFactory.inject(dialog, parentContext);
		if (dialog.open() != Window.OK) return;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(toUpdate);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetDeploymentPage_SaveError + ex.getMessage(), ex);
			}
			parentEditor.getAsset().computeStatus(session);
		}
		sortDeployments();
		tblDeployments.refresh();
		parentEditor.fireAssetModified(false);
		parentEditor.deploymentModified(Collections.singletonList(toUpdate), AssetEvents.ASSETDEPLOYMENT_MODIFIED);
		updateDetailsPane(toUpdate);
	}
	
	private void addDisruption() {
		if (tblDeployments == null) return;
		Object toEdit = ((IStructuredSelection)tblDeployments.getSelection()).getFirstElement();
		if (toEdit == null) return;
		if (!(toEdit instanceof AssetDeploymentWrapper)) return;
		
		AssetDeployment toUpdate = ((AssetDeploymentWrapper)toEdit).getDeployment();
		AssetDeploymentDisruption disruption = new AssetDeploymentDisruption();
		disruption.setAssetDeployment(toUpdate);
		
		editDisruption(disruption);
		
	}
	
	private void editDisruption(AssetDeploymentDisruption disruption) {
		
		AssetDeployment toUpdate = disruption.getAssetDeployment();
		
		DisruptionDialog dialog = new DisruptionDialog(parentEditor.getSite().getShell(), disruption);
		ContextInjectionFactory.inject(dialog, parentContext);
		if (dialog.open() != Window.OK) return;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				if (!toUpdate.getDisruptions().contains(disruption)) toUpdate.getDisruptions().add(disruption);
				session.saveOrUpdate(toUpdate);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetDeploymentPage_SaveError + ex.getMessage(), ex);
			}
			parentEditor.getAsset().computeStatus(session);
		}
		tblDeployments.refresh();
		parentEditor.fireAssetModified(false);
		parentEditor.deploymentModified(Collections.singletonList(toUpdate), AssetEvents.ASSETDEPLOYMENT_MODIFIED);
		updateDetailsPane(toUpdate);
	}
	
	private void deleteDisruption(AssetDeploymentDisruption disruption) {
		
		if (!MessageDialog.openQuestion(detailsPane.getShell(), Messages.AssetDeploymentPage_DeleteDisruptionTitle, 
				Messages.AssetDeploymentPage_DeleteMessage)) return;
		
		AssetDeployment deployment = disruption.getAssetDeployment();
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				deployment.getDisruptions().remove(disruption);
				session.saveOrUpdate(deployment);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetDeploymentPage_DisruptionDeleteError + ex.getMessage(), ex);
			}
			parentEditor.getAsset().computeStatus(session);
		}
		tblDeployments.refresh();
		parentEditor.fireAssetModified(false);
		parentEditor.deploymentModified(Collections.singletonList(deployment), AssetEvents.ASSETDEPLOYMENT_MODIFIED);
		updateDetailsPane(deployment);
	}
	
	private void sortDeployments() {
		allDeployments.sort((a,b)->{
			if (a.getDeployment().getEndDate() == null) return -1;
			if (b.getDeployment().getEndDate() == null) return 1;
			return b.getDeployment().getStartDate().compareTo(a.getDeployment().getStartDate());
		});
	}
	
	private Job refreshSummaryStatsJob = new Job(Messages.AssetDeploymentPage_loadinghistoryJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (assetSummaryValues == null) return org.eclipse.core.runtime.Status.OK_STATUS; 
			
			final Map<IAssetSummary, Label> copy = new HashMap<>();
			copy.putAll(assetSummaryValues);
			
			HashMap<Label, String> values = new HashMap<>();
			try(Session s = HibernateManager.openSession()){
				for (Entry<IAssetSummary, Label> item : copy.entrySet()) {
					String result = item.getKey().getSummaryValue(parentEditor.getAsset(), s);
					values.put(item.getValue(), result);
				}			
			}
			Display.getDefault().syncExec(()->{
				for (Entry<Label, String> value : values.entrySet()) {
					String text = value.getValue();
					String tooltip = null;
					if (parentEditor.isDirty()) {
						text = text + "**"; //$NON-NLS-1$
						tooltip = Messages.AssetDeploymentPage_refreshTooltip;
					}
					value.getKey().setText(text);
					value.getKey().setToolTipText(tooltip);
				}

			});
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
	};
	
	Job loadHistoryDataJob = new Job(Messages.AssetDeploymentPage_loadingdataJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			List<AssetDeploymentTableColumn> tableColumns = new ArrayList<>();
			allDeployments = new ArrayList<>();
			Asset asset = parentEditor.getAsset();
			allDeploymentAttributes = new ArrayList<>();
			
			try(Session s = HibernateManager.openSession()){
				
				//compute table columns
				tableColumns.addAll(AssetDeploymentTableColumn.getTableColumns(asset, parentEditor.currentCrs, s));
				
				//get deployment data
				if (asset.getUuid() != null) {
					List<AssetDeployment> temp = QueryFactory.buildQuery(s,AssetDeployment.class, 
							new Object[] {"asset", asset}).list(); //$NON-NLS-1$
					temp.forEach(a->allDeployments.add(new AssetDeploymentWrapper(a)));
				}
				allDeployments.forEach(d->{
					d.getDeployment().getStationLocation().getId();
					d.getDeployment().getStationLocation().getStation().getId();
					for (AssetDeploymentAttributeValue v : d.getDeployment().getAttributeValues()) {
						v.getAttributeValueAsString(Locale.getDefault(), parentEditor.currentCrs);
					}
					d.getDeployment().getDisruptions().size();
				});
				
				if (asset.getUuid() != null) {
					allDeploymentAttributes.addAll(QueryFactory.buildQuery(s, AssetTypeDeploymentAttribute.class, "id.assetType", asset.getAssetType()).list()); //$NON-NLS-1$
				}
				allDeploymentAttributes.forEach(e->e.getAttribute().getUuid());
			}
			//sort data
			sortDeployments();
			
			//update ui
			Display.getDefault().syncExec(()->{
				for(TableColumn tc : tblDeployments.getTable().getColumns()) tc.dispose();
				for (AssetDeploymentTableColumn column : tableColumns) {
					TableViewerColumn c = new TableViewerColumn(tblDeployments, SWT.NONE);
					c.getColumn().setText(column.getColumnName());
					c.setLabelProvider(column);
				}
				tblDeployments.setInput(allDeployments);
				for (TableColumn c : tblDeployments.getTable().getColumns()) {
					c.pack();
					c.setWidth(c.getWidth() + 20);
				}
				
			});
			computeDeploymentStats.schedule();
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
	};
	
	
	Job computeDeploymentStats = new Job(Messages.AssetDeploymentPage_statsJobName) {
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (allDeployments == null) return Status.OK_STATUS;
			final List<AssetDeploymentWrapper> items = new ArrayList<>(allDeployments);
			
			for (AssetDeploymentWrapper d : items) {
				Map<Statistic, Object> stats = StatisticsEngine.INSTANCE
						.computeStatistics(Collections.singleton(StatisticsEngine.Statistic.NUMBER_INCIDENTS), 
								d.getDeployment());
				d.addStatistic(stats);
					
				Display.getDefault().syncExec(()->{
					if (tblDeployments.getTable().isDisposed()) return;
					tblDeployments.refresh(d, true);
				});
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			}
			
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
	};
}
