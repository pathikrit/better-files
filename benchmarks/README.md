Benchmarks
====
* [Scanner benchmarks](src/main/scala/better/files/Scanners.scala):
```
> sbt "benchmarks/test:run-main better.files.ScannerBenchmark"
[info] Running better.files.ScannerBenchmark 
CharBufferScanner       :  474 ms
IterableScanner         :  637 ms
IteratorScanner         :  757 ms
JavaScanner             : 2950 ms
StreamingScanner        :  554 ms
```
