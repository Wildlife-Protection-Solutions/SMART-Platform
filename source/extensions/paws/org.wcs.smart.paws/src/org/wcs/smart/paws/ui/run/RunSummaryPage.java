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

import java.awt.Desktop;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
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
import org.wcs.smart.paws.engine.PawsTask;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsService;
import org.wcs.smart.paws.ui.HeaderComposite;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * PAWS Editor summary page
 * @author Emily
 *
 */
public class RunSummaryPage extends EditorPart {

	private static final String RUN_KEY = "RUN"; //$NON-NLS-1$
	private HeaderComposite header;
	private Label lblStatus, lblStatusImg, lblStatusMsg;
	
	private Composite statusComp ;
	private Composite detailsComp ;
	
	private RunEditor parent;
	private FormToolkit toolkit;

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

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form main = toolkit.createForm(parent);
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
		
		Composite c = SmartUiUtils.createHeaderLabel(main.getBody(), Messages.RunSummaryPage_StatusSection);
		c.setLayout(new GridLayout(2, false));
		ToolBar tb = new ToolBar(c, SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		ToolItem ti = new ToolItem(tb, SWT.PUSH);
		ti.setToolTipText(Messages.RunSummaryPage_refreshtooltip);
		ti.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		ti.addListener(SWT.Selection, e->RunSummaryPage.this.parent.refresh());
		
		Composite scomp = toolkit.createComposite(main.getBody());
		scomp.setLayout(new GridLayout(2, false));
		scomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		
		lblStatusImg = toolkit.createLabel(scomp, ""); //$NON-NLS-1$
		
		lblStatus = toolkit.createLabel(scomp, ""); //$NON-NLS-1$
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblStatusMsg = toolkit.createLabel(scomp, "", SWT.WRAP); //$NON-NLS-1$
		lblStatusMsg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		statusComp = toolkit.createComposite(scomp);
		statusComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		statusComp.setLayout(new GridLayout());
		((GridLayout)statusComp.getLayout()).marginWidth = 0;
		((GridLayout)statusComp.getLayout()).marginHeight = 0;
		
		SmartUiUtils.createHeaderLabel(main.getBody(), Messages.RunSummaryPage_DetailsSection);
		
		detailsComp = toolkit.createComposite(main.getBody());
		detailsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	}

	public void init(PawsRun run) {
		String surl = ""; //$NON-NLS-1$
		for (Control c : statusComp.getChildren()) c.dispose();
		
		if (run.getRunId() != null) {
			try(Session session = HibernateManager.openSession()){
				PawsService service = QueryFactory.buildQuery(session, PawsService.class,  
					new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult(); //$NON-NLS-1$
				surl = service.getTaskApi() + "/" + run.getTaskId();// + "?subscription-key=" + service.getApiKey(); //$NON-NLS-1$
				//key = service.getApiKey();	
			}			
		
			if (run.getServerStatusJson() != null) {
				Group sgroup = new Group(statusComp, SWT.FLAT);
				sgroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				sgroup.setText(Messages.RunSummaryPage_StatusDetails);
				sgroup.setLayout(new GridLayout(2, false));
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
		
		lblStatusImg.setImage(PawsManager.INSTANCE.getImage(run.getStatus()));
		lblStatus.setText(run.getStatus().name());
		if (run.getStatusMessage() != null) lblStatusMsg.setText(run.getStatusMessage());
		
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
						Desktop.getDesktop().open(  PawsFileManager.INSTANCE.getDirectory(run).toFile() );
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

	@Override
	public void setFocus() {
		lblStatus.setFocus();
	}

}
