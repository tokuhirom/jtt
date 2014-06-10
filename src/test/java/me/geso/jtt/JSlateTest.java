package me.geso.jtt;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import me.geso.jtt.JTT;
import me.geso.jtt.JTTBuilder;
import me.geso.jtt.TemplateLoadingError;
import me.geso.jtt.parser.ParserError;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class JSlateTest {

	@Test
	public void testRenderString() throws IOException, ClassNotFoundException,
			NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			InstantiationException, ParserError, JTTCompilerError, TemplateLoadingError {
		JTTBuilder builder = new JTTBuilder();
		JTT jslate = builder.build();
		assertEquals("hoge", jslate.renderString("hoge", new HashMap<>()));
	}

	@Test
	public void testRenderFile() throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, ParserError, JTTCompilerError, TemplateLoadingError {
		URL resource = this.getClass().getResource("/");
		File includePath = new File(resource.getFile());
		List<Path> paths = new ArrayList<>();
		paths.add(includePath.toPath());
		JTT jslate = new JTTBuilder().setIncludePaths(paths).build();
		assertEquals("foo\n", jslate.render(new File("foo.tt"), new HashMap<>()));
	}

	@Test
	public void testRenderFile2() throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, ParserError, JTTCompilerError, TemplateLoadingError {
		assertEquals("INC1_HEAD\nINC2\n\nINC1_FOOT\n", render("inc1.tt", new HashMap<>()));
	}
	
	private String render(String fileName, Map<String, Object> vars) throws IOException, ParserError, JTTCompilerError, TemplateLoadingError {
		URL resource = this.getClass().getResource("/");
		File includePath = new File(resource.getFile());
		List<Path> paths = new ArrayList<>();
		paths.add(includePath.toPath());
		JTT jslate = new JTTBuilder().setIncludePaths(paths).build();
		return jslate.render(new File(fileName), vars);
	}
}
