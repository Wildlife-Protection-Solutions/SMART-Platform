package org.wcs.smart.event.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

public class EventsPanel extends Composite {

	private TableViewer tblEvents;
	
	public EventsPanel(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout());
		
		Composite main = new Composite(this, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Links filters with actions to create events.");
		
		tblEvents = new TableViewer(main, SWT.BORDER | SWT.FULL_SELECTION);
		tblEvents.setContentProvider(ArrayContentProvider.getInstance());
		tblEvents.getTable().setLinesVisible(true);
		tblEvents.getTable().setHeaderVisible(true);
		tblEvents.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableViewerColumn actionColumn = new TableViewerColumn(tblEvents, SWT.NONE);
		actionColumn.getColumn().setText("Action");
		actionColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof EActionEvent) return ((EActionEvent) element).getAction().getId();
				return super.getText(element);
			}
			@Override
			public Image getImage(Object element) {
				if (element instanceof EActionEvent) return EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_ACTION);
				return super.getImage(element);
			}
		});
		
		TableViewerColumn filterColumn = new TableViewerColumn(tblEvents, SWT.NONE);
		filterColumn.getColumn().setText("Filter");
		filterColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof EActionEvent) return ((EActionEvent) element).getFilter().getId();
				return super.getText(element);
			}
			@Override
			public Image getImage(Object element) {
				if (element instanceof EActionEvent) return EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_FILTER);
				return super.getImage(element);
			}
		});
		
		loadDataJob.schedule();
	}
	
	private Job loadDataJob = new Job("Loading action events") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<EActionEvent> events = new ArrayList<>();
			
			try(Session session = HibernateManager.openSession()){
				events.addAll(QueryFactory.buildQuery(session, EActionEvent.class, new Object[] {"action.conservationArea", SmartDB.getCurrentConservationArea()}).list());
			}
			
			Display.getDefault().syncExec(()->{
				if(tblEvents == null || tblEvents.getControl().isDisposed()) return;
				tblEvents.setInput(events);;
			});
			return Status.OK_STATUS;
		}
		
	};

}
