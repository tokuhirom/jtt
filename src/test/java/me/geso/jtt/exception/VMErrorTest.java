package me.geso.jtt.exception;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;

import me.geso.jtt.JTT;
import me.geso.jtt.JTTBuilder;

import org.junit.Test;

public class VMErrorTest {

	@Test
	public void test() {
		boolean seenError = false;

		JTT jtt = new JTTBuilder().addIncludePath(
				new File("src/test/resources/").toPath()).build();
		try {
			jtt.renderFile("runtime-error.tt", new HashMap<>());
		} catch (VMError e) {
			seenError = true;
			assertThat(e.toString(), containsString("[% null < 3 %]"));
		}
		assertTrue(seenError);
	}

	@Test
	public void testString() {
		boolean seenError = false;

		JTT jtt = new JTTBuilder().build();
		try {
			jtt.renderString("[% null < 3 %]", new HashMap<>());
		} catch (VMError e) {
			seenError = true;
			assertThat(e.toString(), containsString("[% null < 3 %]"));
		}
		assertTrue(seenError);
	}
	
}
