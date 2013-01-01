/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.Test;
import org.projectforge.address.AddressDO;
import org.projectforge.test.TestBase;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseUpdateDaoTest extends TestBase
{
  private DatabaseUpdateDao databaseUpdateDao;

  private DataSource dataSource;

  @Test
  public void createTableScript()
  {
    final Table table = new Table("t_test") //
    .addAttribute(new TableAttribute("pk", TableAttributeType.INT).setPrimaryKey(true))//
    .addAttribute(new TableAttribute("counter", TableAttributeType.INT)) //
    .addAttribute(new TableAttribute("money", TableAttributeType.DECIMAL, 8, 2).setNullable(false)) //
    .addAttribute(new TableAttribute("address_fk", TableAttributeType.INT).setForeignTable(AddressDO.class));
    final DatabaseUpdateDao dao = new DatabaseUpdateDao();
    dao.databaseSupport = new DatabaseSupport(HibernateDialect.HSQL);
    StringBuffer buf = new StringBuffer();
    dao.buildCreateTableStatement(buf, table);
    assertEquals("CREATE TABLE t_test (\n" //
        + "  pk INT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,\n" //
        + "  counter INT,\n" //
        + "  money DECIMAL(8, 2) NOT NULL,\n" //
        + "  address_fk INT,\n" //
        + "  FOREIGN KEY (address_fk) REFERENCES T_ADDRESS(pk)\n" //
        + ");\n", buf.toString());
    dao.databaseSupport = new DatabaseSupport(HibernateDialect.PostgreSQL);
    buf = new StringBuffer();
    dao.buildCreateTableStatement(buf, table);
    assertEquals("CREATE TABLE t_test (\n" //
        + "  pk INT4,\n" //
        + "  counter INT4,\n" //
        + "  money DECIMAL(8, 2) NOT NULL,\n" //
        + "  address_fk INT4,\n" //
        + "  PRIMARY KEY (pk),\n" //
        + "  FOREIGN KEY (address_fk) REFERENCES T_ADDRESS(pk)\n" //
        + ");\n", buf.toString());
  }

  @Test
  public void createAndDropTable()
  {
    logon(ADMIN);
    final Table table = new Table("t_test") //
    .addAttribute(new TableAttribute("name", TableAttributeType.VARCHAR, 5).setPrimaryKey(true))//
    .addAttribute(new TableAttribute("counter", TableAttributeType.INT)) //
    .addAttribute(new TableAttribute("money", TableAttributeType.DECIMAL, 8, 2).setNullable(false)) //
    .addAttribute(new TableAttribute("address_fk", TableAttributeType.INT).setForeignTable("t_address").setForeignAttribute("pk"));
    final StringBuffer buf = new StringBuffer();
    databaseUpdateDao.buildCreateTableStatement(buf, table);
    assertEquals("CREATE TABLE t_test (\n" //
        + "  name VARCHAR(5),\n" //
        + "  counter INT,\n" //
        + "  money DECIMAL(8, 2) NOT NULL,\n" //
        + "  address_fk INT,\n" //
        + "  PRIMARY KEY (name),\n" //
        + "  FOREIGN KEY (address_fk) REFERENCES t_address(pk)\n" //
        + ");\n", buf.toString());
    assertTrue(databaseUpdateDao.createTable(table));
    assertTrue(databaseUpdateDao.doesTableExist("t_test"));
    assertTrue(databaseUpdateDao.dropTable("t_test"));
    assertTrue(databaseUpdateDao.dropTable("t_test"));
    assertTrue(databaseUpdateDao.createTable(table));
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.execute("INSERT INTO t_test (name, counter, money) VALUES('test', 5, 5.12);");
    assertFalse("Data base is not empty!", databaseUpdateDao.dropTable("t_test"));
    jdbc.execute("DELETE FROM t_test;");
  }

  @Test
  public void buildAddUniqueConstraint()
  {
    final StringBuffer buf = new StringBuffer();
    databaseUpdateDao.buildAddUniqueConstraintStatement(buf, "t_user_right", "t_user_right_user_fk_key", "user_fk", "right_id");
    assertEquals("ALTER TABLE t_user_right ADD CONSTRAINT t_user_right_user_fk_key UNIQUE (user_fk, right_id);\n", buf.toString());
  }

  @Test
  public void buildAddTableColumn()
  {
    final StringBuffer buf = new StringBuffer();
    databaseUpdateDao.buildAddTableAttributesStatement(buf, "t_task", new TableAttribute("workpackage_code", TableAttributeType.VARCHAR, 100,
        false), new TableAttribute("user_fk", TableAttributeType.INT).setForeignTable("t_user").setForeignAttribute("pk"));
    assertEquals("-- Does already exist: ALTER TABLE t_task ADD COLUMN workpackage_code VARCHAR(100) NOT NULL;\n" //
        + "ALTER TABLE t_task ADD COLUMN user_fk INT;\n"
        + "ALTER TABLE t_task ADD CONSTRAINT t_task_user_fk FOREIGN KEY (user_fk) REFERENCES t_user(pk);\n", buf.toString());
  }

  @Test
  public void createAndDropTableColumn()
  {
    databaseUpdateDao.addTableAttributes("t_task", new TableAttribute("test1", TableAttributeType.DATE), new TableAttribute("test2",
        TableAttributeType.INT));
    assertTrue(databaseUpdateDao.doesTableAttributeExist("t_task", "test1"));
    assertTrue(databaseUpdateDao.doesTableAttributeExist("t_task", "test2"));
    databaseUpdateDao.dropTableAttribute("t_task", "test1");
    assertFalse(databaseUpdateDao.doesTableAttributeExist("t_task", "test1"));
    assertTrue(databaseUpdateDao.doesTableAttributeExist("t_task", "test2"));
    databaseUpdateDao.dropTableAttribute("t_task", "test2");
    assertFalse(databaseUpdateDao.doesTableAttributeExist("t_task", "test1"));
    assertFalse(databaseUpdateDao.doesTableAttributeExist("t_task", "test2"));
  }

  @Test
  public void renameTableAttribute()
  {
    assertEquals("ALTER TABLE t_test ALTER COLUMN old_col RENAME TO new_col", DatabaseSupport.instance().renameAttribute("t_test",
        "old_col", "new_col"));
  }

  public void setDatabaseUpdateDao(final DatabaseUpdateDao databaseUpdateDao)
  {
    this.databaseUpdateDao = databaseUpdateDao;
  }

  public void setDataSource(final DataSource dataSource)
  {
    this.dataSource = dataSource;
  }
}
