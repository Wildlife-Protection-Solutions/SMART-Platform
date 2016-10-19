/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.i2.birt.IntelReportManager;

/**
 * Cleans up temporary directory of entity exports.
 * 
 * @author Emily
 *
 */
public class CleanUpJob implements ILoginHandler {

	public CleanUpJob() {
	}

	@Override
	public void onLogin() throws Exception {
		Job j = new Job("clean up exported entities job"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				//delete all files in the temporary output directory
				if (Files.exists(IntelReportManager.INSTANCE.getTemporaryDirectory())){
					try (DirectoryStream<Path> stream = Files.newDirectoryStream(IntelReportManager.INSTANCE.getTemporaryDirectory())) {
						for (Path entry : stream) {
							try {
								Files.delete(entry);
							} catch (Exception ex) {
							}
						}
					} catch (Exception ex) {
						Intelligence2PlugIn.log(ex.getMessage(), ex);
					}
				}
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
	}

}
