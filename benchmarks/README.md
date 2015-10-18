Benchamrks
====
* [Scanner benchmarks](benchmarks/src/main/scala/better/files/Scanners.scala):
```
╰─⠠⠵ sbt
[info] Loading global plugins from /Users/pbhowmick/.sbt/0.13/plugins
[info] Loading project definition from /Users/pbhowmick/Projects/better-files/project
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
[info] Set current project to root (in build file:/Users/pbhowmick/Projects/better-files/)
> project benchmarks
[info] Set current project to better-files-benchmarks (in build file:/Users/pbhowmick/Projects/better-files/)
> test:run-main better.files.ScannerBenchmark
[info] Running better.files.ScannerBenchmark 

File = file:///var/folders/wv/x5mzbnjn6sd665vr1tlm4lhnlgmplx/T/6193677791188361467
IterableScanner  : 716 ms
IteratorScanner  : 713 ms
JavaScanner      : 2987 ms
StreamingScanner : 314 ms
```
