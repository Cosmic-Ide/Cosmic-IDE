package com.pranav.common.util;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BinaryExecutorTest {
	@Test
	public void echo_isWorking() {
	    BinaryExecutor exec = new BinaryExecutor();
		exec.execute("echo \"Hello, World!\"");
		Assertions.assertEquals(exec.getLogs(), "Hello, World!");
		exec.clear();
		ArrayList<String> args = new ArrayList<>();
		args.add("echo");
		args.add("\"Hello, World!\"");
		exec.execute(args);
		Assertions.assertEquals(exec.getLogs(), "Hello, World!");
	}
}
