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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.RScriptManager;
import org.wcs.smart.r.internal.Messages;

/**
 * Script page of editor
 * 
 * @author Emily
 *
 */
public class ScriptPage extends EditorPart {

	private RScriptEditor parent;
	
	private Text txtScript;
	private FormToolkit toolkit;

	
	public ScriptPage(RScriptEditor parent) {
		this.parent = parent;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		toolkit.dispose();
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {}

	@Override
	public void doSaveAs() {}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (RScriptManager.INSTANCE.canEditScript()) {
			Composite header = new Composite(main, SWT.NONE);
			header.setLayout(new GridLayout(3, false));
			((GridLayout)header.getLayout()).marginWidth = 0;
			((GridLayout)header.getLayout()).marginHeight = 0;
			header.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			
			Hyperlink lnkRefresh = toolkit.createHyperlink(header, Messages.ScriptPage_ReloadLink, SWT.NONE);
			lnkRefresh.setToolTipText(Messages.ScriptPage_ReloadTooltip);
			lnkRefresh.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			lnkRefresh.addHyperlinkListener(new IHyperlinkListener() {
				@Override
				public void linkExited(HyperlinkEvent e) {}
				@Override
				public void linkEntered(HyperlinkEvent e) {}
				@Override
				public void linkActivated(HyperlinkEvent e) {
					update();
				}
			});
			
			Hyperlink lnkEdit = toolkit.createHyperlink(header, Messages.ScriptPage_showlink, SWT.NONE);
			lnkEdit.setToolTipText(Messages.ScriptPage_showtooltip);
			lnkEdit.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			lnkEdit.addHyperlinkListener(new IHyperlinkListener() {
				@Override
				public void linkExited(HyperlinkEvent e) {}
				@Override
				public void linkEntered(HyperlinkEvent e) {}
				@Override
				public void linkActivated(HyperlinkEvent e) {
					AttachmentUtil.launch(RScriptManager.INSTANCE.getScriptPath(ScriptPage.this.parent.getQuery().getScript()).getParent().toFile());
				}
			});
			
			lnkEdit = toolkit.createHyperlink(header, Messages.ScriptPage_editlink, SWT.NONE);
			lnkEdit.setToolTipText(Messages.ScriptPage_edittooltip);
			lnkEdit.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			lnkEdit.addHyperlinkListener(new IHyperlinkListener() {
				@Override
				public void linkExited(HyperlinkEvent e) {}
				@Override
				public void linkEntered(HyperlinkEvent e) {}
				@Override
				public void linkActivated(HyperlinkEvent e) {
					AttachmentUtil.launch(RScriptManager.INSTANCE.getScriptPath(ScriptPage.this.parent.getQuery().getScript()).toFile());
				}
			});
		}
		
		txtScript = new Text(main, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		txtScript.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtScript.setEditable(false);
	}

	@Override
	public void setFocus() {
		txtScript.setFocus();
	}
	
	public void update() {
		txtScript.setText(""); //$NON-NLS-1$
		Path fileToRead = RScriptManager.INSTANCE.getScriptPath(parent.getQuery().getScript());
		if (fileToRead == null || !Files.exists(fileToRead)) {
			txtScript.setText(Messages.ScriptPage_NotFound);
		}else {
			try(BufferedReader io = Files.newBufferedReader(fileToRead)){
				String line = null;
				StringBuilder sb = new StringBuilder();
				while ((line = io.readLine()) != null) {
					sb.append(line);
					sb.append("\n"); //$NON-NLS-1$
				}
				txtScript.setText(sb.toString());
			} catch (IOException ex) {
				txtScript.setText(Messages.ScriptPage_ReadError + "\n" + ex.getMessage()); //$NON-NLS-1$
				RPlugIn.log(ex.getMessage(), ex);
			}
		}
	}

}
