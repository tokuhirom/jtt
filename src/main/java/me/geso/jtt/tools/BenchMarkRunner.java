package me.geso.jtt.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import me.geso.jtt.InMemoryTemplateCache;
import me.geso.jtt.JTT;
import me.geso.jtt.JTTBuilder;

class Item {
	public String name;
	public String url;
	public String symbol;
	public double price;
	public double change;
	public double ratio;
}

public class BenchMarkRunner {
	public static void main(String[] args) {
		long t1 = System.currentTimeMillis();

		System.out.println("start");
		InMemoryTemplateCache templateCache = new InMemoryTemplateCache(
				InMemoryTemplateCache.CacheMode.CACHE_BUT_DO_NOT_CHECK_UPDATES);
		JTT jtt = new JTTBuilder()
				.addIncludePath(new File("src/test/resources").toPath())
				.setTemplateCache(templateCache).build();
		List<Item> items = new ArrayList<>();
		IntStream.rangeClosed(0, 10000).forEach(i -> {
			Item item = new Item();
			item.name = "John";
			item.symbol = "John";
			item.url = "John";
			item.price = 3.14;
			item.change = 3.14;
			item.ratio = 3.14;
			items.add(item);
		});
		for (int i = 0; i < 1000; ++i) {
			Map<String, Object> vars = new HashMap<>();
			vars.put("items", items);
			jtt.renderFile(new File("loop.tt"), vars);
		}

		long t2 = System.currentTimeMillis();
		System.out.println("done: " + (t2-t1) + " [ms]");
	}
}
