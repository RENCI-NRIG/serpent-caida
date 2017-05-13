package org.renci.serpent.caida.util;

public class CAIDAException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CAIDAException(String e) {
		super("CAIDA: " + e);
	}

}
