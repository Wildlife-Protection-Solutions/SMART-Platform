package org.wcs.smart.patrol.json.connecttest;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Handler to display new patrol wizard.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class ConnectTestJsonHandler {

	private WizardDialog dialog = null;

	@Execute
	public void execute(final Shell activeShell, IEclipseContext context)  {
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){

			ConnectJsonIncidentProcessorTest test3 = new ConnectJsonIncidentProcessorTest(SmartDB.getCurrentConservationArea(), session);
			try {
				test3.test();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(activeShell, "FAIL", "FAILED: " + e.getMessage());
			}
			ConnectJsonPatrolProcessorTest2 test2 = new ConnectJsonPatrolProcessorTest2(SmartDB.getCurrentConservationArea(), session);
			try {
				test2.test();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(activeShell, "FAIL", "FAILED: " + e.getMessage());
			}
			
			ConnectJsonPatrolProcessorTest test1 = new ConnectJsonPatrolProcessorTest(SmartDB.getCurrentConservationArea(), session);
			try {
				test1.test();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(activeShell, "FAIL", "FAILED: " + e.getMessage());
			}
			
			ConnectJsonMissionProcessorTest test4 = new ConnectJsonMissionProcessorTest(SmartDB.getCurrentConservationArea(), session);
			try {
				test4.test();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(activeShell, "FAIL", "FAILED: " + e.getMessage());
			}

			
			System.out.println("DONE");
		}

	}
	
	public static class ConnectTestJsonHandlerWrapper extends DIHandler<ConnectTestJsonHandler>{
		public ConnectTestJsonHandlerWrapper(){
			super(ConnectTestJsonHandler.class);
		}
	}

}

