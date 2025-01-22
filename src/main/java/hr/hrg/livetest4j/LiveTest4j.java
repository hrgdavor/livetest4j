package hr.hrg.livetest4j;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    Set<File> files = new HashSet<>();
    RunWithErr test;

    public LiveTest4j(RunWithErr test) {
//    	https://stackoverflow.com/questions/3776204/how-to-find-out-if-debug-mode-is-enabled
    	boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean()
    			.getInputArguments().toString().contains("-agentlib:jdwp");    	if(!isDebug) {
    		consoleWriter.accept("WARNING: you MUST run LiveTest4j in DEBUG mode.");
    		consoleWriter.accept("WARNING: We could not detect DEBUG mode.");
    		consoleWriter.accept("WARNING: If You are running in DEBUG mode, ignore this message");
    	}
    	this.test = test;
    	checkLambda(test);
    }
    
    
    private void checkLambda(RunWithErr lambda) {
    	SerializedLambda lambdaInfo = extractLambdaInfo(lambda);
    	if(lambdaInfo != null) {
    		try {
    			String className = lambdaInfo.getImplClass().replaceAll("/", "\\.");
    			watch( Class.forName(className));
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
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

    /**
     * Add a class to watch (if object is provided then class of that object is used).
     * It will search {@link #pathsToTest} folders to find generated class file
     * @param obj Class or object
     * @return
     */
    public LiveTest4j watch(Object obj) {
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
        			watch(tmp);
        		}
        	}
        }
        return this;
    }

    /**
     * Add a regular file to watch list.
     */
	public void watch(String filePath) {
		files.add(new File(filePath));
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

	/**
	 * This only works for lambda that is based on a functional interface that is
	 * also Serializable.
	 * 
	 * @param test
	 * @return
	 */
	public static SerializedLambda extractLambdaInfo(Serializable test) {
		// https://stackoverflow.com/questions/21860875/printing-debug-info-on-errors-with-java-8-lambda-expressions
		Serializable s = (Serializable) test;
		try {
			Method method = s.getClass().getDeclaredMethod("writeReplace");
			method.setAccessible(true);
			return (SerializedLambda) method.invoke(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@FunctionalInterface
	public static interface RunWithErr extends Serializable {
		public void run() throws Throwable;
	}
}
