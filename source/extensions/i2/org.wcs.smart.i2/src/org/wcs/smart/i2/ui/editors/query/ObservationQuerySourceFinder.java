package org.wcs.smart.i2.ui.editors.query;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;

public class ObservationQuerySourceFinder implements IQuerySourceFinder {

	public static ObservationQuerySourceFinder INSTANCE = new ObservationQuerySourceFinder();
	
	private ObservationQuerySourceFinder(){}
	
	public void openSource(IResultItem item){
		if (!(item instanceof IntelObservationResultItem)) return;	
		IntelObservationResultItem i = (IntelObservationResultItem)item;
		(new OpenRecordHandler()).openRecord(new RecordEditorInput(i.getRecordTitle(), i.getRecordUuid(),null), false);
	}

	@Override
	public Image getImage() {
		return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD);
	}

	@Override
	public String getName() {
		return "Open Record...";
	}
}
