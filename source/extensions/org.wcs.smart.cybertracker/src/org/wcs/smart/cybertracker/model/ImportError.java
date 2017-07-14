package org.wcs.smart.cybertracker.model;
/**
 * Import error class to track import error messages
 * and error type.
 * @author Emily
 *
 */
public class ImportError {
	
	public enum ErrorType{ERROR, WARNING};
	
	private ErrorType errorType;
	private String problem;
	
	public ImportError(String problem, ErrorType type){
		this.errorType = type;
		this.problem = problem;
	}
	
	public ErrorType getType(){
		return this.errorType;
	}
	
	public String getMessage(){
		return this.problem;
	}
}
