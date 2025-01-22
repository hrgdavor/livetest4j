package hr.hrg.livetest4j;

import java.io.File;

public class BasicExample {
	
	public static void main(String[] args) {
		new Thread(()->{
			File[] files = {new File("target/test-classes/hr/hrg/livetest4j/BasicExample.class")};
			long lastMod = 0;
	        while(!Thread.interrupted()) {
	        	try {
		            long mod = lastMod;
		            for(File file:files) {
		            	if(!file.exists()) {
		            		System.err.println("file not found "+file.getAbsolutePath());
		            		return;
		            	}
		                mod = Math.max(mod, file.lastModified());
		            }
		            if(mod != lastMod) {
		            	// wait a bit more for hot-code-replace to kick in
						Thread.sleep(100);
		            	test();// run this on change
		            }
		            lastMod = mod;
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	        }
		}).start();
	}

	static void test(){
		System.out.println("Hello basic 1");
	}
}
