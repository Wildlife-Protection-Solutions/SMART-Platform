package org.wcs.smart.asset.ui.views.data;

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
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.importer.ActionableWarning;
import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.asset.ui.views.data.StationAssetSelectionDialog.Type;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.ProgressAreaComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;

public class DataImporterView extends EditorPart{

	public static final String ID = "org.wcs.smart.asset.ui.views.data.importer";
	
	private FileProcessor processor;
	
	private boolean isDirty;
	
	private FormToolkit toolkit;
	
	private Composite details; 
	private TableViewer tblResults;
	
	private Composite fileDetailsComposite;
	private TableViewer tblExif; 
	private Label lblDetailsFileName; 
	private Label lblDetailsStatus ;
	private Canvas imageCanvas;
	private Composite statusSection;
	private Composite proxyDetailsComp; 
	private IEclipseContext context;
	private Label fileCnt;
	
	private List<Asset> selectedAssets = new ArrayList<>();
	private List<AssetStationLocation> selectedLocations = new ArrayList<>();
	
	private boolean wasProcessed = false;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		
		
	}

	@Override
	public void doSaveAs() {
	}

	
	private void save(List<FileProxy> items) {
		Set<AssetStation> modifiedStations = new HashSet<>();
		Set<Asset> modifiedAssets = new HashSet<>();
		
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				int i = 0; 
				for (FileProxy p : items) {
					System.out.println(i + " / " + items.size());
					i++;
					
					boolean isNewStation = p.getStation().getUuid() == null;
					boolean isNewLocation = p.getStationLocation().getUuid() == null;
					
					if (isNewStation) {
						p.getStation().setId(generateStationId(session));
						if (p.getStation().getX() == null) p.getStation().setX(p.getX());
						if (p.getStation().getY() == null) p.getStation().setY(p.getY());
						session.save(p.getStation());
					}
					
					if (isNewLocation) {
						p.getStationLocation().setId(generateLocationId(p.getStation(), session));
						if (p.getStationLocation().getX() == null) p.getStationLocation().setX(p.getX());
						if (p.getStationLocation().getY() == null) p.getStationLocation().setY(p.getY());
						session.save(p.getStationLocation());
					}
					session.flush();
					
					Waypoint wp = new Waypoint();
					wp.setConservationArea(SmartDB.getCurrentConservationArea());
					wp.setDateTime(p.getImageDate());
					wp.setId(1);
					wp.setSourceId(AssetWaypointSource.KEY);
					wp.setX(p.getX());
					wp.setY(p.getY());
					wp.setAttachments(new ArrayList<>());
					
					WaypointAttachment wa = new WaypointAttachment();
					wa.setWaypoint(wp);
					wa.setCopyFromLocation(p.getFile().toFile());
					wa.setFilename(p.getFile().getFileName().toString());
					wp.getAttachments().add(wa);
					wp.setObservations(new ArrayList<>());
					
					for (WaypointObservation wo : p.getObservations()) {
						wo.setWaypoint(wp);
						wp.getObservations().add(wo);
					}
					
					session.save(wp);
					session.flush();
					
					AssetDeployment d = processor.findAssetDeployment(wp, p.getAsset(), p.getStationLocation(), session);
					if (d.getUuid() == null) {
						session.save(d);
					}
					session.flush();
					
					AssetWaypoint aw = new AssetWaypoint();
					aw.setWaypoint(wp);
					aw.setAssetDeployment(d);
					if (d.getAssetWaypoints() == null) d.setAssetWaypoints(new ArrayList<>());
					d.getAssetWaypoints().add(aw);
					session.save(aw);
					
					//TODO: verify we do not have overlapping deployments
					if (d.getEndDate() == null) {
						//ensure we have no other deployments that also have no end date
					}else {
						//ensure there are no other deployments whose start date is before this end date
					}
					
					//ensure there are no other deployments whose end date is before this start date
					
					session.flush();
					
					modifiedStations.add(p.getStation());
					modifiedAssets.add(d.getAsset());
				}
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				AssetPlugIn.displayLog("Error saving items: {0}" + ex.getMessage(), ex);
				return;
			}
		}
		items.forEach(e->processor.removeFile(e));
		
		//clear deployments & recompute deployments and refresh table
		processor.getFileDetails().forEach(e->e.setAsset(e.getAsset()));
		refreshProxies(processor.getFileDetails());

		//fire events
		context.get(IEventBroker.class).post(AssetEvents.ASSET_MODIFIED, modifiedAssets);
		context.get(IEventBroker.class).post(AssetEvents.ASSETSTATION_MODIFIED, modifiedStations);
	}
	
	private String generateStationId(Session session) {
		int cnt = 1;
		while(true) {
			String id = "Station " + cnt;
			String query =  "SELECT count(*) FROM AssetStation where LOWER(id) = :id AND conservationArea = :ca ";
			Long stncnt = (Long) session.createQuery(query)
				.setParameter("id",  id.toLowerCase())
				.setParameter("ca", SmartDB.getCurrentConservationArea())
				.uniqueResult();
			
			if (stncnt == 0) return id;
			cnt++;
		}
	}
	
	private String generateLocationId(AssetStation station, Session session) {
		int cnt = 1;
		while(true) {
			String id = station.getId() + " - " + cnt;
			String query =  "SELECT count(*) FROM AssetStationLocation where LOWER(id) = :id AND station.conservationArea = :ca ";
			Long stncnt = (Long) session.createQuery(query)
				.setParameter("id",  id.toLowerCase())
				.setParameter("ca", SmartDB.getCurrentConservationArea())
				.uniqueResult();
			
			if (stncnt == 0) return id;
			cnt++;
		}
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
		tblResults.addSelectionChangedListener(e->updateFileDetails());
		
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
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem mnuSaveFile = new MenuItem(mnu, SWT.PUSH);
		mnuSaveFile.setText("Save");
		mnuSaveFile.addListener(SWT.Selection, e->{
			List<FileProxy> toSave = new ArrayList<>();
			for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object x = (FileProxy) iterator.next();
				if (x instanceof FileProxy) {
					toSave.add((FileProxy) x);
				}
			}
			save(toSave);
		});
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
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
				
				//only save if all items are valid
				boolean ok = true;
				for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
					Object x = (FileProxy) iterator.next();
					if (x instanceof FileProxy) {
						if (!((FileProxy) x).isValid()) {
							ok = false;
							break;
						}
					}
				}
				mnuSaveFile.setEnabled(ok);
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
		fileDetailsComposite.setLayout(new GridLayout());
		
		sash.setWeights(new int[] {7,4});
		details.layout(true);
		
		createDetailsComposite();
		updateStatus();
	}
	
	
	private void createDetailsComposite() {
		Composite top = toolkit.createComposite(fileDetailsComposite);
		top.setLayout(new GridLayout(2, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lblDetailsStatus = toolkit.createLabel(top, "");
		lblDetailsFileName = toolkit.createLabel(top, "");
		lblDetailsFileName .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SashForm detailsSash = new SashForm(fileDetailsComposite, SWT.VERTICAL);
		detailsSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite infoComposite = toolkit.createComposite(detailsSash, SWT.NONE);
		infoComposite.setLayout(new GridLayout());
		infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)infoComposite.getLayout()).marginWidth = 0;
		((GridLayout)infoComposite.getLayout()).marginHeight = 0;
		
		Composite header = toolkit.createComposite(infoComposite);
		header.setLayout(new GridLayout(3, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		
		
		Hyperlink lnkDetails = toolkit.createHyperlink(header, "Details", SWT.NONE);
		Hyperlink lnkExif = toolkit.createHyperlink(header, "EXIF Metadata", SWT.NONE);
		Hyperlink lnkXmp = toolkit.createHyperlink(header, "XMP Metadata", SWT.NONE);
		
		Composite stackComposite = toolkit.createComposite(infoComposite, SWT.BORDER);
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackComposite.setLayout(new StackLayout());
		
		proxyDetailsComp = toolkit.createComposite(stackComposite);
		proxyDetailsComp.setLayout(new GridLayout());
		((GridLayout)proxyDetailsComp.getLayout()).marginWidth = 0;
		((GridLayout)proxyDetailsComp.getLayout()).marginHeight = 0;
		
		Composite exifMetadataComp = toolkit.createComposite(stackComposite);
		exifMetadataComp.setLayout(new GridLayout());
		((GridLayout)exifMetadataComp.getLayout()).marginWidth = 0;
		((GridLayout)exifMetadataComp.getLayout()).marginHeight = 0;
		
		tblExif = new TableViewer(exifMetadataComp, SWT.FULL_SELECTION);
		tblExif.getTable().setLinesVisible(false);
		tblExif.getTable().setHeaderVisible(true);
		tblExif.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblExif.setContentProvider(ArrayContentProvider.getInstance());
		
		Color bgColor = new Color(tblExif.getControl().getDisplay(), 160,185,224);
		tblExif.getControl().addListener(SWT.Dispose, e->bgColor.dispose());
		
		TableViewerColumn colTag = new TableViewerColumn(tblExif, SWT.NONE);
		colTag.getColumn().setText("Tag");
		colTag.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof String[]) return ((String[])element)[0];
				if (element instanceof String) return (String)element;
				return "";
			}
			@Override
			public Color getBackground(Object element) {
				if (element instanceof String) return bgColor;
				return null;
			}
		});
		
		
		TableViewerColumn colTagValue = new TableViewerColumn(tblExif, SWT.NONE);
		colTagValue.getColumn().setText("Value");
		colTagValue.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof String[]) return ((String[])element)[1];
				return "";
			}
			@Override
			public Color getBackground(Object element) {
				if (element instanceof String) return bgColor;
				return null;
			}
		});
		
		Composite lnkComp = toolkit.createComposite(stackComposite);
		lnkComp.setLayout(new GridLayout());
		
		
		FontData fd = lnkDetails.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(lnkDetails.getDisplay(), fd);
		Font normalFont = lnkDetails.getFont(); 
		lnkDetails.addListener(SWT.Dispose, e->boldFont.dispose());
		
		
		lnkDetails.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackComposite.getLayout()).topControl = proxyDetailsComp;
				stackComposite.layout();
				lnkDetails.setFont(boldFont);
				lnkExif.setFont(normalFont);
				lnkXmp.setFont(normalFont);
				header.layout();
			}
		});
		lnkExif.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackComposite.getLayout()).topControl = exifMetadataComp;
				stackComposite.layout();
				lnkDetails.setFont(normalFont);
				lnkExif.setFont(boldFont);
				lnkXmp.setFont(normalFont);
				header.layout();
			}
		});
		lnkXmp.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackComposite.getLayout()).topControl = lnkComp;
				stackComposite.layout();
				lnkDetails.setFont(normalFont);
				lnkExif.setFont(normalFont);
				lnkXmp.setFont(boldFont);
				header.layout();
			}
		});
		((StackLayout)stackComposite.getLayout()).topControl = proxyDetailsComp;
		lnkDetails.setFont(boldFont);
		
		imageCanvas = new Canvas(detailsSash,SWT.BORDER);
		imageCanvas.addListener(SWT.Paint, e->{
			Image img = (Image)imageCanvas.getData("IMAGE");
			if (img == null) return;
			
			Rectangle bounds = img.getBounds();
			Rectangle cbounds = imageCanvas.getBounds();	
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
		imageCanvas.addListener(SWT.Dispose, e->{
			Image img = (Image)imageCanvas.getData("IMAGE");
			if (img != null && img.isDisposed()) img.dispose();
		});
		
		detailsSash.setWeights(new int[] {3,2});
		
		fileDetailsComposite.layout(true);
		

		int cwidth = (tblExif.getTable().getBounds().width - 20)/2;
		colTag.getColumn().setWidth(cwidth);
		colTagValue.getColumn().setWidth(cwidth);
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
	
	private void refreshProxies(final Collection<FileProxy> proxies) {
		Job refreshJob = new Job("refresh table") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
//				try(Session session = HibernateManager.openSession()){
//					proxies.forEach(p->p.updateAssetDeployment(session, processor.getFileDetails()));
//				}
				Display.getDefault().syncExec(()->{
					tblResults.refresh();
					updateStatus();
					updateFileDetails();
					updateFileCount();
				});
				return Status.OK_STATUS;
			}
		};
		refreshJob.schedule();
		
	}
	
	private void updateFileDetails() {
		Object selection = ((IStructuredSelection)tblResults.getSelection()).getFirstElement();
		if (selection == null) return;
		if (!(selection instanceof FileProxy)) return;
		
		FileProxy proxy = (FileProxy)selection;
		
		lblDetailsFileName.setText(proxy.getFile().getFileName().toString());
		lblDetailsStatus.setImage( AssetPlugIn.getDefault().getImageRegistry().get(  proxy.isValid() ? AssetPlugIn.ICON_IMPORT_COMPLETE : AssetPlugIn.ICON_IMPORT_INCOMPLETE));
		if (!proxy.isValid()) lblDetailsStatus.setToolTipText(proxy.validMessage());
		
		if (proxyDetailsComp.isDisposed()) return;
		for (Control c : proxyDetailsComp.getChildren()) c.dispose();
		
		ScrolledComposite scroll = new ScrolledComposite(proxyDetailsComp, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		toolkit.adapt(scroll);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite bits = toolkit.createComposite(scroll);
		scroll.setContent(bits);
		bits.setLayout(new GridLayout());
		bits.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FontData fd = bits.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(bits.getDisplay(), fd);
		bits.addListener(SWT.Dispose, e->boldFont.dispose());
		
		Composite fileSection = toolkit.createComposite(bits);
		fileSection.setLayout(new GridLayout(2, false));
		fileSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = toolkit.createLabel(fileSection, "Summary");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l.setFont(boldFont);
		
		if (!proxy.isValid()) {
			l = toolkit.createLabel(fileSection, "Status Details:");
			l = toolkit.createLabel(fileSection, proxy.validMessage());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		l = toolkit.createLabel(fileSection, "Date/Time:");
		l = toolkit.createLabel(fileSection, proxy.getImageDate() == null ? "" : DateFormat.getDateTimeInstance().format(proxy.getImageDate()) );
		
		l = toolkit.createLabel(fileSection, "Asset:");
		l = toolkit.createLabel(fileSection, proxy.getAsset() == null ? "" : proxy.getAsset().getId() );
		
		l = toolkit.createLabel(fileSection, "Station:");
		l = toolkit.createLabel(fileSection, proxy.getStation() == null ? "" : proxy.getStation().getId() );
		
		l = toolkit.createLabel(fileSection, "Station Location:");
		l = toolkit.createLabel(fileSection, proxy.getStationLocation() == null ? "" : proxy.getStationLocation().getId() );
		
		l = toolkit.createLabel(fileSection, "Longitude:");
		l = toolkit.createLabel(fileSection, proxy.getX() == null ? "" : String.valueOf(proxy.getX()) );
		
		l = toolkit.createLabel(fileSection, "Latitude:");
		l = toolkit.createLabel(fileSection, proxy.getY() == null ? "" : String.valueOf(proxy.getY()) );
		
		Composite obsSection = toolkit.createComposite(bits);
		obsSection.setLayout(new GridLayout(2, false));
		obsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(obsSection, "Observations");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l.setFont(boldFont);
		
		for (WaypointObservation wo : proxy.getObservations()) {
			l = toolkit.createLabel(obsSection, wo.getCategory().getFullCategoryName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			for (WaypointObservationAttribute a : wo.getAttributes()) {
				l = toolkit.createLabel(obsSection, a.getAttribute().getName() + ":");
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).horizontalIndent = 10;
				
				l = toolkit.createLabel(obsSection, a.getAttributeValueAsString(Locale.getDefault()));
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
		}
		
		Composite warnSection = toolkit.createComposite(bits);
		warnSection.setLayout(new GridLayout());
		warnSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(warnSection, "Processing Warnings");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setFont(boldFont);
		
		for (ActionableWarning aw : proxy.getWarnings()) {
			l = toolkit.createLabel(warnSection, aw.getMessage());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).horizontalIndent = 10;
			
		}
		
		scroll.setMinSize(bits.computeSize(SWT.DEFAULT,  SWT.DEFAULT));
		proxyDetailsComp.layout(true);
		
		tblExif.setInput(null);

		Job j2 = new Job("read exif metadata") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				HashMap<Directory, List<Tag>> exif = FileMetadataReader.readExifMetadata(proxy.getFile());
				
				Display.getDefault().syncExec(()->{
					if (tblExif.getTable().isDisposed()) return;
					if (exif == null) {
						tblExif.setInput(new String[] {"Error Reading EXIF Metadata"});
						return;
					}
					List<Object> values = new ArrayList<>();
					for (Entry<Directory, List<Tag>> item : exif.entrySet()) {
						values.add(item.getKey().getName());
						for (Tag t : item.getValue()) {
							values.add(new String[] {t.getTagName(), t.getDescription()});
						}
					}
					tblExif.setInput(values);
				});
				return Status.OK_STATUS;
			}
			
		};
		j2.schedule();
		
		Job j = new Job("loading image job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {			
				try {
					Image img = new Image(imageCanvas.getDisplay(), proxy.getFile().toString());
					Display.getDefault().syncExec(()->{
						if (imageCanvas.isDisposed()) {
							img.dispose();
							return;
						}
						Image lastImage = (Image) imageCanvas.getData("IMAGE");
						if (lastImage != null && !lastImage.isDisposed()) lastImage.dispose();
						
						imageCanvas.setData("IMAGE", img);
						imageCanvas.redraw();
					});
				}catch (Exception ex) {
					//invalid format TODO:
					ex.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
		
		fileDetailsComposite.layout(true);
	}
	
	
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
