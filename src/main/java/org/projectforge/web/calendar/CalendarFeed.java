/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.calendar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.protocol.http.WebApplication;
import org.projectforge.access.AccessException;
import org.projectforge.calendar.ICal4JUtils;
import org.projectforge.common.NumberHelper;
import org.projectforge.plugins.teamcal.TeamCalConfig;
import org.projectforge.registry.Registry;
import org.projectforge.timesheet.TimesheetDO;
import org.projectforge.timesheet.TimesheetDao;
import org.projectforge.timesheet.TimesheetFilter;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.PFUserDO;
import org.projectforge.user.ProjectForgeGroup;
import org.projectforge.user.UserDao;
import org.projectforge.user.UserRights;
import org.projectforge.web.timesheet.TimesheetEventsProvider;

/**
 * Feed Servlet, which generates a 'text/calendar' output of the last four mounts. Currently relevant informations are date, start- and stop
 * time and last but not least the location of an event.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CalendarFeed extends HttpServlet
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CalendarFeed.class);

  private static final long serialVersionUID = 1480433876190009435L;

  private static final int PERIOD_IN_MONTHS = 4;

  private static final String PARAM_NAME_TIMESHEET_USER = "timesheetUser";

  private static final List<CalendarFeedHook> feedHooks = new LinkedList<CalendarFeedHook>();

  public static String getUrl()
  {
    return getUrl(null);
  }

  public static String getUrlParameterTimesheetUser(final PFUserDO timesheetUser)
  {
    if (timesheetUser == null) {
      return null;
    }
    return "&" + PARAM_NAME_TIMESHEET_USER + "=" + timesheetUser.getId();
  }

  /**
   * 
   * @param additionalParams Request parameters such as "&calId=42", may be null.
   * @return
   */
  public static String getUrl(final String additionalParams)
  {
    final PFUserDO user = PFUserContext.getUser();
    final UserDao userDao = Registry.instance().getDao(UserDao.class);
    final String authenticationKey = userDao.getAuthenticationToken(user.getId());
    final String contextPath = WebApplication.get().getServletContext().getContextPath();
    final StringBuffer buf = new StringBuffer();
    buf.append(contextPath).append("/export/ProjectForge.ics?user=").append(user.getUsername()).append("&token=").append(authenticationKey);
    if (additionalParams != null) {
      buf.append(additionalParams);
    }
    return buf.toString();
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException
  {
    if (StringUtils.isBlank(req.getParameter("user")) || StringUtils.isBlank(req.getParameter("token"))) {
      resp.sendError(HttpStatus.SC_BAD_REQUEST);
      return;
    }

    final Calendar calendar = createCal(req, req.getParameter("user"), req.getParameter("token"),
        req.getParameter(PARAM_NAME_TIMESHEET_USER));

    if (calendar == null) {
      resp.sendError(HttpStatus.SC_BAD_REQUEST);
      return;
    }

    resp.setContentType("text/calendar");
    final CalendarOutputter output = new CalendarOutputter(false);
    try {
      output.output(calendar, resp.getOutputStream());
    } catch (final ValidationException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * creates a calendar for the user, identified by his name and authentication key.
   * @param req
   * 
   * @param userName
   * @param userKey
   * @return a calendar, null if authentication fails
   */
  public Calendar createCal(final HttpServletRequest req, final String userName, final String authKey, final String timesheetUserParam)
  {
    final UserDao userDao = Registry.instance().getDao(UserDao.class);
    final PFUserDO loggedInUser = userDao.getUserByAuthenticationToken(userName, authKey);

    if (loggedInUser == null) {
      return null;
    }
    try {
      PFUserContext.setUser(loggedInUser);
      PFUserDO timesheetUser = null;
      if (StringUtils.isNotBlank(timesheetUserParam) == true) {
        final Integer timesheetUserId = NumberHelper.parseInteger(timesheetUserParam);
        if (timesheetUserId != null) {
          timesheetUser = userDao.getUserGroupCache().getUser(timesheetUserId);
        }
      }
      // creating a new calendar
      final Calendar calendar = new Calendar();
      final Locale locale = PFUserContext.getLocale();
      calendar.getProperties().add(
          new ProdId("-//" + loggedInUser.getDisplayUsername() + "//ProjectForge//" + locale.toString().toUpperCase()));
      calendar.getProperties().add(Version.VERSION_2_0);
      calendar.getProperties().add(CalScale.GREGORIAN);

      // setup event is needed for empty calendars
      calendar.getComponents().add(new VEvent(new net.fortuna.ical4j.model.Date(0), "SETUP EVENT"));

      // adding events
      for (final VEvent event : getEvents(req, timesheetUser)) {
        calendar.getComponents().add(event);
      }
      return calendar;
    } finally {
      PFUserContext.setUser(null);
    }
  }

  /**
   * builds the list of events
   * 
   * @return
   */
  private List<VEvent> getEvents(final HttpServletRequest req, PFUserDO timesheetUser)
  {
    final PFUserDO loggedInUser = PFUserContext.getUser();
    if (loggedInUser == null) {
      throw new AccessException("No logged-in-user found!");
    }
    final List<VEvent> events = new ArrayList<VEvent>();
    final TimeZone timezone = ICal4JUtils.getUserTimeZone();
    final java.util.Calendar cal = java.util.Calendar.getInstance(PFUserContext.getTimeZone());

    boolean eventsExist = false;
    for (final CalendarFeedHook hook : feedHooks) {
      final List<VEvent> list = hook.getEvents(req, timezone, cal);
      if (list != null && list.size() > 0) {
        events.addAll(list);
        eventsExist = true;
      }
    }

    if (timesheetUser != null) {
      if (loggedInUser.getId().equals(timesheetUser.getId()) == false && isOtherUsersAllowed() == false) {
        // Only project managers, controllers and administrative staff is allowed to subscribe time-sheets of other users.
        log.warn("User tried to get time-sheets of other user: " + timesheetUser);
        timesheetUser = loggedInUser;
      }
      // initializes timesheet filter
      final TimesheetFilter filter = new TimesheetFilter();
      filter.setUserId(timesheetUser.getId());
      filter.setDeleted(false);
      filter.setStopTime(cal.getTime());
      // calculates the offset of the calendar
      final int offset = cal.get(java.util.Calendar.MONTH) - PERIOD_IN_MONTHS;
      if (offset < 0) {
        setCalDate(cal, cal.get(java.util.Calendar.YEAR) - 1, 12 + offset);
      } else {
        setCalDate(cal, cal.get(java.util.Calendar.YEAR), offset);
      }
      filter.setStartTime(cal.getTime());

      final TimesheetDao timesheetDao = Registry.instance().getDao(TimesheetDao.class);
      final List<TimesheetDO> timesheetList = timesheetDao.getList(filter);

      // iterate over all timesheets and adds each event to the calendar
      for (final TimesheetDO timesheet : timesheetList) {
        final String uid = TeamCalConfig.get().createTimesheetUid(timesheet.getId());
        String summary;
        if (eventsExist == true) {
          summary = TimesheetEventsProvider.getTitle(timesheet) + " (ts)";
        } else {
          summary = TimesheetEventsProvider.getTitle(timesheet);
        }
        final VEvent vEvent = ICal4JUtils.createEvent(timesheet.getStartTime(), timesheet.getStopTime(), uid, summary);
        if (StringUtils.isNotBlank(timesheet.getLocation()) == true) {
          vEvent.getProperties().add(new Location(timesheet.getLocation()));
        }
        events.add(vEvent);
      }
    }

    return events;
  }

  /**
   * sets the calendar to a special date. Used to calculate the year offset of an negative time period. When the time period is set to 4
   * month and the current month is at the begin of a year, the year-number must be decremented by one
   * 
   * @param cal
   * @param year
   * @param mounth
   */
  private void setCalDate(final java.util.Calendar cal, final int year, final int mounth)
  {
    cal.clear();
    cal.set(java.util.Calendar.YEAR, year);
    cal.set(java.util.Calendar.MONTH, mounth);
  }

  private boolean isOtherUsersAllowed()
  {
    return UserRights.getAccessChecker().isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP,
        ProjectForgeGroup.PROJECT_MANAGER);
  }

  public static void registerFeedHook(final CalendarFeedHook hook)
  {
    feedHooks.add(hook);
  }

}
