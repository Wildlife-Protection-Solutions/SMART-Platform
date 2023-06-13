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
package org.wcs.smart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Cleans up query temporary tables from the database.
 * 
 * @author Emily
 *
 */
public class TempDirCleanUpJob extends Job{

	public TempDirCleanUpJob() {
		super(Messages.TempDirCleanUpJob_CleanJob);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//clean up queries directory
		Path dir = SmartContext.INSTANCE.getTempFilestoreLocation();
		
		if (Files.exists(dir) && Files.isDirectory(dir)){
			try {
				try(Stream<Path> files = Files.list(dir)){
					files.forEach(file->{
						try {
							if (Files.isDirectory(file)) {
								SmartUtils.deleteDirectory(file);
							}else {
								Files.delete(file);
							}
						} catch (IOException e) {
							SmartPlugIn.log(e.getMessage(), e);
						}
					});
				}
			}catch (IOException e) {
				SmartPlugIn.log(e.getMessage(), e);
			}
		}
		
		//cleanup query tables
		try(Session session = HibernateManager.openSession()){
			try{
				session.beginTransaction();
				session.createNativeMutationQuery("CALL smart.cleanUpTempData()") //$NON-NLS-1$
					.executeUpdate(); 
				session.getTransaction().commit();
			}catch (Exception ex){
				SmartPlugIn.log("Could not cleanup query temporary tables.", ex); //$NON-NLS-1$
			}finally{
				if (session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			}
		}
		return Status.OK_STATUS;
	}

}
