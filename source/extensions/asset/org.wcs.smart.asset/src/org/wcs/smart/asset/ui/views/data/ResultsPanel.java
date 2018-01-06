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
package org.wcs.smart.asset.ui.views.data;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.importer.ActionableWarning;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.DataDisplaySettings;

/**
 * Part of the data import page that displays the various parts 
 * 
 * @author Emily
 *
 */
public class ResultsPanel {

	private static final String ACTION_MENU_DATA_KEY = "ACTION"; //$NON-NLS-1$
	
	private DataImportPage view;
	private TableViewer tblResults;
	private ImagesTablePanel tblResultsImages;
	private DeletedFilesPanel tblDeletedItems;
	
	private FileDetailsPanel detailsPanel;
	
	public ResultsPanel(Composite parent, DataImportPage view, FormToolkit toolkit) {
		this.view = view;
		createComposite(parent,  toolkit);
	}

	public void setSelection(IStructuredSelection selection) {
		tblResults.setSelection(selection);
	}
	
	public IStructuredSelection getSelection() {
		return tblResults.getStructuredSelection();
	}
	
	public void refresh() {
		tblResults.refresh();
		tblResultsImages.refresh();
		tblDeletedItems.refresh();
		//this works because the images list selection is mapped to the table selection
		detailsPanel.updateFileDetails(tblResults.getStructuredSelection());
	}
	
	public void addSelectionListener(ISelectionChangedListener listener) {
		tblResults.addSelectionChangedListener(listener);
	}
	
