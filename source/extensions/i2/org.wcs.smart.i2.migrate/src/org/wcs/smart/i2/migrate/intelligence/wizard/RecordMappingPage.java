package org.wcs.smart.i2.migrate.intelligence.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.migrate.intelligence.IntelMappingRecord;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileRecordSource;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

public class RecordMappingPage extends WizardPage {

	private static final String DO_NOT_IMPORT = "DO NOT IMPORT";
	private List<IntelMappingRecord> mappings = null;
	private List<Object[]> rows;
	
	private Composite main;
	private ScrolledComposite scomp;

	private Map<ConservationArea, List<Object>> allProfiles;
	private Map<IntelProfile, List<IntelRecordSource>> recordSources;
	
	protected RecordMappingPage() {
		super("MappingPage");
	}

	@Override
	public boolean isPageComplete(){
		return getErrorMessage() == null;
	}
	
	public List<IntelMappingRecord> getMappings(){
		mappings = new ArrayList<>();
		for (Object[] data : rows) {
			Object x = ((TableComboViewer)data[1]).getStructuredSelection().getFirstElement();
			if (!(x instanceof IntelProfile)) continue;
			
			IntelMappingRecord record = (IntelMappingRecord) data[0];
			mappings.add(record);
			
			if (x instanceof IntelProfile) {
				record.setProfile((IntelProfile) x);
			}
			
			x = ((TableComboViewer)data[2]).getStructuredSelection().getFirstElement();
			if (x instanceof IntelRecordSource) {
				record.setRecordSource((IntelRecordSource)x);
			}
			
			x = ((ComboViewer)data[3]).getStructuredSelection().getFirstElement();
			if (x instanceof IntelRecordSourceAttribute) {
				record.setFromDateMapping((IntelRecordSourceAttribute)x);
			}
			
			x = ((ComboViewer)data[4]).getStructuredSelection().getFirstElement();
			if (x instanceof IntelRecordSourceAttribute) {
				record.setToDateMapping((IntelRecordSourceAttribute)x);
			}
		}
		return mappings;
	}
	
	public void setMappings(List<IntelMappingRecord> mappings) {
		this.mappings = mappings;
		createControls();
	}
	
