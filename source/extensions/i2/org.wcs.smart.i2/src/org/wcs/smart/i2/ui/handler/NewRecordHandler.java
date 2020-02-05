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
package org.wcs.smart.i2.ui.handler;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.EntityPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.dialogs.SelectProfileDialog;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

/**
 * Open dialog handler
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class NewRecordHandler {
	
	/**
	 * Context key containing collection of entity uuids that should
	 * be linked to the record when created
	 */
	public static final String ENTITY_UUID_LINK = "org.wcs.smart.i2.ui.handler.NewRecordHandler.entities"; //$NON-NLS-1$
	
	public static final String PROFILE_LINK = "org.wcs.smart.i2.ui.handler.NewRecordHandler.profile"; //$NON-NLS-1$

	
	@SuppressWarnings("unchecked")
	@Execute
	public void createNewRecord(IEclipseContext context){
		
		//need to find the profiles that are active and in which
		//the current user can create records
		IntelProfile p = null;
		if (context.containsKey(PROFILE_LINK) && context.get(PROFILE_LINK) instanceof IntelProfile) {
			p = (IntelProfile)context.get(PROFILE_LINK);
			if (!IntelSecurityManager.INSTANCE.canCreateRecord(p)) {
				MessageDialog.openInformation(context.get(Shell.class), Messages.NewRecordHandler_NewRecordTitle, 
						MessageFormat.format(Messages.NewRecordHandler_NewRecordMsg,p.getName()));
				return;
			}
		}else {
			List<IntelProfile> items = new ArrayList<>(ProfilesManager.INSTANCE.getActiveProfiles());
			for (Iterator<IntelProfile> iterator = items.iterator(); iterator.hasNext();) {
				IntelProfile intelProfile = iterator.next();
				if (!IntelSecurityManager.INSTANCE.canCreateRecord(intelProfile)) iterator.remove();
			}
			
			if (items.isEmpty()) {
				MessageDialog.openInformation(context.get(Shell.class), Messages.NewRecordHandler_NewRecordTitle, 
						Messages.NewRecordHandler_NewRecordNoPermissions);
				return;
			}else if (items.size() > 1) {
				//select profile
				items.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
				SelectProfileDialog dialog = new SelectProfileDialog(context.get(Shell.class), items);
				if (dialog.open() != Window.OK) return;
				p = dialog.getSelection();
			}else {
				p = items.iterator().next();
			}
		}
		
		IntelRecord newRecord = new IntelRecord();
		newRecord.setTitle(Messages.NewRecordHandler_DefaultRecordName);
		newRecord.setStatus(Status.NEW);
		newRecord.setConservationArea(SmartDB.getCurrentConservationArea());
		newRecord.setDateCreated(new Date());
		newRecord.setEntities(new ArrayList<IntelEntityRecord>());
		newRecord.setProfile(p);
		newRecord.setPrimaryDate(new Date());
		RecordEditorInput input = new RecordEditorInput(newRecord);
		
		if (context.containsKey(ENTITY_UUID_LINK) && context.get(ENTITY_UUID_LINK) instanceof Collection) {
			Collection<UUID> entityUuids = (Collection<UUID>) context.get(ENTITY_UUID_LINK);
			
			try(Session session = HibernateManager.openSession()){
				for (UUID entity: entityUuids) {
					IntelEntity ie = session.get(IntelEntity.class,entity);
					if (ie == null) continue;
					ie.getIdAttributeAsText();
					try {
						ie.getPrimaryAttachment().computeFileLocation(session);
					}catch (Exception ex) {}
					
					if (!newRecord.getProfile().equals(ie.getProfile())) return;
					
					IntelEntityRecord rr = new IntelEntityRecord();
					rr.setRecord(newRecord);
					rr.setEntity(ie);
					newRecord.getEntities().add(rr);
				}
			}
			
		}
		//open perspective
		String pId = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getPerspective().getId();
		if (!pId.equals(EntityPerspective.ID) && !pId.equals(IntelDataAssessmentPerspective.ID)) {
			IEclipseContext kid = context.createChild();
			kid.set( org.wcs.smart.ui.ShowPerspectiveHandler.PERSPECTIVE_ID_PARAM, IntelDataAssessmentPerspective.ID);
			ContextInjectionFactory.invoke(new ShowPerspectiveHandler(), Execute.class, kid);
		}
		
		//open editor
		(new OpenRecordHandler()).openRecord(input, true);
	}
	
	// E3
	public static class NewRecordHandlerWrapper extends DIHandler<NewRecordHandler> {
		public NewRecordHandlerWrapper() {
			super(NewRecordHandler.class);
		}
	}
	
}
