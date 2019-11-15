package org.wcs.smart.i2.ui.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.SmartShellDialog;


public class ProfileFilterHandler {
	
	@Execute
	public void execute(IEclipseContext context) {
		
		EModelService m = context.get(EModelService.class);
		ToolItem ti = null;
		MUIElement miItem = m.find("org.wcs.smart.i2.menu.profile.filter", context.get(MWindow.class)); 
		if (miItem != null && miItem.getWidget() instanceof ToolItem) {
			ti = (ToolItem) miItem.getWidget();
		}
		
		Composite cc = ti.getParent();
		
		Point pnt = cc.getParent().toDisplay(cc.getLocation());
		
		List<IntelProfile> profiles = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			profiles.addAll(ProfilesManager.INSTANCE.getProfiles(session, true));
		}
		
		Set<IntelProfile> active = new HashSet<>(ProfilesManager.INSTANCE.getActiveProfiles());
		
		ProfileSelectionShell shell = new ProfileSelectionShell(context, profiles, active);
		shell.open(new Point(pnt.x,pnt.y + cc.getSize().y));
		
		ToolItem fti = ti;
		shell.addListener(SWT.Dispose, e->fti.setSelection(false));
	}
	
	// E3
	public static class ProfileFilterHandlerWrapper extends DIHandler<ProfileFilterHandler> {
		public ProfileFilterHandlerWrapper() {
			super(ProfileFilterHandler.class);
		}
	}
	
	private class ProfileSelectionShell extends SmartShellDialog{
		
		private List<IntelProfile> profiles;
		private Set<IntelProfile> active;
		private CheckboxTableViewer viewer;
		private IEclipseContext context;
		
		public ProfileSelectionShell(IEclipseContext context, List<IntelProfile> profiles, Set<IntelProfile> active) {
			super(context.get(Shell.class), SWT.NO_TRIM);
			this.profiles = profiles;
			this.active = active;
			this.context = context;
		}
		
		
		@Override
		public void createContents(Composite parent) {
			parent.setLayout(new GridLayout());
			((GridLayout)parent.getLayout()).marginWidth = 0;
			((GridLayout)parent.getLayout()).marginHeight = 0;
			
			Composite t = new Composite(parent, SWT.NONE);
			t.setLayout(new TableColumnLayout());
			t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			viewer = CheckboxTableViewer.newCheckList(t, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.setLabelProvider(new ProfileLabelProvider());
			viewer.setInput(profiles);
			for (IntelProfile p : active) viewer.setChecked(p, true);
			
			viewer.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(viewer));	
			viewer.getTable().addSelectionListener(new SelectionListener() {				
				@Override
				public void widgetSelected(SelectionEvent e) {
					
					if ((e.stateMask & SWT.BUTTON3) != 0) return;
					if (e.detail != SWT.CHECK && ((e.stateMask & SWT.SHIFT) == 0) && ((e.stateMask & SWT.CTRL) == 0)) {
						for (Iterator<?> i = viewer.getStructuredSelection().iterator(); i.hasNext();) {
							Object x = i.next();
							viewer.setChecked(x, !viewer.getChecked(x));
						}
					}
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			
			TableColumn tc = new TableColumn(viewer.getTable(), SWT.NONE);
			((TableColumnLayout)t.getLayout()).setColumnData(tc, new ColumnWeightData(1));

			viewer.getControl().addListener(SWT.Dispose, e->{
				active.clear();
				for (Object x : viewer.getCheckedElements()) {
					active.add((IntelProfile)x);
				}
				//TODO: only fire if actually changed
				ProfilesManager.INSTANCE.setActiveProfiles(this.active, context.get(IEventBroker.class));
			});
			
			Menu menu = new Menu(viewer.getControl());
			
			MenuItem miAll = new MenuItem(menu, SWT.PUSH);
			miAll.setText("Select All");
			miAll.addListener(SWT.Selection, e->viewer.setAllChecked(true));
						
			MenuItem miNone = new MenuItem(menu, SWT.PUSH);
			miNone.setText("Select None");
			miNone.addListener(SWT.Selection, e->viewer.setAllChecked(false));
			
			viewer.getControl().setMenu(menu);
		}
	}
}
