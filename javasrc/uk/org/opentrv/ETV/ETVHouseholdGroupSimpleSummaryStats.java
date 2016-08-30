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
 * <dd>the original number of households before filtering</dd>
 * <dt>final households count</dt>
 * <dd>the final number of households in the result computation,
 *     after rejecting those with insufficient or poor data, etc</dd>
 * <dt>normal day count</dt>
 * <dd>the aggregate number of non-control days for which efficacy was been computed after all filtering</dd>
 * <dt>all households count</dt>
 * <dd>the original number of households before filtering</dd>
 * <dt>mean R^2</dt>
 * <dd>mean of R^2 value across final household set</dd>
 * <dt>mean efficacy</dt>
 * <dd>linear regression of kWh/HDD for space-heating fuel,
 *     ie the basic heating efficiency independent of any interventions</dd>
 * <dt>variance in efficacy</dt>
 * <dd>linear regression of kWh/HDD for space-heating fuel,
 *     ie the basic heating efficiency independent of any interventions</dd>
 *
 * <dt>TBD</dt>
 * <dl>
 */
public final class ETVHouseholdGroupSimpleSummaryStats
    {
    public static interface SummaryStats
        {
        /**The original number of households before filtering. */
        int getAllHouseholdCount();
        }

    /**Compute simple stats summary; never null. */
    public static SummaryStats computeSummaryStats(final int allHouseholdCount, final Collection<ETVPerHouseholdComputationResult> perHousehold)
        {
        if(null == perHousehold) { throw new IllegalArgumentException(); }
        if(allHouseholdCount < 0) { throw new IllegalArgumentException(); }
        if(allHouseholdCount < perHousehold.size()) { throw new IllegalArgumentException(); }

// TODO

        return(new SummaryStats(){
            @Override public int getAllHouseholdCount() { return(allHouseholdCount); }
// TODO
            });
        }
    }
