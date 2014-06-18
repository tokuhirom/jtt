package me.geso.jtt.vm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Irep {
	private final Code[] iseq;
	private final Object[] pool;
	private final Integer[] lineNumbers;
	private final boolean fromFile;
	// fileName is "-" or null if fromFile is false.
	private final String fileName;
	// source is null if fromFile is true.
	private final String source;

	public Irep(List<Code> iseq, List<Object> pool, String fileName,
			List<Integer> lineNumbers, boolean fromFile, String source) {
		this.iseq = iseq.toArray(new Code[iseq.size()]);
		this.pool = pool.toArray(new Object[iseq.size()]);
		this.fileName = fileName;
		this.lineNumbers = lineNumbers.toArray(new Integer[lineNumbers.size()]);
		this.fromFile = fromFile;
		this.source = source;
	}

	public Code[] getIseq() {
		return iseq;
	}

	public Object[] getPool() {
		return pool;
	}

	public String toString() {
		return new Disassembler().disasm(this);
	}

	public int getLineNumber(int pos) {
		return lineNumbers[pos];
	}

	public String getFileName() {
		return fileName;
	}
	
	public List<String> getSourceLines() throws IOException {
		if (fromFile) {
			return Files.readAllLines(new File(fileName).toPath());
		} else {
			List<String> list = new ArrayList<String>();
			String[] lines = source.split("\r?\n");
			for (String line: lines) {
				list.add(line);
			}
			return list;
		}
	}
}