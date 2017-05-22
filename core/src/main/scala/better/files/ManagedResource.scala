package better.files

import java.util.concurrent.atomic.AtomicBoolean

class ManagedResource[A <: Disposable](resource: A) {
  private[this] val isDisposed = new AtomicBoolean(false)

  def foreach[U](f: A => U): Unit = {
    val _ = map(f)
  }

  def map[B](f: A => B): B = {
    try {
      f(resource)
    } finally {
      if (isDisposed.getAndSet(true)) resource.close()
    }
  }
}
