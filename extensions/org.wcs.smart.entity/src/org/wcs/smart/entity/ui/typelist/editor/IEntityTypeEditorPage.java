package org.wcs.smart.entity.ui.typelist.editor;

import org.hibernate.Session;

public interface IEntityTypeEditorPage {

	public void updatePage(Session currentSession, boolean typeModified) ;
}
