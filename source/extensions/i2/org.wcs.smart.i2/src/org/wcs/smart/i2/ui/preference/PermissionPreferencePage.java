/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.preference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelPermission;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.security.IntelAdminUserLevel;
import org.wcs.smart.i2.security.IntelUserUserLevel;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.i2.ui.dialogs.ProfileDialog;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.user.UserLevelManager;


/**
 * Dialog for summarizing permissions per user per profile.
 * 
 * @author Emily
 *
 */
public class PermissionPreferencePage extends PreferencePage implements IIntelPreferencePage{


	private TableViewer tblViewer;
	private List<IntelProfile> profiles ;
	
	private String[] headers = new String[] { Messages.PermissionPreferencePage_PermissionAnalyst, 
			Messages.PermissionPreferencePage_PermissionReadonly, Messages.PermissionPreferencePage_Permissioncreateentity, Messages.PermissionPreferencePage_Permissiondeleteentity, Messages.PermissionPreferencePage_Permissionviewentity,
			Messages.PermissionPreferencePage_Permissioneditentity, Messages.PermissionPreferencePage_Permissioncreaterecord, Messages.PermissionPreferencePage_Permissiondeleterecord, 
			Messages.PermissionPreferencePage_Permissionviewrecord, Messages.PermissionPreferencePage_Permissioneditrecord, Messages.PermissionPreferencePage_Permissioneditallrecord,
			Messages.PermissionPreferencePage_Permissionquery, };
	private int[] permissions = new int[] { IntelPermission.ADMIN, 
			IntelPermission.READ_ONLY, IntelPermission.ENTITY_CREATE,
			IntelPermission.ENTITY_DELETE, IntelPermission.ENTITY_VIEW, IntelPermission.ENTITY_EDIT,
			IntelPermission.RECORD_CREATE, IntelPermission.RECORD_DELETE, IntelPermission.RECORD_VIEW,
			IntelPermission.RECORD_EDIT_NOTSTATUS, IntelPermission.RECORD_EDIT_ALL, IntelPermission.QUERY };
	
	public PermissionPreferencePage() {
		super();
		noDefaultAndApplyButton();
		setTitle(Messages.PermissionPreferencePage_PageName);
	}

			
	@Override
	protected Control createContents(Composite parent) {

		parent = new Composite(parent, SWT.NONE);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.setLayout(new TableColumnLayout());
		
		setMessage(Messages.PermissionPreferencePage_PageDescription);
	
		tblViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		tblViewer.setContentProvider(ArrayContentProvider.getInstance());
		tblViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblViewer.getTable().setHeaderVisible(true);
		ColumnViewerToolTipSupport.enableFor(tblViewer);

		
		Menu mnuEdit = new Menu(tblViewer.getControl());
		
		MenuItem miEdit = new MenuItem(mnuEdit, SWT.NONE);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.setEnabled(true);
		miEdit.addListener(SWT.Selection, e->{
			if (miEdit.getData() == null) return;
			edit((int)miEdit.getData());
		});
		
		tblViewer.getControl().addListener(SWT.MenuDetect, e->{
			miEdit.setData(null);
			Object x = tblViewer.getStructuredSelection().getFirstElement();
			if (x == null || !(x instanceof RowData)) {
				e.doit = false;
				return;
			}
			ViewerCell vc = tblViewer.getCell(tblViewer.getControl().toControl(e.x, e.y));
			if (vc == null || vc.getColumnIndex() < 1) {
				e.doit = false;
				return;
			}
			miEdit.setData(vc.getColumnIndex());
		});
		
		tblViewer.getControl().setMenu(mnuEdit);
		loadDataJob.schedule();
		return parent;
	}

	private void edit(int index) {
		IntelProfile profile = profiles.get(index - 1);
		
		List<IntelProfile> others = new ArrayList<>(profiles);
		others.remove(profile);
		
		ProfileDialog pd = new ProfileDialog(getShell(), profile, others, ProfileDialog.Tab.PERMISSIONS);
		pd.open();
		refresh();
	}
	
	
	@Override
	public void refresh(){
		loadDataJob.schedule();
	}

