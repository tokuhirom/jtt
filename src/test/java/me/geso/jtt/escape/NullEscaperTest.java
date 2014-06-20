package me.geso.jtt.escape;

import static org.junit.Assert.*;

import org.junit.Test;


class Foo {
	public String toString() {
		return "<>";
	}
}

public class NullEscaperTest {

	@Test
	public void test() {
		NullEscaper ne = new NullEscaper();
		assertEquals(ne.escape("<>"), "<>");
	}

}
