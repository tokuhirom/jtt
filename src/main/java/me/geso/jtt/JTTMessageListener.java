package me.geso.jtt;

/**
 * Warning/Error message listener interface for JTT.
 * 
 * @author tokuhirom
 *
 */
@FunctionalInterface
public interface JTTMessageListener {
	public void sendMessage(String message, int lineno, String fileName);
}
