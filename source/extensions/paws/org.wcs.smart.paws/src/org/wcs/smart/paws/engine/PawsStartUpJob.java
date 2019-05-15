package org.wcs.smart.paws.engine;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsRun;

/**
 * This job is intended to be run when a user logs into a Conservation
 * Area.  It will check the state of all the PAWS runs.  If there are
 * any that are not complete, it will process them accordingly.  Items
 * that were in the processing of uploading data are cancelled, items that
 * were running or downloading and restarted. 
 * 
 * @author Emily
 *
 */
public class PawsStartUpJob extends Job {

	public PawsStartUpJob() {
		super("validating paws results state");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<PawsRun> items = new ArrayList<>();
		
		try(Session session = HibernateManager.openSession()){
			List<PawsRun.Status> notcomplete = new ArrayList<>();
			notcomplete.add(PawsRun.Status.COMPILING_DATA);
			notcomplete.add(PawsRun.Status.UPLOADING_DATA);
			notcomplete.add(PawsRun.Status.DOWNLOADING_RESULTS);
			notcomplete.add(PawsRun.Status.RUNNING);
			
			
			String query = "FROM PawsRun WHERE conservationArea = :ca and status in (:stats)";
			
			items.addAll(session.createQuery(query, PawsRun.class)
					.setParameter("ca", SmartDB.getCurrentConservationArea())
					.setParameterList("stats", notcomplete)
					.list());
		
		
			if (items.isEmpty()) return Status.OK_STATUS;
			
			List<PawsRun> compiling = new ArrayList<>();
			
			for (PawsRun r : items){
				if (r.getStatus() == PawsRun.Status.COMPILING_DATA || r.getStatus() == PawsRun.Status.UPLOADING_DATA) compiling.add(r);
			}
			if (!compiling.isEmpty()){
				//update status to error/cancelled
				StringBuilder sb = new StringBuilder();
				sb.append(MessageFormat.format("The following {0} paws analysis were not able to compile and upload data before SMART was shutdone.  These will be cancelled, and must be restarted manually.  Orphaned data may remain on your Azure blob and must be deleted manually.", compiling.size()));
				sb.append("\n\n");
				for (PawsRun r : compiling){
					sb.append(r.getId());
					sb.append(", ");
				}
				sb.delete(sb.length() - 2, sb.length());
				
				Display.getDefault().asyncExec(()->{
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "PAWS",sb.toString());
				});
				
				session.beginTransaction();
				try{
					for (PawsRun r : compiling){
						r.setStatus(PawsRun.Status.ERROR);
						r.setStatusMessage("Analysis cancelled due to SMART shutdown before data compiled and sent to server.");
					}
					session.getTransaction().commit();
				}catch (Exception ex){
					try{
						session.getTransaction().rollback();
					}catch (Exception ex2){
						PawsPlugIn.log(ex2.getMessage(), ex2);
					}
					PawsPlugIn.displayLog(ex.getMessage(), ex);
				}
			}
			items.removeAll(compiling);
			
			if (items.isEmpty()) return Status.OK_STATUS;
			
			//restart associated job
			for (PawsRun r : items){
				if (r.getStatus() == PawsRun.Status.RUNNING) PawsStatusJob.getInstance().addItem(r);
				if (r.getStatus() == PawsRun.Status.DOWNLOADING_RESULTS) (new PawsDownloadResultJob(r)).schedule();
			}
		}
		return Status.OK_STATUS;
	}

}
