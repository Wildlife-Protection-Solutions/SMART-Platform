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
package org.wcs.smart.paws.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.engine.PawsRunJob;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsService;
import org.wcs.smart.paws.model.PawsWorkspace;
import org.wcs.smart.paws.ui.config.ConfigEditorInput;
import org.wcs.smart.paws.ui.config.EditConfigHandler;
import org.wcs.smart.util.UuidUtils;

import com.ibm.icu.text.MessageFormat;

/**
 * Configures and start a PAWS analysis run.
 * 
 * Request an injected eclipse context.
 * 
 * @author Emily
 *
 */
public class NewPawsRunHandler {

	@Inject
	private IEclipseContext context;
	
	public void createAndRun(PawsConfiguration config, PawsRun copy, String initName) throws Exception {
		if (config == null) return;
		if (!validateSetup()) return;
	
		//validate configuration
		try{
			PawsManager.INSTANCE.validateConfiguration(config);
		}catch (Exception ex){
			MessageDialog.openWarning(Display.getDefault().getActiveShell(), "PAWS", 
					MessageFormat.format("The selected PAWS Configuration ''{0}'' is not valid.  Errors must be resolved before you can run.", config.getName()) + "\n\n" + ex.getMessage());
			
			
			ConfigEditorInput in = new ConfigEditorInput(config);
			(new EditConfigHandler()).execute(context.get(MWindow.class), in);
			return;
		}
		
		
		RunDialog dialog = new RunDialog(context.get(Shell.class));

		if (copy != null) dialog.setDates(copy.getTrainStartYear(), copy.getTrainEndYear(), copy.getTestStartYear(), copy.getTestEndYear(), copy.getForecastStartYear(), copy.getForecastEndYear(), copy.getDataStartDate(), copy.getDataEndDate());
		if (initName != null) dialog.setId(initName);
		
		if (dialog.open() != Window.OK) return;
		
		PawsRun temp = new PawsRun();
		temp.setTrainStartYear(dialog.getTrainStart());
		temp.setTrainEndYear(dialog.getTrainEnd());
		temp.setTestStartYear(dialog.getTestStart());
		temp.setTestEndYear(dialog.getTestEnd());
		temp.setForecastStartYear(dialog.getForcastStart());
		temp.setForecastEndYear(dialog.getForcastEnd());
		temp.setDataStartDate(dialog.getDataStart());
		temp.setDataEndDate(dialog.getDataEnd());
		
		initName = dialog.getId();
		
		PawsRun rr = createInternal(config, temp, initName);
		
		if (run(rr)) {
			open(rr);
		}
	}

	private PawsRun createInternal(PawsConfiguration config, PawsRun copy, String initName) throws Exception{
		PawsRun prun = null;
		if (copy == null) throw new IllegalArgumentException("PawsRun copy cannot be null.");
		
		try(Session session = HibernateManager.openSession()){
			PawsConfiguration pw = session.get(PawsConfiguration.class, config.getUuid());
			if (pw == null) throw new Exception("Configuration not found.");
			
			prun = new PawsRun();
			prun.setConfiguration(pw);
			prun.setConservationArea(pw.getConservationArea());
			prun.setId(initName);
				
			prun.setStatus(PawsRun.Status.COMPILING_DATA);
				
			prun.setDataStartDate(copy.getDataStartDate());
			prun.setDataEndDate(copy.getDataEndDate());
			prun.setForecastEndYear(copy.getForecastEndYear());
			prun.setForecastStartYear(copy.getForecastStartYear());
			prun.setTestEndYear(copy.getTestEndYear());
			prun.setTestStartYear(copy.getTestStartYear());
			prun.setTrainEndYear(copy.getTrainEndYear());
			prun.setTrainStartYear(copy.getTrainStartYear());
				
			
		}
		return prun;
	}
	
	private boolean validateSetup(){
		boolean openconfig = false;
		try(Session session = HibernateManager.openSession()){
			PawsWorkspace pw = QueryFactory.buildQuery(session, PawsWorkspace.class, 
					new Object[]{"conservationArea", SmartDB.getCurrentConservationArea()})
				.uniqueResult();
			
			if (pw == null || !pw.isConfigured()) {
				openconfig = true;
			}else{
			
				PawsService service = QueryFactory.buildQuery(session, PawsService.class,
					new Object[]{"conservationArea", SmartDB.getCurrentConservationArea()})
					.uniqueResult();
				if (service == null || service.getApiKey() == null || service.getUrl() == null || service.getUrl().isBlank() || service.getApiKey().isBlank()){
					openconfig = true;
				}
			}
		}
		if (openconfig){
			MessageDialog.openWarning(Display.getDefault().getActiveShell(), "PAWS", 
					"Cannot perform PAWS Analysis until an Azure workspace and PAWS service are configured.");
			
			ContextInjectionFactory.invoke(new ShowConfigurationHandler(), Execute.class, context);
			return false;
		}
		return true;
	}
	
	private void open(PawsRun rr) {
		(new ShowRunHandler()).execute(context.get(MWindow.class), rr);
	}
	
	private boolean run(PawsRun rr){
		//perform validation; get token as required
		LoginDialog dialog = new LoginDialog(Display.getDefault().getActiveShell());
		dialog.open();
		
		String authorizationCode = dialog.getAuthorizationCode();
		if (authorizationCode == null) {
			//fail
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Authorization Failed", "Authorization failed, run not created.");
			return false;
		}
		
		//else save and run job
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.save(rr);
				
				LocalDateTime now = LocalDateTime.now();
				String dpart = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
				rr.setRunId( dpart + "_" + UuidUtils.uuidToString( rr.getUuid() ));
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				try {
					session.getTransaction().rollback();
				}catch (Exception ex2) {
					PawsPlugIn.log(ex2.getMessage(), ex2);
				}
				PawsPlugIn.displayLog("Unable to save run to database: " + ex.getMessage(), ex);
				return false;
				
			}
		}
		
		try {
			context.get(IEventBroker.class).post(PawsEvent.PAWS_RUN_NEW, Collections.singletonList(rr));
		}catch (Exception ex) {
			PawsPlugIn.log(ex.getMessage(), ex);
		}
		
		PawsRunJob job = new PawsRunJob(rr, authorizationCode, context.get(IEventBroker.class));
		job.schedule();
		return true;
	}
	
	
}
