package me.geso.jtt.tools;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import me.geso.jtt.JTT;
import me.geso.jtt.JTTBuilder;

public class BenchMarkRunner {
	public static void main(String[] args) {
		System.out.println("start");
		JTT jtt = new JTTBuilder().addIncludePath(
				new File("src/test/resources").toPath()).build();
		for (int i = 0; i < 10000; ++i) {
			Map<String, Object> vars = new HashMap<>();
			vars.put("n", 1000000);
			jtt.renderFile(new File("loop.tt"), vars);
		}
		System.out.println("done");
	}
}
