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
package org.wcs.smart.er.ui;

import java.util.UUID;

/**
 * Tree node for the survey list tree.
 * @author Emily
 *
 */
public class SurveyListTreeNode {

	/**
	 * Type of element the node represents
	 * @author Emily
	 *
	 */
	public enum Type {SURVEY, MISSION};
	
	private String label;
	private UUID uuid;
	private Type type;
	private SurveyListTreeNode parent;
	
	public SurveyListTreeNode(String label, UUID uuid, Type type, SurveyListTreeNode parent){
		this.label = label;
		this.uuid = uuid;
		this.type = type;
		this.parent = parent;
	}
	
	public SurveyListTreeNode(String label, UUID uuid, Type type){
		this(label, uuid, type, null);
	}
	
	public String getLabel(){
		return this.label;
	}
	
	public UUID getUuid(){
		return this.uuid;
	}
	
	public Type getType(){
		return this.type;
	}
	
	public SurveyListTreeNode getParent(){
		return this.parent;
	}
	
	@Override
	public int hashCode(){
		return uuid.hashCode();
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof SurveyListTreeNode){
			return uuid.equals(((SurveyListTreeNode)other).uuid);
		}
		return false;
	}
}
