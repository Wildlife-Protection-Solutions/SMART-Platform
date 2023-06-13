
/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.r;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.r.ui.RunScriptMenuContribution;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.user.UserLevelManager;

/**
 * Tools for managing r scripts
 * 
 * @author Emily
 *
 */
public enum RScriptManager {
	
	INSTANCE;
	
	public static final String R_ALL = "R_SCRIPT/*"; //$NON-NLS-1$
	public static final String R_NEW = "R_SCRIPT/NEW"; //$NON-NLS-1$
	public static final String R_EDIT = "R_SCRIPT/EDIT"; //$NON-NLS-1$
	public static final String R_DELETE = "R_SCRIPT/DELETE"; //$NON-NLS-1$
	public static final String R_SCRIPT = "R_SCRIPT/SCRIPTS"; //$NON-NLS-1$
	
	public static final String RSCRIPT_DIR = "rscripts"; //$NON-NLS-1$
	
	/**
	 * Gets the full path to the rscript file
	 * 
	 * @param script
	 * @return
	 */
	public Path getScriptPath(RScript script) {
		return Paths.get(script.getConservationArea().getFileDataStoreLocation())
				.resolve(RSCRIPT_DIR)
				.resolve(script.getFilename());
	}
	
	/**
	 * Computes a script name based on an input file and conservation area
	 * 
	 * @param inputFile
	 * @param ca
	 * @return
	 */
	public Path computeScriptFileName(Path inputFile, ConservationArea ca) {
		String filename = inputFile.getFileName().toString();
		
		Path rootPath = Paths.get(ca.getFileDataStoreLocation())
				.resolve(RSCRIPT_DIR);
		
		Path temp = rootPath.resolve(filename);
		
		int index = filename.lastIndexOf('.');
		String prefix = filename;
		String suffix = ""; //$NON-NLS-1$
		if (index >= 0) {
			prefix = filename.substring(0, index);
			suffix = filename.substring(index + 1);
		}
		int cnt = 1;
		while(Files.exists(temp)) {
			String newfilename = prefix + "." + cnt + "." + suffix; //$NON-NLS-1$ //$NON-NLS-2$
			temp = rootPath.resolve(newfilename);
			if (cnt > 10_000) {
				throw new IllegalStateException("Unable to determine filename in filestore for r script file"); //$NON-NLS-1$
			}
			cnt++;
		}
		return temp;
	}
	
	/**
	 * 
	 * @return true if the current user can edit the script
	 */
	public boolean canEditScript() {
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN) ||
				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.MANAGER) ||
				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ANALYST);
	}
	
	
	public void deleteScripts(List<RScript> toDelete, Shell activeShell) {
		StringBuilder sb = new StringBuilder();
		
		for (RScript r : toDelete) {
			sb.append(r.getName());
			sb.append(", "); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		if (!MessageDialog.openConfirm(activeShell, Messages.RScriptListDialog_DeleteTitle, MessageFormat.format(Messages.RScriptListDialog_DeleteMessage, sb.toString()))){
			return;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {

					monitor.beginTask(Messages.RScriptListDialog_DeleteTask, toDelete.size());
					List<RScript> deleted = new ArrayList<RScript>();
					try(Session s = HibernateManager.openSession(new RScriptInterceptor())){

						for (RScript t : toDelete){
							monitor.subTask(t.getName());
							try {
								DeleteManager.canDelete(t, s);
							}catch (Exception ex) {
								//cannot delete
								activeShell.getDisplay().syncExec(()->MessageDialog.openError(activeShell, DialogConstants.ERROR_STRING, ex.getMessage()));
								continue;
							}
							
							s.beginTransaction();
							try{
								s.remove(t);
								s.getTransaction().commit();
								deleted.add(t);
							}catch(Exception ex){
								s.getTransaction().rollback();
								RPlugIn.displayLog(MessageFormat.format(Messages.RScriptListDialog_DeleteError1, t.getName(), ex.getMessage()), ex);
							}
							monitor.worked(1);
						}
					}
					deleted.forEach(e->RunScriptMenuContribution.removeScript(e));
					monitor.done();
				}
			});
		} catch (Exception e) {
			RPlugIn.displayLog(Messages.RScriptListDialog_DeleteError2 + e.getMessage(), e);
		}
	}
}
