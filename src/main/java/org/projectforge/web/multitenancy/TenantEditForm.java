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

package org.projectforge.web.multitenancy;

import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.multitenancy.TenantDO;
import org.projectforge.multitenancy.TenantDao;
import org.projectforge.user.PFUserDO;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.UsersComparator;
import org.projectforge.web.user.UsersProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

import com.vaynberg.wicket.select2.Select2MultiChoice;

public class TenantEditForm extends AbstractEditForm<TenantDO, TenantEditPage>
{
  private static final long serialVersionUID = -1813583013436792979L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TenantEditForm.class);

  @SpringBean(name = "tenantDao")
  private TenantDao tenantDao;

  MultiChoiceListHelper<PFUserDO> assignUsersListHelper;

  public TenantEditForm(final TenantEditPage parentPage, final TenantDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    {
      // Short name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("multitenancy.tenant.shortName"));
      final RequiredMaxLengthTextField title = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "shortName"));
      title.add(WicketUtils.setFocus());
      fs.add(title);
    }
    {
      // Name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("name"));
      final RequiredMaxLengthTextField title = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "name"));
      fs.add(title);
    }
    {
      // Option default
      gridBuilder.newFieldset(getString("multitenancy.defaultTenant")).addCheckBox(new PropertyModel<Boolean>(data, "defaultTenant"), null)
      .setTooltip(getString("multitenancy.defaultTenant.tooltip"));
    }
    {
      // Assigned users
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("multitenancy.assignedUsers")).setLabelSide(false);
      final Set<PFUserDO> assignedUsers = getData().getAssignedUsers();
      final UsersProvider usersProvider = new UsersProvider();
      assignUsersListHelper = new MultiChoiceListHelper<PFUserDO>().setComparator(new UsersComparator()).setFullList(
          usersProvider.getSortedUsers());
      if (assignedUsers != null) {
        for (final PFUserDO user : assignedUsers) {
          assignUsersListHelper.addOriginalAssignedItem(user).assignItem(user);
        }
      }
      final Select2MultiChoice<PFUserDO> users = new Select2MultiChoice<PFUserDO>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<PFUserDO>>(this.assignUsersListHelper, "assignedItems"), usersProvider);
      fs.add(users);
    }
    {
      // Description
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "description")));
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}