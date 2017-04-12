/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v3_2.pipes

import org.neo4j.cypher.internal.compiler.v3_2._
import org.neo4j.cypher.internal.compiler.v3_2.planDescription.Id

import scala.collection.Iterator.empty
import scala.collection.{AbstractIterator, GenTraversableOnce, Iterator, SeqLike}

trait PipeMonitor {
  def startSetup(queryId: AnyRef, pipe: Pipe)
  def stopSetup(queryId: AnyRef, pipe: Pipe)
  def startStep(queryId: AnyRef, pipe: Pipe)
  def stopStep(queryId: AnyRef, pipe: Pipe)
}

/**
  * Pipe is a central part of Cypher. Most pipes are decorators - they
  * wrap another pipe. ParamPipe and NullPipe the only exception to this.
  * Pipes are combined to form an execution plan, and when iterated over,
  * the execute the query.
  *
  * ** WARNING **
  * Pipes are re-used between query executions, and must not hold state in instance fields.
  * Not heeding this warning will lead to bugs that do not manifest except for under concurrent use.
  * If you need to keep state per-query, have a look at QueryState instead.
  */
trait Pipe {
  self: Pipe =>

  def monitor: PipeMonitor

  def createResults(state: QueryState) : PipeIterator[ExecutionContext] = {
    val decoratedState = state.decorator.decorate(self, state)
    monitor.startSetup(state.queryId, self)
    val innerResult = internalCreateResults(decoratedState)
    val result = new PipeIterator[ExecutionContext] {
      override def hasNext: Boolean = innerResult.hasNext
      override def next(): ExecutionContext = {
        monitor.startStep(state.queryId, self)
        val value = innerResult.next()
        monitor.stopStep(state.queryId, self)
        value
      }
      override def close(): Unit = innerResult.close()
    }
    monitor.stopSetup(state.queryId, self)
    state.decorator.decorate(self, result)
  }

  protected def internalCreateResults(state: QueryState): PipeIterator[ExecutionContext]

  // Used by profiling to identify where to report dbhits and rows
  def id: Id
}

case class SingleRowPipe()(val id: Id = new Id)(implicit val monitor: PipeMonitor) extends Pipe {

  def internalCreateResults(state: QueryState) =
    PipeIterator(state.createOrGetInitialContext())
}

abstract class PipeWithSource(source: Pipe, val monitor: PipeMonitor) extends Pipe {
  override def createResults(state: QueryState): PipeIterator[ExecutionContext] = {
    val sourceResult = source.createResults(state)

    val decoratedState = state.decorator.decorate(this, state)
    val result = internalCreateResults(sourceResult, decoratedState)
    state.decorator.decorate(this, result)
  }

  protected def internalCreateResults(state: QueryState): PipeIterator[ExecutionContext] =
    throw new UnsupportedOperationException("This method should never be called on PipeWithSource")

  protected def internalCreateResults(input:PipeIterator[ExecutionContext], state: QueryState): PipeIterator[ExecutionContext]
}

// TODO: by NOT extending Iterator, we could simplify this type, since we could declare tighter parameter types
abstract class PipeIterator[+T] extends AbstractIterator[T] with java.lang.AutoCloseable {
  self =>
  override def flatMap[B](f: (T) => GenTraversableOnce[B]): PipeIterator[B] = new PipeIterator[B] {
    private var inner: Iterator[B] = empty
    private def nextInner() {
      closeInner()
      inner = f(self.next()).toIterator
    }

    def hasNext: Boolean = {
      while (!inner.hasNext) {
        if (!self.hasNext) {
          closeInner()
          inner = empty
          return false
        }
        nextInner()
      }
      true
    }
    def next(): B = (if (hasNext) inner else empty).next()

    private def closeInner() = {
      inner match {
        case closeable: AutoCloseable => closeable.close()
      }
    }

    override def close(): Unit = {
      closeInner()
      inner = empty
      self.close()
    }
  }

  override def map[B](f: (T) => B): PipeIterator[B] = new PipeIterator[B] {
    override def hasNext: Boolean = self.hasNext
    override def next(): B = f(self.next())
    override def close(): Unit = self.close()
  }

  override def filter(p: (T) => Boolean): PipeIterator[T] = new PipeIterator[T] {
    private var hd: T = _
    private var hdDefined: Boolean = false

    def hasNext: Boolean = hdDefined || {
      do {
        if (!self.hasNext) return false
        hd = self.next()
      } while (!p(hd))
      hdDefined = true
      true
    }

    def next() = if (hasNext) { hdDefined = false; hd } else empty.next()
    override def close(): Unit = self.close()
  }

  override def ++[B >: T](that: => GenTraversableOnce[B]): PipeIterator[B] = new PipeIterator[B] {
    private val other = that.toIterator
    override def hasNext: Boolean = self.hasNext || other.hasNext

    override def next(): B = if (self.hasNext) self.next() else if (other.hasNext) other.next() else empty.next()

    override def close(): Unit = {
      self.close()
      other match {
        case closeable: AutoCloseable => closeable.close()
      }
    }
  }

  override def drop(n: Int): PipeIterator[T] = {
    var j = 0
    while (j < n && hasNext) {
      next()
      j += 1
    }
    this
  }

  override def collect[B](pf: PartialFunction[T, B]): PipeIterator[B] = ???
}

object PipeIterator {
  def apply[T](single: Array[T]): PipeIterator[T] = new PipeIterator[T] {
    override def hasNext: Boolean = ???

    override def next(): T = ???

    override def close(): Unit = ()
  }
  def apply[T](single: SeqLike[T,_]): PipeIterator[T] = new PipeIterator[T] {
    override def hasNext: Boolean = ???

    override def next(): T = ???

    override def close(): Unit = ()
  }

  def apply[T](single: T): PipeIterator[T] = new PipeIterator[T] {
    private var hasnext = true

    override def hasNext: Boolean = hasnext

    override def next(): T =
      if(hasnext) {
        hasnext = false
        single
      }
      else empty.next()

    override def close(): Unit = ()
  }

  val empty: PipeIterator[Nothing] = new PipeIterator[Nothing] {
    override def hasNext: Boolean = false

    override def next(): Nothing = Iterator.empty.next()

    override def close(): Unit = ()
  }
}
