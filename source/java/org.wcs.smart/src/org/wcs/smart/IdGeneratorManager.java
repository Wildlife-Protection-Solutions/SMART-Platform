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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.ca.Employee;
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
	 * key for supplying leader employee to token generator
	 */
	public static final String LEADER_KEY = "leader"; //$NON-NLS-1$
	/**
	 * key for supplygin observer to token generator
	 */
	public static final String OBSERVER_KEY = "observer"; //$NON-NLS-1$
	
	/**
	 * Pattern tokens 
	 * @author Emily
	 *
	 */
	public enum Token{
		HOUR("{h}"), //$NON-NLS-1$
		MINUTE("{m}"), //$NON-NLS-1$
		SECOND("{s}"), //$NON-NLS-1$
		DAY("{D}"), //$NON-NLS-1$
		MONTH("{M}"), //$NON-NLS-1$
		YEAR("{Y}"), //$NON-NLS-1$
		TIMESTAMP("{t}"), //$NON-NLS-1$
		
		LEADER_GIVEN("{leader.given}"), //$NON-NLS-1$
		LEADER_FAMILY("{leader.family}"), //$NON-NLS-1$
		LEADER_INITIALS("{leader.initials}"), //$NON-NLS-1$
		
		OBSERVER_GIVEN("{observer.given}"), //$NON-NLS-1$
		OBSERVER_FAMILY("{observer.family}"), //$NON-NLS-1$
		OBSERVER_INITIALS("{observer.initials}"); //$NON-NLS-1$
		
		public String token;
		
		Token(String token){
			this.token = token;
		}
		
		public String getDescription() {
			switch(this) {
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
	}
	
	/**
	 * Tokens that are universal
	 */
	public static final Token[] SHARED = {Token.HOUR, Token.MINUTE, Token.SECOND, Token.DAY, Token.MONTH, Token.YEAR, Token.TIMESTAMP}; 
	
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
	
	private String formatValue2(int value) {
		if (value < 10) {
			return "0" + String.valueOf(value); //$NON-NLS-1$
		}
		return String.valueOf(value);
	}
	
	/**
	 * 
	 * @param pattern
	 * @param employees link from employee key to employee; current
	 * supported employee keys are leader and observer
	 * 
	 * @return
	 */
	public String generateId(String pattern, Map<String,Employee> employees) {
		ZonedDateTime now = ZonedDateTime.now();
		
		String day = formatValue2(now.getDayOfMonth());
		String month = formatValue2(now.getMonthValue());
		String year = String.valueOf(now.getYear());
		
		String hour = formatValue2(now.getHour());
		String min = formatValue2(now.getMinute());
		String sec = formatValue2(now.getSecond());
		
		
		String timestamp = String.valueOf(now.toEpochSecond());
		
		pattern = pattern.replace(Token.MINUTE.token, min);
		pattern = pattern.replace(Token.SECOND.token, sec);
		pattern = pattern.replace(Token.HOUR.token, hour);
		
		pattern = pattern.replace(Token.YEAR.token, year);
		pattern = pattern.replace(Token.MINUTE.token, month);
		pattern = pattern.replace(Token.DAY.token, day);
		
		pattern = pattern.replace(Token.TIMESTAMP.token, timestamp);
		
		
		Employee leader = employees.get(LEADER_KEY);
		String lfamily = ""; //$NON-NLS-1$
		String lgiven = ""; //$NON-NLS-1$
		String linit = ""; //$NON-NLS-1$
		if (leader != null) {
			lfamily = leader.getFamilyName();
			lgiven = leader.getGivenName();
			linit = String.valueOf(lgiven.charAt(0)) + String.valueOf(lfamily.charAt(0));
		}
		
		pattern = pattern.replace(Token.LEADER_FAMILY.token, lfamily);
		pattern = pattern.replace(Token.LEADER_GIVEN.token, lgiven);
		pattern = pattern.replace(Token.LEADER_INITIALS.token, linit);
		
		Employee observer = employees.get(OBSERVER_KEY);
		String ofamily = ""; //$NON-NLS-1$
		String ogiven = ""; //$NON-NLS-1$
		String oinit = ""; //$NON-NLS-1$
		if (observer != null) {
			ofamily = observer.getFamilyName();
			ogiven = observer.getGivenName();
			oinit = String.valueOf(ogiven.charAt(0)) + String.valueOf(ofamily.charAt(0));
		}
		
		pattern = pattern.replace(Token.OBSERVER_FAMILY.token, ofamily);
		pattern = pattern.replace(Token.OBSERVER_GIVEN.token, ogiven);
		pattern = pattern.replace(Token.OBSERVER_INITIALS.token, oinit);
		
		
		return pattern;
	}
	
	/**
	 * 
	 * @param pattern
	 * @return null if the pattern is valid otherwise message
	 * describing the problem
	 */
	public String validatePattern(String pattern) {
		return null;
	}
	
}

