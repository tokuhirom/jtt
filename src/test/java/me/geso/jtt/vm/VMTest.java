package me.geso.jtt.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.Syntax;
import me.geso.jtt.TemplateLoader;
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
		return new VM(syntax, loader, null, null, irep, vars).run();
	}

	@Test
	public void test() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		IrepBuilder builder = IrepBuilder.fromString("-");
		builder.addPool(OP.LOAD_CONST, "hoge", nop);
		builder.add(OP.APPEND, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build();
		String got = run(irep, new HashMap<>());
		assertEquals("hoge", got);
	}

	@Test
	public void testAdd() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		IrepBuilder builder = IrepBuilder.fromString("-");
		builder.addPool(OP.LOAD_CONST, new Integer(3), nop);
		builder.addPool(OP.LOAD_CONST, new Integer(4), nop);
		builder.add(OP.ADD, nop);
		builder.add(OP.APPEND, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build();
		String got = run(irep, new HashMap<>());
		assertEquals("7", got);
	}

	@Test
	public void testSubtract() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		IrepBuilder builder = IrepBuilder.fromString("-");
		builder.addPool(OP.LOAD_CONST, new Integer(3), nop);
		builder.addPool(OP.LOAD_CONST, new Integer(4), nop);
		builder.add(OP.SUBTRACT, nop);
		builder.add(OP.APPEND, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build();
		String got = run(irep, new HashMap<>());
		assertEquals("1", got);
	}

	@Test
	public void testElemMap() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, String> map = new HashMap<>();
		map.put("hoge", "fuga");

		IrepBuilder builder = IrepBuilder.fromString("-");
		builder.addPool(OP.LOAD_CONST, map, nop);
		builder.addPool(OP.LOAD_CONST, "hoge", nop);
		builder.add(OP.GET_ELEM, nop);
		builder.add(OP.APPEND, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build();
		String got = run(irep, new HashMap<>());
		assertEquals("fuga", got);
	}

	@Test
	public void testElemList() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		List<String> list = new ArrayList<>();
		list.add("HAH");
		list.add("Huh");

		IrepBuilder builder = IrepBuilder.fromString("-");
		builder.addPool(OP.LOAD_CONST, list, nop);
		builder.addPool(OP.LOAD_CONST, new Integer(1), nop);
		builder.add(OP.GET_ELEM, nop);
		builder.add(OP.APPEND, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build();
		String got = run(irep, new HashMap<>());
		assertEquals("Huh", got);
	}

	@Test
	public void testLoadVar() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();
		vars.put("foo", "bar");

		IrepBuilder builder = IrepBuilder.fromString("-");
		builder.addPool(OP.LOAD_VAR, "foo", nop);
		builder.add(OP.APPEND, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build();
		String got = run(irep, vars);
		assertEquals("bar", got);
	}

	@Test
	public void testSetVar() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		IrepBuilder builder = IrepBuilder.fromString("-");
		builder.addPool(OP.LOAD_CONST, "foo", nop);
		builder.addPool(OP.SET_VAR, "v", nop);
		builder.addPool(OP.LOAD_VAR, "v", nop);
		builder.add(OP.APPEND, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build();

		String got = run(irep, new HashMap<String, Object>());
		assertEquals("foo", got);
	}

	@Test
	public void testForEach() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();

		IrepBuilder builder = IrepBuilder.fromString("-");
		builder.addPool(OP.LOAD_CONST, Lists.newArrayList("foo", "bar"), nop);
		builder.add(OP.ITER_START, nop);
		builder.addPool(OP.SET_VAR, "v", nop);
		builder.addPool(OP.LOAD_VAR, "v", nop);
		builder.add(OP.APPEND, nop);
		builder.add(OP.FOR_ITER, nop);
		builder.add(OP.RETURN, nop);
		Irep irep = builder.build();
		String got = run(irep, vars);
		assertEquals("foobar", got);
	}

	@Test
	public void testEquals() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();

		IrepBuilder builder = IrepBuilder.fromString("-");
		builder.addPool(OP.LOAD_CONST, "foo", nop);
		builder.addPool(OP.LOAD_CONST, "foo", nop);
		builder.add(OP.EQUALS, nop);
		builder.add(OP.APPEND, nop);
		builder.addPool(OP.LOAD_CONST, "foo", nop);
		builder.addPool(OP.LOAD_CONST, "bar", nop);
		builder.add(OP.EQUALS, nop);
		builder.add(OP.APPEND, nop);
		builder.add(OP.RETURN, nop);

		Irep irep = builder.build();
		String got = run(irep, vars);
		assertEquals("truefalse", got);
	}

	@Test
	public void testGraterThan() throws JTTError, IOException, ParserError,
			TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();

		IrepBuilder builder = IrepBuilder.fromString("-");

		// 3<9
		builder.addPool(OP.LOAD_CONST, 3, nop);
		builder.addPool(OP.LOAD_CONST, 9, nop);
		builder.add(OP.GT, nop);
		builder.add(OP.APPEND, nop);

		// 3<3
		builder.addPool(OP.LOAD_CONST, 3, nop);
		builder.addPool(OP.LOAD_CONST, 3, nop);
		builder.add(OP.GT, nop);
		builder.add(OP.APPEND, nop);

		// 9<3
		builder.addPool(OP.LOAD_CONST, 9, nop);
		builder.addPool(OP.LOAD_CONST, 3, nop);
		builder.add(OP.GT, nop);
		builder.add(OP.APPEND, nop);

		// 3.04<3.14
		builder.addPool(OP.LOAD_CONST, 3.04, nop);
		builder.addPool(OP.LOAD_CONST, 3.14, nop);
		builder.add(OP.GT, nop);
		builder.add(OP.APPEND, nop);

		builder.add(OP.RETURN, nop);

		Irep irep = builder.build();
		String got = run(irep, vars);
		assertEquals("truefalsefalsetrue", got);
	}

	@Test
	public void testDoGE() throws JTTError {
		IrepBuilder builder = IrepBuilder.fromString("-");
		Irep irep = builder.build();
		VM vm = new VM(syntax, loader, null, null, irep, new HashMap<>());

		// 3 >= 4
		assertFalse(vm.doGE(new Integer(3), new Integer(4)));
		// 3 >= 3
		assertTrue(vm.doGE(new Integer(3), new Integer(3)));
		// 4 >= 3
		assertTrue(vm.doGE(new Integer(4), new Integer(3)));
	}
}
