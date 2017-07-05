package org.wcs.smart.qa.er;

import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.ui.mision.editor.MissionTrackEditDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaError.Status;
import org.wcs.smart.qa.routine.IQaAction;

public class EditTrackAction  implements IQaAction {

	@Override
	public void doAction(List<QaError> items) {
		if (items.isEmpty()) return;
		QaError item = items.get(0);
		
		MissionTrack track = null;
		Mission p = null;
		Session s = HibernateManager.openSession();
		try{
			track = (MissionTrack) s.get(MissionTrack.class, item.getSourceId());
			if (track != null){
				//load hibernate objects necessary for editing
				p = track.getMissionDay().getMission();
				p.equals(null);
				track.getMissionDay().equals(null);
				track.getMissionDay().getTracks().size();
				int x = track.getGeom().length;
			}
		}finally{
			s.close();
		}
		
		if (track == null){
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Not found", "Track not found");
			return ;
		}
		
		try{
			track.getLineString();
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Not found", "Unable to parse track linestring.  Track should be regenerated or re-imported in the patrol editor.");
			return ;
		}

		boolean[] changes = new boolean[]{false};
		MissionTrackEditDialog dialog = new MissionTrackEditDialog(Display.getDefault().getActiveShell(),track.getMissionDay()){
			public boolean saveChanges() {
				if (super.saveChanges()){
					changes[0] = true;
					return true;
				}
				return false;
			}
		};
		dialog.open();
		if (changes[0]){
			item.setStatus(Status.FIXED);
			item.setFixMessage("Track manually modified.");
			//TODO: do something here
			//item.setGeometryObject(dialog.getEditTrackLineString());
		}
	}

	@Override
	public boolean supportsMultiple() {
		return false;
	}

	@Override
	public String getId() {
		return "org.wcs.smart.qa.mission.track.edit"; //$NON-NLS-1$
	}

	@Override
	public String getName(Locale l) {
		return "Edit Track...";
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON);
	}

}
