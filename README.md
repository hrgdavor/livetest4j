
#  LiveTest4j

Write code and see changes live as soon as you save.

This is a small utility to allow you to write Java code and see results immediately after save.

It relies on built-in Java hot code reload that is available in debug. No additional libraries are needed,
but if you like the approach there are more advanced things you can do.

- run a specialised agent that allows for reloading more things (built-in code reload is limited to method bodies)
- setup live reload in Spring
- use a commercial solution like JRebel

For now, this not published as a maven library just copy the utility class [LiveTest4j.java](src/main/java/hr/hrg/livetest4j/LiveTest4j.java)  to your project.

In principle this is pretty simple, but to make it more usable it has bit more code.

## Using

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

*NOTICE!!: we manually add LiveTestSimple class to watch list, as automatic detection via lambda might not work, it is a bit hacky* 

## IDE support

For now only tested in Eclipse, but should work in others.

## Execution delay

After discovering a class file has changed a delay is needed before running the code

- 50 ms can be ok for very simple project 
- 200 ms is still responsive and should be ok for medium projects
- 700 ms if oyu have 200MB of dependencies

The numbers above are just for orientation and to adjust your expectations. You
need to try some values your self to ge a low value that reliable executes after hot code replace
is done.

## Debug mode detection

To minimize problems that will happen if you try to run LiveTest4j without debug code ha adopted a imperfect but still useful solution
from https://stackoverflow.com/questions/3776204/how-to-find-out-if-debug-mode-is-enabled

```java
boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean()
    .getInputArguments().toString().contains("-agentlib:jdwp");
```

 Do not ignore this message if you see it.

```
WARNING: you MUST run LiveTest4j in DEBUG mode.
WARNING: We could not detect DEBUG mode.
WARNING: If You are running in DEBUG mode, ignore this message
```

## A step further, detect class from lambda

We can shave off one line of code from the first code example `watch(LiveTestSimple.class)` by using a serialization trick to extract class information from a lambda expression (based on this [stackoverflow](https://stackoverflow.com/questions/21860875/printing-debug-info-on-errors-with-java-8-lambda-expressions).

For trick to work it is imperative to use an interface that is Serializable (simple Runnable will not work). 

The simple example gets a bit simpler.

```Java
public class LiveTestSimple {
	public static void main(String[] args) throws Exception{
		new LiveTest4j(()->{
			System.out.println("Hello live 1");
		})
		.run();
	}
}
```

This trick relies on `method.setAccessible(true)` so this may not work in future JRE's without additional VM arguments. 

## Troubleshooting

It really can not be stressed enough, you MUST run in DEBUG mode for this to work. Watch the initial output to check if all the files that need to be tracked are recognized.

```
WATCH: ..../target/test-classes/hr/hrg/livetest4j/LiveTestSimple.class
```

