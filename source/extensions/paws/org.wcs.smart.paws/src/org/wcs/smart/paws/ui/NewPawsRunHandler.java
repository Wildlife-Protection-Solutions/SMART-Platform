package org.wcs.smart.paws.ui;

import java.time.LocalDate;
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
import org.wcs.smart.paws.engine.PawsRunJob;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsService;
import org.wcs.smart.paws.model.PawsWorkspace;
import org.wcs.smart.paws.ui.config.ConfigEditorInput;
import org.wcs.smart.paws.ui.config.EditConfigHandler;
import org.wcs.smart.util.UuidUtils;

import com.ibm.icu.text.MessageFormat;

public class NewPawsRunHandler {

	@Inject
	private IEclipseContext context;
	
	public void createAndRun(PawsConfiguration config, LocalDate start, LocalDate end, String initName) throws Exception {
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
		if (start != null)dialog.setStart(start);
		if (end != null) dialog.setEnd(end);
		if (initName != null) dialog.setId(initName);
		
		if (dialog.open() != Window.OK) return;
		
		start = dialog.getStartDate();
		end = dialog.getEndDate();
		initName = dialog.getId();
		
		PawsRun rr = createInternal(config, start, end, initName);
		
		run(rr);
		open(rr);
	}

	private PawsRun createInternal(PawsConfiguration config, LocalDate start, LocalDate end, String initName) throws Exception{
		PawsRun prun = null;
		
		try(Session session = HibernateManager.openSession()){
			PawsConfiguration pw = session.get(PawsConfiguration.class, config.getUuid());
			if (pw == null) throw new Exception("Configuration not found.");
			session.beginTransaction();
			try {
				prun = new PawsRun();
				prun.setConfiguration(pw);
				prun.setConservationArea(pw.getConservationArea());
				prun.setId(initName);
				
				prun.setStatus(PawsRun.Status.COMPILING_DATA);
				prun.setDataStartDate(start);
				prun.setDataEndDate(end);
				session.save(prun);
				
				prun.setRunId( UuidUtils.uuidToString( prun.getUuid() ));
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				throw ex;
			}
		}
		

		context.get(IEventBroker.class).post(PawsEvent.PAWS_RUN_NEW, Collections.singletonList(prun));
		
		return prun;
	}
	private boolean validateSetup(){
		boolean openconfig = false;
		try(Session session = HibernateManager.openSession()){
			PawsWorkspace pw = QueryFactory.buildQuery(session, PawsWorkspace.class, 
					new Object[]{"conservationArea", SmartDB.getCurrentConservationArea()})
				.uniqueResult();
			
			if (pw == null || pw.getApiKey() == null || pw.getUrl() == null || pw.getUrl().isBlank() || pw.getApiKey().isBlank()){
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
	
	private void run(PawsRun rr){
		PawsRunJob job = new PawsRunJob(rr, context.get(IEventBroker.class));
		job.schedule();
	}
}
