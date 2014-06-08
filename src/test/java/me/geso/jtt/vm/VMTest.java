package me.geso.jtt.vm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geso.jtt.Compiler;
import me.geso.jtt.TemplateLoader;
import me.geso.jtt.TemplateLoadingError;
import me.geso.jtt.parser.ParserError;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.vm.IrepBuilder;
import me.geso.jtt.vm.JSlateException;
import me.geso.jtt.vm.OP;
import me.geso.jtt.vm.VM;

import org.junit.Test;

import com.google.common.collect.Lists;

public class VMTest {
	TemplateLoader loader = new TemplateLoader(null, null);
	Compiler compiler = new Compiler();
	VM vm = new VM(compiler, loader, null, null);

	@Test
	public void test() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		IrepBuilder builder = new IrepBuilder();
		builder.addPool(OP.LOAD_CONST, "hoge");
		builder.add(OP.APPEND);
		builder.add(OP.RETURN);
		Irep irep = builder.build();
		String got = vm.run(irep, new HashMap<>());
		assertEquals("hoge", got);
	}

	@Test
	public void testAdd() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		IrepBuilder builder = new IrepBuilder();
		builder.addPool(OP.LOAD_CONST, new Integer(3));
		builder.addPool(OP.LOAD_CONST, new Integer(4));
		builder.add(OP.ADD);
		builder.add(OP.APPEND);
		builder.add(OP.RETURN);
		Irep irep = builder.build();
		String got = vm.run(irep, new HashMap<>());
		assertEquals("7", got);
	}

	@Test
	public void testSubtract() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		IrepBuilder builder = new IrepBuilder();
		builder.addPool(OP.LOAD_CONST, new Integer(3));
		builder.addPool(OP.LOAD_CONST, new Integer(4));
		builder.add(OP.SUBTRACT);
		builder.add(OP.APPEND);
		builder.add(OP.RETURN);
		Irep irep = builder.build();
		String got = vm.run(irep, new HashMap<>());
		assertEquals("1", got);
	}

	@Test
	public void testElemMap() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		Map<String, String> map = new HashMap<>();
		map.put("hoge", "fuga");

		IrepBuilder builder = new IrepBuilder();
		builder.addPool(OP.LOAD_CONST, map);
		builder.addPool(OP.LOAD_CONST, "hoge");
		builder.add(OP.ELEM);
		builder.add(OP.APPEND);
		builder.add(OP.RETURN);
		Irep irep = builder.build();
		String got = vm.run(irep, new HashMap<>());
		assertEquals("fuga", got);
	}

	@Test
	public void testElemList() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		List<String> list = new ArrayList<>();
		list.add("HAH");
		list.add("Huh");

		IrepBuilder builder = new IrepBuilder();
		builder.addPool(OP.LOAD_CONST, list);
		builder.addPool(OP.LOAD_CONST, new Integer(1));
		builder.add(OP.ELEM);
		builder.add(OP.APPEND);
		builder.add(OP.RETURN);
		Irep irep = builder.build();
		String got = vm.run(irep, new HashMap<>());
		assertEquals("Huh", got);
	}

	@Test
	public void testLoadVar() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();
		vars.put("foo", "bar");

		IrepBuilder builder = new IrepBuilder();
		builder.addPool(OP.LOAD_VAR, "foo");
		builder.add(OP.APPEND);
		builder.add(OP.RETURN);
		Irep irep = builder.build();
		String got = vm.run(irep, vars);
		assertEquals("bar", got);
	}

	@Test
	public void testSetVar() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		IrepBuilder builder = new IrepBuilder();
		builder.addPool(OP.LOAD_CONST, "foo");
		builder.addPool(OP.SET_VAR, "v");
		builder.addPool(OP.LOAD_VAR, "v");
		builder.add(OP.APPEND);
		builder.add(OP.RETURN);
		Irep irep = builder.build();

		String got = vm.run(irep, new HashMap<String, Object>());
		assertEquals("foo", got);
	}

	@Test
	public void testForEach() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();

		IrepBuilder builder = new IrepBuilder();
		builder.addPool(OP.LOAD_CONST, Lists.newArrayList("foo", "bar"));
		builder.add(OP.ITER_START);
		builder.addPool(OP.SET_VAR, "v");
		builder.addPool(OP.LOAD_VAR, "v");
		builder.add(OP.APPEND);
		builder.add(OP.FOR_ITER);
		builder.add(OP.RETURN);
		Irep irep = builder.build();
		String got = vm.run(irep, vars);
		assertEquals("foobar", got);
	}

	@Test
	public void testEquals() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();

		IrepBuilder builder = new IrepBuilder();
		builder.addPool(OP.LOAD_CONST, "foo");
		builder.addPool(OP.LOAD_CONST, "foo");
		builder.add(OP.EQAULS);
		builder.add(OP.APPEND);
		builder.addPool(OP.LOAD_CONST, "foo");
		builder.addPool(OP.LOAD_CONST, "bar");
		builder.add(OP.EQAULS);
		builder.add(OP.APPEND);
		builder.add(OP.RETURN);

		Irep irep = builder.build();
		String got = vm.run(irep, vars);
		assertEquals("truefalse", got);
	}

	@Test
	public void testGraterThan() throws JSlateException, IOException, ParserError, TemplateLoadingError {
		Map<String, Object> vars = new HashMap<>();

		IrepBuilder builder = new IrepBuilder();

		// 3<9
		builder.addPool(OP.LOAD_CONST, 3);
		builder.addPool(OP.LOAD_CONST, 9);
		builder.add(OP.GT);
		builder.add(OP.APPEND);

		// 3<3
		builder.addPool(OP.LOAD_CONST, 3);
		builder.addPool(OP.LOAD_CONST, 3);
		builder.add(OP.GT);
		builder.add(OP.APPEND);

		// 9<3
		builder.addPool(OP.LOAD_CONST, 9);
		builder.addPool(OP.LOAD_CONST, 3);
		builder.add(OP.GT);
		builder.add(OP.APPEND);

		// 3.04<3.14
		builder.addPool(OP.LOAD_CONST, 3.04);
		builder.addPool(OP.LOAD_CONST, 3.14);
		builder.add(OP.GT);
		builder.add(OP.APPEND);

		builder.add(OP.RETURN);

		Irep irep = builder.build();
		String got = vm.run(irep, vars);
		assertEquals("truefalsefalsetrue", got);
	}

	@Test
	public void testDoGE() throws JSlateException {
		// 3 >= 4
		assertFalse(vm.doGE(new Integer(3), new Integer(4)));
		// 3 >= 3
		assertTrue(vm.doGE(new Integer(3), new Integer(3)));
		// 4 >= 3
		assertTrue(vm.doGE(new Integer(4), new Integer(3)));
	}
}
