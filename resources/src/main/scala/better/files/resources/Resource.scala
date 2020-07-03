package better.files.resources

import java.io.IOException

object Resource {
  @throws[Exception]
  def using[T, R <% AutoCloseable](resource: R)(f: R => T): T =
    try {
      f(resource)
    } catch {
      case ex: Exception => throw ex
    } finally {
      if (null != resource) resource.close()
    }

  @throws[Exception]
  def using[T, R <% AutoCloseable](resources: Seq[R])(f: Seq[R] => T): T =
    try {
      f(resources)
    } catch {
      case ex: Exception => throw ex
    } finally {
      if (null != resources) resources.foreach { r => if (null != r) r.close() }
    }

  implicit def toCloseable[T <: { def close(): Unit }](obj: T): AutoCloseable =
    new AutoCloseable {
      import scala.language.reflectiveCalls

      // This causes a reflective call
      def close(): Unit = obj.close()
    }

  def apply[T <: AutoCloseable](resource: T): Resource[T] = toResource(resource)

  implicit def toResource[T <: AutoCloseable](resource: T): Resource[T] = SingleUseResource(resource)

  val empty: Resource[Unit] = UnitResource

  //
  // Helpers for using multiple resource
  //

  def use[RES, A](a: Resource[A])(fun: A => RES): RES                         = a.use { aa => fun(aa) }
  def use[RES, A, B](a: Resource[A], b: Resource[B])(fun: (A, B) => RES): RES = a.use { aa => b.use { bb => fun(aa, bb) } }
  def use[RES, A, B, C](a: Resource[A], b: Resource[B], c: Resource[C])(fun: (A, B, C) => RES): RES =
    a.use { aa => b.use { bb => c.use { cc => fun(aa, bb, cc) } } }
  def use[RES, A, B, C, D](a: Resource[A], b: Resource[B], c: Resource[C], d: Resource[D])(fun: (A, B, C, D) => RES): RES =
    a.use { aa => b.use { bb => c.use { cc => d.use { dd => fun(aa, bb, cc, dd) } } } }
  def use[RES, A, B, C, D, E](a: Resource[A], b: Resource[B], c: Resource[C], d: Resource[D], e: Resource[E])(
      fun: (A, B, C, D, E) => RES
  ): RES = a.use { aa => b.use { bb => c.use { cc => d.use { dd => e.use { ee => fun(aa, bb, cc, dd, ee) } } } } }
  def use[RES, A, B, C, D, E, F](a: Resource[A], b: Resource[B], c: Resource[C], d: Resource[D], e: Resource[E], f: Resource[F])(
      fun: (A, B, C, D, E, F) => RES
  ): RES = a.use { aa => b.use { bb => c.use { cc => d.use { dd => e.use { ee => f.use { ff => fun(aa, bb, cc, dd, ee, ff) } } } } } }
  def use[RES, A, B, C, D, E, F, G](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G]
  )(fun: (A, B, C, D, E, F, G) => RES): RES =
    a.use { aa =>
      b.use { bb => c.use { cc => d.use { dd => e.use { ee => f.use { ff => g.use { gg => fun(aa, bb, cc, dd, ee, ff, gg) } } } } } }
    }
  def use[RES, A, B, C, D, E, F, G, H](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H]
  )(fun: (A, B, C, D, E, F, G, H) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc => d.use { dd => e.use { ee => f.use { ff => g.use { gg => h.use { hh => fun(aa, bb, cc, dd, ee, ff, gg, hh) } } } } } }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I]
  )(fun: (A, B, C, D, E, F, G, H, I) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee => f.use { ff => g.use { gg => h.use { hh => i.use { ii => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii) } } } } }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J]
  )(fun: (A, B, C, D, E, F, G, H, I, J) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff => g.use { gg => h.use { hh => i.use { ii => j.use { jj => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj) } } } } }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh => i.use { ii => j.use { jj => k.use { kk => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk) } } } }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii => j.use { jj => k.use { kk => l.use { ll => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll) } } } }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk => l.use { ll => m.use { mm => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm) } } }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M, N](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M],
      n: Resource[N]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M, N) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk =>
                          l.use { ll => m.use { mm => n.use { nn => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm, nn) } } }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M],
      n: Resource[N],
      o: Resource[O]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk =>
                          l.use { ll =>
                            m.use { mm => n.use { nn => o.use { oo => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm, nn, oo) } } }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M],
      n: Resource[N],
      o: Resource[O],
      p: Resource[P]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk =>
                          l.use { ll =>
                            m.use { mm =>
                              n.use { nn =>
                                o.use { oo => p.use { pp => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm, nn, oo, pp) } }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M],
      n: Resource[N],
      o: Resource[O],
      p: Resource[P],
      q: Resource[Q]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk =>
                          l.use { ll =>
                            m.use { mm =>
                              n.use { nn =>
                                o.use { oo =>
                                  p.use { pp => q.use { qq => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm, nn, oo, pp, qq) } }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M],
      n: Resource[N],
      o: Resource[O],
      p: Resource[P],
      q: Resource[Q],
      r: Resource[R]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk =>
                          l.use { ll =>
                            m.use { mm =>
                              n.use { nn =>
                                o.use { oo =>
                                  p.use { pp =>
                                    q.use { qq =>
                                      r.use { rr => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm, nn, oo, pp, qq, rr) }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M],
      n: Resource[N],
      o: Resource[O],
      p: Resource[P],
      q: Resource[Q],
      r: Resource[R],
      s: Resource[S]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk =>
                          l.use { ll =>
                            m.use { mm =>
                              n.use { nn =>
                                o.use { oo =>
                                  p.use { pp =>
                                    q.use { qq =>
                                      r.use { rr =>
                                        s.use { ss => fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm, nn, oo, pp, qq, rr, ss) }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M],
      n: Resource[N],
      o: Resource[O],
      p: Resource[P],
      q: Resource[Q],
      r: Resource[R],
      s: Resource[S],
      t: Resource[T]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk =>
                          l.use { ll =>
                            m.use { mm =>
                              n.use { nn =>
                                o.use { oo =>
                                  p.use { pp =>
                                    q.use { qq =>
                                      r.use { rr =>
                                        s.use { ss =>
                                          t.use { tt =>
                                            fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm, nn, oo, pp, qq, rr, ss, tt)
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M],
      n: Resource[N],
      o: Resource[O],
      p: Resource[P],
      q: Resource[Q],
      r: Resource[R],
      s: Resource[S],
      t: Resource[T],
      u: Resource[U]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk =>
                          l.use { ll =>
                            m.use { mm =>
                              n.use { nn =>
                                o.use { oo =>
                                  p.use { pp =>
                                    q.use { qq =>
                                      r.use { rr =>
                                        s.use { ss =>
                                          t.use { tt =>
                                            u.use { uu =>
                                              fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm, nn, oo, pp, qq, rr, ss, tt, uu)
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  def use[RES, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V](
      a: Resource[A],
      b: Resource[B],
      c: Resource[C],
      d: Resource[D],
      e: Resource[E],
      f: Resource[F],
      g: Resource[G],
      h: Resource[H],
      i: Resource[I],
      j: Resource[J],
      k: Resource[K],
      l: Resource[L],
      m: Resource[M],
      n: Resource[N],
      o: Resource[O],
      p: Resource[P],
      q: Resource[Q],
      r: Resource[R],
      s: Resource[S],
      t: Resource[T],
      u: Resource[U],
      v: Resource[V]
  )(fun: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V) => RES): RES =
    a.use { aa =>
      b.use { bb =>
        c.use { cc =>
          d.use { dd =>
            e.use { ee =>
              f.use { ff =>
                g.use { gg =>
                  h.use { hh =>
                    i.use { ii =>
                      j.use { jj =>
                        k.use { kk =>
                          l.use { ll =>
                            m.use { mm =>
                              n.use { nn =>
                                o.use { oo =>
                                  p.use { pp =>
                                    q.use { qq =>
                                      r.use { rr =>
                                        s.use { ss =>
                                          t.use { tt =>
                                            u.use { uu =>
                                              v.use { vv =>
                                                fun(aa, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk, ll, mm, nn, oo, pp, qq, rr, ss, tt, uu, vv)
                                              }
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

  // ERROR: type Function23 is not a member of package scala
  //def use[RES,A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W](a: Resource[A], b: Resource[B], c: Resource[C], d: Resource[D], e: Resource[E], f: Resource[F], g: Resource[G], h: Resource[H], i: Resource[I], j: Resource[J], k: Resource[K], l: Resource[L], m: Resource[M], n: Resource[N], o: Resource[O], p: Resource[P], q: Resource[Q], r: Resource[R], s: Resource[S], t: Resource[T], u: Resource[U], v: Resource[V], w: Resource[W])(fun: (A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W) => RES): RES = a.use{ aa => b.use{ bb => c.use{ cc => d.use { dd => e.use { ee => f.use{ ff => g.use { gg => h.use{ hh => i.use{ ii => j.use{ jj => k.use { kk => l.use { ll => m.use { mm => n.use{ nn => o.use { oo => p.use { pp => q.use { qq => r.use { rr => s.use { ss => t.use { tt => u.use{ uu => v.use { vv => w.use{ ww => fun(aa,bb,cc,dd,ee,ff,gg,hh,ii,jj,kk,ll,mm,nn,oo,pp,qq,rr,ss,tt,uu,vv,ww) } } } } } } } } } } } } } } } } } } } } } } }
}

/**
  * An Automatically Managed Resource that can either be used once (e.g. reading an input stream) or
  * multiple times (e.g. reading a file).
  *
  * Resource provides several benefits:
  * - Ensure every use of the resource is close()ed after use.
  * - Abstract single vs multiple use resources
  * - Scala idiomatic map/flatMap/foreach usage syntax of resources
  */
trait Resource[+A] {
  def use[T](f: A => T): T

  /** Is this resource usable?  i.e. will the use() method work? */
  def isUsable: Boolean

  /** Can this resource be used multiple times? */
  def isMultiUse: Boolean

  final def map[B](f: A => B): Resource[B] = new MappedResource(this, f)

  final def flatMap[B](f: A => Resource[B]): Resource[B] = new FlatMappedResource(this, f)

  final def foreach[U](f: A => U): Unit = use(f)
}

/**
  * A Unit Resource
  */
object UnitResource extends Resource[Unit] {
  def use[T](f: Unit => T): T = f(Unit)

  def isUsable: Boolean   = true
  def isMultiUse: Boolean = true
}

/**
  * An Empty Resource that does nothing
  */
final case class EmptyResource[A](a: A) extends Resource[A] {
  def use[T](f: A => T): T = f(a)
  def isUsable: Boolean    = true
  def isMultiUse: Boolean  = false
}

object MultiUseResource {
  def apply[A <% AutoCloseable](makeIO: => A) = new MultiUseResource(makeIO)
}

/**
  * A Resource that can be used multiple times (e.g. opening an InputStream or Reader for a File)
  */
final class MultiUseResource[+A <% AutoCloseable](makeIO: => A) extends Resource[A] {
  final def isUsable: Boolean   = true
  final def isMultiUse: Boolean = true

  final def use[T](f: A => T): T = Resource.using(makeIO)(f)
}

object SingleUseResource {
  def apply[A <% AutoCloseable](resource: A): SingleUseResource[A] =
    new SingleUseResource(resource)
}

/**
  * A Resource that can only be used once (e.g. reading an InputStream)
  */
final class SingleUseResource[+A <% AutoCloseable](resource: A) extends Resource[A] {
  @volatile private[this] var used: Boolean = false

  final def isUsable: Boolean   = !used
  final def isMultiUse: Boolean = false

  final def use[T](f: A => T): T = {
    if (used) throw new IOException("The SingleUseResource has already been used and cannot be used again")
    used = true
    Resource.using(resource)(f)
  }
}

/**
  * For Resource.map
  */
final class MappedResource[A, B](resource: Resource[A], mapping: A => B) extends Resource[B] {
  def use[T](f: B => T): T = resource.use { a => f(mapping(a)) }

  def isUsable            = resource.isUsable
  def isMultiUse: Boolean = resource.isMultiUse
}

/**
  * For Resource.flatMap
  */
final class FlatMappedResource[A, B](resource: Resource[A], mapping: A => Resource[B]) extends Resource[B] {
  def use[T](f: B => T): T = resource.use { a => mapping(a).use[T](f) }

  def isUsable            = resource.isUsable
  def isMultiUse: Boolean = resource.isMultiUse
}
