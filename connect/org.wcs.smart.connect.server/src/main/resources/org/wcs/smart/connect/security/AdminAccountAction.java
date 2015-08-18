/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.security;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.connect.i18n.Messages;

/**
 * Administrator actions.  This action allows uers to do anything in the system.
 * 
 * @author Emily
 *
 */
public class AdminAccountAction implements ISmartConnectAction{

	public static final String KEY = "admin"; //$NON-NLS-1$
	
	@Override
	public String getActionName(String actionKey, Locale l) {
		if (actionKey.equals(KEY)){
			return Messages.getString("AdminAccountAction.AdminPermission", l); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String[] getActionKeys() {
		return new String[]{KEY};
	}

	@Override
	public List<ResourceOption> getResourceOptions(String actionKey, Session s, Locale l) {
		return null;
	}

	@Override
	public String getResourceName(UUID resource, Session s, Locale l) {
		return null;
	}

}
