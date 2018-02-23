package org.wcs.smart.startup;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cipher.EncryptUtils;

public class EncryptCleanUp implements IDatabaseStartupRunner {

	private Path cleanUpPath;
	
	public EncryptCleanUp() {
	}

	@Override
	public void run(Session session) throws Exception {
		cleanUpPath = Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).resolve(EncryptUtils.TEMP_DIR);
		job.setSystem(true);
		job.schedule();
	}
	
	private Job job = new Job("clean up temporary files directory") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				FileUtils.deleteDirectory(cleanUpPath.toFile());
			} catch (IOException e) {
				SmartPlugIn.log(e.getMessage(), e);
			}
			return Status.OK_STATUS;
		}
		
	};

}
