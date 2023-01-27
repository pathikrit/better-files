package better.files

import scala.quoted.{Quotes, Expr}
import scala.quoted.*

private[files] trait ResourceScalaCompat {

  /** Look up class resource files.
    *
    * This Resource looks up resources relative to the JVM class file for `T`, using
    * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]. For example, if
    * `com.example.ExampleClass` is given for `T`, then resource files will be searched for in the `com/example` folder containing
    * `ExampleClass.class`.
    *
    * If you want to look up resource files relative to the call site instead (that is, you want a class to look up one of its own
    * resources), use the `my` method instead.
    *
    * @example
    *   {{{Resource.at[YourClass].url("config.properties")}}}
    * @tparam T
    *   The class, trait, or object to look up from. Objects must be written with a `.type` suffix, such as `Resource.at[SomeObject.type]`.
    * @return
    *   A Resource for `T`.
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    */
  inline def at[T]: Resource = ${ Macros.atStaticImpl[T] }

  /** Look up class resource files.
    *
    * This Resource looks up resources from the given Class, using
    * [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]. For example, if
    * `classOf[com.example.ExampleClass]` is given for `clazz`, then resource files will be searched for in the `com/example` folder
    * containing `ExampleClass.class`.
    *
    * If you want to look up resource files relative to the call site instead (that is, you want your class to look up one of its own
    * resources), use the `my` method instead.
    *
    * @example
    *   {{{Resource.at(Class.forName("your.AppClass")).url("config.properties")}}}
    *
    * In this example, a file named `config.properties` is expected to appear alongside the file `AppClass.class` in the package `your`.
    * @param clazz
    *   The class to look up from.
    * @return
    *   A Resource for `clazz`.
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    */
  def at(clazz: Class[_]): Resource = new Resource {
    override def url(name: String) = Option(clazz.getResource(name))
  }

  /** Look up own resource files.
    *
    * This Resource looks up resources from the [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html Class]] surrounding the
    * call, using [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]. For
    * example, if `my` is called from `com.example.ExampleClass`, then resource files will be searched for in the `com/example` folder
    * containing `ExampleClass.class`.
    *
    * @example
    *   {{{Resource.my.url("config.properties")}}}
    * @return
    *   A Resource for the call site.
    * @see
    *   [[https://docs.oracle.com/javase/10/docs/api/java/lang/Class.html#getResource(java.lang.String) Class#getResource]]
    */
  inline def my: Resource = ${ Macros.myImpl }
}

private[files] object Macros {

  def atStaticImpl[T: Type](using qc: Quotes): Expr[Resource] = {
    import qc.reflect.*
    val tpe           = TypeRepr.of[T]
    val typeSymbolStr = tpe.typeSymbol.toString
    if (typeSymbolStr.startsWith("class ") || typeSymbolStr.startsWith("module class ")) {
      val baseClass = tpe.baseClasses.head
      return '{
        new Resource {
          override def url(name: String) = Option(
            Class
              .forName(${
                Expr(baseClass.fullName)
              })
              .getResource(name)
          )
        }
      }
    } else {
      report.errorAndAbort(s"${tpe.show} is not a concrete type")
    }
  }

  def myImpl(using qc: Quotes): Expr[Resource] = {
    import qc.reflect.*
    var callee = Symbol.spliceOwner
    while (callee != null && callee != Symbol.noSymbol) {
      callee = callee.owner
      if (callee.isClassDef) {
        return '{
          new Resource {
            override def url(name: String) = Option(
              Class
                .forName(${
                  Expr(callee.fullName)
                })
                .getResource(name)
            )
          }
        }
      }
    }
    report.errorAndAbort("this location doesn't correspond to a Java class file")
  }
}
