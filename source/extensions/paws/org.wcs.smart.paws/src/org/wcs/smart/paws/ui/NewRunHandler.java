package org.wcs.smart.paws.ui;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.engine.PawsRunJob;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.util.UuidUtils;

public class NewRunHandler {

	@Inject
	private IEclipseContext context;
	
	public void createAndRun(PawsConfiguration config, LocalDate start, LocalDate end) throws Exception {
		
		DateDialog dialog = new DateDialog(context.get(Shell.class));
		if (start != null)dialog.setStart(start);
		if (end != null) dialog.setEnd(end);
		if (dialog.open() != Window.OK) return;
		
		start = dialog.getStartDate();
		end = dialog.getEndDate();
		
		PawsRun rr = createInternal(config, start, end);
		
		//TODO: run
		run(rr);
		
		open(rr);
	}
	
//	public void create(PawsConfiguration config) throws Exception{
//		PawsRun rr = createInternal(config);
//		open(rr);
//		
//	}
	
	private PawsRun createInternal(PawsConfiguration config, LocalDate start, LocalDate end) throws Exception{
		PawsRun prun = null;
		
		try(Session session = HibernateManager.openSession()){
			PawsConfiguration pw = session.get(PawsConfiguration.class, config.getUuid());
			if (pw == null) throw new Exception("Configuration not found.");
					
			String id = pw.getName();
			int cnt = 1;
			while(true) {
				if (QueryFactory.buildCountQuery(session, PawsRun.class, 
						new Object[] {"conservationArea", pw.getConservationArea()},
						new Object[] {"id", id}) > 0) {
					id = pw.getName() + " " + (cnt++);
				}else {
					break;
				}
			}
			session.beginTransaction();
			try {
				prun = new PawsRun();
				prun.setConfiguration(pw);
				prun.setConservationArea(pw.getConservationArea());
				prun.setId(id);
				prun.setRunId( UuidUtils.uuidToString( UUID.randomUUID() ));
				prun.setStatus(PawsRun.Status.COMPILING_DATA);
				prun.setDataStartDate(start);
				prun.setDataEndDate(end);
				session.save(prun);
				session.getTransaction().commit();
			}catch (Exception ex) {
				throw ex;
			}
		}
		

		context.get(IEventBroker.class).post(PawsEvent.PAWS_RUN_NEW, Collections.singletonList(prun));
		
		return prun;
	}
	private void open(PawsRun rr) {
		(new ShowRunHandler()).execute(context.get(MWindow.class), rr);
	}
	
	private void run(PawsRun rr){
		PawsRunJob job = new PawsRunJob(rr);
		job.schedule();
	}
}
