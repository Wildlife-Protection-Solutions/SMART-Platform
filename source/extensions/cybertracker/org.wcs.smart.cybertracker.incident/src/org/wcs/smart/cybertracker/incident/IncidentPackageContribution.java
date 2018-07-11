package org.wcs.smart.cybertracker.incident;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXmlConverter;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

public class IncidentPackageContribution implements IPackageContribution{

	public static final String INCIDENT_MODEL_FILE = "incident_model.xml";
	public static final String INCIDENT_METADATA_FILE = "incident_metadata.json";
	
	public static final String INCIDENT_RESOURCE_ID = "incident"; //$NON-NLS-1$
	
	private static Object DATAMODEL = new Object();
	
	private Button btnCollect ;
	private ComboViewer cmbModel;
	
	@Override
	public Composite createUi(Composite parent) {
		Group incidentComposite = new Group(parent, SWT.NONE);
		incidentComposite.setLayout(new GridLayout());
		incidentComposite.setText("Incident Configuration");
		
		btnCollect = new Button(incidentComposite, SWT.CHECK);
		btnCollect.setText("Collect Independent Incidents");
		
		Composite modelComp = new Composite(incidentComposite, SWT.NONE);
		modelComp.setLayout(new GridLayout(2, false));
		((GridLayout)modelComp.getLayout()).marginWidth = 0;
		((GridLayout)modelComp.getLayout()).marginHeight = 0;
		modelComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)modelComp.getLayoutData()).horizontalIndent = 20;

		Label l = new Label(modelComp, SWT.NONE);
		l.setText("Incident Model:");
		
		cmbModel = new ComboViewer(modelComp, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbModel.setContentProvider(ArrayContentProvider.getInstance());
		cmbModel.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof ConfigurableModel) return ((ConfigurableModel) element).getName();
				if (element == DATAMODEL) return "Original Data Model";
				return super.getText(element);
			}
		});
		cmbModel.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbModel.setInput(new String[] {DialogConstants.LOADING_TEXT});
		loadCm.schedule();
		
		l.setEnabled(false);
		cmbModel.getControl().setEnabled(false);
		
		btnCollect.setSelection(false);
		btnCollect.addListener(SWT.Selection, e->{
			l.setEnabled(btnCollect.getSelection());
			cmbModel.getControl().setEnabled(btnCollect.getSelection());
		});
		
		return incidentComposite;
	}

	private Job loadCm = new Job("loading configurable models") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> items = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				items.addAll(
						QueryFactory.buildQuery(session,  ConfigurableModel.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
			}
			items.add(DATAMODEL);
			
			Display.getDefault().asyncExec(()->{
				cmbModel.setInput(items);
				cmbModel.setSelection(new StructuredSelection(items.get(0)));
			});
			
			return Status.OK_STATUS;
		} 
		
	};
	
	
	@Override
	public PackageUpdates packageFiles(IProgressMonitor monitor) throws IOException {
		
		final Object[] selection = new Object[] {null,null};
		Display.getDefault().syncExec(()->{
			selection[0] = btnCollect.getSelection();
			selection[1] = cmbModel.getStructuredSelection().getFirstElement();
		});
		if (!((Boolean)selection[0])) return null;
		
		monitor.subTask("Adding incident configuration details...");
		Object x = selection[1];
		
		PackageUpdates updates = new PackageUpdates();
		ConfigurableModel cm = null;
		if (x == DATAMODEL) {
			try(Session session = HibernateManager.openSession()){
				DataModelWrapper wrapper = new DataModelWrapper();
				cm = wrapper.buildConfigurableModel(session, monitor);
				cm.setConservationArea(SmartDB.getCurrentConservationArea());
			}
		}else if (x instanceof ConfigurableModel) {
			cm = (ConfigurableModel)x;
		}else {
			return null;
		}
		
		Path tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		Path incidentFile = tempDir.resolve(INCIDENT_MODEL_FILE);
		org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlModel = CmSmartToXmlConverter.convert(cm, monitor);
		try(OutputStream out = Files.newOutputStream(incidentFile)){
			try {
				CmXmlManager.writeDataModel(xmlModel, out);
			} catch (JAXBException e) {
				throw new IOException(e);
			}
		}
		updates.addFile(incidentFile);
		
		Path metadataFile = tempDir.resolve(INCIDENT_METADATA_FILE);
		createIncidentJson(metadataFile);
		updates.addFile(metadataFile);
		
		
		JSONObject metadata = new JSONObject();
		metadata.put("decoder","sourceparser_smartconfigurabledatamodel"); //$NON-NLS-1$ //$NON-NLS-2$
		metadata.put("definition",incidentFile.getFileName().toString()); //$NON-NLS-1$
		metadata.put("metadata", metadataFile.getFileName().toString()); //$NON-NLS-1$
		updates.setProjectMetadata("incident", metadata);
		return updates;
		
	}

	private void createIncidentJson(Path incidentJson) throws IOException {
		JSONArray metadataScreens = new JSONArray();
		metadataScreens.add(CtJsonExportUtils.createDataType(INCIDENT_RESOURCE_ID));
		try(BufferedWriter fw = Files.newBufferedWriter(incidentJson)){
			fw.write(metadataScreens.toJSONString());
		}
	}
}
