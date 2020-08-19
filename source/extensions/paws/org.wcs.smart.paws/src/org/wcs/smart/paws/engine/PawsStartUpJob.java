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
package org.wcs.smart.paws.engine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsFileManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * This job is intended to be run when a user logs into a Conservation
 * Area.  It will check the state of all the PAWS runs.  If there are
 * any that are not complete, it will process them accordingly.  Items
 * that were in the processing of uploading data are cancelled, items that
 * were running or downloading and restarted. 
 * 
 * It also cleans up the PAWS filestore.  If the run doesn't exist in the
 * database, but the directory exists in the filestore it
 * attempts to delete it.  This is to deal with the issue of geotools hanging on
 * to raster files and not allowing them to be deleted in some cases.
 * 
 * @author Emily
 *
 */
public class PawsStartUpJob extends Job {

	public PawsStartUpJob() {
		super(Messages.PawsStartUpJob_jobname);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		cleanUp();
		
		List<PawsRun> items = new ArrayList<>();
		
		try(Session session = HibernateManager.openSession()){
			List<PawsRun.Status> notcomplete = new ArrayList<>();
			notcomplete.add(PawsRun.Status.COMPILING_DATA);
			notcomplete.add(PawsRun.Status.UPLOADING_DATA);
			notcomplete.add(PawsRun.Status.DOWNLOADING_RESULTS);
			notcomplete.add(PawsRun.Status.RUNNING);
			
			
			String query = "FROM PawsRun WHERE conservationArea = :ca and status in (:stats)"; //$NON-NLS-1$
			
			items.addAll(session.createQuery(query, PawsRun.class)
					.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.setParameterList("stats", notcomplete) //$NON-NLS-1$
					.list());
		
		
			if (items.isEmpty()) return Status.OK_STATUS;
			
			List<PawsRun> compiling = new ArrayList<>();
			
			for (PawsRun r : items){
				if (r.getStatus() == PawsRun.Status.COMPILING_DATA || r.getStatus() == PawsRun.Status.UPLOADING_DATA) compiling.add(r);
			}
			if (!compiling.isEmpty()){
				//update status to error/cancelled
				StringBuilder sb = new StringBuilder();
				sb.append(MessageFormat.format(Messages.PawsStartUpJob_ShutdownMsg, compiling.size()));
				sb.append("\n\n"); //$NON-NLS-1$
				for (PawsRun r : compiling){
					sb.append(r.getId());
					sb.append(", "); //$NON-NLS-1$
				}
				sb.delete(sb.length() - 2, sb.length());
				
				Display.getDefault().asyncExec(()->{
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "PAWS",sb.toString()); //$NON-NLS-1$
				});
				
				session.beginTransaction();
				try{
					for (PawsRun r : compiling){
						r.setStatus(PawsRun.Status.ERROR);
						r.setStatusMessage(Messages.PawsStartUpJob_Cancelled);
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

	
	private void cleanUp() {
		Job cleanUp = new Job("cleanup paws") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Path dir = PawsFileManager.INSTANCE.getRunDirectory(SmartDB.getCurrentConservationArea());
				if (!Files.exists(dir)) return Status.OK_STATUS;
				
				List<Path> toDelete = new ArrayList<>();

				try (Session session = HibernateManager.openSession()){
					Files.list(dir).forEach(path->{
						if (Files.exists(path) && Files.isDirectory(path)) {
							UUID runuuid = UuidUtils.stringToUuid(path.getFileName().toString());
							PawsRun r = session.get(PawsRun.class, runuuid);
							if (r == null) {
								toDelete.add(path);
							}
						}
					});
					
				}catch (Exception ex) {
					ex.printStackTrace();
				}
				
				for(Path p : toDelete) {
					try {
						SmartUtils.deleteDirectory(p);
					}catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				return Status.OK_STATUS;
			}
			
		};
		cleanUp.schedule();
	}
	
}
