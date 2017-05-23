Benchmarks
====
* [Scanner benchmarks](src/main/scala/better/files/Scanners.scala):
```
> sbt "benchmarks/test:run-main better.files.ScannerBenchmark"
[info] Running better.files.ScannerBenchmark 
JavaScanner             : 2222 ms
StreamingScanner        :  249 ms
ArrayBufferScanner      :  261 ms
CharBufferScanner       : 1158 ms
IteratorScanner         :  291 ms
IterableScanner         :  363 ms
StringBuilderScanner    : 1161 ms
FastJavaIOScanner       :  170 ms
BetterFilesScanner      :  273 ms
```

----

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

YourKit supports better-files with its full-featured Java Profiler. 
YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/) and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), 
innovative and intelligent tools for profiling Java and .NET applications.
