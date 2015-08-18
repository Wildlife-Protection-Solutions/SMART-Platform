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
package org.wcs.smart.er.ui.mision.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job for saving {@link MissionTrack} objects
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class DeleteMissionTracksJob extends Job {

	private List<MissionTrack> tracks;
	
    public DeleteMissionTracksJob(List<MissionTrack> tracks) {
        super(Messages.DeleteMissionTracksJob_Title);
        this.tracks = tracks;
    }

    public DeleteMissionTracksJob(MissionTrack track) {
        super(Messages.DeleteMissionTracksJob_Title);
        this.tracks = new ArrayList<MissionTrack>();
        this.tracks.add(track);
    }
    
    @Override
    protected IStatus run(IProgressMonitor monitor) {
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			for (MissionTrack t : tracks) {
				session.delete(t);
			}
			session.getTransaction().commit();
        	return Status.OK_STATUS;
		} catch (Exception ex) {
			session.getTransaction().rollback();
			EcologicalRecordsPlugIn.displayLog(Messages.DeleteMissionTracksJob_Error + "\n"+ ex.getLocalizedMessage(), ex); //$NON-NLS-1$
	        return Status.CANCEL_STATUS;
		} finally {
			session.close();
		}
    }
	
}
