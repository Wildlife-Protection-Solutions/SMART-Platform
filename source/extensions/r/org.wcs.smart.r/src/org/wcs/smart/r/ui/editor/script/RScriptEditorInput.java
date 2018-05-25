package org.wcs.smart.r.ui.editor.script;

import java.util.List;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.r.model.RScript;

public class RScriptEditorInput implements IEditorInput {

	private UUID rscriptUuid = null;
	private List<QueryEditorInput> queries = null;
	
	public RScriptEditorInput(RScript script) {
		this.rscriptUuid = script.getUuid();
	}
	
	public RScriptEditorInput(UUID script) {
		this.rscriptUuid = script;
	}
	
	public RScriptEditorInput(RScript script, List<QueryEditorInput> queries) {
		this(script);
		this.queries = queries;
	}
	
	public List<QueryEditorInput> getDefaultQueries(){
		return this.queries;
	}
	
	public UUID getRScript() {
		return this.rscriptUuid;
	}
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return "R Script Editor";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}

}
