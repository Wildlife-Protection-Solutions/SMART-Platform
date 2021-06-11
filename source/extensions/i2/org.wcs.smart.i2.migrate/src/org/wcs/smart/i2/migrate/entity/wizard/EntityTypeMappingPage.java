/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.entity.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
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
import org.wcs.smart.i2.migrate.entity.EntityTypeMappingRecord;
import org.wcs.smart.i2.migrate.internal.Messages;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Wizard page to collect mapping information for converting
 * intelligence records.
 * 
 * @author Emily
 *
 */
public class EntityTypeMappingPage extends WizardPage {

	private static final String DO_NOT_IMPORT = Messages.RecordMappingPage_DoNotImportOption;
	
	private List<EntityTypeMappingRecord> mappings = null;
	private List<Object[]> rows;
	
	private Composite main;
	private ScrolledComposite scomp;

	private Map<ConservationArea, List<Object>> allProfiles;
	
	protected EntityTypeMappingPage() {
		super("MappingPage"); //$NON-NLS-1$
	}

	@Override
	public boolean isPageComplete(){
		return getErrorMessage() == null;
	}
	
	public List<EntityTypeMappingRecord> getMappings(){
		mappings = new ArrayList<>();
		for (Object[] data : rows) {
			Object x = ((TableComboViewer)data[1]).getStructuredSelection().getFirstElement();
			if (!(x instanceof IntelProfile)) continue;
			
			EntityTypeMappingRecord record = (EntityTypeMappingRecord) data[0];
			mappings.add(record);
			record.setProfile((IntelProfile) x);
		}
		return mappings;
	}
	
	public void setMappings(List<EntityTypeMappingRecord> mappings) {
		this.mappings = mappings;
		createControls();
	}
	
	private void createControls() {
		for (Control c : main.getChildren()) c.dispose();

		
		Composite inner = new Composite(main, SWT.NONE);
		inner.setLayout(new GridLayout(3, true));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		String[]  headers = new String[]{"Conservation Area", "Entity Type", "Profile"};
		for (String h : headers) {
			Label l = new Label(inner, SWT.NONE);
			l.setText(h);
		}
		
		rows = new ArrayList<>();
		for (EntityTypeMappingRecord record : mappings) {
			Label l = new Label(inner, SWT.NONE);
			l.setText(record.getConservationArea().getId());
			
			l = new Label(inner, SWT.NONE);
			//TODO: display name
			l.setText(record.getEntitytype().getKeyId());
			
			TableComboViewer cmbProfile = new TableComboViewer(inner, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
			cmbProfile.setContentProvider(ArrayContentProvider.getInstance());
			cmbProfile.setLabelProvider(new ProfileLabelProvider());
			cmbProfile.setInput(new String[]{DialogConstants.LOADING_TEXT});
			cmbProfile.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			SmartUiUtils.configure(cmbProfile);
			
			
			rows.add(new Object[] {record, cmbProfile,});
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
		scomp.setExpandHorizontal(true);
		
		main = new Composite(scomp, SWT.NONE);
		scomp.setContent(main);
		main.setLayout(new GridLayout());
		
		
		setTitle("Mappings");
		setMessage("Map Entity Types to Profiles");
	}

	
	Job loadData = new Job("load data") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Set<ConservationArea> cas = new HashSet<>();
			for (Object[] row : rows) {
				ConservationArea ca = ((EntityTypeMappingRecord)row[0]).getConservationArea();
				cas.add(ca);
			}
			
			allProfiles = new HashMap<>();
			
			try(Session session = HibernateManager.openSession()){
				for (ConservationArea ca : cas) {
					List<IntelProfile> profiles = QueryFactory.buildQuery(session, 
							IntelProfile.class,
							new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
					
					profiles.forEach(e->e.getName());
					List<Object> pprofiles = new ArrayList<>(profiles);
					pprofiles.add(0, DO_NOT_IMPORT);
					allProfiles.put(ca, pprofiles);
				}
			}
			
			Display.getDefault().syncExec(()->{
				for (Object[] row : rows) {
					ConservationArea ca = ((EntityTypeMappingRecord)row[0]).getConservationArea();
					
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
