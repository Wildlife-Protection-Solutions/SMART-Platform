package org.wcs.smart.event.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.event.model.EEventFilter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

public class FiltersPanel extends Composite {

	private TableViewer lstFilters;
	private Composite rightPart;
	
	public FiltersPanel(Composite parent, int style) {
		super(parent, style);
		createComposite();
	}
	
	private void createComposite() {
		setLayout(new GridLayout());
		
		Label l = new Label(this, SWT.NONE);
		l.setText("Lists all the filters configured by the users.");
		
		SashForm parts = new SashForm(this,  SWT.NONE);
		parts.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = new Composite(parts, SWT.BORDER);
		leftPart.setLayout(new GridLayout());
		((GridLayout)leftPart.getLayout()).marginWidth = 0;
		((GridLayout)leftPart.getLayout()).marginHeight = 0;
		
		lstFilters = new TableViewer(leftPart, SWT.V_SCROLL | SWT.NONE | SWT.FULL_SELECTION | SWT.MULTI);
		lstFilters.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstFilters.setContentProvider(ArrayContentProvider.getInstance());
		lstFilters.getTable().setHeaderVisible(true);
		
		TableViewerColumn column1 = new TableViewerColumn(lstFilters, SWT.NONE);
		column1.getColumn().setText("Filter");
		column1.getColumn().setWidth(200);
		
		column1.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof EEventFilter) return ((EEventFilter) element).getId();
				return super.getText(element);
			}
		});
		
		lstFilters.setInput(new String[] {DialogConstants.LOADING_TEXT});
		lstFilters.addDoubleClickListener(e->editFilter());
		
		Menu filterMenu = new Menu(lstFilters.getControl());
		
		MenuItem addMnu = new MenuItem(filterMenu, SWT.PUSH);
		addMnu.setText(DialogConstants.ADD_BUTTON_TEXT);
		addMnu.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addMnu.addListener(SWT.Selection, e->addFilter());
		
		MenuItem editMnu = new MenuItem(filterMenu, SWT.PUSH);
		editMnu.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editMnu.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editMnu.addListener(SWT.Selection, e->editFilter());
		
		MenuItem deleteMnu = new MenuItem(filterMenu, SWT.PUSH);
		deleteMnu.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteMnu.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteMnu.addListener(SWT.Selection, e->deleteFilter());
		
		lstFilters.getControl().setMenu(filterMenu);
		
		editMnu.setEnabled(false);
		deleteMnu.setEnabled(false);
		
		lstFilters.addSelectionChangedListener(e->{
			Object x = lstFilters.getStructuredSelection().getFirstElement();
			boolean isSelected = (x != null && x instanceof EEventFilter);
			editMnu.setEnabled(isSelected);
			deleteMnu.setEnabled(isSelected);
			updateDetails();
		});
		
		
		Composite rightPartOuter = new Composite(parts, SWT.BORDER);
		rightPartOuter.setLayout(new GridLayout());
		((GridLayout)rightPartOuter.getLayout()).marginWidth = 0;
		((GridLayout)rightPartOuter.getLayout()).marginHeight = 0;
		
		rightPart = new Composite(rightPartOuter, SWT.NONE);
		rightPart.setLayout(new GridLayout());
		rightPart.setBackground(parts.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		rightPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		parts.setWeights(new int[] {4, 4});
		
		updateDetails();
		//TODO: only schedule once tab is activated
		System.out.println("HERE");
		loadEventsJob.schedule();
	}
	
	private void addFilter() {
		NewFilterDialog dialog = new NewFilterDialog(getShell());
		dialog.open();
		loadEventsJob.schedule();
	}
	
	public void editFilter() {
		Object x = lstFilters.getStructuredSelection().getFirstElement();
		if (!(x instanceof EEventFilter)) return;
		EEventFilter toUpdate = (EEventFilter)x;
		
		NewFilterDialog dialog = new NewFilterDialog(getShell(), toUpdate);
		dialog.open();
		loadEventsJob.schedule();
	}
	
	public void deleteFilter() {
		List<EEventFilter> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = lstFilters.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object efilter = (Object) iterator.next();
			if (efilter instanceof EEventFilter) toDelete.add((EEventFilter)efilter);
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected filters? This action cannot be undone.", toDelete.size() ))){
			return;
		}
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (EEventFilter e : toDelete) {
					List<EActionEvent> events = QueryFactory.buildQuery(session, EActionEvent.class, new Object[] {"event", e}).list();
					events.forEach(ae->session.delete(ae));
					session.delete(e);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				EventPlugIn.displayLog("Unable to delete selected filters: " + ex.getMessage(), ex);
				toDelete.clear();
			}
		}
		((List)lstFilters.getInput()).removeAll(toDelete);
		lstFilters.refresh();
		lstFilters.setSelection(null);
	}
	
	private void createToolbar(Composite parent, boolean hasSelection) {
		ToolBar tb = new ToolBar(parent,  SWT.FLAT);
		tb.setBackground(parent.getBackground());
		
		ToolItem addItem = new ToolItem(tb, SWT.PUSH);
		addItem.setToolTipText("create a new filter");
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.addListener(SWT.Selection, e->addFilter());
		
		ToolItem editItem = new ToolItem(tb, SWT.PUSH);
		editItem.setToolTipText("edit selected filter");
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editItem.addListener(SWT.Selection, e->editFilter());
		editItem.setEnabled(hasSelection);
		
		ToolItem deleteItem = new ToolItem(tb, SWT.PUSH);
		deleteItem.setToolTipText("delete the selected filter");
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addListener(SWT.Selection, e->deleteFilter());
		deleteItem.setEnabled(hasSelection);
	}
	
	private void updateDetails() {
		for (Control k : rightPart.getChildren()) k.dispose();
		Object element = lstFilters.getStructuredSelection().getFirstElement();
		if (!(element instanceof EEventFilter)) {
			
			Composite headerPart = new Composite(rightPart, SWT.NONE);
			headerPart.setLayout(new GridLayout(2, false));
			headerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			headerPart.setBackground(rightPart.getBackground());		
			((GridLayout)headerPart.getLayout()).marginWidth = 0;
			((GridLayout)headerPart.getLayout()).marginHeight = 0;
			
			Label l = new Label(headerPart, SWT.NONE);
			l.setBackground(rightPart.getBackground());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			FontData fd = l.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			fd.setHeight(fd.getHeight() + 1);
			Font boldFont = new Font(l.getDisplay(), fd);
			l.addListener(SWT.Dispose, e->boldFont.dispose());
			l.setFont(boldFont);
			l.setBackground(rightPart.getBackground());
			
			createToolbar(headerPart, false);
			
			rightPart.layout(true);
			return;
		}
		EEventFilter filter = (EEventFilter) element;
		
		Composite headerPart = new Composite(rightPart, SWT.NONE);
		headerPart.setLayout(new GridLayout(2, false));
		headerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		headerPart.setBackground(rightPart.getBackground());		
		((GridLayout)headerPart.getLayout()).marginWidth = 0;
		((GridLayout)headerPart.getLayout()).marginHeight = 0;
		
		Label l = new Label(headerPart, SWT.NONE);
		l.setText(filter.getId());
		l.setBackground(rightPart.getBackground());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont.dispose());
		l.setFont(boldFont);
		l.setBackground(rightPart.getBackground());
		
		createToolbar(headerPart, true);
		
		l = new Label(rightPart, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ScrolledComposite scroll = new ScrolledComposite(rightPart, SWT.V_SCROLL );
		scroll.setBackground(rightPart.getBackground());
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite content = new Composite(scroll, SWT.NONE);
		content.setBackground(rightPart.getBackground());
		content.setLayout(new GridLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scroll.setContent(content);
		
		l = new Label(content, SWT.NONE);
		l.setText("Filter");
		l.setBackground(rightPart.getBackground());
		fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont2 = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont2.dispose());
		l.setFont(boldFont2);
		
		l = new Label(content, SWT.NONE);
		l.setText(filter.getFilterString());
		l.setBackground(rightPart.getBackground());
		
		rightPart.layout(true);
		content.setSize(content.computeSize(scroll.getSize().x-20, SWT.DEFAULT));
		scroll.addListener(SWT.Resize, e->{
			content.setSize(content.computeSize(scroll.getSize().x-20, SWT.DEFAULT));	
		});
		
	}

	private Job loadEventsJob = new Job("loading filters") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> selectedItems = new ArrayList<>();
			Display.getDefault().syncExec(()->{
				for (Iterator<?> iterator = lstFilters.getStructuredSelection().iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					selectedItems.add(object);
				}
			});
			
			List<EEventFilter> filters = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				filters.addAll(QueryFactory.buildQuery(session, EEventFilter.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
			}
			Display.getDefault().syncExec(()->{
				if (lstFilters.getControl().isDisposed()) return;
				lstFilters.setInput(filters);
				lstFilters.setSelection(new StructuredSelection(selectedItems));
			});
			return Status.OK_STATUS;
		}
		
	};
}