package me.geso.jtt;

import me.geso.jtt.vm.Irep;

/**
 * Warning/Error message listener interface for JTT.
 * 
 * @author tokuhirom
 *
 */
@FunctionalInterface
public interface JTTMessageListener {
	public void sendMessage(String message, int pc, Irep irep);
}
