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
package org.wcs.smart.cybertracker.incident;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXmlConverter;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

public class IncidentPackageContribution implements IPackageContribution{

	//package files
	public static final String INCIDENT_MODEL_FILE = "incident_model.xml"; //$NON-NLS-1$
	public static final String INCIDENT_METADATA_FILE = "incident_metadata.json"; //$NON-NLS-1$
	//incident resource key
	public static final String INCIDENT_RESOURCE_ID = "incident"; //$NON-NLS-1$
	//data model 
	private static final String DATAMODEL = "DATAMODEL"; //$NON-NLS-1$
	
	//preference keys
	private static final String COLLECT_PREF_KEY = IncidentPackageContribution.class.getCanonicalName() + ".collect"; //$NON-NLS-1$
	private static final String CM_PREF_KEY = IncidentPackageContribution.class.getCanonicalName() + ".cm"; //$NON-NLS-1$
	
	private Button btnCollect ;
	private ComboViewer cmbModel;
	private String prefKey = null;
	
	@Override
	public Composite createUi(Composite parent, String prefKey) {
		this.prefKey = prefKey;
		Group incidentComposite = new Group(parent, SWT.NONE);
		incidentComposite.setLayout(new GridLayout());
		incidentComposite.setText(Messages.IncidentPackageContribution_ConfigurationGroupLablel);
		
		btnCollect = new Button(incidentComposite, SWT.CHECK);
		btnCollect.setText(Messages.IncidentPackageContribution_CollectIncidentsOp);
		
		Composite modelComp = new Composite(incidentComposite, SWT.NONE);
		modelComp.setLayout(new GridLayout(2, false));
		((GridLayout)modelComp.getLayout()).marginWidth = 0;
		((GridLayout)modelComp.getLayout()).marginHeight = 0;
		modelComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)modelComp.getLayoutData()).horizontalIndent = 20;

		Label l = new Label(modelComp, SWT.NONE);
		l.setText(Messages.IncidentPackageContribution_CmModelLabel);
		
		cmbModel = new ComboViewer(modelComp, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbModel.setContentProvider(ArrayContentProvider.getInstance());
		cmbModel.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof ConfigurableModel) return ((ConfigurableModel) element).getName();
				if (element == DATAMODEL) return Messages.IncidentPackageContribution_OriginalDmLabel;
				return super.getText(element);
			}
		});
		cmbModel.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbModel.setInput(new String[] {DialogConstants.LOADING_TEXT});
		loadCm.schedule();
		
		l.setEnabled(false);
		cmbModel.getControl().setEnabled(false);
		
		btnCollect.addListener(SWT.Selection, e->{
			l.setEnabled(btnCollect.getSelection());
			cmbModel.getControl().setEnabled(btnCollect.getSelection());
		});
		
		boolean isSelected = CtIncidentPlugIn.getDefault().getPreferenceStore().getBoolean(getPreferenceKey(IncidentPackageContribution.COLLECT_PREF_KEY));
		btnCollect.setSelection(isSelected);
		l.setEnabled(btnCollect.getSelection());
		cmbModel.getControl().setEnabled(btnCollect.getSelection());
			
		
		return incidentComposite;
	}

	/*
	 * append the conservation area uuid to preference key so each conservation
	 * area will have it's own preferences
	 */
	private String getPreferenceKey(String key) {
		return key + "." + prefKey + "." + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private Job loadCm = new Job(Messages.IncidentPackageContribution_LoadingCmJobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> items = new ArrayList<>();
			ConfigurableModel selection = null;
			String cmUuid = CtIncidentPlugIn.getDefault().getPreferenceStore().getString(getPreferenceKey(IncidentPackageContribution.CM_PREF_KEY));
			UUID selectionUuid =  null;
			if (cmUuid != null && !cmUuid.isEmpty() && !cmUuid.equals(DATAMODEL)) selectionUuid = UuidUtils.stringToUuid(cmUuid);
			
			try(Session session = HibernateManager.openSession()){
				items.addAll(
						QueryFactory.buildQuery(session,  ConfigurableModel.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				for (Object i : items) {
					if (((ConfigurableModel)i).getUuid().equals(selectionUuid)) {
						selection = (ConfigurableModel) i;
					}
				}
			}
			items.add(DATAMODEL);
			
			final ConfigurableModel fselection = selection;
			Display.getDefault().asyncExec(()->{
				cmbModel.setInput(items);
				
				if (cmUuid.equals(DATAMODEL)) {
					cmbModel.setSelection(new StructuredSelection(DATAMODEL));
				}else if (fselection == null) {
					cmbModel.setSelection(new StructuredSelection(items.get(0)));
				}else {
					cmbModel.setSelection(new StructuredSelection(fselection));
				}
			});
			
			return Status.OK_STATUS;
		} 
		
	};
	
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public PackageContribution packageFiles(IProgressMonitor monitor) throws IOException {
		
		final Object[] selection = new Object[] {null,null};
		Display.getDefault().syncExec(()->{
			selection[0] = btnCollect.getSelection();
			selection[1] = cmbModel.getStructuredSelection().getFirstElement();
			
			//save preferences to preference store
			CtIncidentPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(IncidentPackageContribution.COLLECT_PREF_KEY), (Boolean)selection[0]);
			if (selection[1] instanceof ConfigurableModel) {
				CtIncidentPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(IncidentPackageContribution.CM_PREF_KEY), UuidUtils.uuidToString( ((ConfigurableModel)selection[1]).getUuid()) );
			}else {
				CtIncidentPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(IncidentPackageContribution.CM_PREF_KEY), DATAMODEL );
			}
		});
		if (!((Boolean)selection[0])) return null;
		
		monitor.subTask(Messages.IncidentPackageContribution_TaskName);
		Object x = selection[1];
		
		
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
		
		PackageContribution updates = new PackageContribution() {
			@Override
			public void cleanUp() throws IOException{
				//delete tempDir and any subfiles
				Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						if (exc != null) throw exc;
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
				}});
			}
		};
		
		
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
		updates.setProjectMetadata("incident", metadata); //$NON-NLS-1$
		return updates;
		
	}

	@SuppressWarnings("unchecked")
	private void createIncidentJson(Path incidentJson) throws IOException {
		JSONArray metadataScreens = new JSONArray();
		metadataScreens.add(CtJsonExportUtils.createDataType(INCIDENT_RESOURCE_ID));
		try(BufferedWriter fw = Files.newBufferedWriter(incidentJson)){
			fw.write(metadataScreens.toJSONString());
		}
	}
}
