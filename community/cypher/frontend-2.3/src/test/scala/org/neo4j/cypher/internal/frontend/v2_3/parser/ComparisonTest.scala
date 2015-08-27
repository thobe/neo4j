/*
 * Copyright (c) 2002-2015 "Neo Technology,"
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
package org.neo4j.cypher.internal.frontend.v2_3.parser

import org.neo4j.cypher.internal.frontend.v2_3.ast

class ComparisonTest extends ParserAstTest[ast.Expression] with Expressions {
  implicit val parser = Expression

  test("a < b") {
    yields(lt(id("a"), id("b")))
  }

  test("a > b") {
    yields(gt(id("a"), id("b")))
  }

  test("1 = {b}") {
    yields(eq(int(1), param("b")))
  }

  test("0x5 <> c") {
    yields(ne(hex(0x5), id("c")))
  }

  test("a != 077") {
    yields(bne(id("a"), oct(63)))
  }

  test("x <= y") {
    yields(lte(id("x"), id("y")))
  }

  test("x >= y") {
    yields(gte(id("x"), id("y")))
  }

  test("a < b + c") {
    yields(lt(id("a"), add(id("b"), id("c"))))
  }

  test("a > b AND b > c") {
    yields(and(gt(id("a"), id("b")), gt(id("b"), id("c"))))
  }

  test("a > b > c") {
    yields(ands(
      gt( id("a"), id("b") ),
      gt( id("b"), id("c") )  ))
  }

  test("a - 1 < b + 2 <= c * 3 = d / 4 <> e ^ 5 != f % 6 >= +g > -h") {
    yields(ands(
      lt(  sub( id("a"), int(1) ), add( id("b"), int(2) ) ),
      lte( add( id("b"), int(2) ), mul( id("c"), int(3) ) ),
      eq(  mul( id("c"), int(3) ), div( id("d"), int(4) ) ),
      ne(  div( id("d"), int(4) ), pow( id("e"), int(5) ) ),
      bne( pow( id("e"), int(5) ), mod( id("f"), int(6) ) ),
      gte( mod( id("f"), int(6) ), pos( id("g")            ) ),
      gt(  pos( id("g")         ), neg( id("h")            ) )  ))
  }

  test( "10 < a < 30 AND 2^a >= b >= 1" ) {
    yields(and(
      ands( lt(  int(10),              id("a")),  lt( id("a"), int(30) ) ),
      ands( gte( pow(int(2), id("a")), id("b")), gte( id("b"),  int(1) ) )  ))
  }
}
