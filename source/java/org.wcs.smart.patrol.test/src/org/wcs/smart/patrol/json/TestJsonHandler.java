package org.wcs.smart.patrol.json;

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
public class TestJsonHandler {

	private WizardDialog dialog = null;

	@Execute
	public void execute(final Shell activeShell, IEclipseContext context)  {
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			JsonPatrolProcessorTest test = new JsonPatrolProcessorTest(SmartDB.getCurrentConservationArea(), session);
			try {
				test.test();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(activeShell, "FAIL", "FAILED: " + e.getMessage());
			}
			
			JsonIncidentProcessorTest test3 = new JsonIncidentProcessorTest(SmartDB.getCurrentConservationArea(), session);
			try {
				test3.test();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(activeShell, "FAIL", "FAILED: " + e.getMessage());
			}
			
			JsonMissionProcessorTest test2 = new JsonMissionProcessorTest(SmartDB.getCurrentConservationArea(), session);
			try {
				test2.test();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(activeShell, "FAIL", "FAILED: " + e.getMessage());
			}
			
			JsonPatrolProcessorTest2 test4 = new JsonPatrolProcessorTest2(SmartDB.getCurrentConservationArea(), session);
			try {
				test4.test();
			}catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(activeShell, "FAIL", "FAILED: " + e.getMessage());
			}
			
			System.out.println("DONE");
		}

	}
	
	public static class TestJSonHandlerWrapper extends DIHandler<TestJsonHandler>{
		public TestJSonHandlerWrapper(){
			super(TestJsonHandler.class);
		}
	}

}

