/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.startup;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.util.SmartUtils;

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
				SmartUtils.deleteDirectory(cleanUpPath);
			} catch (IOException e) {
				SmartPlugIn.log(e.getMessage(), e);
			}
			return Status.OK_STATUS;
		}
		
	};

}
