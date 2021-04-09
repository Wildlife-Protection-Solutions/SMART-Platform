/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.IdGeneratorEngine.Token;
import org.wcs.smart.internal.Messages;

/**
 * Tools for managing the generation of identifiers (patrol ids, incident ids etc).
 * 
 * @author Emily
 *
 */
public enum IdGeneratorManager {

	INSTANCE;
	
	public static final String EXTENSION_ID = "org.wcs.smart.idgenerator"; //$NON-NLS-1$

	/**
	 * Tokens that are universal
	 */
	public static final Token[] SHARED = {Token.HOUR, Token.MINUTE, Token.SECOND, Token.DAY, Token.MONTH, Token.YEAR, Token.TIMESTAMP};
	
	/**
	 * Get the description for a given token
	 * 
	 * @param token
	 * @return
	 */
	public String getDescription(Token token) {
		switch(token) {
			case DAY: return Messages.IdGeneratorManager_DayToken;
			case HOUR: return Messages.IdGeneratorManager_HourToken;
			case LEADER_FAMILY: return Messages.IdGeneratorManager_LeaderFamilyToken;
			case LEADER_GIVEN: return Messages.IdGeneratorManager_LeaderGivenToekn;
			case LEADER_INITIALS: return Messages.IdGeneratorManager_LeaderInitialToken;
			case MINUTE: return Messages.IdGeneratorManager_MinuteToken;
			case MONTH: return Messages.IdGeneratorManager_MonthToken;
			case OBSERVER_FAMILY: return Messages.IdGeneratorManager_ObserverFamilyToken;
			case OBSERVER_GIVEN: return Messages.IdGeneratorManager_ObserverGivenTooken;
			case OBSERVER_INITIALS: return Messages.IdGeneratorManager_ObserverInitialToken;
			case SECOND: return Messages.IdGeneratorManager_SecondToken;
			case TIMESTAMP: return Messages.IdGeneratorManager_TimestampToken;
			case YEAR: return Messages.IdGeneratorManager_YearToken;
		}
		return ""; //$NON-NLS-1$
	}
	
		
	public List<IdGeneratorContribution> getContributions(){
		
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IdGeneratorContribution> items = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add((IdGeneratorContribution)e.createExecutableExtension("uicontribution")); //$NON-NLS-1$
			}
		}catch (Exception ex){
			SmartPlugIn.log("Error getting id generator extensions", ex); //$NON-NLS-1$
		}
		return items;
	}

}

