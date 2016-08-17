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

import java.io.IOException;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.function.Function;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationInput;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationResult;
import uk.org.opentrv.hdd.Util.HDDMetrics;

/**Compute space-heat energy efficiency change per ETV protocol for one household; supports lambdas.
 * Typically used over one heating season,
 * or back-to-back heating seasons without significant changes in occupancy or heating season.
 * <p>
 * Reports on the change in slope of a linear regression of kWh/HDD for space-heating fuel.
 * <p>
 * Note:
 * <ul>
 * <li>There must be no significant secondary heating.</li>
 * <li>The heating fuel can also be used for other purposed,
 *     eg gas for cooking and DHW (domestic hot water) as well as space heating.</li>
 * </ul>
 */
public interface ETVPerHouseholdComputation
    extends Function<ETVPerHouseholdComputationInput, ETVPerHouseholdComputationResult>
    {
    public enum SavingEnabledAndDataStatus { Enabled, Disabled, DontUse };

    /**Get heating fuel energy consumption (kWh) by whole local days (local midnight-to-midnight).
     * Days may not be contiguous and the result may be empty.
     */
    public interface ETVPerHouseholdComputationInputKWh
        {
        /**Interval heating fuel consumption (kWh) by whole local days; never null.
         * @return  never null though may be empty
         * @throws IOException  in case of failure, eg parse problems
         */
        SortedMap<Integer, Float> getKWhByLocalDay() throws IOException;
        }

    /**Get Heating Degree Days (HDD, Celsius) by whole local days (local midnight-to-midnight).
     * Days may not be contiguous and the result may be empty.
     */
    public interface ETVPerHouseholdComputationInputHDD
        {
        /**Heating Degree Days (HDD, Celsius) by whole local days; never null.
         * Uses values for either a 'standard' base temperature (typically 15.5C)
         * or per-household value determined in other ways, eg by best-fit.
         *
         * @return  never null though may be empty
         * @throws IOException  in case of failure, eg parse problems
         */
        SortedMap<Integer, Float> getHDDByLocalDay() throws IOException;

        /**Get base temperature for this data set as float; never Inf, may be NaN if unknown or not constant. */
        float getBaseTemperatureAsFloat();
        }

    /**Abstract input for running the computation for one household.
     * This should have an implementation that is backed by
     * plain-text CSV input data files,
     * though these may need filtering, transforming, and cross-referencing.
     */
    public interface ETVPerHouseholdComputationInput
        extends ETVPerHouseholdComputationInputKWh, ETVPerHouseholdComputationInputHDD
        {
        /**Get unique house ID as alphanumeric String; never null. */
        String getHouseID();
        // TO BE DOCUMENTED
        SortedMap<Integer, SavingEnabledAndDataStatus> getOptionalEnabledAndUsableFlagsByLocalDay();
        TimeZone getLocalTimeZoneForKWhAndHDD();
        SortedMap<Long, String> getOptionalJSONStatsByUTCTimestamp();
        SortedMap<String, Boolean> getJSONStatusValveElseBoilerControlByID();
        }

    /**Result of running the computation for one household.
     * There should be an implementation that can write to
     * plain-text CSV output file(s).
     */
    public interface ETVPerHouseholdComputationResult
        {
        /**Get unique house ID as alphanumeric String; never null. */
        String getHouseID();
        /**Return HDD metrics; null if not computable. */
        HDDMetrics getHDDMetrics();
        /**Return energy efficiency improvement (more than 1.0 is good), +ve, null if not computable. */
        Float getRatiokWhPerHDDNotSmartOverSmart();
        }

    /**Helper sort class for ETVPerHouseholdComputationResult (by house ID). */
    public static final class ResultSortByHouseID implements Comparator<ETVPerHouseholdComputationResult>
        {
        @Override
        public int compare(final ETVPerHouseholdComputationResult o1, final ETVPerHouseholdComputationResult o2)
            { return(o1.getHouseID().compareTo(o2.getHouseID())); }
        }

    /**Convert the input data to the output result; never null. */
    @Override
    ETVPerHouseholdComputationResult apply(ETVPerHouseholdComputationInput in)
        throws IllegalArgumentException;
    }
