package me.geso.jtt.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.Source;
import me.geso.jtt.Syntax;
import me.geso.jtt.TemplateLoader;
import me.geso.jtt.escape.HTMLEscaper;
import me.geso.jtt.exception.JTTError;
import me.geso.jtt.exception.ParserError;
import me.geso.jtt.exception.TemplateLoadingError;
import me.geso.jtt.parser.Node;
import me.geso.jtt.parser.NodeType;
import me.geso.jtt.tt.TTSyntax;

import org.junit.Test;

import com.google.common.collect.Lists;

public class VMTest {
	TemplateLoader loader = new TemplateLoader(null, null);
	Syntax syntax = new TTSyntax();
	Node nop = new Node(NodeType.NULL, 1);

	private String run(Irep irep, Map<String, Object>vars) {
		return new VM(syntax, loader, null, null, new HTMLEscaper(), irep, vars).run();
	}

	@Test
	public void test() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		IrepBuilder builder = newIrepBuilder();
		builder.addPool(OP.LOAD_CONST, "hoge", 0, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build(0, 1);
		String got = run(irep, new HashMap<>());
		assertEquals("hoge", got);
	}

	@Test
	public void testAdd() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		IrepBuilder builder = newIrepBuilder();
		builder.addPool(OP.LOAD_CONST, new Integer(3), 0, nop);
		builder.addPool(OP.LOAD_CONST, new Integer(4), 1, nop);
		// a = a + b
		builder.add(OP.ADD, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build(0, 2);
		String got = run(irep, new HashMap<>());
		assertEquals("7", got);
	}

	@Test
	public void testSubtract() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		IrepBuilder builder = newIrepBuilder();
		builder.addPool(OP.LOAD_CONST, new Integer(4), 0, nop);
		builder.addPool(OP.LOAD_CONST, new Integer(3), 1, nop);
		builder.add(OP.SUBTRACT, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build(0, 2);
		String got = run(irep, new HashMap<>());
		assertEquals("1", got);
	}

	@Test
	public void testElemMap() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, String> map = new HashMap<>();
		map.put("hoge", "fuga");

		IrepBuilder builder = newIrepBuilder();
		builder.addPool(OP.LOAD_CONST, map, 0, nop);
		builder.addPool(OP.LOAD_CONST, "hoge", 1, nop);
		builder.add(OP.GET_ELEM, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build(0, 2);
		String got = run(irep, new HashMap<>());
		assertEquals("fuga", got);
	}

	@Test
	public void testElemList() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		List<String> list = new ArrayList<>();
		list.add("HAH");
		list.add("Huh");

		IrepBuilder builder = newIrepBuilder();
		builder.addPool(OP.LOAD_CONST, list, 0, nop);
		builder.addPool(OP.LOAD_CONST, new Integer(1), 1, nop);
		builder.add(OP.GET_ELEM, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build(0, 2);
		String got = run(irep, new HashMap<>());
		assertEquals("Huh", got);
	}

	@Test
	public void testLoadVar() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();
		vars.put("foo", "bar");

		IrepBuilder builder = newIrepBuilder();
		builder.addPool(OP.LOAD_VAR, "foo", 0, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build(0, 1);
		String got = run(irep, vars);
		assertEquals("bar", got);
	}

	@Test
	public void testSetVar() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		IrepBuilder builder = newIrepBuilder();
		builder.addPool(OP.LOAD_CONST, "foo", 0, nop);
		builder.addPool(OP.SET_VAR, "v", 0, nop);
		builder.addPool(OP.LOAD_VAR, "v", 0, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build(0, 1);

		String got = run(irep, new HashMap<String, Object>());
		assertEquals("foo", got);
	}

	@Test
	public void testForEach() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();

		IrepBuilder builder = newIrepBuilder();
		builder.addPool(OP.LOAD_CONST, Lists.newArrayList("foo", "bar"), 0, nop);
		builder.increaseLoopStackSize();
		builder.add(OP.ITER_START, 0, nop);
		builder.addPool(OP.SET_VAR, "v", 0, nop);
		builder.addPool(OP.LOAD_VAR, "v", 0, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.add(OP.FOR_ITER, 0, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build(0, 1);
		String got = run(irep, vars);
		assertEquals("foobar", got);
	}

	@Test
	public void testEquals() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();

		IrepBuilder builder = newIrepBuilder();
		builder.addPool(OP.LOAD_CONST, "foo", 0, nop);
		builder.addPool(OP.LOAD_CONST, "foo", 1, nop);
		builder.add(OP.EQUALS, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.addPool(OP.LOAD_CONST, "foo", 0, nop);
		builder.addPool(OP.LOAD_CONST, "bar", 1, nop);
		builder.add(OP.EQUALS, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);
		builder.add(OP.RETURN, nop);

		Irep irep = builder.build(0, 2);
		String got = run(irep, vars);
		assertEquals("truefalse", got);
	}

	@Test
	public void testGraterThan() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();

		IrepBuilder builder = newIrepBuilder();

		// 3<9
		builder.addPool(OP.LOAD_CONST, 9, 0, nop);
		builder.addPool(OP.LOAD_CONST, 3, 1, nop);
		builder.add(OP.GT, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);

		// 3<3
		builder.addPool(OP.LOAD_CONST, 3, 0, nop);
		builder.addPool(OP.LOAD_CONST, 3, 1, nop);
		builder.add(OP.GT, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);

		// 9<3
		builder.addPool(OP.LOAD_CONST, 3, 0, nop);
		builder.addPool(OP.LOAD_CONST, 9, 1, nop);
		builder.add(OP.GT, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);

		// 3.04<3.14
		builder.addPool(OP.LOAD_CONST, 3.14, 0, nop);
		builder.addPool(OP.LOAD_CONST, 3.04, 1, nop);
		builder.add(OP.GT, 0, 1, nop);
		builder.add(OP.APPEND, 0, nop);

		builder.add(OP.RETURN, 0, nop);

		Irep irep = builder.build(0, 2);
		String got = run(irep, vars);
		assertEquals("truefalsefalsetrue", got);
	}

	@Test
	public void testDoGE() throws JTTError {
		IrepBuilder builder = newIrepBuilder();
		Irep irep = builder.build(0, 2);
		VM vm = new VM(syntax, loader, null, null, new HTMLEscaper(), irep, new HashMap<>());

		// 3 >= 4
		assertFalse(vm.doGE(new Integer(3), new Integer(4)));
		// 3 >= 3
		assertTrue(vm.doGE(new Integer(3), new Integer(3)));
		// 4 >= 3
		assertTrue(vm.doGE(new Integer(4), new Integer(3)));
	}

	private IrepBuilder newIrepBuilder() {
		return new IrepBuilder(Source.fromString("-"));
	}
}
