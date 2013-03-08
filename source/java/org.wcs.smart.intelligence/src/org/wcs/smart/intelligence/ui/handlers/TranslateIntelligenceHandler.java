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
package org.wcs.smart.intelligence.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditorInput;
import org.wcs.smart.ui.TranslateNamesHandler;

/**
 * Handler that brings up the intelligence translate dialog.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class TranslateIntelligenceHandler extends TranslateNamesHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection thisSelection = HandlerUtil.getCurrentSelection(event);
		if (thisSelection == null || thisSelection.isEmpty() || !(thisSelection instanceof IStructuredSelection)) {
			return null;
		}
		
		Object obj = ((IStructuredSelection)thisSelection).getFirstElement();

		Intelligence intelligence = null;
		if (obj instanceof IntelligenceEditorInput) {
			byte[] uuid = ((IntelligenceEditorInput) obj).getUuid();
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			intelligence = (Intelligence) session.load(Intelligence.class, uuid);
			session.getTransaction().rollback();
			session.close();
		} else if (obj instanceof Intelligence) {
			intelligence = (Intelligence) obj;
		}
		
		if (intelligence != null) {
			translateItem(intelligence, event);
			IntelligenceEventManager.getInstance().intelligenceChanged(0, intelligence);
		}
		
		return null;
	}

}