	private Job loadDataJob = new Job(Messages.PermissionPreferencePage_loadingjobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				
				List<Employee> allempolyees = new ArrayList<>();
				
				List<Employee> items = QueryFactory.buildQuery(session, Employee.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				for (Employee e : items) {
					if (UserLevelManager.INSTANCE.supportsUser(e, IntelAdminUserLevel.INSTANCE, IntelUserUserLevel.INSTANCE)) {
						allempolyees.add(e);
					}
				}
				
				List<IntelPermission> current = session.createQuery("FROM IntelPermission WHERE id.employee.conservationArea = :ca", IntelPermission.class) //$NON-NLS-1$
						.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list();
						
				
				profiles = ProfilesManager.INSTANCE.getProfiles(session, false);
				
				List<RowData> data = new ArrayList<>();
				//each employee/profile combo needs an entry
				for (Employee e : allempolyees) {
					
					RowData d = new RowData(e);
					data.add(d);
					for (IntelPermission temp : current) {
						if (temp.getEmployee().equals(e)) d.addPermission(temp);
					}
				}

				
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						if (tblViewer.getControl().isDisposed()) return;
						
						TableColumnLayout tlayout = (TableColumnLayout) tblViewer.getControl().getParent().getLayout(); 
						
						for (TableColumn c : tblViewer.getTable().getColumns()) c.dispose();
						
						TableViewerColumn col = new TableViewerColumn(tblViewer, SWT.NONE);
						col.setLabelProvider(new ColumnLabelProvider() {
							public String getText(Object element) {
								return SmartLabelProvider.getShortLabel( ((RowData)element).getEmployee() );
							}
						});
						col.getColumn().setText(Messages.PermissionPreferencePage_EmployeeColumnName);
						tlayout.setColumnData(col.getColumn(), new ColumnWeightData(3));
						
						ProfileLabelProvider temp = new ProfileLabelProvider();
						tblViewer.getTable().addListener(SWT.Dispose, e->temp.dispose());
						
						
						
						for (IntelProfile p : profiles) {
							TableViewerColumn col2 = new TableViewerColumn(tblViewer, SWT.NONE);
							col2.getColumn().setText(p.getName());
							col2.getColumn().setToolTipText(p.getName());
							col2.getColumn().setWidth(100);	
							col2.getColumn().setImage(temp.getImage(p));
							
							col2.setLabelProvider(new ColumnLabelProvider() {
								
								public String getText(Object element) {
									return ""; //$NON-NLS-1$
								}
								
								@Override
								public String getToolTipText(Object element) {
									IntelPermission ip = ((RowData)element).getPermission(p);
									if (ip == null) return null;
									
									StringBuilder sb = new StringBuilder();
									for (int i  = 0; i < headers.length; i ++) {
										int fpermission = permissions[i];
										if ((ip.getPermission() & fpermission) == fpermission) {
											sb.append(headers[i]);
											sb.append("\n"); //$NON-NLS-1$
										}
									}
									if (sb.length() > 0) return sb.substring(0, sb.length() - 1);
									return null;
								}

								@Override
								public Image getImage(Object element) {
									IntelPermission ip = ((RowData)element).getPermission(p);
									if (ip == null) return null;
									
									for (int i  = 0; i < headers.length; i ++) {
										int fpermission = permissions[i];
										if ((ip.getPermission() & fpermission) == fpermission) {

											return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CHECK);
										}
									}
									return null;
								}
								
							});
							tlayout.setColumnData(col2.getColumn(), new ColumnWeightData(1));
						}
						
						tblViewer.setInput(data);
						tblViewer.getControl().getParent().layout(true);
					}
				});
				
			}
			return Status.OK_STATUS;
		}};
		
		class RowData{
			Employee e;
			Map<IntelProfile, IntelPermission> pp;
			
			public RowData(Employee e) {
				this.e = e;
				pp = new HashMap<>();
			}
			
			public void addPermission(IntelPermission s) {
				pp.put(s.getProfile(), s);
			}
			
			public Employee getEmployee() {
				return this.e;
			}
			
			public IntelPermission getPermission(IntelProfile p) {
				return pp.get(p);
			}
		}
}
