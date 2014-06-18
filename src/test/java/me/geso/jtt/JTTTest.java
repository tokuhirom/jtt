package me.geso.jtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.exception.JTTError;
import me.geso.jtt.exception.ParserError;
import me.geso.jtt.exception.TemplateLoadingError;

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
	public void testWarningsHandler() throws ParserError, JTTError,
			TemplateLoadingError, IOException {
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

	@Test
	public void testRenderFile() throws IOException {
		assertEquals("foo\n", render("foo.tt", new HashMap<>()));
	}

	@Test
	public void testRenderFile2() throws IOException, JTTError {
		assertEquals("INC1_HEAD\nINC2\n\nINC1_FOOT\n",
				render("inc1.tt", new HashMap<>()));
	}

	@Test
	public void testRenderFile3() throws IOException, JTTError {
		assertEquals("<body>\nfoo\n</body>\n\n",
				render("wrap.tt", new HashMap<>()));
	}

	@Test
	public void testRenderString1() throws IOException, JTTError {
		List<String> items = new ArrayList<>();
		items.add("hoge");
		items.add("fuga");
		items.add("hige");

		HashMap<String, Object> params = new HashMap<>();
		params.put("items", items);

		assertEquals(
				"evenoddeven",
				renderString(
						"[% FOR item IN items %][% ['odd','even'][loop.getCount()%2] %][% END %]",
						params));
	}

	@Test
	public void testRenderString2() throws IOException, JTTError {
		HashMap<String, Object> params = new HashMap<>();

		assertEquals("even", renderString("[% ['odd','even'][1%2] %]", params));
		assertEquals("odd", renderString("[% ['odd','even'][2%2] %]", params));
	}

	// Added test case for ParserError contains error position.
	@Test
	public void testError() throws IOException, JTTError {
		HashMap<String, Object> params = new HashMap<>();

		boolean errorSeen = false;
		try {
			render("error.tt", params);
		} catch (ParserError e) {
			errorSeen = true;
			assertTrue(e.toString().contains("[% IF %]"));
		}
		assertTrue(errorSeen);
	}

	// ---------------------------------------------------------------
	// utils.

	private String renderString(String tmpl, Map<String, Object> vars) {
		JTT jslate = new JTTBuilder().setIncludePaths(buildIncludePaths())
				.build();
		return jslate.renderString(tmpl, vars);
	}

	private String render(String fileName, Map<String, Object> vars)
			throws JTTError, TemplateLoadingError, IOException {
		JTT jslate = new JTTBuilder().setIncludePaths(buildIncludePaths())
				.build();
		return jslate.render(new File(fileName), vars);
	}

	private List<Path> buildIncludePaths() {
		URL resource = this.getClass().getResource("/");
		File includePath = new File(resource.getFile());

		// Note: You can't use Lists.newArrayList() here.
		// Because Path is iterable.
		List<Path> paths = new ArrayList<>();
		paths.add(includePath.toPath());

		return paths;
	}
}
