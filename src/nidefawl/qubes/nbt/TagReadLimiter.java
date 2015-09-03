package nidefawl.qubes.nbt;

import java.io.IOException;

public class TagReadLimiter {
	public final static TagReadLimiter UNLIMITED = new TagReadLimiter() {
		public void add(int bytes) {};
		public void push() throws IOException {};
		public void pop() {};
	};
	private int totalReadBytes;
	private int limit = 1024*1024*32;
	private int maxStackDepth = 13;
	private int stackDepth;
	public TagReadLimiter() {
	}

	public TagReadLimiter(int byteLimit, int stackLimit) {
		this.limit = byteLimit;	
		this.maxStackDepth = stackLimit;
	}
	

	public void add(int bytes) throws IOException {
		this.totalReadBytes += bytes;
		if(totalReadBytes >= limit) {
			throw new IOException("Limit reached ("+totalReadBytes+" >= "+limit+")");
		}
	}
	public void push() throws IOException {
		this.stackDepth++;
		if(stackDepth >= maxStackDepth) {
			throw new IOException("Stack overflow ("+stackDepth+" >= "+maxStackDepth+")");
		}
	}
	public void pop() {
		this.stackDepth--;
	}
}
