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
package org.wcs.smart.splashHandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;

/**
 * Tracks user logins 
 * 
 * @author Scott Lewis
 *
 */
public class UserLoginInfo implements Comparable<UserLoginInfo> {

	private String key;
	private static final String USERNAME_NODE = "username"; //$NON-NLS-1$
	private String username;
	private static final String LASTUSED_NODE = "lastUsed"; //$NON-NLS-1$
	private int lastUsed;

	private UserLoginInfo() {
	}

	public UserLoginInfo(String username) {
		Assert.isNotNull(username);
		this.username = username;
		this.lastUsed = new Long(System.currentTimeMillis() / 10000).intValue();
	}

	@Override
	public int compareTo(UserLoginInfo o) {
		if (o == null)
			return 0;
		return o.lastUsed - this.lastUsed;
	}

	private static IEclipsePreferences getPreferences() {
		// TODO Instance scope might be more appropriate, but
		// let's figure that out
		return ConfigurationScope.INSTANCE
				.getNode("org.wcs.smart.userLoginInfoPrefs"); //$NON-NLS-1$
	}

	public String getUsername() {
		return this.username;
	}

	public static UserLoginInfo[] readAllFromStore() {
		return readAllFromStore(getPreferences());
	}

	private static UserLoginInfo[] readAllFromStore(Preferences preferences) {
		List<UserLoginInfo> userInfos = new ArrayList<UserLoginInfo>();
		try {
			for (String key : preferences.childrenNames()) {
				if (preferences.nodeExists(key)) {
					Preferences node = preferences.node(key);
					if (node != null) {
						UserLoginInfo user = new UserLoginInfo();
						user.key = key;
						user.username = node.get(USERNAME_NODE, ""); //$NON-NLS-1$
						user.lastUsed = node.getInt(LASTUSED_NODE, 0);
						userInfos.add(user);
					}
				}
			}
		} catch (BackingStoreException e1) {
			SmartPlugIn.displayLog(Messages.UserLoginInfo_ReadError, e1);
			return new UserLoginInfo[0];
		}
		UserLoginInfo[] results = userInfos.toArray(new UserLoginInfo[userInfos
				.size()]);
		Arrays.sort(results);
		return results;
	}

	public boolean writeToStore() {
		Preferences preferences = getPreferences();
		UserLoginInfo[] persistedUserInfos = readAllFromStore(preferences);
		for (UserLoginInfo existingUserInfo : persistedUserInfos)
			if (existingUserInfo.username.equals(this.username))
				this.key = existingUserInfo.key;
		if (this.key == null)
			this.key = String.valueOf(persistedUserInfos.length);
		Preferences userInfoPrefs = preferences.node(this.key);
		userInfoPrefs.put(USERNAME_NODE, this.username);
		userInfoPrefs.putInt(LASTUSED_NODE, this.lastUsed);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			SmartPlugIn.log("Unexpected exception flushing userInfo", e); //$NON-NLS-1$
			return false;
		}
		return true;
	}
}