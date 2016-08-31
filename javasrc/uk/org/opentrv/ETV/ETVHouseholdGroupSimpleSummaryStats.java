/*
The OpenTRV project licenses this file to you
under the Apache Licence, Version 2.0 (the "Licence");
you may not use this file except in compliance
with the Licence. You may obtain a copy of the Licence at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the Licence is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the Licence for the
specific language governing permissions and limitations
under the Licence.

Author(s) / Copyright (s): Damon Hart-Davis 2016
*/

package uk.org.opentrv.ETV;

import java.util.Collection;

import org.apache.commons.math3.stat.StatUtils;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationResult;

/**Compute space-heat energy efficiency stats per ETV protocol for a group of households.
 * Typically used for a group of households in reasonable geographic proximity,
 * in one timezone and within the ambit of a single weather station for Heating Degree Days.
 * <p>
 * Typically used over one heating season,
 * or back-to-back heating seasons without significant changes in occupancy or heating season.
 * <p>
 * Reports on:
 * <dt>all households count</dt>
 * <dd>the original count of households before filtering</dd>
 * <dt>final households count</dt>
 * <dd>the final count of households in the result computation,
 *     after rejecting those with insufficient or poor data, etc</dd>
 * <dt>normal day count</dt>
 * <dd>the aggregate number of non-control days for which efficacy was been computed after all filtering</dd>
 * <dt>all households count</dt>
 * <dd>the original number of households before filtering</dd>
 * <dt>stats over R^2</dt>
 * <dd>stats over R^2 across final household set</dd>
 * <dt>stats over basic heating efficiency (kWh/HDD)</dt>
 * <dd>stats over linear regression of kWh/HDD for space-heating fuel,
 *     ie the basic heating efficiency independent of any interventions;
 *     SD expected to be relatively large even over similar-sized and located homes</dd>
 * <dt>stats over efficacy of energy-saving measures</dt>
 * <dd>stats over ratio of before and after enabling energy-saving measures
 *     in kWh/HDD basic efficiency</dd>
 *
 * <dt>TBD</dt>
 * <dl>
 * <p>
 * For Apache Commons Maths see:
 * http://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/index.html
 * <p>
 * For some lambda/stats trickery see:
 * http://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/stat/StatUtils.html
 * http://stackoverflow.com/questions/23079003/how-to-convert-a-java-8-stream-to-an-array
 * http://stackoverflow.com/questions/30125296/how-to-sum-a-list-of-integers-with-java-streams
 * ...
 */
public final class ETVHouseholdGroupSimpleSummaryStats
    {
    /**Concrete immutable implementation computing mean and variance over a set of inputs.
     * The population variance/SD is computed on the assumption that
     * data from the entire set of households with adequate data is used.
     */
    public static final class MeanAndPopSD
        {
        /**Count of data points; non-negative. */
        public final int n;
        /**Arithmetic mean of data points. */
        public final double mean;
        /**Population variance of data points. */
        public final double pVariance;
        /**Population standard deviation. */
        public final double pSD;
        /**Construct from data points.*/
        public MeanAndPopSD(final double data[])
            {
            if(null == data) { throw new IllegalArgumentException(); }
            n = data.length;
            mean = StatUtils.mean(data);
            pVariance = StatUtils.populationVariance(data, mean);
            pSD = Math.sqrt(pVariance);
            }
        }

    /**Interface providing standard simple summary stats across multiple households. */
    public static interface SummaryStats
        {
        /**The original count of households before filtering; non-negative. */
        int getAllHouseholdsCount();
        /**The final households count; non-negative and no greater than getAllHouseholdCount(). */
        int getFinalHouseholdsCount();
        /**The normal day count; non-negative. */
        int getNormalDayCount();
        /**Get standard stats over R^2; never null though elements may be NaN. */
        MeanAndPopSD getStatsOverRSquared();
        /**Get standard stats over kWh/HDD slope; never null though elements may be NaN. */
        MeanAndPopSD getStatsOverSlope();
        /**Get standard stats over efficacy; never null though elements may be NaN. */
        MeanAndPopSD getStatsOverEfficacy();
        }

    /**Compute simple stats summary; never null. */
    public static SummaryStats computeSummaryStats(final int allHouseholdCount, final Collection<ETVPerHouseholdComputationResult> perHousehold)
        {
        if(null == perHousehold) { throw new IllegalArgumentException(); }
        if(allHouseholdCount < 0) { throw new IllegalArgumentException(); }
        final int n = perHousehold.size();
        if(allHouseholdCount < n) { throw new IllegalArgumentException(); }

        // Count all 'enabled'/normal days.
        final int normalDayCount = perHousehold.stream().mapToInt(p -> p.getHDDMetrics().n).sum();

        // Compute standard stats sets.
        final MeanAndPopSD sRSquared = new MeanAndPopSD(perHousehold.stream().mapToDouble(p -> new Double(p.getHDDMetrics().rsqFit)).toArray());
        final MeanAndPopSD sSlope = new MeanAndPopSD(perHousehold.stream().mapToDouble(p -> new Double(p.getHDDMetrics().slopeEnergyPerHDD)).toArray());
        final MeanAndPopSD sEfficacy = new MeanAndPopSD(perHousehold.stream().mapToDouble(p -> new Double(p.getRatiokWhPerHDDNotSmartOverSmart())).toArray());

        return(new SummaryStats(){
            @Override public int getAllHouseholdsCount() { return(allHouseholdCount); }
            @Override public int getFinalHouseholdsCount() { return(n); }
            @Override public int getNormalDayCount() { return(normalDayCount); }
            @Override public MeanAndPopSD getStatsOverRSquared() { return(sRSquared); }
            @Override public MeanAndPopSD getStatsOverSlope() { return(sSlope); }
            @Override public MeanAndPopSD getStatsOverEfficacy() { return(sEfficacy); }
            });
        }
    }
