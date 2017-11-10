package org.wcs.smart.asset.ui.views.data;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.views.data.StationAssetSelectionDialog.Type;
import org.wcs.smart.common.control.ProgressAreaComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public class DataImporterView extends EditorPart{

	public static final String ID = "org.wcs.smart.asset.ui.views.data.importer";
	
	private FileProcessor processor;
	
	private boolean isDirty;
	
	private FormToolkit toolkit;
	
	private Composite details; 
	private TableViewer tblResults;
	
	private Composite fileDetailsComposite;

	private Composite statusSection;

	private IEclipseContext context;
	private Label fileCnt;
	
	private List<Asset> selectedAssets = new ArrayList<>();
	private List<AssetStationLocation> selectedLocations = new ArrayList<>();
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		
		
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		context = (IEclipseContext) getSite().getService(IEclipseContext.class);
		processor = new FileProcessor(SmartDB.getCurrentConservationArea(), ((DataImporterInput)input).getFiles());
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	public void setDirty(boolean isDirty) {
		boolean fire = isDirty != this.isDirty;
		this.isDirty = isDirty;
		if (fire) firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form mainform = toolkit.createForm(parent);
		mainform.setText("Import Asset Data");
		mainform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainform.getBody().setLayout(new GridLayout());
		
		Composite main = toolkit.createComposite(mainform.getBody());
		
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		fileCnt = toolkit.createLabel(main, "");
		fileCnt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		details = toolkit.createComposite(main);
		details.setLayout(new GridLayout());
		details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)details.getLayout()).marginWidth = 0;
		((GridLayout)details.getLayout()).marginHeight = 0;
		
		updateFileCount();
		createProcessComposite(false);
	}
	
	private void updateFileCount() {
		fileCnt.setText(MessageFormat.format("Number of Files: {0}",processor.getFiles().size()));
	}
	
	private void createProcessComposite(boolean isCancelled) {
		for (Control c : details.getChildren()) c.dispose();
		
		if (isCancelled) {
			toolkit.createLabel(details, "Processing cancelled");
		}
		
		Button btnProcessFiles = toolkit.createButton(details, "Process Files",  SWT.PUSH);
		btnProcessFiles.addListener(SWT.Selection, e->processFiles());
		details.layout(true);
		
		setDirty(false);
	}

	private void updateStatus() {
		boolean isValid = processor.isValid();
		Boolean lastState = ((Boolean)statusSection.getData("LAST_STATUS"));
		//status not changed
		if ( lastState != null && lastState == isValid ) return;
		
		for (Control c : statusSection.getChildren()) c.dispose();
		
		if (isValid) {
			Button btnSave = toolkit.createButton(statusSection, "Load", SWT.PUSH);
			Label l = toolkit.createLabel(statusSection, "Data processed and ready for loading");
			l.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		}else {
			Label statusImage = toolkit.createLabel(statusSection, "");
			statusImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
			
			Label statusMsg = toolkit.createLabel(statusSection, "Data processing not complete.  You must ensure row in the table below are complete before you can continue.");
			statusMsg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		statusSection.getParent().layout(true);
		statusSection.setData("LAST_STATUS", isValid);
	}
	
	private void createFileSummary() {
		for (Control c : details.getChildren()) c.dispose();
		
		// status section
		statusSection = toolkit.createComposite(details, SWT.BORDER);
		statusSection.setLayout(new GridLayout(2, false));
		statusSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		//main section
		SashForm sash = new SashForm(details, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = toolkit.createComposite(sash, SWT.NONE);
		leftPart.setLayout(new GridLayout());
		((GridLayout)leftPart.getLayout()).marginWidth = 0;
		((GridLayout)leftPart.getLayout()).marginHeight = 0;
		
		tblResults = new TableViewer(leftPart, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		tblResults.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblResults.getTable().setHeaderVisible(true);
		tblResults.getTable().setLinesVisible(true);
		
		for (ResultsColumn c : ResultsColumn.values()) {
			TableViewerColumn column = new TableViewerColumn(tblResults, SWT.NONE);
			column.getColumn().setResizable(true);
			column.getColumn().setText(c.guiName);
			column.getColumn().setWidth(c.getWidth());
			column.setLabelProvider(c.getLabelProvider());
		}
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		tblResults.setInput(processor.getFileDetails());
		tblResults.addSelectionChangedListener(e->updateFileDetils());
		
		Hyperlink reprocess = toolkit.createHyperlink(leftPart, "Reprocess files", SWT.NONE);
		reprocess.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkExited(HyperlinkEvent e) {
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				processFiles();
			}
		});
		
		Menu mnu = new Menu(tblResults.getControl());
		tblResults.getControl().setMenu(mnu);
		
		MenuItem mnuSetAsset = new MenuItem(mnu, SWT.CASCADE);
		mnuSetAsset.setText("Set Asset ...");
		Menu assetMenu = new Menu(mnuSetAsset);
		mnuSetAsset.setMenu(assetMenu);
		assetMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				// TODO Auto-generated method stub
				for (MenuItem mi : assetMenu.getItems()) mi.dispose();
				for(Asset a : selectedAssets) {
					MenuItem otherAsset = new MenuItem(assetMenu, SWT.PUSH);
					otherAsset.setText(a.getId());
					otherAsset.addListener(SWT.Selection, evt->setAsset(a));
				}
				if (!selectedAssets.isEmpty()) new MenuItem(assetMenu, SWT.SEPARATOR);
				
				MenuItem otherAsset = new MenuItem(assetMenu, SWT.PUSH);
				otherAsset.setText("Other Asset....");
				otherAsset.addListener(SWT.Selection, evt->setAsset(null));
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
				// TODO Auto-generated method stub
				for (MenuItem mi : locationMenu.getItems()) mi.dispose();
				for(AssetStationLocation a : selectedLocations) {
					MenuItem otherAsset = new MenuItem(locationMenu, SWT.PUSH);
					otherAsset.setText(MessageFormat.format("{0} [{1}]", a.getId(),a.getStation().getId()));
					otherAsset.addListener(SWT.Selection, evt->setLocation(a));
				}
				if (!selectedLocations.isEmpty()) new MenuItem(locationMenu, SWT.SEPARATOR);
				
				MenuItem otherAsset = new MenuItem(locationMenu, SWT.PUSH);
				otherAsset.setText("Other Location....");
				otherAsset.addListener(SWT.Selection, evt->setLocation(null));
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		
		
		MenuItem mnuSetDate = new MenuItem(mnu, SWT.PUSH);
		mnuSetDate.setText("Set Date/Time...");
		mnuSetDate.addListener(SWT.Selection, e->setDateTime());
		
		MenuItem mnuRemoveFile = new MenuItem(mnu, SWT.PUSH);
		mnuRemoveFile.setText("Remove File");
		mnuRemoveFile.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuRemoveFile.addListener(SWT.Selection, e->removeFile());
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mnuSetAsset.setEnabled(!tblResults.getSelection().isEmpty());
				mnuSetLocation.setEnabled(!tblResults.getSelection().isEmpty());
				mnuSetDate.setEnabled(!tblResults.getSelection().isEmpty());
				mnuRemoveFile.setEnabled(!tblResults.getSelection().isEmpty());

			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
				
		Composite rightPart = toolkit.createComposite(sash, SWT.NONE);
		rightPart.setLayout(new GridLayout());
		((GridLayout)rightPart.getLayout()).marginWidth = 0;
		((GridLayout)rightPart.getLayout()).marginHeight = 0;
		fileDetailsComposite = toolkit.createComposite(rightPart, SWT.BORDER);
		fileDetailsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sash.setWeights(new int[] {7,3});
		details.layout(true);
		
		updateStatus();
	}
	
	private List<FileProxy> getSelection(){
		List<FileProxy> proxies = new ArrayList<>();
		IStructuredSelection selection = (IStructuredSelection) tblResults.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof FileProxy) {
				proxies.add((FileProxy)type);
			}
		}
		return proxies;
	}
	
	/**
	 * Updates the selected objects to the given asset.  If asset is null user
	 * will be prompted to pick and asset
	 * @param asset
	 */
	private void setAsset( Asset asset ) {
		List<FileProxy> proxies = getSelection();
		int cnt = 0;
		for (FileProxy p : proxies) {
			if (p.getAsset() != null)cnt ++;
		}
		if (cnt > 0) {
			boolean n = MessageDialog.openQuestion(getSite().getShell(), "Warning", 
				MessageFormat.format("{0} of the {1} selected files already have assets associated with them.  These will be overwritten.  Are you sure you want to continue?", cnt, proxies.size()));
			if (!n) return;
		}
		
		if (asset == null) {
			StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(getSite().getShell(), Type.ASSET);
			ContextInjectionFactory.inject(dialog, context);
			if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
			asset = dialog.getSelectedAsset();
		}
		addToQueue(selectedAssets, asset);
		final Asset newAsset = asset;
		proxies.forEach(proxy->{
				proxy.setAsset(newAsset);
		});
		refreshProxies(proxies);
	}
	
	private <T> void addToQueue(List<T> items, T item) {
		if (items.contains(item)) return;
		items.add(0, item);
		while(items.size() > 5) items.remove(items.size() - 1);
	}
	
	/**
	 * Updates the selected objects to the given station location .  If the location is null the user
	 * will be prompted to pick the location
	 * 
	 * @param location
	 */
	private void setLocation( AssetStationLocation location ) {
		List<FileProxy> proxies = getSelection();
		int cnt = 0;
		for (FileProxy p : proxies) {
			if (p.getStationLocation() != null) cnt++;
		}
		if (cnt > 0) {
			boolean n = MessageDialog.openQuestion(getSite().getShell(), "Warning", 
				MessageFormat.format("{0} of the {1} selected files already have station locations associated with them.  These will be replaced overwritten if you process.  Are you sure you want to continue?", cnt, proxies.size()));
			if (!n) return;
		}
		if (location == null) {
			StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(getSite().getShell(), Type.LOCATION);
			ContextInjectionFactory.inject(dialog, context);
			if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
			location = dialog.getSelectedLocation();
		}
		final AssetStationLocation newLocation = location;
		addToQueue(selectedLocations, newLocation);
		proxies.forEach(proxy->{
				proxy.setStationLocation(newLocation);
		});
		refreshProxies(proxies);
	}
	
	private void setDateTime() {
		List<FileProxy> proxies = getSelection();
		int cnt = 0;
		for (FileProxy p : proxies) {
			if (p.getImageDate() != null) {
				cnt ++;
			}
		}
		if (cnt > 0) {
			boolean n = MessageDialog.openQuestion(getSite().getShell(), "Warning", 
				MessageFormat.format("{0} of the {1} selected files already have dates associated with them.  These dates will be replaced with new selected date.  Are you sure you want to continue?", cnt, proxies.size()));
			if (!n) return;
		}
		
		StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(getSite().getShell(), Type.DATE);
		ContextInjectionFactory.inject(dialog, context);
		if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
		
		proxies.forEach(p->p.setImageDate(dialog.getSelectedDate()));
		refreshProxies(proxies);
	}
	
	private void removeFile() {
		boolean n = MessageDialog.openQuestion(getSite().getShell(), "Warning", 
			"Are you sure you want to remove the selected file?  When removed, the files and associated observations will not be imported into SMART.");
		if (!n) return;
		
		List<FileProxy> files = getSelection();
		files.forEach(f->processor.removeFile(f));
		refreshProxies(Collections.emptyList());
	}
	
	private void refreshProxies(final List<FileProxy> proxies) {
		Job refreshJob = new Job("refresh table") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try(Session session = HibernateManager.openSession()){
					proxies.forEach(p->p.updateAssetDeployment(session));
				}
				Display.getDefault().syncExec(()->{
					tblResults.refresh();
					updateStatus();
					updateFileDetils();
					updateFileCount();
				});
				return Status.OK_STATUS;
			}
		};
		refreshJob.schedule();
		
	}
	
	private void updateFileDetils() {
		for (Control c : fileDetailsComposite.getChildren()) c.dispose();
		
		fileDetailsComposite.setLayout(new GridLayout());
		
		Object selection = ((IStructuredSelection)tblResults.getSelection()).getFirstElement();
		
		if (selection == null) return;
		if (!(selection instanceof FileProxy)) return;
		FileProxy proxy = (FileProxy)selection;
		
		Label l = toolkit.createLabel(fileDetailsComposite, proxy.getFile().getFileName().toString());
		toolkit.createLabel(fileDetailsComposite, "Status: " + (proxy.isValid() ? "COMPLETE" : "INCOMPLETE"));
		
		SashForm detailsSash = new SashForm(fileDetailsComposite, SWT.VERTICAL);
		detailsSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		detailsSash.addDisposeListener(e->fileDetailsComposite.setData("SASHWEIGHTS", detailsSash.getWeights()));
		ScrolledComposite scroll = new ScrolledComposite(detailsSash, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		toolkit.adapt(scroll);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		
		Composite scrollComp = toolkit.createComposite(scroll);
		scroll.setContent(scrollComp);
		scrollComp.setLayout(new GridLayout(2, false));
		
		toolkit.createLabel(scrollComp, "Status Details:");
		toolkit.createLabel(scrollComp, proxy.validMessage());
		
		toolkit.createLabel(scrollComp, "Date/Time:");
		toolkit.createLabel(scrollComp, proxy.getImageDate() == null ? "" : DateFormat.getDateTimeInstance().format(proxy.getImageDate()));
		
		toolkit.createLabel(scrollComp, "Asset:");
		toolkit.createLabel(scrollComp, proxy.getAsset() == null ? "" : proxy.getAsset().getId());
		
		toolkit.createLabel(scrollComp, "Station:");
		toolkit.createLabel(scrollComp, proxy.getStation() == null ? "" : proxy.getStation().getId());
		
		toolkit.createLabel(scrollComp, "Station Location:");
		toolkit.createLabel(scrollComp, proxy.getStationLocation() == null ? "" : proxy.getStationLocation().getId());
		
		toolkit.createLabel(scrollComp, "Longitude:");
		toolkit.createLabel(scrollComp, proxy.getX() == null ? "" : String.valueOf(proxy.getX()));
		toolkit.createLabel(scrollComp, "Latitiude:");
		toolkit.createLabel(scrollComp, proxy.getY() == null ? "" : String.valueOf(proxy.getY()));
		
		l = toolkit.createLabel(scrollComp, "", SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		//TODO: fix this
		Job j2 = new Job("read exif metadata") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				HashMap<String, String> exif = FileMetadataReader.readExifMetadata(proxy.getFile());
				
				Display.getDefault().syncExec(()->{
					if (scrollComp.isDisposed()) return;
					if (exif == null) {
						Label l = toolkit.createLabel(scrollComp, "Error Reading EXIF Metadata");
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
						return;
					}
					for (Entry<String,String> item : exif.entrySet()) {
						Label l = toolkit.createLabel(scrollComp, item.getKey());
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
						
						l = toolkit.createLabel(scrollComp, item.getValue());
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					}
					
					scroll.setMinSize(scrollComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

				});
				return Status.OK_STATUS;
			}
			
		};
		j2.schedule();
		Canvas c = new Canvas(detailsSash,SWT.BORDER);
		Job j = new Job("loading image job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {			
				try {
					Image img = new Image(c.getDisplay(), proxy.getFile().toString());
					Display.getDefault().syncExec(()->{
						if (c.isDisposed()) {
							img.dispose();
							return;
						}
						c.addListener(SWT.Dispose, e->img.dispose());
						c.setData("IMAGE", img);
						c.redraw();
					});
				}catch (Exception ex) {
					//invalid format TODO:
				}
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
			
		c.addListener(SWT.Paint, e->{
			Image img = (Image)c.getData("IMAGE");
			if (img == null) return;
			
			Rectangle bounds = img.getBounds();
			Rectangle cbounds = c.getBounds();	
			// scale image
			int x = 0, y = 0, width = 0, height = 0;
			if (cbounds.width > cbounds.height) {
				height = cbounds.height;
				width = bounds.width * height / bounds.height;
				x = (cbounds.width - width) / 2;
			} else {
				width = cbounds.width;
				height = bounds.height * width / bounds.width;
				y = (cbounds.height - height) / 2;
			}
			e.gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, x, y, width, height);
		});
		
		int[] weights = (int[])fileDetailsComposite.getData("SASHWEIGHTS");
		if (weights != null) { 
			detailsSash.setWeights(weights);
		}else {
			detailsSash.setWeights(new int[] {3,2});
		}
		
		fileDetailsComposite.layout(true);
		scroll.setMinSize(scrollComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	private boolean wasProcessed = false;
	
	private void processFiles() {
		if (wasProcessed) {
			//TODO: validate with user
		}
		wasProcessed = true;
		for (Control c : details.getChildren()) c.dispose();
		
		ProgressAreaComposite progressComp = new ProgressAreaComposite(details);
		final IProgressMonitor pmonitor = progressComp.createProgressMonitor();
		details.layout(true);
		
		Job processingJob = new Job("processing asset files") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				processor.processFiles(pmonitor);
				if (monitor.isCanceled()) {
					Display.getDefault().syncExec(()->{createProcessComposite(true);});
					return Status.CANCEL_STATUS;
				}
				Display.getDefault().syncExec(()->{createFileSummary();});

				return Status.OK_STATUS;
			}
			
		};
		processingJob.schedule();
	}
	
	
	
	@Override
	public void setFocus() {
	}

	
	private enum ResultsColumn{
		STATUS("Status"),
//		ERROR("Error"),
		FILE("File"),
		DATE("Date"),
		ASSET("Asset"),
		LOCATION("Station Location"),
		STATION("Station"),
		OBSERVATIONS("Observation");
		
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
//			case ERROR:
//				return proxy.getProcessingException() == null ? "" : proxy.getProcessingException().getMessage();
			case FILE:
				return proxy.getFile().getFileName().toString();
			case LOCATION:
				return proxy.getStationLocation() == null ? "" : proxy.getStationLocation().getId();
			case OBSERVATIONS:
				return "TODO";
			case STATION:
				return proxy.getStation() == null ? "" : proxy.getStation().getId();
			case STATUS:
				return proxy.isValid() ? "COMPLETE" : "INCOMPLETE";			
			}
			return "";
		}
		
		public ColumnLabelProvider getLabelProvider() {
			if (this == STATUS) {
				return new ColumnLabelProvider() {
					@Override
					public Image getImage(Object element) {
						if (element instanceof FileProxy) {
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
				};	
			}
				
			return new ColumnLabelProvider() {
				public String getText(Object element) {
					if (element instanceof FileProxy) return getValue((FileProxy)element);
					return super.getText(element);
				}
			};
		}
		
		public int getWidth() {
			if (this == STATUS) return 22;
			return 100;
		}
	}
}
