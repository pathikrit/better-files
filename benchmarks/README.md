Benchmarks
====
* [Scanner benchmarks](src/main/scala/better/files/Scanners.scala):
```
> sbt "benchmarks/test:run-main better.files.ScannerBenchmark"
[info] Running better.files.ScannerBenchmark 
JavaScanner             : 2364 ms
StreamingScanner        :  346 ms
ArrayBufferScanner      :  318 ms
IterableScanner         :  408 ms
IteratorScanner         :  386 ms
StringBuilderScanner    : 1237 ms
```
