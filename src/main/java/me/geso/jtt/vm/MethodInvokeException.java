package me.geso.jtt.vm;

import me.geso.jtt.JTTError;


class MethodInvokeException extends JTTError {
	private static final long serialVersionUID = 1L;

	public MethodInvokeException(Throwable e) {
		super(e);
	}
}