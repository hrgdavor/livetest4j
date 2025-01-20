package hr.hrg.livetest4j;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LiveTest4j {
    public static String[] PATHS = { "target/classes", "target/test-classes", "bin"};
    public static int WATCH_DELAY = 50;

    /** for very simple project you can go as low as 50, 
	 *	for very large projects with hundred MB of deps you may nedd to use 600,700 or more
     * */
    public static int RUN_DELAY = 200;

    long watchDelay = WATCH_DELAY;
    long runDelay = RUN_DELAY;
    Consumer<String> consoleWriter = System.out::println;

    String[] pathsToTest = PATHS;
    RunWithErr[] init = {};
    RunWithErr[] tearDown = {};
    List<File> files = new ArrayList<>();
    RunWithErr test;

    public LiveTest4j(RunWithErr test) {
    	this.test(test);
    }
    
    public LiveTest4j test(RunWithErr test) {
    	this.test = test;
    	return this;
    }
    
    public LiveTest4j delay(int watchDelay, int runDelay) {
    	this.watchDelay = watchDelay;
    	this.runDelay = runDelay;
    	return this;
    }

    public LiveTest4j init(RunWithErr ...init) {
    	this.init = init;
    	return this;
    }
    
    public LiveTest4j consoleWriter(Consumer<String> consoleWriter) {
    	this.consoleWriter = consoleWriter;
    	return this;
    }
    
    public LiveTest4j tearDown(RunWithErr ...tearDown) {
    	this.tearDown = tearDown;
    	return this;
    }
    
    public LiveTest4j watch(Object obj) {
//    	https://stackoverflow.com/questions/3776204/how-to-find-out-if-debug-mode-is-enabled
    	boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean()
    			.getInputArguments().toString().contains("-agentlib:jdwp");
    	if(!isDebug) { 
    		consoleWriter.accept("WARNING: you must run LiveTest4j in debug mode. We could not detect debug mode. If You are running in debug mode, ignOre this mesSage");
    	}
    	Class<?> clazz = obj instanceof Class ? (Class)obj : obj.getClass();
        files.add(resolveClassFile(clazz, pathsToTest));
        WatchDepends watchDepends = clazz.getAnnotation(WatchDepends.class);
        if(watchDepends != null) {
        	if(watchDepends.value() != null) {
        		for(var tmp:watchDepends.value()) {
        			files.add(resolveClassFile(tmp, pathsToTest));
        		}
        	}
        	if(watchDepends.resources() != null) {
        		for(var tmp:watchDepends.resources()) {
        			files.add(new File(tmp));
        		}
        	}
        }
        return this;
    }
    
    public static File resolveClassFile(Class<?> clazz, String[] pathsToTest) {
        String className = clazz.getName();
        String classPath = className.replaceAll("\\.", "/") + ".class";
        File file = null;

        for (var prefix : pathsToTest) {
            file = new File(prefix +"/"+ classPath);
            if (file.exists()) {
            	return file;
            }
        }

        throw new NullPointerException("Can not find file for " + className);
    }
    
    public void run() throws Exception{
        for(File file:files) {
        	consoleWriter.accept("WATCH: "+file.getAbsolutePath());        	
        }

        long lastMod = 0;
        int seq = 1;
        while(true) {
            long mod = lastMod;
            for(File file:files) {
            	if(!file.exists()) continue;
                mod = Math.max(mod, file.lastModified());
            }
            if(mod != lastMod) {
            	consoleWriter.accept("");
            	consoleWriter.accept("modified");
            	Thread.sleep(runDelay);
                consoleWriter.accept("RUN tests");
                try {
                    long ms = System.currentTimeMillis();
                    if(init.length > 0) {
                    	for(int i=0; i<init.length; i++) {
                    		init[i].run();                    		
                    	}
                        consoleWriter.accept(" RUN tests init "+(System.currentTimeMillis()-ms));
                        ms = System.currentTimeMillis();
                    }
                    try {
                    	consoleWriter.accept("RUN tests exec");                    
                    	consoleWriter.accept("-----------------------------------------------------------");
                    	consoleWriter.accept("");
                        test.run();                        
                        consoleWriter.accept("");
                        consoleWriter.accept("-----------------------------------------------------------");
                        consoleWriter.accept("RUN tests exec done "+(System.currentTimeMillis()-ms));
                        ms = System.currentTimeMillis();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        consoleWriter.accept("RUN tests exec err "+(System.currentTimeMillis()-ms));
                    }finally {
                        if(tearDown != null && tearDown.length > 0) {                            
                            ms = System.currentTimeMillis();
                        	for(int i=0; i<init.length; i++) {
                        		tearDown[i].run();                    		
                        	}
                            consoleWriter.accept("RUN tests teardown "+(System.currentTimeMillis()-ms)+" <-------------------------------");                    
                        }
                    }
                    lastMod = mod;
                    consoleWriter.accept("RUN tests done   "+(seq++)+"   (if you are not seing chnages applied, make sure you are running in debug mode and that RUN_DELAY is large enough)");
                } catch (Throwable e) {
                    e.printStackTrace();
                    break;
                }
            }
            Thread.sleep(watchDelay);
        }
    }

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface WatchDepends {
		Class<?>[] value() default {};
	    String[] resources() default {};
	}

	@FunctionalInterface
    public static interface RunWithErr extends Serializable{
        public void run() throws Throwable;
    }
}
