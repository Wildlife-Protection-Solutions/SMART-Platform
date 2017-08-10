package org.wcs.smart.i2.ui.dialogs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.xml.IntelDataToXml;
import org.wcs.smart.ui.properties.DialogConstants;


public class ExportModelElementsDialog extends TitleAreaDialog{

	public static final String PREFERENCE_DIR_KEY = ExportModelElementsDialog.class.getCanonicalName() + ".dir"; //$NON-NLS-1$
	
	private Font boldFont;
	
	private CheckboxTableViewer lstEntities;
	private CheckboxTableViewer lstRelations;
	private CheckboxTableViewer lstAttributes;
	private CheckboxTableViewer lstRecordSource;
	private Button btnSrcTemplate ;
	private Text txtOutputFile;
	
	public ExportModelElementsDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void cancelPressed(){
		super.cancelPressed();
	}
	
	@Override
	public void okPressed(){
		
		List<UUID> sources = getSelected(IntelRecordSource.class, lstRecordSource).stream().map(x->x.getUuid()).collect(Collectors.toList());
		List<UUID> entities = getSelected(IntelEntityType.class, lstEntities).stream().map(x->x.getUuid()).collect(Collectors.toList());
		List<UUID> relationships = getSelected(IntelRelationshipType.class, lstRelations).stream().map(x->x.getUuid()).collect(Collectors.toList());
		List<UUID> attributes = getSelected(IntelAttribute.class, lstAttributes).stream().map(x->x.getUuid()).collect(Collectors.toList());
		
		Path outputFile = Paths.get(txtOutputFile.getText());
		boolean includeRecordSrcTemplate = btnSrcTemplate.getSelection();
		
		Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(PREFERENCE_DIR_KEY, outputFile.toString());
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try(Session s = HibernateManager.openSession()){
						IntelDataToXml hh = new IntelDataToXml(s);
						hh.export(outputFile, attributes, sources, relationships, entities, includeRecordSrcTemplate, monitor);
					} catch (OperationCanceledException ex) {
						getShell().getDisplay().syncExec(()->MessageDialog.openInformation(getShell(), "Canceled", "Export canceled"));
					} catch (IOException e) {
						Intelligence2PlugIn.log("Error exporting intelligence elements to xml: " + e.getMessage(), e);
					}
				}
			});
		} catch (Exception e) {
			Intelligence2PlugIn.log("Error exporting intelligence elements to xml: " + e.getMessage(), e);
		}
		
		
		super.okPressed();
	}

	private <T> List<T> getSelected(Class<T> c, CheckboxTableViewer viewer){
		List<T> items = new ArrayList<>();
		for (Object x : viewer.getCheckedElements()) if (x.getClass().equals(c)) items.add((T)x);
		return items;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		parent.addListener(SWT.Dispose, e->boldFont.dispose());
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite fileSection = new Composite(outer, SWT.NONE);
		fileSection.setLayout(new GridLayout(3, false));
		fileSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(fileSection, SWT.NONE);
		l.setText("Output File:");
		
		txtOutputFile = new Text(fileSection, SWT.BORDER);
		txtOutputFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String initDir = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_DIR_KEY);
		if (initDir != null) {
			txtOutputFile.setText(initDir);
		}
		Button btnBrowse = new Button(fileSection, SWT.NONE);
		btnBrowse.setText("...");
		
		btnBrowse.addListener(SWT.Selection, e->{
			FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
			dialog.setFilterExtensions(new String[] {"*.zip", "*.*"});
			dialog.setFilterNames(new String[] {"zip Files (*.zip)", "All Files (*.*)"});
			String t = txtOutputFile.getText();
			if (t.length() > 0) {
				dialog.setFileName(t);
			}
			dialog.setText("Export To XML");
			String file = dialog.open();
			if (file != null) txtOutputFile.setText(file);
		});
		
		Composite itemsSection = new Composite(outer, SWT.NONE);
		itemsSection.setLayout(new GridLayout());
		itemsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//entity section
		Composite entitySection = new Composite(itemsSection, SWT.NONE);
		entitySection.setLayout(new GridLayout());
		((GridLayout)entitySection.getLayout()).marginWidth = 0;
		((GridLayout)entitySection.getLayout()).marginHeight = 0;
		entitySection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(entitySection, "Entity Types");
		lstEntities = createTableViewer(entitySection, new EntityTypeLabelProvider(), null);

		//relations section
		Composite relationsSection = new Composite(itemsSection, SWT.NONE);
		relationsSection.setLayout(new GridLayout());
		((GridLayout)relationsSection.getLayout()).marginWidth = 0;
		((GridLayout)relationsSection.getLayout()).marginHeight = 0;
		relationsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(relationsSection, "Relationship Types");
		lstRelations = createTableViewer(relationsSection, new RelationshipTypeLabelProvider(), null);
		
		//source section
		Composite sourceSection = new Composite(itemsSection, SWT.NONE);
		sourceSection.setLayout(new GridLayout());
		((GridLayout)sourceSection.getLayout()).marginWidth = 0;
		((GridLayout)sourceSection.getLayout()).marginHeight = 0;
		sourceSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(sourceSection, "Record Sources");
		ICheckStateListener listener = new ICheckStateListener() {			
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				btnSrcTemplate.setSelection(lstRecordSource.getCheckedElements().length != 0);
			}
		};
		lstRecordSource = createTableViewer(sourceSection, new RecordSourceLabelProvider(), listener);
		btnSrcTemplate = new Button(sourceSection, SWT.CHECK);
		btnSrcTemplate.setText("Include BIRT Record Source Template");
		lstRecordSource.addCheckStateListener(listener);
		
		//attribute section
		Composite attributeSection = new Composite(itemsSection, SWT.NONE);
		attributeSection.setLayout(new GridLayout());
		((GridLayout)attributeSection.getLayout()).marginWidth = 0;
		((GridLayout)attributeSection.getLayout()).marginHeight = 0;
		attributeSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(attributeSection, "Attributes");
		l = new Label(attributeSection, SWT.WRAP);
		l.setText("Attributes associated with entities, relationships, and record sources will be automatically included in the export.  You only need to select attributes here if you want to include them and they are not included by other selections.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 100;
		lstAttributes = createTableViewer(attributeSection, new AttributeLabelProvider(), null);
				
		//load data
		initializeLists.schedule();
		
		setTitle("Export Intelligence Model Elements");
		setMessage("Exports selected intelligence model elemnts to xml format");
		getShell().setText("Intelligence Model Export");

		return parent;
	}
	
	private CheckboxTableViewer createTableViewer(Composite parent, ILabelProvider lblProvider, ICheckStateListener listener) {
	
		Composite action = new Composite(parent, SWT.NONE);
		action.setLayout(new GridLayout(2, false));
		((GridLayout)action.getLayout()).marginWidth = 0;
		((GridLayout)action.getLayout()).marginHeight = 0;
		
		Link selectAll = new Link(action, SWT.NONE);
		selectAll.setText("<a>" + "All" + "</a>");
		
		Link selectNone = new Link(action, SWT.NONE);
		selectNone.setText("<a>" + "None" + "</a>");
		selectNone.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)viewer.getControl().getLayoutData()).heightHint = 50;
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(lblProvider);
		viewer.setInput(new String[] { DialogConstants.LOADING_TEXT });
		
		selectAll.addListener(SWT.Selection, e->{viewer.setAllChecked(true); if (listener != null) listener.checkStateChanged(null);});
		selectNone.addListener(SWT.Selection, e->{viewer.setAllChecked(false); if (listener != null) listener.checkStateChanged(null);});
		
		return viewer;
	}
	
	
	
	private void createHeader(Composite parent, String text) {
		Composite header = new Composite(parent, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.setLayout(new GridLayout(2, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		
		Label l = new Label(header, SWT.NONE);
		l.setText(text);
		l.setFont(boldFont);
		
		Label sep = new Label(header, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}
	
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	Job initializeLists = new Job("loading types") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelEntityType> entityTypes;
			List<IntelRelationshipType> relationshipTypes;
			List<IntelRecordSource> sources;
			List<IntelAttribute> attributes;
			
			try (Session s = HibernateManager.openSession()){
				entityTypes = QueryFactory.buildQuery(s, IntelEntityType.class, "conservationArea", SmartDB.getCurrentConservationArea()).list();
				entityTypes.forEach(e->e.getName());
				
				relationshipTypes = QueryFactory.buildQuery(s, IntelRelationshipType.class, "conservationArea", SmartDB.getCurrentConservationArea()).list();
				relationshipTypes.forEach(r->{
					r.getName();
					if (r.getRelationshipGroup() != null) r.getRelationshipGroup().getName();
					
				});
			
				attributes = QueryFactory.buildQuery(s,  IntelAttribute.class, "conservationArea", SmartDB.getCurrentConservationArea()).list();
				attributes.forEach(a->a.getName());
				
				sources = QueryFactory.buildQuery(s,  IntelRecordSource.class, "conservationArea", SmartDB.getCurrentConservationArea()).list();
				sources.forEach(a->a.getName());
			}
			
			Display.getDefault().syncExec(()->{
				lstEntities.setInput(entityTypes);
				lstRelations.setInput(relationshipTypes);
				lstAttributes.setInput(attributes);
				lstRecordSource.setInput(sources);
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
