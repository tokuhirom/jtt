package me.geso.jtt;

import com.google.common.collect.Lists;
import me.geso.jtt.lexer.Token;
import me.geso.jtt.parser.Node;
import me.geso.jtt.parser.ParserError;
import me.geso.jtt.tt.TTSyntax;
import me.geso.jtt.vm.Irep;
import me.geso.jtt.vm.JSlateException;
import me.geso.jtt.vm.VM;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CompilerTest {
    Compiler compiler = new Compiler();
    TemplateLoader loader = new TemplateLoader(null, null);
    VM vm = new VM(compiler, loader, null);

    @Test
    public void test() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Irep irep = compiler.compile("hoge");
        assertNotEquals(0, irep.getIseq().length);
        String got = vm.run(irep, null);
        assertEquals("hoge", got);
    }

    @Test
    public void testInt() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Irep irep = compiler.compile("hoge[% 5963 %]");
        String got = vm.run(irep, null);
        assertEquals("hoge5963", got);
    }

    @Test
    public void testAdd() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Irep irep = compiler.compile("hoge[% 5900 + 63 %]");

        String got = vm.run(irep, null);
        assertEquals("hoge5963", got);
    }

    @Test
    public void testSub() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Irep irep = compiler.compile("hoge[% 5900 - 63 %]");

        String got = vm.run(irep, null);
        assertEquals("hoge5837", got);
    }

    @Test
    public void testElem() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("a", 5963);
        Irep irep = compiler.compile("[% a %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("5963", got);
    }

    @Test
    public void testElem2() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("a", 5963);
        Irep irep = compiler.compile("[% a + 3 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("5966", got);
    }

    @Test
    public void testMultiply() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        Irep irep = compiler.compile("[% 4 * 3 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("12", got);
    }

    @Test
    public void testDivide() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        Irep irep = compiler.compile("[% 12 / 3 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("4", got);
    }

    @Test
    public void testParen() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        Irep irep = compiler.compile("[% (3+2)*4 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("20", got);
    }

    @Test
    public void testDouble() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        Irep irep = compiler.compile("[% 3.14 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("3.14", got);
    }

    @Test
    public void testDoubleMultiply() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        Irep irep = compiler.compile("[% 3.11 * 2 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("6.22", got);
    }

    @Test
    public void testEscape() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("x", "<>");
        vars.put("y", new JTTRawString("<>"));
        Irep irep = compiler.compile("<>[% x %][% y %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("<>&lt;&gt;<>", got);
    }

    @SuppressWarnings("serial")
    @Test
    public void testForeach() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("y", new ArrayList<Integer>() {
            {
                add(5);
                add(9);
                add(6);
                add(3);
            }
        });

        Irep irep = compiler.compile("[% FOR x IN y %][% x %][% END %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("5963", got);
    }

    @Test
    public void testEqauls() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% 5==3 %][% 3 == 3 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("falsetrue", got);
    }

    @Test
    public void testGt() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% 5>3 %] [% 3>3 %] [% 3>5 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("true false false", got);
    }

    @Test
    public void testGe() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% 5>=3 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("true", got);
    }

    @Test
    public void testGe2() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% 5>=3 %] [% 3>=3 %] [% 3>=5 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("true true false", got);
    }

    @Test
    public void testLT() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% 5<3 %] [% 3<3 %] [% 3<5 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("false false true", got);
    }

    @Test
    public void testLE() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% 5<=3 %] [% 3<=3 %] [% 3<=5 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("false true true", got);
    }

    @Test
    public void testArray() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% FOR x IN [1,2,3,] %][% x %][% END %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("123", got);
    }

    @Test
    public void testMod() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% 62 % 3 %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("2", got);
    }

    @Test
    public void testTrue() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% true %]");

        String got = vm.run(irep, vars);
        assertEquals("true", got);
    }

    @Test
    public void testFalse() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% false %]");

        String got = vm.run(irep, vars);
        assertEquals("false", got);
    }

    @Test
    public void testIfTrue() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% IF true %]hoge[% END %]fuga");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("hogefuga", got);
    }

    @Test
    public void testIfFalse() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% IF false %]hoge[% END %]fuga");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("fuga", got);
    }

    @Test
    public void testIfElsIfFalse() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% IF false %]hoge[% ELSIF false %]piyo[% END %]fuga");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("fuga", got);
    }

    @Test
    public void testIfElsIf() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% IF false %]hoge[% ELSIF true %]piyo[% END %]fuga");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("piyofuga", got);
    }

    @Test
    public void testIfElse() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% IF false %]hoge[% ELSE %]ooo[% END %]fuga");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("ooofuga", got);
    }

    @Test
    public void testIfElsIfElse() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% IF false %]hoge[% ELSIF false %]piyo[% ELSE %]ooo[% END %]fuga");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("ooofuga", got);
    }

    @Test
    public void testIfElsIfTrueElse() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% IF false %]hoge[% ELSIF true %]piyo[% ELSE %]ooo[% END %]fuga");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("piyofuga", got);
    }

    @Test
    public void testString() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% \"hoge\" %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("hoge", got);
    }

    @Test
    public void testStringConcat() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% \"ho\" _ \"ge\" %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("hoge", got);
    }

    @Test
    public void testSet() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% SET s=3 %][% s %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("3", got);
    }

    @Test
    public void testWhile() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% SET x=3 %][% WHILE x > 0 %][% x %][% SET x = x - 1 %][% END %]ok");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("321ok", got);
    }

    @Test
    public void testWhileFalse() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler.compile("[% WHILE false %]fail[% END %]ok");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("ok", got);
    }

    @Test
    public void testForLast() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("o", Lists.newArrayList("a", "b", "c", "d"));

        Irep irep = compiler
                .compile("[% FOR x IN o %][% IF x==\"c\" %][% LAST %][% END %][% x %][% END %]ok");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("abok", got);
    }

    @Test
    public void testWhileLast() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("o", Lists.newArrayList("a", "b", "c", "d"));

        Irep irep = compiler
                .compile("[% SET x=5 %][% WHILE x>0 %][% IF x==3 %][% LAST %][% END %][% x %][% SET x=x-1 %][% END %]ok");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("54ok", got);
    }

    @Test
    public void testAssign() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        String src = "[% x=5 %]ok";

        TTSyntax syntax = new TTSyntax("[%", "%]");
        List<Token> tokens = syntax.tokenize(src);
        Node ast = syntax.parse(src, tokens);
        Irep irep = compiler.compile(ast);
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("ok", got);
    }

    @Test
    public void testForNext() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("o", Lists.newArrayList("a", "b", "c", "d"));

        Irep irep = compiler
                .compile("[% x=5 %][% FOR x IN [1,2,3,4] %][% IF x==3 %][% NEXT %][% END %][% x %][% x=x-1 %][% END %]ok");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("124ok", got);
    }

    @Test
    public void testConditionalOperator() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% true ? 4 : 9 %][% false ? 5 : 3 %]ok");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("43ok", got);
    }

    @Test
    public void testMapAccess() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> map = new HashMap<>();
        map.put("hoge", "fuga");

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("o", map);

        Irep irep = compiler.compile("[% o.hoge %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("fuga", got);
    }

    @Test
    public void testMapLiteral() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> map = new HashMap<>();
        map.put("hoge", "fuga");

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("o", map);

        Irep irep = compiler.compile("[% {hoge=>\"fuga\", gogo=>4649}.hoge %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("fuga", got);
    }

    @Test
    public void testSwitch() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Irep irep = compiler
                .compile("[% SWITCH n %][% CASE 1 %]one[% CASE 2 %]two[% CASE %]more[% END %]");
        // System.out.println(new Disassembler().disasm(irep));

        {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("n", 1);
            String got = vm.run(irep, vars);
            assertEquals("one", got);
        }

        {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("n", 2);
            String got = vm.run(irep, vars);
            assertEquals("two", got);
        }

        {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("n", 3);
            String got = vm.run(irep, vars);
            assertEquals("more", got);
        }

        {
            Map<String, Object> vars = new HashMap<String, Object>();
            String got = vm.run(irep, vars);
            assertEquals("more", got);
        }
    }

    @Test
    public void testLowerCase() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> map = new HashMap<>();
        map.put("hoge", "fuga");

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("o", map);

        Irep irep = compiler.compile("[% lc(\"HoGe\") %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("hoge", got);
    }

    @Test
    public void testBuiltinFunctionUri() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> map = new HashMap<>();

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("o", map);

        Irep irep = compiler.compile("[% uri(\"& +3\") %]");

        String got = vm.run(irep, vars);
        assertEquals("%26+%2B3", got);
    }

    @Test
    public void testUpperCase() throws JSlateException, ParserError,
            IOException, TemplateLoadingError {
        Map<String, Object> map = new HashMap<>();
        map.put("hoge", "fuga");

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("o", map);

        Irep irep = compiler.compile("[% uc(\"HoGe\") %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("HOGE", got);
    }

    @Test
    public void testSprintf() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> map = new HashMap<>();
        map.put("hoge", "fuga");

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("o", map);

        Irep irep = compiler
                .compile("[% sprintf(\"hehe %03d, %s\", 4, \"ahaha\") %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("hehe 004, ahaha", got);
    }

    @Test
    public void testRange() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% FOR x IN 1..5 %][% x %],[% END %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("1,2,3,4,5,", got);
    }

    @Test
    public void testNot() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% !true %],[% !false %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("false,true", got);
    }

    @Test
    public void testPipe() throws JSlateException, ParserError, IOException,
            TemplateLoadingError {
        Map<String, Object> vars = new HashMap<String, Object>();

        Irep irep = compiler
                .compile("[% \"hoge+&\" | uri %]");
        // System.out.println(new Disassembler().disasm(irep));

        String got = vm.run(irep, vars);
        assertEquals("hoge%2B%26", got);
    }

}
