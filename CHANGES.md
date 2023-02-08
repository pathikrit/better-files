better-files follows the following `MAJOR.MINOR.PATCH` release conventions:
- **Changes in `PATCH` version**: 
    - Minor functionality changes (usually bug fixes)
    - No breaking public API changes
    - New APIs might be added
- **Change in `MINOR` version**:
    - In addition to `PATCH` changes
    - Minor API shape changes e.g. renaming, deprecations 
    - Trivial to modify code to address compilation issues
- **Change in `MAJOR` version**:
    - In addition to `MINOR` changes
    - Significant structural and API changes
    
-----------

## v4.0.0 [WIP]
* [x] [PR #584](https://github.com/pathikrit/better-files/pull/621): Remove implicit options from all APIs
* [ ] [Issue #129](https://github.com/pathikrit/better-files/issues/129): JSR-203 and JimFS compatibility
* [ ] [Issue #88](https://github.com/pathikrit/better-files/issues/88): Strongly typed relative and absolute path APIs
* [ ] [Issue #204](https://github.com/pathikrit/better-files/issues/204): Universal converter APIs
* [ ] Remove akka utils

##  [v3.9.2](https://github.com/pathikrit/better-files/releases/tag/v3.9.2)
* [PR #573](https://github.com/pathikrit/better-files/pull/573): Scala 3 Support!
* [PR #426](https://github.com/pathikrit/better-files/pull/426): Add `UnicodeCharset.isValid()`
* [PR #428](https://github.com/pathikrit/better-files/pull/428): Add `File.resourcePathAsString` 
* [PR #436](https://github.com/pathikrit/better-files/pull/436): Exclude destination zip file from final output
* [PR #429](https://github.com/pathikrit/better-files/pull/429): Add `URI` and `URL` helpers
* [Issue #478](https://github.com/pathikrit/better-files/issues/478): Handle broken symlinks in `size()`
* [Issue #412](https://github.com/pathikrit/better-files/issues/412): Better error message when URI is not a valid file 

## [v3.9.1](https://github.com/pathikrit/better-files/releases/tag/v3.9.1)
* [Issue #417](https://github.com/pathikrit/better-files/issues/417): Fix `NoSuchMethodError` when reading `contentAsString` on JDK8 

## [v3.9.0](https://github.com/pathikrit/better-files/releases/tag/v3.9.0)
* [Issue #326](https://github.com/pathikrit/better-files/issues/326): Do not mark end of input when charset is detected from BOM
* [Issue #332](https://github.com/pathikrit/better-files/issues/332): Fix `setGroup` - it was using semantics of `setOwner`
* [Checksum utils for streams](https://github.com/pathikrit/better-files/issues/330)
* [Issue #316](https://github.com/pathikrit/better-files/issues/316): Rename `isWriteable` to `isWritable`
* [Issue #380](https://github.com/pathikrit/better-files/issues/380): Zip API exception in JDK 11
* [Issue #391](https://github.com/pathikrit/better-files/issues/391): Handle NPE in `FileMonitor` for large folders
* [Issue #362](https://github.com/pathikrit/better-files/issues/362): Add API to skip missing files when calculating size on a directory
* [Issue #320](https://github.com/pathikrit/better-files/issues/320): Change extension works when file is not present

## [v3.8.0](https://github.com/pathikrit/better-files/releases/tag/v3.8.0)
* [PR #312](https://github.com/pathikrit/better-files/issues/312): Scala 2.13.0-RC1 release
* [Issue #309](https://github.com/pathikrit/better-files/issues/312): Recursive `deleteOnExit` support
* [Rename](https://github.com/pathikrit/better-files/commit/ae45c6b419a53a7095e3dadccda010eb4d624fc6) certain implicit utils

## [v3.7.1](https://github.com/pathikrit/better-files/releases/tag/v3.7.1)
* [Issue #283](https://github.com/pathikrit/better-files/issues/283): Fix resource not closing bug on File#list
* [Issue #279](https://github.com/pathikrit/better-files/issues/279): Better manage open file handles in recursive deletion of large directories
* [Issue #285](https://github.com/pathikrit/better-files/issues/285): Add canonical file/path APIs
* [PR #290](https://github.com/pathikrit/better-files/pull/290) Add maxDepth to File#glob and File#globRegex

## [v3.7.0](https://github.com/pathikrit/better-files/releases/tag/v3.7.0)
* [Issue #248](https://github.com/pathikrit/better-files/issues/248): Release for Scala 2.13.0-M5
* [Issue #270](https://github.com/pathikrit/better-files/issues/270): `FileTreeIterator` can be traversed multiple times safely
* [Issue #262](https://github.com/pathikrit/better-files/issues/262): Handle backslashes in zip entry name
* [Issue #278](https://github.com/pathikrit/better-files/issues/278): Dispose multiple resources
* [Util](https://github.com/pathikrit/better-files/commit/07f0f69b7a544e74720ac60f0f5921d8a0becc8e) to fetch root Resource URL
* [`using`](https://github.com/pathikrit/better-files/commit/2a7c438ef672d2b414027e96c7fcecc11a9b791b) util for disposable resources
* [file.lineCount](https://github.com/pathikrit/better-files/commit/af315c9b1311c9baeab9b0a70a388e772b6a5eaf) util
* [inputstream.byteArray](https://github.com/pathikrit/better-files/commit/1657d8b30c836059813637a5a0d412d7a924467f) util

## [v3.6.0](https://github.com/pathikrit/better-files/releases/tag/v3.6.0)
* [Issue #123](https://github.com/pathikrit/better-files/issues/233): Rename ManagedResource to Dispose
* [Issue #241](https://github.com/pathikrit/better-files/issues/241): Remove resource leak from directory empty check
* [Issue #242](https://github.com/pathikrit/better-files/issues/242): Support for JDK 9 and JDK 10
* [Remove Files alias](https://github.com/pathikrit/better-files/commit/bfccb5041239bc5413afade4218ec1fb90d3e3d5)
* [List with filter API](https://github.com/pathikrit/better-files/commit/41e521b9a95a7f3ae5affb1a8eb798a0b2358445)
* More [createIfNotExists() APIs](https://github.com/pathikrit/better-files/commit/9c83d8b6c6eeb361eed5ffcf3e0810b207af7939)
* [Issue #247](https://github.com/pathikrit/better-files/issues/247): Strict equality for contains/isParentOf/isChildOf
* [Issue #249](https://github.com/pathikrit/better-files/issues/249): Make File serializable
* More [ZIP I/O helpers](https://github.com/pathikrit/better-files/commit/59c17c60eb22daad4a8690c052169c379fe3d5e3)
* More [String to I/O helpers](https://github.com/pathikrit/better-files/commit/5afb5f1ac58b248582e5cffcd8f32ebb2d91cd83)

## [v3.5.0](https://github.com/pathikrit/better-files/releases/tag/v3.5.0)
* [PR #230](https://github.com/pathikrit/better-files/pull/230): New Resource APIs with [module safety](https://github.com/pathikrit/better-files/pull/227)
* [Issue #224](https://github.com/pathikrit/better-files/issues/224): FileMonitor should not block threads

## [v3.4.0](https://github.com/pathikrit/better-files/releases/tag/v3.4.0)
* [PR #202](https://github.com/pathikrit/better-files/pull/202): for-comprehension friendly ARM
* [PR #203](https://github.com/pathikrit/better-files/pull/203): Type-class for Scanner construction
* Remove [redundant `count` param](https://github.com/pathikrit/better-files/commit/8cc66d0e8ac6517597eeb1db1814903f2256b805) from `File.Monitor#onUnknownEvent`

## [v3.3.1](https://github.com/pathikrit/better-files/releases/tag/v3.3.1)
* [Issue #146](https://github.com/pathikrit/better-files/issues/146): Release for Scala 2.11

## [v3.3.0](https://github.com/pathikrit/better-files/releases/tag/v3.3.0)
* [Issue #193](https://github.com/pathikrit/better-files/issues/193): Handle fast changing directory watching on Windows
* [Issue #195](https://github.com/pathikrit/better-files/issues/195): Do not swallow `FileAlreadyExistsException` when creating directory or file
* [Add](https://github.com/pathikrit/better-files/commit/00f27867ebd0cddec1ace7835dcc2375869fb3ae) method to check verified file existence (or non-existence)
* [Issue #198](https://github.com/pathikrit/better-files/issues/198): `InputStreamOps#asString` doesn't close the stream on exception
* [PR #199](https://github.com/pathikrit/better-files/pull/199): Utils for Object I/O
* [PR #200](https://github.com/pathikrit/better-files/pull/200): GZIP APIs

## [v3.2.0](https://github.com/pathikrit/better-files/releases/tag/v3.2.0)
* [Rename](https://github.com/pathikrit/better-files/commit/ec34a6f843fec941b51bdddafc2e07e5bc0e1cbb) PosixFilePermissions.OTHERS* APIs
* [Issue #186](https://github.com/pathikrit/better-files/issues/186): Splitter based Scanner
* [Issue #173](https://github.com/pathikrit/better-files/issues/173): Better ARM handling of fatal errors
* [Issue #182](https://github.com/pathikrit/better-files/issues/182): Move and Copy *into* directory utils
* [Issue #189](https://github.com/pathikrit/better-files/issues/189): Util to read String from an InputStream
* [Issue #187](https://github.com/pathikrit/better-files/issues/187): Readers for `java.time.*` and `java.sql.*`
* [Restore File.usingTemp](https://github.com/pathikrit/better-files/commit/35184a642245db3d1e41fc02c7bfbec0b19a43bb) first introduced in [7c60ca](https://github.com/pathikrit/better-files/commit/d3522e8da63b55c7d3fa14cc9b0b76acd57c60ca)
* [Fix](https://github.com/pathikrit/better-files/pull/184) bug in appendBytes

## [v3.1.0](https://github.com/pathikrit/better-files/releases/tag/v3.1.0)
* [Issue #140](https://github.com/pathikrit/better-files/issues/140): Batch up events for file monitoring
* [Issue #136](https://github.com/pathikrit/better-files/issues/136): Use execution contexts for file monitoring
* [Issue #152](https://github.com/pathikrit/better-files/issues/152): Streamed unzipping
* [Issue #150](https://github.com/pathikrit/better-files/issues/150): `ManagedResource[File]` for temp files
* [Issue #126](https://github.com/pathikrit/better-files/pull/159): New Typeclassed approach to ARM
* [Issue #160](https://github.com/pathikrit/better-files/issues/160): Ability to convert Reader/Writer to Input/Output streams
* [Issue #77](https://github.com/pathikrit/better-files/issues/77): Better UNIX-y behaviour for `cp` and `mv` DSL utils
* [Issue #169](https://github.com/pathikrit/better-files/issues/169): Support for symbols in file DSL
* [Issue #171](https://github.com/pathikrit/better-files/issues/171): Handle `createDirectories()` on symlinks to existing directories

## [v3.0.0](https://github.com/pathikrit/better-files/releases/tag/v3.0.0)
* [Issue #9](https://github.com/pathikrit/better-files/issues/9): File resource utils
* [Issue #114](https://github.com/pathikrit/better-files/issues/114): Glob with automatic path
* [Issue #107](https://github.com/pathikrit/better-files/issues/107): Handle Byte-order markers
* [PR #113](https://github.com/pathikrit/better-files/pull/113): File anchor util
* [Issue #105](https://github.com/pathikrit/better-files/issues/105): Remove dependency on scala.io
* [File.usingTemp](https://github.com/pathikrit/better-files/commit/d3522e8da63b55c7d3fa14cc9b0b76acd57c60ca)
* [Optional symbolic operations](https://github.com/pathikrit/better-files/issues/102)
* [PR #100](https://github.com/pathikrit/better-files/pull/100): Fix issue in unzip of parents
* [PR #101](https://github.com/pathikrit/better-files/pull/101): Removed File.Type
* [Issue #96](https://github.com/pathikrit/better-files/issues/96): Teeing outputstreams
* [File.testPermission](https://github.com/pathikrit/better-files/commit/7b175c582643790e4d2fd21552e47cc9c615dfbb)
* [File.nonEmpty](https://github.com/pathikrit/better-files/commit/18c9cd51b7b2e503ff4944050ac5119470869e6e)
* [Update metadata API](https://github.com/pathikrit/better-files/commit/c3d65951d80f09b813e158a9e3a1785c622353b3)
* [Issue #80](https://github.com/pathikrit/better-files/issues/80): Unzip filters
* [PR #107](https://github.com/pathikrit/better-files/pull/127): Java serialization utils

## [v2.17.1](https://github.com/pathikrit/better-files/releases/tag/v2.17.1)
* [PR #99](https://github.com/pathikrit/better-files/pull/99): Release for Scala 2.12

## [v2.17.0](https://github.com/pathikrit/better-files/releases/tag/v2.17.0)
* [PR #78](https://github.com/pathikrit/better-files/pull/78): Change `write(Array[Byte])` to `writeByteArray()`. Same for `append`
* [Issue #76](https://github.com/pathikrit/better-files/issues/76): Move `better.files.Read` typeclass to `better.files.Scanner.Read`
