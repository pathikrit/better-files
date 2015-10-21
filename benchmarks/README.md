Benchmarks
====
* [Scanner benchmarks](src/main/scala/better/files/Scanners.scala):
```
> sbt "benchmarks/test:run-main better.files.ScannerBenchmark"
[info] Running better.files.ScannerBenchmark 
StreamingScanner        :  326 ms
IteratorScanner         :  457 ms
IterableScanner         :  388 ms
ArrayBufferScanner      :  285 ms
StringBuilderScanner    : 1136 ms
JavaScanner             : 2303 msofile a
```
