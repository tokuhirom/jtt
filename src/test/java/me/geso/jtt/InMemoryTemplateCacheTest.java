package me.geso.jtt;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import me.geso.jtt.exception.JTTError;

import org.junit.Test;

/**
 * This class tests in memotry template cache class.
 * 
 * @author tokuhirom
 *
 */
public class InMemoryTemplateCacheTest {

	@Test
	public void testNoCache() throws IOException, JTTError {
		Path tmpdir = Files.createTempDirectory("jtt");

		// write index.tt in tmpdir.
		Files.write(tmpdir.resolve("index.tt"), "hoge".getBytes());

		TemplateCache templateCache = new InMemoryTemplateCache(
				InMemoryTemplateCache.CacheMode.NO_CACHE);
		JTT jtt = new JTTBuilder().setTemplateCache(templateCache)
				.addIncludePath(tmpdir).build();

		{
			String got = jtt.render("index.tt", new HashMap<>());
			assertEquals(got, "hoge");
			// no cache.
			assertEquals(((InMemoryTemplateCache) templateCache).size(), 0);
		}

		// rewrite
		Files.write(tmpdir.resolve("index.tt"), "fuga".getBytes());


		{
			String got = jtt.render("index.tt", new HashMap<>());
			assertEquals(got, "fuga"); // modified.
			assertEquals(((InMemoryTemplateCache) templateCache).size(), 0); // no
																				// cache.
		}
	}

	@Test
	public void testCacheWithUpdateCheck() throws IOException, JTTError {
		Path tmpdir = Files.createTempDirectory("jtt");

		// write index.tt in tmpdir.
		Files.write(tmpdir.resolve("index.tt"), "hoge".getBytes());

		TemplateCache templateCache = new InMemoryTemplateCache(
				InMemoryTemplateCache.CacheMode.CACHE_WITH_UPDATE_CHECK);
		JTT jtt = new JTTBuilder().setTemplateCache(templateCache)
				.addIncludePath(tmpdir).build();

		{
			String got = jtt.render("index.tt", new HashMap<>());
			assertEquals(got, "hoge");
			assertEquals(((InMemoryTemplateCache) templateCache).size(), 1);
		}

		// rewrite
		Files.write(tmpdir.resolve("index.tt"), "fuga".getBytes());

		{
			String got = jtt.render("index.tt", new HashMap<>());
			assertEquals("fuga", got); // modified.
			assertEquals(((InMemoryTemplateCache) templateCache).size(), 1);
		}
	}

	@Test
	public void testCacheButDoNotCheckUpdates() throws IOException, JTTError {
		Path tmpdir = Files.createTempDirectory("jtt");

		// write index.tt in tmpdir.
		Files.write(tmpdir.resolve("index.tt"), "hoge".getBytes());

		TemplateCache templateCache = new InMemoryTemplateCache(
				InMemoryTemplateCache.CacheMode.CACHE_BUT_DO_NOT_CHECK_UPDATES);
		JTT jtt = new JTTBuilder().setTemplateCache(templateCache)
				.addIncludePath(tmpdir).build();

		{
			String got = jtt.render("index.tt", new HashMap<>());
			assertEquals(got, "hoge");
			assertEquals(((InMemoryTemplateCache) templateCache).size(), 1);
		}

		// rewrite index.tt in tmpdir.
		Files.write(tmpdir.resolve("index.tt"), "fuga".getBytes());

		{
			String got = jtt.render("index.tt", new HashMap<>());
			assertEquals(got, "hoge"); // not modified.
			assertEquals(((InMemoryTemplateCache) templateCache).size(), 1);
		}
	}

}
