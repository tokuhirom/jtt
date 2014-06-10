package me.geso.jtt.exception;

public class JTTError extends Error {
	public JTTError(String string) {
		super(string);
	}
	
	public JTTError() { }
	
	public JTTError(Throwable e) {
		super(e);
	}

	private static final long serialVersionUID = 1L;
}
