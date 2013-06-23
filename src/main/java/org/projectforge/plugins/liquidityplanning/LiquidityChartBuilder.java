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

package org.projectforge.plugins.liquidityplanning;

import java.awt.Color;
import java.util.Calendar;

import org.apache.commons.lang.Validate;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.projectforge.calendar.DayHolder;
import org.projectforge.charting.XYChartBuilder;
import org.projectforge.user.PFUserContext;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LiquidityChartBuilder
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LiquidityChartBuilder.class);

  /**
   * @param forecast
   * @param nextDays
   * @return
   */
  public JFreeChart create(final LiquidityForecast forecast, final LiquidityForecastSettings settings)
  {
    Validate.isTrue(settings.getNextDays() > 0 && settings.getNextDays() < 500);

    final LiquidityForecastCashFlow cashFlow = new LiquidityForecastCashFlow(forecast, settings.getNextDays());

    final TimeSeries accumulatedSeries = new TimeSeries(PFUserContext.getLocalizedString("plugins.liquidityplanning.forecast.dueDate"));
    final TimeSeries accumulatedSeriesExpected = new TimeSeries(PFUserContext.getLocalizedString("plugins.liquidityplanning.forecast.expected"));
    final TimeSeries worstCaseSeries = new TimeSeries(PFUserContext.getLocalizedString("plugins.liquidityplanning.forecast.worstCase"));
    // final TimeSeries creditSeries = new TimeSeries("credits");
    // final TimeSeries debitSeries = new TimeSeries("debits");
    double accumulated = settings.getStartAmount().doubleValue();
    double accumulatedExpected = accumulated;
    double worstCase = accumulated;

    final DayHolder dh = new DayHolder();
    for (int i = 0; i < settings.getNextDays(); i++) {
      if (log.isDebugEnabled() == true) {
        log.debug("day: " + i + ", credits=" + cashFlow.getCredits()[i] + ", debits=" + cashFlow.getDebits()[i]);
      }
      final Day day = new Day(dh.getDayOfMonth(), dh.getMonth() + 1, dh.getYear());
      accumulated += cashFlow.getDebits()[i].doubleValue() + cashFlow.getCredits()[i].doubleValue();
      accumulatedSeries.add(day, accumulated);
      accumulatedExpected += cashFlow.getDebitsExpected()[i].doubleValue() + cashFlow.getCreditsExpected()[i].doubleValue();
      accumulatedSeriesExpected.add(day, accumulatedExpected);
      worstCase += cashFlow.getCredits()[i].doubleValue();
      worstCaseSeries.add(day, worstCase);
      // creditSeries.add(day, -credits);
      // debitSeries.add(day, debits);
      dh.add(Calendar.DATE, 1);
    }
    // final XYChartBuilder cb = new XYChartBuilder(ChartFactory.createXYBarChart(null, null, false, null, null, PlotOrientation.VERTICAL,
    // false, false, false));
    final XYChartBuilder cb = new XYChartBuilder(null, null, null, null, true);

    int counter = 0;

    // final TimeSeriesCollection cashflowSet = new TimeSeriesCollection();
    // cashflowSet.addSeries(debitSeries);
    // cashflowSet.addSeries(creditSeries);
    // final XYBarRenderer barRenderer = new XYBarRenderer(.5);
    // barRenderer.setSeriesPaint(0, cb.getGreenFill());
    // barRenderer.setSeriesPaint(1, cb.getRedFill());
    // barRenderer.setShadowVisible(false);
    // cb.setRenderer(counter, barRenderer).setDataset(counter++, cashflowSet);

    final TimeSeriesCollection xyDataSeries = new TimeSeriesCollection();
    xyDataSeries.addSeries(accumulatedSeries);
    xyDataSeries.addSeries(worstCaseSeries);
    final XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
    lineRenderer.setSeriesPaint(0, Color.BLACK);
    // lineRenderer.setSeriesStroke(0, cb.getDashedStroke());
    lineRenderer.setSeriesVisibleInLegend(0, true);
    lineRenderer.setSeriesPaint(1, cb.getGrayMarker());
    lineRenderer.setSeriesStroke(1, cb.getDashedStroke());
    lineRenderer.setSeriesVisibleInLegend(1, true);
    cb.setRenderer(counter, lineRenderer).setDataset(counter++, xyDataSeries);

    final TimeSeriesCollection accumulatedSet = new TimeSeriesCollection();
    accumulatedSet.addSeries(accumulatedSeriesExpected);
    final XYDifferenceRenderer diffRenderer = new XYDifferenceRenderer(cb.getGreenFill(), cb.getRedFill(), true);
    diffRenderer.setSeriesPaint(0, cb.getRedMarker());
    cb.setRenderer(counter, diffRenderer).setDataset(counter++, accumulatedSet)
    .setStrongStyle(diffRenderer, false, accumulatedSeriesExpected);
    diffRenderer.setSeriesVisibleInLegend(0, true);

    cb.setDateXAxis(true).setYAxis(true, null);
    return cb.getChart();
  }
}
