/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.jdbc.v2

import java.sql.Connection

import test.scala.org.apache.spark.sql.jdbc.v2.V2JDBCPushdownTest

import org.apache.spark.SparkConf
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog
import org.apache.spark.sql.jdbc.{DatabaseOnDocker, DockerJDBCIntegrationSuite, MySQLDatabaseOnDocker}

class MySqlPushdownIntegrationSuite
  extends DockerJDBCIntegrationSuite
    with V2JDBCPushdownTest {

  override val db: DatabaseOnDocker = new MySQLDatabaseOnDocker

  override def sparkConf: SparkConf = super.sparkConf
    .set("spark.sql.catalog.mysql", classOf[JDBCTableCatalog].getName)
    .set("spark.sql.catalog.mysql.url", db.getJdbcUrl(dockerIp, externalPort))
    .set("spark.sql.catalog.mysql.pushDownAggregate", "true")
    .set("spark.sql.catalog.mysql.pushDownLimit", "true")

  /**
   * Prepare databases and tables for testing.
   */
  override def dataPreparation(connection: Connection): Unit = prepareData()

  override protected val catalog: String = "mysql"
  override protected val tablePrefix: String = "testtbl"
  override protected val schema: String = "testschema"

  override protected def executeUpdate(sql: String): Unit = {
    getConnection().prepareStatement(sql).executeUpdate()
  }

  override protected def commonAssertionOnDataFrame(df: DataFrame): Unit = {

  }

  override def prepareTable(): Unit = {
    executeUpdate(
      s"""CREATE SCHEMA $schema"""
    )

    executeUpdate(
      s"""CREATE TABLE $schema.$tablePrefix
         | (id INTEGER, st TEXT, num_col INT);""".stripMargin
    )

    executeUpdate(
      s"""CREATE TABLE $schema.${tablePrefix}_coalesce
         | (id INTEGER, col1 TEXT, col2 INT);""".stripMargin
    )

    executeUpdate(
      s"""CREATE TABLE $schema.${tablePrefix}_string_test
         | (id INTEGER, st TEXT, num_col INT);""".stripMargin
    )

    executeUpdate(
      s"""CREATE TABLE $schema.${tablePrefix}_with_nulls
         | (id INTEGER, st TEXT);""".stripMargin
    )

    executeUpdate(
      s"""CREATE TABLE $schema.${tablePrefix}_numeric_test
         | (id INTEGER,
         | dec_col DECIMAL(10,2)
         | );""".stripMargin
    )
  }

  override protected def prepareData(): Unit = {

    prepareTable()

    executeUpdate(s"""INSERT INTO $schema.${tablePrefix}_coalesce VALUES (1, NULL, 1)""")
    executeUpdate(s"""INSERT INTO $schema.${tablePrefix}_coalesce VALUES (2, '2', NULL)""")
    executeUpdate(s"""INSERT INTO $schema.${tablePrefix}_coalesce VALUES (3, NULL, NULL)""")

    executeUpdate(
      s"""INSERT INTO $schema.${tablePrefix}_with_nulls VALUES (1, 'first')""")
    executeUpdate(
      s"""INSERT INTO $schema.${tablePrefix}_with_nulls VALUES (2, 'second')""")
    executeUpdate(
      s"""INSERT INTO $schema.${tablePrefix}_with_nulls VALUES (3, 'third')""")
    executeUpdate(
      s"""INSERT INTO $schema.${tablePrefix}_with_nulls VALUES (NULL, 'null')""")

    executeUpdate(
      s"""INSERT INTO $schema.${tablePrefix}_string_test VALUES (0, 'ab''', 1000)""")
    executeUpdate(
      s"""INSERT INTO $schema.${tablePrefix}_string_test VALUES (0, 'FiRs''T', 1000)""")
    executeUpdate(
      s"""INSERT INTO $schema.${tablePrefix}_string_test VALUES (0, 'sE Co nD', 1000)""")
    executeUpdate(
      s"""INSERT INTO $schema.${tablePrefix}_string_test VALUES (0, '   forth   ', 1000)""")

    executeUpdate(s"""INSERT INTO $schema.$tablePrefix VALUES (1, 'ab', 1000)""")
    executeUpdate(s"""INSERT INTO $schema.$tablePrefix VALUES (2, 'aba', NULL)""")
    executeUpdate(s"""INSERT INTO $schema.$tablePrefix VALUES (3, 'abb', 800)""")
    executeUpdate(s"""INSERT INTO $schema.$tablePrefix VALUES (4, 'abc', NULL)""")
    executeUpdate(s"""INSERT INTO $schema.$tablePrefix VALUES (5, 'abd', 1200)""")
    executeUpdate(s"""INSERT INTO $schema.$tablePrefix VALUES (6, 'abe', 1250)""")
    executeUpdate(s"""INSERT INTO $schema.$tablePrefix VALUES (7, 'abf', 1200)""")
    executeUpdate(s"""INSERT INTO $schema.$tablePrefix VALUES (8, 'abg', -1300)""")

    executeUpdate(
      s"""INSERT INTO $schema.${tablePrefix}_numeric_test VALUES (1, 42.42)""")
  }

}
