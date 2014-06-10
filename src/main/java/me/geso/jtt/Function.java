package me.geso.jtt;

/**
 * Function object for JSlate template.
 * @author tokuhirom
 *
 */
@FunctionalInterface
public interface Function {
	public Object call(Object[] objects);
}
