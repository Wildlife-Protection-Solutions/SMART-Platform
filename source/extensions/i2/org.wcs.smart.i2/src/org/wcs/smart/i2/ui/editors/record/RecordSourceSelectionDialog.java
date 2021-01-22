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
package org.wcs.smart.i2.ui.editors.record;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.Thumbnail;

/**
 * Dialog where users can search for and select 
 * a waypoint.  Waypoints can be searched for based
 * on the waypoint source and waypoint date
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class RecordSourceSelectionDialog extends SmartStyledTitleDialog{

	private static final int MAX_RESULTS = 1000;
	private Session session;
	
	private TableViewer tWaypoints;
	private ComboViewer cmbSource;
	private DateFilterDropDownComposite dFilter ;
	
	private Waypoint selection;
	private LocalDate targetDate;
	
	private Composite waypointDetailsComp;
	private Font boldFont;
	
	public RecordSourceSelectionDialog(Shell parent, LocalDate targetDate) {
		
		super(parent);
		this.targetDate = targetDate;
	}

	public Waypoint getWaypoint() {
		return this.selection;
	}
	
	@Override
	public int open() {
		session = HibernateManager.openSession();
		return super.open();
	}
	
	@Override
	public boolean close() {
		session.close();
		return super.close();
	}
		
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	protected void okPressed() {
		selection = (Waypoint) tWaypoints.getStructuredSelection().getFirstElement();
		if (selection == null) return;
		
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed() {
		selection = null;
		super.cancelPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartUiUtils.createHeaderLabel(main, Messages.RecordSourceSelectionDialog_FiltersSectionTitle);
		
		Composite filterComp = new Composite(main, SWT.NONE);
		filterComp.setLayout(new GridLayout(2, false));
		filterComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label l = new Label(filterComp, SWT.NONE);
		l.setText(Messages.RecordSourceSelectionDialog_SourceFilter);
		
		cmbSource = new ComboViewer(filterComp, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbSource.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((IWaypointSource)element).getName(Locale.getDefault());
			}
		});
		cmbSource.setContentProvider(ArrayContentProvider.getInstance());
		cmbSource.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Collection<IWaypointSource> srcs = WaypointSourceEngine.INSTANCE.getSupportedSources();
		cmbSource.setInput(srcs);
		if (!srcs.isEmpty()) cmbSource.setSelection(new StructuredSelection(srcs.iterator().next()));

		l = new Label(filterComp, SWT.NONE);
		l.setText(Messages.RecordSourceSelectionDialog_DateFilter);
		
		DateFilterComposite.DateFilter[] filters = new DateFilterComposite.DateFilter[] {
				DateFilterComposite.DateFilter.LAST_30_DAYS,
				DateFilterComposite.DateFilter.LAST_60_DAYS,
				DateFilterComposite.DateFilter.CURRENT_YEAR,
				DateFilterComposite.DateFilter.CUSTOM,
		};
		
		DateFilter defaultFilter = DateFilter.LAST_30_DAYS;
		LocalDate[] dates = null;
		if (targetDate != null) {
			defaultFilter = DateFilter.CUSTOM;
			dates = new LocalDate[] {targetDate.minusDays(2), targetDate.plusDays(2)};
		}
		dFilter = new DateFilterDropDownComposite(filterComp, filters, defaultFilter, true) ;
		dFilter.setDateFilter(defaultFilter, dates);
		dFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		new Label(filterComp, SWT.NONE);
		
		Button btnSearch = new Button(filterComp, SWT.PUSH);
		btnSearch.setText(Messages.RecordSourceSelectionDialog_SearchButton);
		btnSearch.addListener(SWT.Selection, e->searchWaypoints());
		btnSearch.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		btnSearch.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnSearch.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RUN_ICON));
		((GridData)btnSearch.getLayoutData()).widthHint = (int)(btnSearch.computeSize(SWT.DEFAULT,  SWT.DEFAULT).x * 1.2);
		
		SmartUiUtils.createHeaderLabel(main, Messages.RecordSourceSelectionDialog_ResultsSection);
		
		SashForm wpComp = new SashForm(main, SWT.HORIZONTAL);
//		wpComp.setLayout(new GridLayout(2, false));
		wpComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)wpComp.getLayoutData()).heightHint = 200;
		
		tWaypoints = new TableViewer(wpComp, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE); 
		tWaypoints.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tWaypoints.getTable().setLinesVisible(false);
		tWaypoints.getTable().setHeaderVisible(true);
		tWaypoints.setContentProvider(ArrayContentProvider.getInstance());
		tWaypoints.addSelectionChangedListener(e->{
			if (tWaypoints.getSelection().isEmpty()) {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			}else {
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
			showWaypointDetails();
		});
		tWaypoints.addDoubleClickListener(e->okPressed());
		
		TableViewerColumn srcColumn = new TableViewerColumn(tWaypoints, SWT.NONE);
		srcColumn.getColumn().setText(Messages.RecordSourceSelectionDialog_SourceColumnName);
		
		srcColumn.getColumn().setWidth(110);
		srcColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Waypoint) {
					String srcKey = ((Waypoint)element).getSourceId();
					return WaypointSourceEngine.INSTANCE.getSource(srcKey).getName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn dateColumn = new TableViewerColumn(tWaypoints, SWT.NONE);
		dateColumn.getColumn().setText(Messages.RecordSourceSelectionDialog_DateTimeColumnName);
		dateColumn.getColumn().setWidth(180);
		dateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Waypoint) {
					return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format( ((Waypoint)element).getDateTime() );
				}
				return super.getText(element);
			}
		});

		TableViewerColumn labelColumn = new TableViewerColumn(tWaypoints, SWT.NONE);
		labelColumn.getColumn().setText(Messages.RecordSourceSelectionDialog_DetailsColumnName);
		labelColumn.getColumn().setWidth(300);
		labelColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Waypoint) {
					String srcKey = ((Waypoint)element).getSourceId();
					return WaypointSourceEngine.INSTANCE.getSource(srcKey).getSourceLabel(element, session, Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		
		waypointDetailsComp = new Composite(wpComp, SWT.NONE);
		waypointDetailsComp.setLayout(new GridLayout());
		waypointDetailsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		((GridLayout)waypointDetailsComp.getLayout()).marginWidth = 0;
		((GridLayout)waypointDetailsComp.getLayout()).marginHeight = 0;
		
		wpComp.setWeights(new int[] {6,4});
		
		setTitle(Messages.RecordSourceSelectionDialog_Title);
		getShell().setText(Messages.RecordSourceSelectionDialog_Title);
		setMessage(Messages.RecordSourceSelectionDialog_Message);
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);	
		boldFont = new Font(parent.getDisplay(), fd);
		parent.addListener(SWT.Dispose, e->boldFont.dispose());
		
		searchWaypoints();
		return parent;
	}
	
	private void showWaypointDetails() {
		for (Control c : waypointDetailsComp.getChildren()) c.dispose();
		
		Waypoint wp = (Waypoint) tWaypoints.getStructuredSelection().getFirstElement();
		if (wp == null) return;
		
		
		ScrolledComposite scroll = new ScrolledComposite(waypointDetailsComp,  SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER );
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setBackground(waypointDetailsComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Composite owner = new Composite(scroll, SWT.NONE);
		owner.setLayout(new GridLayout());
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		owner.setBackground(waypointDetailsComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		scroll.setContent(owner);
		
		SmartUiUtils.createHeaderLabel(owner, MessageFormat.format(Messages.RecordSourceSelectionDialog_WaypointId, wp.getId(), DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(wp.getDateTime())));

		Composite obsinfo = new Composite(owner, SWT.NONE);
		obsinfo.setLayout(new GridLayout(2, false));
		obsinfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		obsinfo.setBackground(waypointDetailsComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));			
		
		for (WaypointObservationGroup group : wp.getObservationGroups()) {
			if (wp.getObservationGroups().size() > 1) {
				Composite t = SmartUiUtils.createSubHeaderLabel(obsinfo, Messages.RecordSourceSelectionDialog_GroupLabel);
				((GridData)t.getLayoutData()).horizontalSpan = 2;
			}
						
			for (WaypointObservation o : group.getObservations()) {
				Label l = new Label(obsinfo, SWT.NONE);
				l.setText(o.getCategory().getName());
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				l.setFont(boldFont);
				l.setBackground(waypointDetailsComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				for (WaypointObservationAttribute a : o.getAttributes()) {
					createInfo(obsinfo, a.getAttribute().getName(), a.getAttributeValueAsString(Locale.getDefault()));
				}
				createAttachments(owner, o.getAttachments());
			}
						
		}
		if (!wp.getAttachments().isEmpty()) {
			Label l = new Label(owner, SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(waypointDetailsComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			createAttachments(owner,  wp.getAttachments());
		}
		 scroll.setMinSize(owner.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		 waypointDetailsComp.layout(true, true);
		 
		 
	}
	
	private void createInfo(Composite parent, String field, String value) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(MessageFormat.format("{0}:", field)); //$NON-NLS-1$
		l.setBackground(waypointDetailsComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		l = new Label(parent, SWT.NONE);
		l.setText(value == null ? "" : value); //$NON-NLS-1$
		l.setBackground(waypointDetailsComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
	}
	
	private void createAttachments(Composite parent, List<? extends ISmartAttachment> attachments) {
		if (attachments.isEmpty()) return;
		
		Composite a = new Composite(parent, SWT.NONE);
		a.setLayout(new GridLayout(3, false));
		a.setBackground(waypointDetailsComp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		for (ISmartAttachment wa : attachments) {
			try {
				wa.computeFileLocation(session);
				Thumbnail t = new Thumbnail(wa,100);
				Composite cc = t.createThumbnail(a);
				cc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				((GridData)cc.getLayoutData()).widthHint = 100;
				((GridData)cc.getLayoutData()).heightHint = 100;
			}catch (Exception ex) {
				ObservationPlugIn.log(ex.getMessage(), ex);
			}
		}
	}
	
	public void searchWaypoints() {
		setErrorMessage(null);
		
		IWaypointSource source = (IWaypointSource) cmbSource.getStructuredSelection().getFirstElement();
		if (source == null) {
			setErrorMessage(Messages.RecordSourceSelectionDialog_SourceRequired);
			return ;
		}
		
		DateFilter dd = dFilter.getDateFilter();
		LocalDate startdate = dd.getStartDate();
		LocalDate enddate = dd.getEndDate();
		if (dd == DateFilter.CUSTOM) {
			startdate = dFilter.getCustomStartDate();
			enddate = dFilter.getCustomEndDate();
		}
		String query = " FROM Waypoint WHERE source = :source "; //$NON-NLS-1$
		if (startdate != null && enddate != null) {
			query += " AND dateTime >= :start AND dateTime <= :end"; //$NON-NLS-1$
		}
		query += " order by dateTime"; //$NON-NLS-1$
		
		List<Waypoint> waypoints = session.createQuery(query, Waypoint.class)
		.setParameter("source", source.getKey()) //$NON-NLS-1$
		.setParameter("start", startdate.atStartOfDay()) //$NON-NLS-1$
		.setParameter("end", enddate.atTime(LocalTime.MAX)) //$NON-NLS-1$
		.setMaxResults(MAX_RESULTS).list();
		
		if (waypoints.size() >= MAX_RESULTS) {
			setErrorMessage(MessageFormat.format(Messages.RecordSourceSelectionDialog_TooManyResults, MAX_RESULTS));
		}
		
		tWaypoints.setInput(waypoints);

	}
}
