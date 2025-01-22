
#  LiveTest4j

Write code and see changes live as soon as you save.

This is a small utility to allow you to write Java code and see results immediately after save.

It relies on built-in Java hot code reload that is available in debug. No additional libraries are needed,
but if you like the approach there are more advanced things you can do.

- run a specialised agent that allows for reloading more things (built-in code reload is limited to method bodies)
- setup live reload in Spring
- use a commercial solution like JRebel

For now, this not published as a maven library just copy the utility class [LiveTest4j.java](src/main/java/hr/hrg/livetest4j/LiveTest4j.java)  to your project (with test code, no need for it in production).

In principle this is pretty simple, but to make it more usable it has bit more code.

## using

To make it work, code MUST run in debug mode, as we are relying on Java built-in code replace. Basic Java 
hot code replace only works for method bodies, and method must not be blocked in some thread. 

Also we need to know what files to watch for changes (after file changes we need to wait a bit to give time for hot code replace to finish)

```Java
public class LiveTestSimple {
	public static void main(String[] args) throws Exception{
		new LiveTest4j(()->{
			System.out.println("Hello live 1");
		})
		.watch(LiveTestSimple.class)// at minimum we need to watch our file
		.run();
	}
}
```






## IDE support

For now only tested in Eclipse, but should work in others.

## execution delay

After discovering a class file has changed a delay is needed before running the code

- 50 ms can be ok for very simple project 
- 200 ms is still responsive and should be ok for medium projects
- 700 ms if oyu have 200MB of dependencies

The numbers above are just for orientation and to adjust your expectations. You
need to try some values your self to ge a low value that reliable executes after hot code replace
is done.

## debug mode detection

To minimize problems that will happen if you try to run LiveTest4j without debug code ha adopted a imperfect but still useful solution
from https://stackoverflow.com/questions/3776204/how-to-find-out-if-debug-mode-is-enabled

```java
boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean()
    .getInputArguments().toString().contains("-agentlib:jdwp");
```



