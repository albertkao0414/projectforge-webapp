/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class SearchDao extends HibernateDaoSupport
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SearchDao.class);

  @SuppressWarnings("unchecked")
  public List<SearchResultData> getEntries(final BaseSearchFilter filter, final Class clazz, final BaseDao baseDao)
  {
    if (filter == null) {
      log.info("Filter or rows in filter is null (may be Search as redirect after login): " + filter);
      return null;
    }
    log.debug("Searching in " + clazz);
    if (baseDao.hasLoggedInUserSelectAccess(false) == false || baseDao.hasLoggedInUserHistoryAccess(false) == false) {
      // User has in general no access to history entries of the given object type (clazz).
      return null;
    }
    if (filter.getModifiedByUserId() != null
        || filter.getStartTimeOfLastModification() != null
        || filter.getStopTimeOfLastModification() != null) {
      filter.setUseModificationFilter(true);
    } else {
      filter.setUseModificationFilter(false);
    }
    final List<ExtendedBaseDO> list = baseDao.getList(filter);
    final List<SearchResultData> result = new ArrayList<SearchResultData>();
    if (list.size() == 0) {
      return result;
    }
    // TODO: Search for history entries.
    // Now put the stuff together:
    int counter = 0;
    for (ExtendedBaseDO entry : list) {
      SearchResultData data = new SearchResultData();
      // Integer userId = NumberHelper.parseInteger(entry.getUserName());
      // if (userId != null) {
      // data.modifiedByUser = userGroupCache.getUser(userId);
      // }
      data.dataObject = entry;
      // data.historyEntry = entry;
      // data.propertyChanges = baseDao.convert(entry, session);
      result.add(data);
      if (++counter >= filter.getMaxRows()) {
        result.add(new SearchResultData()); // Add null entry for gui for displaying 'more entries'.
        break;
      }
    }
    return result;
  }
}