	private void createComposite(Composite parent, FormToolkit toolkit) {
		// main section
		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite leftPart = toolkit.createComposite(sash, SWT.NONE);
		leftPart.setLayout(new GridLayout());
		((GridLayout) leftPart.getLayout()).marginWidth = 0;
		((GridLayout) leftPart.getLayout()).marginHeight = 0;

		Composite bottom = toolkit.createComposite(leftPart);
		bottom.setLayout(new GridLayout(4, false));
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout) bottom.getLayout()).marginWidth = 0;
		((GridLayout) bottom.getLayout()).marginHeight = 0;
		
		Hyperlink tblLink = toolkit.createHyperlink(bottom, "Table", SWT.NONE);
		Hyperlink imgsLink = toolkit.createHyperlink(bottom, "Images", SWT.NONE);
		Hyperlink deletedLink = toolkit.createHyperlink(bottom, "Deleted Files", SWT.NONE);
		deletedLink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		// icon size for images table
		Composite iconSizeComp = toolkit.createComposite(bottom);
		iconSizeComp.setLayout(new GridLayout(2, false));
		((GridLayout)iconSizeComp.getLayout()).marginWidth = 0;
		((GridLayout)iconSizeComp.getLayout()).marginHeight = 0;
		iconSizeComp.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		toolkit.createLabel(iconSizeComp, "Icon Size:");
		
		DataDisplaySettings.IconSize defaultSize = DataDisplaySettings.IconSize.MEDIUM;
		try {
			String iconSizePref = AssetPlugIn.getDefault().getPreferenceStore().getString(ImagesTablePanel.ICON_SIZE_PREF);
			if (iconSizePref != null) {
				defaultSize = DataDisplaySettings.IconSize.valueOf(iconSizePref);
			}
		}catch (Exception ex) {}
		
		Button btnIconSize = new Button(iconSizeComp, SWT.ARROW | SWT.DOWN);
		Menu mnuIconSize = new Menu(btnIconSize);
		for(DataDisplaySettings.IconSize s : DataDisplaySettings.IconSize.values()) {
			MenuItem item = new MenuItem(mnuIconSize,SWT.RADIO);
			item.setText(s.getOptionName());
			item.addListener(SWT.Selection, e->{
				if (item.getSelection()) tblResultsImages.setThumbnailSize(s);	
			});
			if (s == defaultSize) item.setSelection(true);
		}
		btnIconSize.addListener(SWT.Selection, e->{
			mnuIconSize.setVisible(true);
		});
		iconSizeComp.setVisible(false);
		
		Composite stackPanel = toolkit.createComposite(leftPart, SWT.NONE);
		stackPanel.setLayout(new StackLayout());
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tblResults = new TableViewer(stackPanel, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		tblResults.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblResults.getTable().setHeaderVisible(true);
		tblResults.getTable().setLinesVisible(true);

		for (ResultsColumn c : ResultsColumn.values()) {
			TableViewerColumn column = new TableViewerColumn(tblResults, SWT.NONE);
			column.getColumn().setResizable(true);
			column.getColumn().setText(c.guiName);
			column.getColumn().setWidth(c.getWidth());
			if (c == ResultsColumn.WAYPOINT) {
				column.getColumn().setToolTipText("* indicates user defined group");
			}
			column.setLabelProvider(c.getLabelProvider(view.getRowColors()));
		}
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		tblResults.setInput(view.getProcessor().getFiles());
		tblResults.addSelectionChangedListener(e -> detailsPanel.updateFileDetails(tblResults.getStructuredSelection()));

		TableColumn dateColumn = tblResults.getTable().getColumn(ResultsColumn.DATE.ordinal());
		dateColumn.pack();
		if (dateColumn.getWidth() < ResultsColumn.DATE.getWidth()) dateColumn.setWidth(ResultsColumn.DATE.getWidth());
		tblResults.refresh();

		Menu mnu = new Menu(tblResults.getControl());
		tblResults.getControl().setMenu(mnu);

		MenuItem mnuSetAsset = new MenuItem(mnu, SWT.CASCADE);
		mnuSetAsset.setText("Set Asset ...");
		Menu assetMenu = new Menu(mnuSetAsset);
		mnuSetAsset.setMenu(assetMenu);
		assetMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuShown(MenuEvent e) {
				for (MenuItem mi : assetMenu.getItems())
					mi.dispose();
				for (Asset a : view.getSelectedAssets()) {
					MenuItem otherAsset = new MenuItem(assetMenu, SWT.PUSH);
					otherAsset.setText(a.getId());
					otherAsset.addListener(SWT.Selection, evt -> view.setAsset(a));
				}
				if (!view.getSelectedAssets().isEmpty())
					new MenuItem(assetMenu, SWT.SEPARATOR);

				MenuItem otherAsset = new MenuItem(assetMenu, SWT.PUSH);
				otherAsset.setText("Other Asset....");
				otherAsset.addListener(SWT.Selection, evt -> view.setAsset(null));
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});

		MenuItem mnuSetLocation = new MenuItem(mnu, SWT.CASCADE);
		mnuSetLocation.setText("Set Station/Location ...");
		Menu locationMenu = new Menu(mnuSetAsset);
		mnuSetLocation.setMenu(locationMenu);
		locationMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuShown(MenuEvent e) {
				for (MenuItem mi : locationMenu.getItems())
					mi.dispose();
				for (AssetStationLocation a : view.getSelectedLocations()) {
					MenuItem otherAsset = new MenuItem(locationMenu, SWT.PUSH);
					otherAsset.setText(MessageFormat.format("{0} [{1}]", a.getId(), a.getStation().getId()));
					otherAsset.addListener(SWT.Selection, evt -> view.setLocation(a));
				}
				if (!view.getSelectedLocations().isEmpty())
					new MenuItem(locationMenu, SWT.SEPARATOR);

				MenuItem otherAsset = new MenuItem(locationMenu, SWT.PUSH);
				otherAsset.setText("Other Location....");
				otherAsset.addListener(SWT.Selection, evt -> view.setLocation(null));
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});

		MenuItem mnuSetDate = new MenuItem(mnu, SWT.PUSH);
		mnuSetDate.setText("Set Date/Time...");
		mnuSetDate.addListener(SWT.Selection, e -> view.setDateTime());

		new MenuItem(mnu, SWT.SEPARATOR);

		MenuItem mnuGroup = new MenuItem(mnu, SWT.PUSH);
		mnuGroup.setText("Create Custom Incident Group...");
		mnuGroup.addListener(SWT.Selection, e -> view.groupSelected());

		MenuItem mnuRemoveGroup = new MenuItem(mnu, SWT.PUSH);
		mnuRemoveGroup.setText("Remove Custom Incident Group...");
		mnuRemoveGroup.addListener(SWT.Selection, e -> view.ungroupSelected());

		new MenuItem(mnu, SWT.SEPARATOR);

		MenuItem mnuSaveFile = new MenuItem(mnu, SWT.PUSH);
		mnuSaveFile.setText("Save");
		mnuSaveFile.addListener(SWT.Selection, e -> {
			List<FileProxy> toSave = new ArrayList<>();
			for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object x = (FileProxy) iterator.next();
				if (x instanceof FileProxy) {
					toSave.add((FileProxy) x);
				}
			}
			view.save(toSave);
		});

		new MenuItem(mnu, SWT.SEPARATOR);

		MenuItem mnuRemoveFile = new MenuItem(mnu, SWT.PUSH);
		mnuRemoveFile.setText("Remove File");
		mnuRemoveFile.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuRemoveFile.addListener(SWT.Selection, e -> view.removeFiles());

		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mnuSetAsset.setEnabled(!tblResults.getSelection().isEmpty());
				mnuSetLocation.setEnabled(!tblResults.getSelection().isEmpty());
				mnuSetDate.setEnabled(!tblResults.getSelection().isEmpty());
				mnuRemoveFile.setEnabled(!tblResults.getSelection().isEmpty());
				mnuGroup.setEnabled(!tblResults.getSelection().isEmpty());
				mnuSaveFile.setEnabled(!tblResults.getSelection().isEmpty());
				// only save if all items are valid
				boolean ok = true;
				boolean canUngroup = false;
				for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
					Object x = (FileProxy) iterator.next();
					if (x instanceof FileProxy) {
						if (!((FileProxy) x).isValid())
							ok = false;
						if (((FileProxy) x).isFixed())
							canUngroup = true;
					}
				}
				if (mnuSaveFile.isEnabled()) mnuSaveFile.setEnabled(ok);
				if (mnuGroup.isEnabled()) mnuGroup.setEnabled(ok);
				mnuRemoveGroup.setEnabled(ok && canUngroup);

				for (MenuItem i : mnu.getItems()) {
					Boolean x = (Boolean) i.getData(ACTION_MENU_DATA_KEY);
					if (x != null && x)
						i.dispose();
				}

				if (tblResults.getStructuredSelection().size() == 1) {
					FileProxy proxy = (FileProxy) tblResults.getStructuredSelection().getFirstElement();
					int sep = 0;
					for (ActionableWarning aw : proxy.getWarnings()) {
						ImportAction ia = ActionManager.findAction(aw, view.getContext());
						if (ia == null)
							continue;
						MenuItem mi = new MenuItem(mnu, SWT.PUSH, 0);
						mi.setText(ia.getMenuLabel());
						mi.setData(ACTION_MENU_DATA_KEY, true);
						mi.addListener(SWT.Selection, er -> {
							if (ia.preformAction(view.getProcessor(), proxy)) {
								view.refreshProxies();
							}
						});
						sep++;
					}
					if (sep > 0) {
						MenuItem mi = new MenuItem(mnu, SWT.SEPARATOR, sep);
						mi.setData(ACTION_MENU_DATA_KEY, true);
					}

				}
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});

		tblResultsImages = new ImagesTablePanel(stackPanel, view, toolkit);

		
		tblDeletedItems = new DeletedFilesPanel(stackPanel, view, toolkit);
		tblDeletedItems.addSelectionChangedListener(e->detailsPanel.updateFileDetails(tblDeletedItems.getSelection()));
		
		((StackLayout) stackPanel.getLayout()).topControl = tblResults.getControl();

		FontData fd = tblLink.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(tblLink.getDisplay(), fd);
		tblLink.addDisposeListener(e -> boldFont.dispose());
		Font normalFont = tblLink.getFont();
		tblLink.setFont(boldFont);
		tblLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout) stackPanel.getLayout()).topControl = tblResults.getControl();
				stackPanel.layout();

				tblLink.setFont(boldFont);
				imgsLink.setFont(normalFont);
				deletedLink.setFont(normalFont);
				tblLink.getParent().layout();
				iconSizeComp.setVisible(false);
				detailsPanel.updateFileDetails(tblResults.getStructuredSelection());
			}
		});

		imgsLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout) stackPanel.getLayout()).topControl = tblResultsImages.getControl();
				stackPanel.layout();

				tblLink.setFont(normalFont);
				imgsLink.setFont(boldFont);
				deletedLink.setFont(normalFont);
				tblLink.getParent().layout();
				iconSizeComp.setVisible(true);
				detailsPanel.updateFileDetails(tblResults.getStructuredSelection());
			}
		});
		deletedLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout) stackPanel.getLayout()).topControl = tblDeletedItems.getControl();
				stackPanel.layout();

				tblLink.setFont(normalFont);
				imgsLink.setFont(normalFont);
				deletedLink.setFont(boldFont);
				tblLink.getParent().layout();
				iconSizeComp.setVisible(false);
				detailsPanel.updateFileDetails(null);
			}
		});

		Composite rightPart = toolkit.createComposite(sash, SWT.NONE);
		rightPart.setLayout(new GridLayout());
		((GridLayout) rightPart.getLayout()).marginWidth = 0;
		((GridLayout) rightPart.getLayout()).marginHeight = 0;

		detailsPanel = new FileDetailsPanel(rightPart, view, toolkit);
		
		sash.setWeights(new int[] { 7, 4 });
		parent.layout(true);
	}
	
	private enum ResultsColumn{
		STATUS("Status"),
		FILE("File"),
		DATE("Date"),
		ASSET("Asset"),
		LOCATION("Station Location"),
		STATION("Station"),
		WAYPOINT("Incident Group");
		
		public String guiName;
		
		private ResultsColumn(String name) {
			this.guiName = name;
		}
		
		public String getValue(FileProxy proxy) {
			switch(this) {
			case ASSET:
				return proxy.getAsset() == null ? "" : proxy.getAsset().getId();
			case DATE:
				return proxy.getImageDate() == null ? "" : DateFormat.getDateTimeInstance().format(proxy.getImageDate());
			case FILE:
				return proxy.getFile().getFileName().toString();
			case LOCATION:
				return proxy.getStationLocation() == null ? "" : proxy.getStationLocation().getId();
			case WAYPOINT:
				if (proxy.getIncidentGroup() == null) return "";
				return proxy.getIncidentGroup().toString() + (proxy.isFixed() ? "*" : "");
			case STATION:
				return proxy.getStation() == null ? "" : proxy.getStation().getId();
			case STATUS:
				return proxy.isValid() ? "COMPLETE" : "INCOMPLETE";			
			}
			return "";
		}
		
		public ColumnLabelProvider getLabelProvider(Color[] rowColors) {
			return new ColumnLabelProvider() {
				@Override
				public Image getImage(Object element) {
					if (ResultsColumn.this == STATUS && element instanceof FileProxy) {
						if (((FileProxy) element).isValid()) return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_IMPORT_COMPLETE);
						return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_IMPORT_INCOMPLETE);
					}
					return null;
				}
				@Override
				public String getText(Object element) {
					if (element instanceof FileProxy) return getValue((FileProxy)element);
					return super.getText(element);
				}
				
				@Override
				public Color getBackground(Object element) {
					if (element instanceof FileProxy){
						if (((FileProxy) element).getIncidentGroup() == null) return null;
						int colorIndex = ((FileProxy) element).getIncidentGroup() % rowColors.length;
						return rowColors[colorIndex];
					}
					return null;
				}
			};
		}
		
		public int getWidth() {
			if (this == STATUS) return 22;
			return 100;
		}
	}
}
