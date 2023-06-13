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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.diagram.style.RelationshipDiagramStyleDefaultNameComparator;
import org.wcs.smart.i2.diagram.style.RelationshipDiagramStyleFactory;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;

/**
 * Functions for managing relationship diagram related items.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public enum RelationshipDiagramManager {

	INSTANCE;
	
	/**
	 * A name for default style.
	 */
	private static final String DEFAULT_STYLE_NAME = "Default"; //$NON-NLS-1$
	
	private RelationshipDiagramManager() {}
	
	/**
	 * Fetches a {@link RelationshipDiagramStyle} with all it's details
	 * 
	 * @param session session
	 * @return {@link RelationshipDiagramStyle}
	 */
	public RelationshipDiagramStyle getStyle(Session session, UUID uuid) {
		RelationshipDiagramStyle style = (RelationshipDiagramStyle) session.get(RelationshipDiagramStyle.class, uuid);
		style.getNames().size();
		style.getEntityTypeStyles().size();
		style.getRelationshipTypeStyles().size();
		return style;
	}
	
	/**
	 * Fetches a list of {@link RelationshipDiagramStyle} for current conservation area
	 * 
	 * @param session session
	 * @return List of {@link RelationshipDiagramStyle}
	 */
	public List<RelationshipDiagramStyle> getStyles(Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		List<RelationshipDiagramStyle>  styles =
				QueryFactory.buildQuery(session, RelationshipDiagramStyle.class,"conservationArea", ca).getResultList(); //$NON-NLS-1$
		if (styles.isEmpty()) {
			RelationshipDiagramStyle defaultProfile = createDefaultStyle(session);
			return Arrays.asList(defaultProfile);
		}
		Collections.sort(styles, new RelationshipDiagramStyleDefaultNameComparator());
		return styles;
	}

	/**
	 * Delete a {@link RelationshipDiagramStyle}
	 */
	public boolean saveStyle(Shell shell, RelationshipDiagramStyle style) {
		final boolean[] isOk = {false};
		final List<RelationshipDiagramStyle> styleList = new ArrayList<RelationshipDiagramStyle>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.RelationshipDiagramManager_Task_Save, 1);
					try(Session session = HibernateManager.openSession()){
						session.beginTransaction();
						try {
							if (style.getUuid() == null) {
								session.persist(style);
							}else {
								session.merge(style);
							}
							session.getTransaction().commit();
							isOk[0] = true;
							styleList.addAll(getStyles(session));
						} catch (Exception ex) {
							session.getTransaction().rollback();
							SmartPlugIn.displayLog(Messages.RelationshipDiagramManager_SaveError, ex);
						}
					}
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog(Messages.RelationshipDiagramManager_SaveError, e);
		}
		
		if (isOk[0] && !styleList.isEmpty()) {
	        IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			context.get(IEventBroker.class).send(IntelEvents.GRAPH_STYLESET_CHANGED, styleList);
		}

		return isOk[0];
	}


	/**
	 * Delete a {@link RelationshipDiagramStyle}
	 */
	public void deleteStyle(Shell shell, RelationshipDiagramStyle style) {
		final List<RelationshipDiagramStyle> styleList = new ArrayList<RelationshipDiagramStyle>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.RelationshipDiagramManager_Task_Delete, 1);
					
					try(Session session = HibernateManager.openSession()){
						session.beginTransaction();
						try {
							session.remove(style);
							session.getTransaction().commit();
							styleList.addAll(getStyles(session));
						}catch (Exception ex){
							session.getTransaction().rollback();
							SmartPlugIn.displayLog(Messages.RelationshipDiagramManager_DeleteError, ex);
						}
					} finally {
						monitor.done();
					}
				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.RelationshipDiagramManager_DeleteError, ex);
		}
		
        IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		context.get(IEventBroker.class).send(IntelEvents.GRAPH_STYLESET_CHANGED, styleList);
	}
	
	/**
	 * This method must create and persist a default style,
	 * but it is not allowed to use current session for that (as transaction may be reverted)
	 * At the same time returned(created) style must be attached to current session.
	 * 
	 * @param session
	 * @return default style
	 */
	private RelationshipDiagramStyle createDefaultStyle(Session session) {
		final RelationshipDiagramStyle defaultStyle = RelationshipDiagramStyleFactory.createUsingDefaults(DEFAULT_STYLE_NAME);
		defaultStyle.setDefault(true);
		
		Thread thread = new Thread() { //new thread is created so it will have it's own hibernate session
		    public void run() {
		    	try(Session s = HibernateManager.openSession()){
		    		try {
		    			s.beginTransaction();
		    			s.persist(defaultStyle);
		    			s.getTransaction().commit();
		    		} catch (Exception e) {
		    			SmartPlugIn.displayLog(Messages.RelationshipDiagramManager_DefaultStyleSaveError, e);
		    		}
	    		}
		    }  
		};
		thread.start();
		try {
			thread.join(); //we need to wait till thread is completed
		} catch (InterruptedException e) {
			SmartPlugIn.displayLog(Messages.RelationshipDiagramManager_DefaultStyleSaveError, e);
		}
		
		return (RelationshipDiagramStyle) session.merge(defaultStyle); //attaching create object to current session
	}
	
}
