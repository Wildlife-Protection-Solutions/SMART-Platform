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
import java.util.Map;

import org.wcs.smart.ca.Employee;

/**
 * ID Generator
 * 
 * @author Emily
 *
 */
public enum IdGeneratorEngine {

	INSTANCE;
	
	
	/**
	 * key for supplying leader employee to token generator
	 */
	public static final String LEADER_KEY = "leader"; //$NON-NLS-1$
	/**
	 * key for supplygin observer to token generator
	 */
	public static final String OBSERVER_KEY = "observer"; //$NON-NLS-1$
	
	/**
	 * Id pattern tokens
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
		pattern = pattern.replace(Token.MONTH.token, month);
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
}
