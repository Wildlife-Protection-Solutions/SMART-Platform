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
package org.wcs.smart.r.ui.editor.script;

import java.util.List;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.r.model.RQuery;
import org.wcs.smart.r.model.RScript;

/**
 * R Script editor input
 * @author Emily
 *
 */
public class RScriptEditorInput implements IEditorInput {

	
	private UUID rqueryUuid = null;
	private UUID rscriptUuid = null;
	private List<QueryEditorInput> queries = null;
	
	public RScriptEditorInput(RScript script) {
		this.rscriptUuid = script.getUuid();
	}
	
	public RScriptEditorInput(RQuery query) {
		this.rqueryUuid = query.getUuid();
	}
	
	public RScriptEditorInput(RScript script, List<QueryEditorInput> queries) {
		this(script);
		this.queries = queries;
	}
	
	public List<QueryEditorInput> getDefaultQueries(){
		return this.queries;
	}
	
	public UUID getRQuery() {
		return this.rqueryUuid;
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
		return Messages.RScriptEditorInput_Name;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}
	
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		if (rqueryUuid != null) {
			return rqueryUuid.equals(((RScriptEditorInput)other).rqueryUuid );
		}
		return false;	
	}
	
	public int hashCode() {
		if (rqueryUuid != null) return rqueryUuid.hashCode();
		return super.hashCode();
	}

}
