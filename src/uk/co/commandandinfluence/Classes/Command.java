package uk.co.commandandinfluence.Classes;

import java.util.HashMap;

/**
 * A class for the Command object returned by Pusher
 * 
 * @author nathan
 */
public class Command {
	public int STATE_RECEIVED = 1;
	public int STATE_FAILED = 2;
	public int STATE_SUCCESS = 3;
	
	public String id = "";
	public String message = "";
	public String command = "";
	public int state = STATE_RECEIVED;
	
	public HashMap<String, String> extras = new HashMap<String, String>();
	
	Command() {
		
	}
	
	@Override
	public String toString() {
		return this.message;
	}
}