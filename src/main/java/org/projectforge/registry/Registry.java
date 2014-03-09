/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.Validate;
import org.projectforge.core.BaseDO;
import org.projectforge.core.BaseDao;
import org.projectforge.fibu.KontoCache;
import org.projectforge.fibu.RechnungCache;
import org.projectforge.fibu.kost.KostCache;
import org.projectforge.multitenancy.TenantDO;
import org.projectforge.multitenancy.TenantRegistry;
import org.projectforge.multitenancy.TenantRegistryMap;
import org.projectforge.multitenancy.TenantsCache;
import org.projectforge.task.TaskTree;
import org.projectforge.user.UserCache;
import org.projectforge.user.UserGroupCache;
import org.projectforge.user.UserXmlPreferencesDao;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * Registry for dao's. Here you can register additional daos and plugins (extensions of ProjectForge).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class Registry
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Registry.class);

  private static final Registry instance = new Registry();

  private final Map<String, RegistryEntry> mapByName = new HashMap<String, RegistryEntry>();

  private final Map<Class< ? extends BaseDao< ? >>, RegistryEntry> mapByDao = new HashMap<Class< ? extends BaseDao< ? >>, RegistryEntry>();

  private final Map<Class< ? extends BaseDO< ? >>, RegistryEntry> mapByDO = new HashMap<Class< ? extends BaseDO< ? >>, RegistryEntry>();

  private final List<RegistryEntry> orderedList = new ArrayList<RegistryEntry>();

  private TenantsCache tenantsCache;

  private UserCache userCache;

  private UserXmlPreferencesDao userXmlPreferencesDao;

  private DataSource dataSource;

  private HibernateTemplate hibernateTemplate;

  private ConfigurableListableBeanFactory beanFactory;

  public static Registry instance()
  {
    return instance;
  }

  /**
   * Registers the given entry and appends it to the ordered list of registry entries.
   * @param entry The entry to register.
   * @return this for chaining.
   */
  public Registry register(final RegistryEntry entry)
  {
    Validate.notNull(entry);
    mapByName.put(entry.getId(), entry);
    mapByDao.put(entry.getDaoClassType(), entry);
    mapByDO.put(entry.getDOClass(), entry);
    orderedList.add(entry);
    return this;
  }

  /**
   * Registers the given entry and inserts it to the ordered list of registry entries at the given position.
   * @param existingEntry A previous added entry, at which the new entry should be inserted.
   * @param insertBefore If true then the given entry will be added before the existing entry, otherwise after.
   * @param entry The entry to register.
   * @return this for chaining.
   */
  public Registry register(final RegistryEntry existingEntry, final boolean insertBefore, final RegistryEntry entry)
  {
    Validate.notNull(existingEntry);
    Validate.notNull(entry);
    mapByName.put(entry.getId(), entry);
    mapByDao.put(entry.getDaoClassType(), entry);
    mapByDO.put(entry.getDOClass(), entry);
    final int idx = orderedList.indexOf(existingEntry);
    if (idx < 0) {
      log.error("Registry entry '" + existingEntry.getId() + "' not found. Appending the given entry to the list.");
      orderedList.add(entry);
    } else if (insertBefore == true) {
      orderedList.add(idx, entry);
    } else {
      orderedList.add(idx + 1, entry);
    }
    return this;
  }

  public RegistryEntry getEntry(final String id)
  {
    return mapByName.get(id);
  }

  public RegistryEntry getEntry(final Class< ? extends BaseDao< ? >> daoClass)
  {
    return mapByDao.get(daoClass);
  }

  public RegistryEntry getEntryByDO(final Class< ? extends BaseDO< ? >> doClass)
  {
    return mapByDO.get(doClass);
  }

  /**
   * @return The list of entries in the order of their registration: the first registered entry is the first of the returned list etc.
   */
  public List<RegistryEntry> getOrderedList()
  {
    return orderedList;
  }

  public BaseDao< ? > getDao(final String id)
  {
    final RegistryEntry entry = getEntry(id);
    return entry != null ? entry.getDao() : null;
  }

  @SuppressWarnings("unchecked")
  public <T extends BaseDao< ? >> T getDao(final Class<T> daoClass)
  {
    final RegistryEntry entry = getEntry(daoClass);
    return entry != null ? (T) entry.getDao() : null;
  }

  public TaskTree getTaskTree()
  {
    return getTaskTree(null);
  }

  public TaskTree getTaskTree(final TenantDO tenant)
  {
    final TenantRegistry tenantRegistry = TenantRegistryMap.getInstance().getTenantRegistry(tenant);
    return tenantRegistry.getTaskTree();
  }

  public TaskTree getTaskTree(final BaseDO< ? > obj)
  {
    return getTaskTree(obj.getTenant());
  }

  /**
   * @return the userCache
   */
  public UserCache getUserCache()
  {
    if (userCache == null) {
      userCache = new UserCache();
    }
    return userCache;
  }

  public UserGroupCache getUserGroupCache()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
  }

  public TenantsCache getTenantsCache()
  {
    return tenantsCache;
  }

  public void setTenantsCache(final TenantsCache tenantsCache)
  {
    this.tenantsCache = tenantsCache;
  }

  public UserXmlPreferencesDao getUserXmlPreferencesDao()
  {
    return userXmlPreferencesDao;
  }

  public void setUserXmlPreferencesDao(final UserXmlPreferencesDao userXmlPreferencesDao)
  {
    this.userXmlPreferencesDao = userXmlPreferencesDao;
  }

  public KontoCache getKontoCache()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry().getKontoCache();
  }

  /**
   * @return the kostCache
   */
  public KostCache getKostCache()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry().getKostCache();
  }

  /**
   * @return the invoiceCache
   */
  public RechnungCache getInvoiceCache()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry().getInvoiceCache();
  }

  public DataSource getDataSource()
  {
    return dataSource;
  }

  void setDataSource(final DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  /**
   * @return the hibernateTemplate
   */
  public HibernateTemplate getHibernateTemplate()
  {
    return hibernateTemplate;
  }

  void setHibernateTemplate(final HibernateTemplate hibernateTemplate)
  {
    this.hibernateTemplate = hibernateTemplate;
  }

  public void setBeanFactory(final ConfigurableListableBeanFactory beanFactory)
  {
    this.beanFactory = beanFactory;
  }

  public ConfigurableListableBeanFactory getBeanFactory()
  {
    return this.beanFactory;
  }

  private Registry()
  {
  }
}
