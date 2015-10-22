Benchmarks
====
* [Scanner benchmarks](src/main/scala/better/files/Scanners.scala):
```
> sbt "benchmarks/test:run-main better.files.ScannerBenchmark"
[info] Running better.files.ScannerBenchmark 
JavaScanner           : 2512 ms
StreamingScanner      :  310 ms
ArrayBufferScanner    :  240 ms
IterableScanner       :  352 ms
IteratorScanner       :  314 ms
StringBuilderScanner  : 1190 ms
```
