Benchmarks
====
* [Scanner benchmarks](src/main/scala/better/files/Scanners.scala):
```
> sbt "benchmarks/test:run-main better.files.ScannerBenchmark"
[info] Running better.files.ScannerBenchmark 
JavaScanner	            : 2602 ms
StreamingScanner	    :  259 ms
ArrayBufferScanner	    :  284 ms
CharBufferScanner	    : 1195 ms
IteratorScanner	        :  345 ms
IterableScanner	        :  385 ms
StringBuilderScanner	: 1440 ms
BetterFilesScanner	    :  309 ms
```

----

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

YourKit supports better-files with its full-featured Java Profiler. 
YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/) and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), 
innovative and intelligent tools for profiling Java and .NET applications.
