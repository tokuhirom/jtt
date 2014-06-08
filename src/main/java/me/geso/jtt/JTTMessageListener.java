package me.geso.jtt;

/**
 * Warning listener interface for JTT.
 * 
 * @author tokuhirom
 *
 */
public interface JTTMessageListener {
	public void sendMessage(String message, int lineno, String fileName);
}
