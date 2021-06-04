/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui.run;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Twistie;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsFileManager;
import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.engine.PawsStatusJob;
import org.wcs.smart.paws.engine.PawsTask;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsResultFile;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsRun.Status;
import org.wcs.smart.paws.model.PawsService;
import org.wcs.smart.paws.ui.HeaderComposite;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SmartFileUtils;

/**
 * PAWS Editor summary page
 * @author Emily
 *
 */
public class RunSummaryPage extends EditorPart {

	private static final String RUN_KEY = "RUN"; //$NON-NLS-1$
	private HeaderComposite header;
	private Label lblStatus, lblStatus2, lblStatusImg;
	private ComboViewer cmbTimeFrame ;
	
	private Composite statusComp ;
	private Composite detailsComp ;
	
	private Button btnRetry;
	
	private RunEditor parent;
	private FormToolkit toolkit;

	private PawsResultManager mgr = null;
	
	public RunSummaryPage(RunEditor parent) {
		this.parent = parent;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void dispose() {
		super.dispose();
		toolkit.dispose();
	}
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private ScrolledForm main;
	
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		
		main = toolkit.createScrolledForm(parent);
		main.getBody().setLayout(new GridLayout()); 
		
		header = new HeaderComposite(main.getBody(), toolkit, main.getFont(), main.getForeground());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.addListener(SWT.Selection, e->{
			if (header.getData(RUN_KEY) == null) return;
			PawsRun run = (PawsRun)header.getData(RUN_KEY);
			run.setId(header.getText());
			
			try(Session s = HibernateManager.openSession()){
				PawsRun toupdate = s.get(PawsRun.class, run.getUuid());
				s.beginTransaction();
				try{
					toupdate.setId(run.getId());
					s.getTransaction().commit();
				}catch (Exception ex){
					try{ s.getTransaction().rollback(); }catch (Exception ex2){ PawsPlugIn.log(ex2.getMessage(),ex2); }
					PawsPlugIn.displayLog(ex.getMessage(), ex);
				}
			}
			RunSummaryPage.this.parent.setPartName(run.getId());
			HashMap<Object,Object> info = new HashMap<>();
			info.put(IEventBroker.DATA, Collections.singletonList(info));
			info.put(RunEditor.class.toString(), RunSummaryPage.this.parent);
			RunSummaryPage.this.parent.getContext().get(IEventBroker.class).post(PawsEvent.PAWS_RUN_MODIFY, info);
			
		});

		createStatusSection(main.getBody());
		createResultsSection(main.getBody());
		createStatusDetailsSection(main.getBody());
		createDetailsSection(main.getBody());
	}
	
	
	private void createStatusSection(Composite parent) {
		Composite c = SmartUiUtils.createHeaderLabel(parent, Messages.RunSummaryPage_StatusSection);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginHeight = 2;
		
		ToolBar tb = new ToolBar(c, SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		ToolItem ti = new ToolItem(tb, SWT.PUSH);
		ti.setToolTipText(Messages.RunSummaryPage_refreshtooltip);
		ti.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		ti.addListener(SWT.Selection, e->RunSummaryPage.this.parent.refresh());

		Composite body = toolkit.createComposite(parent, SWT.NONE);
		body.setLayout(new GridLayout());
		((GridLayout)body.getLayout()).marginWidth = 0;
		((GridLayout)body.getLayout()).marginHeight = 0;
		((GridLayout)body.getLayout()).verticalSpacing = 0;
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Composite statusArea = toolkit.createComposite(body);
		statusArea.setLayout(new GridLayout(4, false));
		((GridLayout)statusArea.getLayout()).marginHeight = 0;
		lblStatusImg = toolkit.createLabel(statusArea, ""); //$NON-NLS-1$
		
		lblStatus = toolkit.createLabel(statusArea, ""); //$NON-NLS-1$
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		lblStatus2 = toolkit.createLabel(statusArea, ""); //$NON-NLS-1$
		lblStatus2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		btnRetry = toolkit.createButton(statusArea, Messages.RunSummaryPage_RetryButton,  SWT.PUSH);
		btnRetry.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		btnRetry.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		btnRetry.addListener(SWT.Selection, e->{
			if (mgr != null) {
				btnRetry.setEnabled(false);
				lblStatus.setText(Messages.RunSummaryPage_AttemptReAuth);
				lblStatus.getParent().layout(true);
				PawsStatusJob.getInstance().addItem(mgr.getRun());
			}
		});
		btnRetry.setVisible(false);
	}
	
	private void showHideStatusDetails(boolean expanded) {
		showHidePanel(expanded, statusComp);
	}
	
	private void showHidePawsDetails(boolean expanded) {
		showHidePanel(expanded, detailsComp);
	}
	
	private void showHidePanel(boolean expanded, Composite panel) {
		if (expanded) {
			panel.setVisible(true);
			((GridData)panel.getLayoutData()).heightHint = panel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		}else {
			panel.setVisible(false);
			((GridData)panel.getLayoutData()).heightHint = 0;
		}
		main.reflow(true);
	}
	
	private void createStatusDetailsSection(Composite parent) {
		Composite part = SmartUiUtils.createHeaderLabel(parent, Messages.RunSummaryPage_StatusDetailsSection);
		
		Label headerLabel = (Label) part.getChildren()[0];
		
		((GridLayout)part.getLayout()).numColumns = 2;
		
		Twistie tw = new Twistie(part, SWT.NONE);
		tw.setBackground(part.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		tw.moveAbove(headerLabel);
		
		headerLabel.addListener(SWT.MouseEnter, e->headerLabel.setCursor(headerLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND)));
		headerLabel.addListener(SWT.MouseExit, e->headerLabel.setCursor(null));
		headerLabel.addListener(SWT.MouseDown,e->{
			tw.setExpanded(!tw.isExpanded());
			showHideStatusDetails(tw.isExpanded());
		});

		tw.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showHideStatusDetails(tw.isExpanded());
			}
		});
		
		statusComp = toolkit.createComposite(parent, SWT.NONE);
		statusComp.setLayout(new GridLayout());
		statusComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		showHideStatusDetails(tw.isExpanded());

	}
	
	private void createDetailsSection(Composite parent) {
		Composite part = SmartUiUtils.createHeaderLabel(parent, Messages.RunSummaryPage_PawsRunSectionSettings);
		
		Label headerLabel = (Label) part.getChildren()[0];
		
		((GridLayout)part.getLayout()).numColumns = 2;
		
		Twistie tw = new Twistie(part, SWT.NONE);
		tw.setBackground(part.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		tw.moveAbove(headerLabel);
		
		headerLabel.addListener(SWT.MouseEnter, e->headerLabel.setCursor(headerLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND)));
		headerLabel.addListener(SWT.MouseExit, e->headerLabel.setCursor(null));
		headerLabel.addListener(SWT.MouseDown,e->{
			tw.setExpanded(!tw.isExpanded());
			showHidePawsDetails(tw.isExpanded());
		});

		tw.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showHidePawsDetails(tw.isExpanded());
			}
		});
		
		detailsComp = toolkit.createComposite(parent);
		detailsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		showHidePawsDetails(tw.isExpanded());

	}
	
	private void createResultsSection(Composite parent) {
		SmartUiUtils.createHeaderLabel(parent, Messages.RunSummaryPage_ViewingResultsSection);
		Composite resultscomp = toolkit.createComposite(parent);
		resultscomp.setLayout(new GridLayout(2, false));
		resultscomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		
		toolkit.createLabel(resultscomp, "View Results For Time Period:"); //$NON-NLS-1$
		
		cmbTimeFrame = new ComboViewer(resultscomp, SWT.READ_ONLY | SWT.DROP_DOWN);
		toolkit.adapt(cmbTimeFrame.getControl(), true, true);
		cmbTimeFrame.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbTimeFrame.setContentProvider(ArrayContentProvider.getInstance());
		cmbTimeFrame.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((PawsResultFile)element).getTimeFrameString();
			}
		});
		
		Composite btnComp = toolkit.createComposite(resultscomp);
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnComp.setLayout(new GridLayout(2, false));
		((GridLayout)btnComp.getLayout()).marginWidth = 0;
		((GridLayout)btnComp.getLayout()).marginHeight = 0;
		
		Button btnView = new Button(btnComp, SWT.PUSH);
		btnView.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
		btnView.setText(Messages.RunSummaryPage_MapButton);
		btnView.setBackground(resultscomp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnView.setEnabled(false);
		
		btnView.addListener(SWT.Selection,e->{
			RunSummaryPage.this.parent.showMap();
		});
		
		Button btnViewTable = new Button(btnComp, SWT.PUSH);
		btnViewTable.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.TABLE_ICON));
		btnViewTable.setText(Messages.RunSummaryPage_TableButton);
		btnViewTable.setBackground(resultscomp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnViewTable.setEnabled(false);
		
		btnViewTable.addListener(SWT.Selection,e->{
			RunSummaryPage.this.parent.showTable();
		});
		
		cmbTimeFrame.addSelectionChangedListener(e->{
			btnView.setEnabled( !cmbTimeFrame.getStructuredSelection().isEmpty() && cmbTimeFrame.getStructuredSelection().getFirstElement() instanceof PawsResultFile);
			btnViewTable.setEnabled( !cmbTimeFrame.getStructuredSelection().isEmpty() && cmbTimeFrame.getStructuredSelection().getFirstElement() instanceof PawsResultFile);

			this.parent.updateResultsView();
		});
	}

	public void init(PawsRun run) {
		if (statusComp.isDisposed()) return;
		String surl = ""; //$NON-NLS-1$
		for (Control c : statusComp.getChildren()) c.dispose();
		
		Composite sgroup = toolkit.createComposite(statusComp);
		sgroup.setLayout(new GridLayout(2, false));
		sgroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(sgroup, Messages.RunSummaryPage_StatusLabel);
		toolkit.createLabel(sgroup, run.getStatusMessage() == null ? "" : run.getStatusMessage()); //$NON-NLS-1$
		
		if (run.getRunId() != null) {
			try(Session session = HibernateManager.openSession()){
				PawsService service = QueryFactory.buildQuery(session, PawsService.class,  
					new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult(); //$NON-NLS-1$
				surl = service.getTaskApiUrl() + "/" + run.getTaskId();// + "?subscription-key=" + service.getApiKey(); //$NON-NLS-1$
				//key = service.getApiKey();	
			}			
		
			if (run.getServerStatusJson() != null) {

				toolkit.createLabel(sgroup, Messages.RunSummaryPage_TaskURL);
				toolkit.createLabel(sgroup, surl);
				
				try {
					PawsTask task = PawsTask.parse(run.getServerStatusJson());
				
					toolkit.createLabel(sgroup, Messages.RunSummaryPage_Status);
					toolkit.createLabel(sgroup, task.getStatus());
				
					toolkit.createLabel(sgroup, Messages.RunSummaryPage_Id);
					toolkit.createLabel(sgroup, task.getTaskId());
					
					if (task.getTimestamp() != null) {
						toolkit.createLabel(sgroup, Messages.RunSummaryPage_Timestamp);
						toolkit.createLabel(sgroup, task.getTimestamp().toString());
					}
					if (task.getEndPoint() != null) {
						toolkit.createLabel(sgroup, Messages.RunSummaryPage_endpoint);
						toolkit.createLabel(sgroup, task.getEndPoint());
					}
					if (task.getEndPointPath() != null) {
						toolkit.createLabel(sgroup, Messages.RunSummaryPage_endpointPath);
						toolkit.createLabel(sgroup, task.getEndPointPath());
					}
					if (task.getPublishToGrid() != null) {
						toolkit.createLabel(sgroup, Messages.RunSummaryPage_pubToGrid);
						toolkit.createLabel(sgroup, task.getPublishToGrid() ? SmartLabelProvider.BOOLEAN_TRUE_LABEL : SmartLabelProvider.BOOLEAN_FALSE_LABEL);
					}
					if (task.getBody() != null) {
						toolkit.createLabel(sgroup, Messages.RunSummaryPage_Body);
						toolkit.createLabel(sgroup, task.getBody());
					}
				}catch (Exception ex) {
					PawsPlugIn.log(ex.getMessage(),  ex);
				}
			}
		}
		statusComp.layout(true);
		btnRetry.setVisible(run.getStatus() == Status.AUTH_TIMEOUT);
		
		lblStatusImg.setImage(PawsManager.INSTANCE.getImage(run.getStatus()));
		
		
		lblStatus.setText(PawsManager.INSTANCE.getStatusLabel(run.getStatus()));
		if (run.getStatusMessage() != null && run.getStatus().isRunning()) {
			lblStatus2.setText(run.getStatusMessage());
		}else {
			lblStatus2.setText(""); //$NON-NLS-1$
		}
		
		lblStatusImg.getParent().getParent().layout(true);
		
		header.setText(run.getId());
		header.setData(RUN_KEY, run);
		
		for (Control kid : detailsComp.getChildren()) kid.dispose();
		
		detailsComp.setLayout(new GridLayout(2, false));
		
		toolkit.createLabel(detailsComp, Messages.RunSummaryPage_RunId);
		toolkit.createLabel(detailsComp, run.getRunId());
		
		toolkit.createLabel(detailsComp, Messages.RunSummaryPage_ExecutedDate);
		toolkit.createLabel(detailsComp, run.getRunDate() == null ? "" : run.getRunDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))); //$NON-NLS-1$
		
		toolkit.createLabel(detailsComp, Messages.RunSummaryPage_DataDates);
		if (run.getDataEndDate() != null && run.getDataStartDate() != null){
			String value = run.getDataStartDate().format( DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) ) +
					Messages.RunSummaryPage_To + run.getDataEndDate().format( DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) );
			toolkit.createLabel(detailsComp, value);
		}else{
			toolkit.createLabel(detailsComp, ""); //$NON-NLS-1$
		}
		
		toolkit.createLabel(detailsComp, Messages.RunSummaryPage_TrainingDates);
		String value = run.getTrainStartYear() + Messages.RunSummaryPage_To + run.getTrainEndYear();
		toolkit.createLabel(detailsComp, value);
		
		toolkit.createLabel(detailsComp, Messages.RunSummaryPage_ForcastingDate);
		value = run.getForecastStartYear() + Messages.RunSummaryPage_To + run.getForecastEndYear();
		toolkit.createLabel(detailsComp, value);
		
		
		toolkit.createLabel(detailsComp, Messages.RunSummaryPage_Configuration);
		if (run.getConfiguration() != null){
			toolkit.createLabel(detailsComp, run.getConfiguration().getName());
		}else{
			toolkit.createLabel(detailsComp, Messages.RunSummaryPage_NotFound);
		}
		
		toolkit.createLabel(detailsComp, Messages.RunSummaryPage_PackageFile);
		if (run.getPackageFile() != null){
			Hyperlink openhl = toolkit.createHyperlink(detailsComp, run.getPackageFile(), SWT.NONE);
			openhl.addHyperlinkListener(new IHyperlinkListener() {
				
				@Override
				public void linkExited(HyperlinkEvent e) {}
				
				@Override
				public void linkEntered(HyperlinkEvent e) {}
				
				@Override
				public void linkActivated(HyperlinkEvent e) {
					try {
						Path f =  PawsFileManager.INSTANCE.getDirectory(run);
						SmartFileUtils.openFileBrowser(f);
					} catch (IOException e1) {
						PawsPlugIn.displayLog(e1.getMessage(), e1);
					}
					
				}
			});
		}else{
			toolkit.createLabel(detailsComp, ""); //$NON-NLS-1$
		}
		
		
		toolkit.createLabel(detailsComp, Messages.RunSummaryPage_Resultsfiles);
		toolkit.createLabel(detailsComp, run.getResultLocation());
		
		detailsComp.layout(true);
		header.getParent().layout(true);
		
	}
	
	public void refresh(final PawsResultManager results) {
		mgr = results;
		if (!results.getResults().isEmpty()) {
			List<PawsResultFile> files = new ArrayList<>(results.getResults());
			files.sort((a,b)->{
				int i1 = getMonth(a.getTimeFrameString());
				int i2 = getMonth(b.getTimeFrameString());
				return Integer.compare(i1, i2);
			});
			cmbTimeFrame.setInput(files);
			cmbTimeFrame.setSelection(new StructuredSelection(results.getResults().get(0)));
		}
	}
	
	private static final int getMonth(String s1) {
		if (s1.toLowerCase().startsWith("jan")) return 0; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("feb")) return 1; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("mar")) return 2; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("apr")) return 3; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("may")) return 4; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("jun")) return 5; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("jul")) return 6; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("aug")) return 7; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("sep")) return 8; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("oct")) return 9; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("nov")) return 10; //$NON-NLS-1$
		if (s1.toLowerCase().startsWith("dec")) return 11; //$NON-NLS-1$
		return -1;
	}
	public PawsResultFile getResultsSelection() {
		return (PawsResultFile) cmbTimeFrame.getStructuredSelection().getFirstElement();
	}

	@Override
	public void setFocus() {
		lblStatus.setFocus();
	}

}
