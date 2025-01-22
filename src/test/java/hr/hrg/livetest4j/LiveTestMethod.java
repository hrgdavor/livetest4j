package hr.hrg.livetest4j;

public class LiveTestMethod {

	public static void main(String[] args) throws Exception{
		LiveTest4j.RUN_DELAY = 50;
		new LiveTest4j(LiveTestMethod::method).run();
	}
	
	public static void method() {
		System.out.println("Hello method 1");
	}
}
