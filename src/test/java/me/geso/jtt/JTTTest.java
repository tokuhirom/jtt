package me.geso.jtt;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.geso.jtt.exception.JTTError;
import me.geso.jtt.exception.TemplateLoadingError;
import me.geso.jtt.parser.ParserError;

import org.junit.Test;

public class JTTTest {
	int n = 0;
	List<List<Object>> registry = new ArrayList<>();

	@Test
	public void test() throws ParserError, JTTError, IOException,
			TemplateLoadingError {
		JTT jtt = new JTTBuilder().addFunction("twice", x -> {
			return ((Integer) x[0]) * 2;
		}).build();
		assertEquals("6", jtt.renderString("[% twice(3) %]", null));
	}

	@Test
	public void testFuncallWith2Args() throws ParserError, JTTError,
			IOException, TemplateLoadingError {
		JTT jtt = new JTTBuilder().addFunction("sub", x -> {
			return ((Integer) x[0]) - ((Integer) x[1]);
		}).build();
		assertEquals("95", jtt.renderString("[% sub(99, 4) %]", null));
	}

	@Test
	public void testFuncallArgumentOrder() throws ParserError, JTTError,
			IOException, TemplateLoadingError {
		this.n = 0;

		JTT jtt = new JTTBuilder().addFunction("record", args -> {
			ArrayList<Object> objs = new ArrayList<Object>();
			for (Object o : args) {
				objs.add(o);
			}
			registry.add(objs);
			return objs;
		}).addFunction("cnt", args -> {
			return n++;
		}).build();

		assertEquals("[0, 1, 2]",
				jtt.renderString("[% record(cnt(), cnt(),cnt()) %]", null));
	}

	@Test
	public void testWarningsHandler() throws ParserError, JTTError, TemplateLoadingError, IOException {
		final List<String> messages = new ArrayList<>();
		assertEquals(
				"(null)",
				new JTTBuilder()
						.setWarningListener((message, lineno, fileName) -> {
							messages.add(message);
						}).build().renderString("[% null %]", null));
		assertEquals(1, messages.size());
		assertEquals("Appending null", messages.get(0));
	}
}
