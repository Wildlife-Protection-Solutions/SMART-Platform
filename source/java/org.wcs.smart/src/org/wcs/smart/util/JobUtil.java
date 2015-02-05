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
package org.wcs.smart.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.wcs.smart.SmartPlugIn;

/**
 * Util class to provide functionality to deal with {@link Job}.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class JobUtil {
	
	private static final int STOP_WAIT_TIME = 2000; //2 sec

	public static final void stopJobs(Job... jobs) {
		for (Job job : jobs) {
			if (job != null) {
				job.cancel();
			}
		}
		for (Job job : jobs) {
			stopJob(job);
		}
	}
	
	/**
	 * Method will try to cancel job. If job cannot be cancelled right away it will wait but no longer than 2 seconds.
	 * @param job
	 */
	public static final void stopJob(Job job) {
        if (job == null || job.cancel()) return;
        final AtomicBoolean done = new AtomicBoolean(job.cancel());
        IJobChangeListener listener = new JobChangeAdapter(){
            @Override
            public void done( IJobChangeEvent event ) {
                done.set(true);
                synchronized (done) {
                    done.notify();
                }

            }
        };
        job.addJobChangeListener(listener);
        try {
            long start = System.currentTimeMillis();
            while( !done.get() && !job.cancel() && start + STOP_WAIT_TIME > System.currentTimeMillis() ) {
                synchronized (done) {
                    try {
                        done.wait(200);
                    } catch (InterruptedException e) {
                    	SmartPlugIn.log("Internal error while trying to stop job - " + job.getName(), e); //$NON-NLS-1$
                    }
                }
            }
            if (!done.get() && !job.cancel()) {
                SmartPlugIn.log("After 2 seconds unable to cancel job - " + job.getName(), null); //$NON-NLS-1$
            }
        } finally {
            job.removeJobChangeListener(listener);
        }
	}

}
