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
import org.neo4j.cypher.internal.compiler.v3_2.commands.expressions.Expression
import org.neo4j.cypher.internal.compiler.v3_2.planDescription.Id
import org.neo4j.cypher.internal.frontend.v3_2.CypherTypeException
import org.neo4j.cypher.internal.frontend.v3_2.ast.{LabelToken, PropertyKeyToken}
import org.neo4j.graphdb.Node

abstract class AbstractNodeIndexStringScanPipe(ident: String,
                                               label: LabelToken,
                                               propertyKey: PropertyKeyToken,
                                               valueExpr: Expression)(implicit pipeMonitor: PipeMonitor)
  extends Pipe {

  private val descriptor = IndexDescriptor(label.nameId.id, propertyKey.nameId.id)

  valueExpr.registerOwningPipe(this)

  override protected def internalCreateResults(state: QueryState): PipeIterator[ExecutionContext] = {
    val baseContext = state.createOrGetInitialContext()
    val value = valueExpr(baseContext)(state)

    val resultNodes = value match {
      case value: String =>
        queryContextCall(state, descriptor, value).
          map(node => baseContext.newWith1(ident, node))
      case null =>
        Iterator.empty
      case x => throw new CypherTypeException(s"Expected a string value, but got $x")
    }

    resultNodes
  }

  protected def queryContextCall(state: QueryState, indexDescriptor: IndexDescriptor, value: String): Iterator[Node]

  override def monitor = pipeMonitor
}

case class NodeIndexContainsScanPipe(ident: String,
                                     label: LabelToken,
                                     propertyKey: PropertyKeyToken,
                                     valueExpr: Expression)
                                    (val id: Id = new Id)
                                    (implicit pipeMonitor: PipeMonitor)
  extends AbstractNodeIndexStringScanPipe(ident, label, propertyKey, valueExpr) {

  override protected def queryContextCall(state: QueryState, indexDescriptor: IndexDescriptor, value: String) =
    state.query.indexScanByContains(indexDescriptor, value)
}

case class NodeIndexEndsWithScanPipe(ident: String,
                                     label: LabelToken,
                                     propertyKey: PropertyKeyToken,
                                     valueExpr: Expression)
                                    (val id: Id = new Id)
                                    (implicit pipeMonitor: PipeMonitor)
  extends AbstractNodeIndexStringScanPipe(ident, label, propertyKey, valueExpr) {

  override protected def queryContextCall(state: QueryState, indexDescriptor: IndexDescriptor, value: String) =
    state.query.indexScanByEndsWith(indexDescriptor, value)
}
