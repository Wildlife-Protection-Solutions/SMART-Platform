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
package org.wcs.smart.patrol.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class PatrolEditor extends MultiPageEditorPart {

	public static final String ID = "org.wcs.smart.patrol.ui.PatrolEditor"; //$NON-NLS-1$

//	private static Session session = null; /* single session for all patrol editors */
//	private static int patrolcnt = 0;
	private Patrol patrol = null;
	
	
	private WorkerThread worker = null;
	class WorkerThread extends Thread{
		Session session  = null;
		
		boolean isFinished = false;
		public void setFinished(boolean finished){
			this.isFinished = true;
		}
		
		static final int LOAD = 1;
		static final int SAVE = 2;
		int action = 0;
		
		byte[] puuid = null;
		
		
		private void loadPatrol(){
			PatrolEditor.this.patrol =  (Patrol) session.load(Patrol.class, puuid);
		}
		private void savePatrol(){
			session.save(PatrolEditor.this.patrol);
		}
		
		@Override
		public void run(){
			session = HibernateManager.openSession();
			try{
			while(!isFinished){
				synchronized (this) {
					this.wait();
				}
				if (action == LOAD){
					loadPatrol();
					notifyAll();
				}else if (action == SAVE){
					savePatrol();
					notifyAll();
				}
				action = 0;
				
			}
			}catch (Exception ex){
				
			}
			session.close();
		}
		
	}
	
	public PatrolEditor() {
//		patrolcnt++;
		worker = new WorkerThread();
		worker.setDaemon(true);
		worker.setName("Patrol editor worker thread");
		worker.start();

	}

	@Override
	public void dispose() {
//		worker.setFinished(true);
//		worker.notify();
		super.dispose();
		
//		if (session != null && session.isOpen()){
//			session.clear();
//		}
//		patrolcnt --;
//		if (patrolcnt == 0){
//			if (session != null && session.isOpen()){
//				session.close();
//			}	
//		}
	}
	
//	public Session getSession(){
//		System.out.println("ED:: " + Thread.currentThread().getId());
//		if (session == null || !session.isOpen()){
//			session = PatrolHibernateManager.openSession();
//		}
//		return session;
//	}
	
	
	public Patrol getPatrol(){
		if (this.patrol == null){
			byte[] puuid = ((PatrolEditorInput) getEditorInput()).getUuid();
			
//			worker.puuid = puuid;
//			worker.action = WorkerThread.LOAD;
//			synchronized (worker) {
//				worker.notify();
//			}
//			worker.notify();
//			this.notifyAll();
			
//			try {
//				worker.wait();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			this.patrol = (Patrol) getSession().load(Patrol.class, puuid);
//			if (this.patrol == null){
//				throw new IllegalStateException("Could not find patrol.");
//			}
			Session session = HibernateManager.openSession();		
			this.patrol = (Patrol) session.load(Patrol.class, puuid);
			this.patrol.getLegs().size();
			session.close();
		}
		return this.patrol;
	}
	
	
	@Override
	protected void createPages() {
		PatrolEditorInput input = ((PatrolEditorInput) getEditorInput());
		
		PatrolSummaryEditor summaryEditor = new PatrolSummaryEditor(this);
		try {
			addPage(summaryEditor, getEditorInput());
			super.setPageText(0, "Summary");
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.setPartName("Patrol " + input.getPatrolId());
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		
		worker.action = WorkerThread.SAVE;
		worker.notify();
	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub
	}

}