	private void createControls() {
		for (Control c : main.getChildren()) c.dispose();

		
		Composite inner = new Composite(main, SWT.NONE);
		inner.setLayout(new GridLayout(6, true));
		
		String[]  headers = new String[]{"Conservation Area", "Intelligence Source", "Profile", "Record Source", "From Attribute", "To Attribute"};
		for (String h : headers) {
			Label l = new Label(inner, SWT.NONE);
			l.setText(h);
		}
		
		rows = new ArrayList<>();
		for (IntelMappingRecord record : mappings) {
			Label l = new Label(inner, SWT.NONE);
			l.setText(record.getConservationArea().getId());
			
			l = new Label(inner, SWT.NONE);
			l.setText(record.getSmart6Source().getName());
			
			TableComboViewer cmbProfile = new TableComboViewer(inner, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
			cmbProfile.setContentProvider(ArrayContentProvider.getInstance());
			cmbProfile.setLabelProvider(new ProfileLabelProvider());
			cmbProfile.setInput(new String[]{DialogConstants.LOADING_TEXT});
			cmbProfile.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			SmartUiUtils.configure(cmbProfile);
			
			
			TableComboViewer cmbRecordSource = new TableComboViewer(inner, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
			cmbRecordSource.setContentProvider(ArrayContentProvider.getInstance());
			cmbRecordSource.setLabelProvider(new RecordSourceLabelProvider());
			cmbRecordSource.setInput(new String[]{DialogConstants.LOADING_TEXT});
			cmbRecordSource.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			SmartUiUtils.configure(cmbRecordSource);
			
			
			
			ComboViewer cmbFrom = new ComboViewer(inner, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
			cmbFrom.setContentProvider(ArrayContentProvider.getInstance());
			cmbFrom.getControl().setEnabled(false);
			cmbFrom.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbFrom.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof IntelRecordSourceAttribute) {
						IntelRecordSourceAttribute aa = (IntelRecordSourceAttribute)element;
						if (aa.getName() != null) return aa.getName();
						return aa.getAttribute().getName();
					}
					return super.getText(element);
				}
			});
					
			ComboViewer cmbTo = new ComboViewer(inner, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
			cmbTo.setContentProvider(ArrayContentProvider.getInstance());
			cmbTo.getControl().setEnabled(false);
			cmbTo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbTo.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof IntelRecordSourceAttribute) {
						IntelRecordSourceAttribute aa = (IntelRecordSourceAttribute)element;
						if (aa.getName() != null) return aa.getName();
						return aa.getAttribute().getName();
					}
					return super.getText(element);
				}
			});
			
			ISelectionChangedListener validator = new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					Object x1 = cmbTo.getStructuredSelection().getFirstElement();
					Object x2 = cmbFrom.getStructuredSelection().getFirstElement();
					setErrorMessage(null);
					if (x2 instanceof IntelRecordSourceAttribute && x1 instanceof IntelRecordSourceAttribute) {
						if(x1.equals(x2)) {
							setErrorMessage("Cannot map the From and To attributes to the same record source attribute.");
						}
					}
					getContainer().updateButtons();	
				}
			};
			cmbFrom.addPostSelectionChangedListener(validator);
			cmbTo.addPostSelectionChangedListener(validator);
			
			cmbProfile.addSelectionChangedListener(e->{
				Object x = cmbProfile.getStructuredSelection().getFirstElement();
				
				cmbRecordSource.getControl().setEnabled(x != DO_NOT_IMPORT);
				cmbFrom.getControl().setEnabled(x != DO_NOT_IMPORT);
				cmbTo.getControl().setEnabled(x != DO_NOT_IMPORT);
				
				if (!(x instanceof IntelProfile)) return;
				
				Object currentSelection = cmbRecordSource.getStructuredSelection().getFirstElement();
				List<IntelRecordSource> items = recordSources.get((IntelProfile)x);
				List<Object> input = new ArrayList<>();
				input.add("");
				input.addAll(items);
				cmbRecordSource.setInput(input);
				if (items.contains(currentSelection)) {
					cmbRecordSource.setSelection(new StructuredSelection(currentSelection));
				}else if (!items.isEmpty()) {
					currentSelection = null;
					for (IntelRecordSource item : items) {
						if (item.getName().contains(record.getSmart6Source().getName())) {
							currentSelection = item;
							break;
						}
					}
					if (currentSelection == null) {
						for (IntelRecordSource item : items) {
							if (item.getName().contains(record.getSmart6Source().getKey())) {
								currentSelection = item;
								break;
							}
						}	
					}
					if (currentSelection == null) currentSelection = items.get(0);
					
					cmbRecordSource.setSelection(new StructuredSelection(currentSelection));
				}
				
			});
			
			cmbRecordSource.addPostSelectionChangedListener(e->{
				Object x = cmbRecordSource.getStructuredSelection().getFirstElement();
				if (x == null || !(x instanceof IntelRecordSource)) {
					cmbFrom.getControl().setEnabled(false);
					cmbTo.getControl().setEnabled(false);
				}else {
					cmbFrom.getControl().setEnabled(true);
					cmbTo.getControl().setEnabled(true);
					List<Object> atts = 
							((IntelRecordSource)x).getAttributes()
							.stream()
							.filter(xx -> (xx.getAttribute() != null && xx.getAttribute().getType() == IntelAttribute.AttributeType.DATE))
							.collect(Collectors.toList());
					atts.add(0,  "");
					cmbFrom.setInput(atts);
					cmbTo.setInput(atts);
				}
			});
			
			rows.add(new Object[] {record, cmbProfile, cmbRecordSource, cmbFrom, cmbTo});
		}
		SmartUiUtils.makeTransparent(main);
		main.layout(true);
		main.setSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scomp.getParent().layout(true);

		loadData.schedule();
	}
	@Override
	public void createControl(Composite parent) {
	
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout());
		
		setControl(temp);
		
		scomp = new ScrolledComposite(temp, SWT.V_SCROLL | SWT.H_SCROLL);
		scomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		main = new Composite(scomp, SWT.NONE);
		scomp.setContent(main);
		main.setLayout(new GridLayout());
		
		setTitle("Mappings");
		setMessage("Map the SMART 6 intelligence source to Profile record sources.");
	}

	
	Job loadData = new Job("load data") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Set<ConservationArea> cas = new HashSet<>();
			for (Object[] row : rows) {
				ConservationArea ca = ((IntelMappingRecord)row[0]).getConservationArea();
				cas.add(ca);
			}
			
			allProfiles = new HashMap<>();
			recordSources = new HashMap<>();
			
			try(Session session = HibernateManager.openSession()){
				for (ConservationArea ca : cas) {
					List<IntelProfile> profiles = QueryFactory.buildQuery(session, 
							IntelProfile.class,
							new Object[] {"conservationArea", ca}).list();
					
					profiles.forEach(e->e.getName());
					List<Object> pprofiles = new ArrayList<>(profiles);
					pprofiles.add(0, DO_NOT_IMPORT);
					allProfiles.put(ca, pprofiles);
					
					for (IntelProfile profile : profiles) {
						List<IntelProfileRecordSource> items = QueryFactory.buildQuery(session, IntelProfileRecordSource.class, 
								new Object[] {"id.profile", profile}).list();
						List<IntelRecordSource> sources = items.stream().map(e->e.getRecordSource()).collect(Collectors.toList());
						sources.forEach(e->{
							e.getName();
							e.getAttributes().forEach(at->at.getName());
						});
						recordSources.put(profile, sources);
					}
				}
			}
			
			Display.getDefault().syncExec(()->{
				for (Object[] row : rows) {
					ConservationArea ca = ((IntelMappingRecord)row[0]).getConservationArea();
					
					List<Object> items = allProfiles.get(ca);
					((TableComboViewer)row[1]).setInput(items);
					if (!items.isEmpty()) {
						((TableComboViewer)row[1]).setSelection(new StructuredSelection(items.get(1)));
					}
					
				}
			});
			return Status.OK_STATUS;
		}
		
	};
}
