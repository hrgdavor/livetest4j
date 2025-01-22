package hr.hrg.livetest4j;

public class LiveTestSimple {

	public static void main(String[] args) throws Exception{
		new LiveTest4j(()->{
			System.out.println("Hello live 1");
		})
		.run();
	}
}
