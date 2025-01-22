package hr.hrg.livetest4j;

import java.nio.file.Files;
import java.nio.file.Path;

import hr.hrg.livetest4j.LiveTest4j.WatchDepends;

@WatchDepends(value= {LiveTestMethod.class}, resources = {LiveTestDependencies.TEXT_FILE})
public class LiveTestDependencies {
	static final String TEXT_FILE = "src/test/java/hr/hrg/livetest4j/dep.file.txt";
	public static void main(String[] args) throws Exception{
		new LiveTest4j(()->{
			LiveTestMethod.method();
			System.out.println(Files.readString(Path.of(TEXT_FILE)));
		})
		.run();
	}
}
