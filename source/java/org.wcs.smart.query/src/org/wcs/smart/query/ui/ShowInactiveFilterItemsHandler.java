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
package org.wcs.smart.query.ui;

import org.eclipse.core.commands.State;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.wcs.smart.query.QueryFilterConfigManager;

/**
 * Handler for "Show inactive Filter Items" action.
 * 
 * @author elitvin
 * @since 4.1.0
 */
public class ShowInactiveFilterItemsHandler {

	@Execute
	public void execute(EPartService pService) {
		State state = QueryFilterConfigManager.getShowInavtiveItemsState();
		state.setValue(!(Boolean)state.getValue());
	}

	public static class ShowInactiveFilterItemsHandlerWrapper extends DIHandler<ShowInactiveFilterItemsHandler> {
		public ShowInactiveFilterItemsHandlerWrapper() {
			super(ShowInactiveFilterItemsHandler.class);
		}
	}

}
