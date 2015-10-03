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
package org.wcs.smart.patrol.meta;

import org.wcs.smart.patrol.internal.Messages;

/**
 * @author elitvin
 * @since 3.3.0
 */
public class MetaLabelUtil {

	public static final String SC_OP_TYPE = Messages.ScreenOption_PatrolType;
	public static final String SC_OP_TRANSPORT = Messages.ScreenOption_TransportType;
	public static final String SC_OP_ARMED = Messages.ScreenOption_Armed;
	public static final String SC_OP_STATION = Messages.ScreenOption_Station;
	public static final String SC_OP_TEAM = Messages.ScreenOption_Team;
	public static final String SC_OP_MANDATE = Messages.ScreenOption_Mandate;
	public static final String SC_OP_OBJECTIVE = Messages.ScreenOption_Objective;
	public static final String SC_OP_COMMENT = Messages.ScreenOption_Comment;
	public static final String SC_OP_MEMBERS = Messages.ScreenOption_Members;
	public static final String SC_OP_LEADER = Messages.ScreenOption_Leader;
	public static final String SC_OP_PILOT = Messages.ScreenOption_Pilot;

	public static String getLabel(PatrolScreenOptionMeta op){
		switch(op){
		case ARMED:return SC_OP_ARMED;
		case COMMENT: return SC_OP_COMMENT;
		case LEADER:return SC_OP_LEADER;
		case MANDATE:return SC_OP_MANDATE;
		case MEMBERS:return SC_OP_MEMBERS;
		case OBJECTIVE:return SC_OP_OBJECTIVE;
		case PILOT:return SC_OP_PILOT;
		case STATION:return SC_OP_STATION;
		case TEAM:return SC_OP_TEAM;
		case TRANSPORT:return SC_OP_TRANSPORT;
		case TYPE: return SC_OP_TYPE;
		default:
			return null;
			
		}
	}
}
