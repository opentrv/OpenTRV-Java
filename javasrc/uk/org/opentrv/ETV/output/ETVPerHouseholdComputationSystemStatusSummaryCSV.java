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

package uk.org.opentrv.ETV.output;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationResult;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationSystemStatus;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.SavingEnabledAndDataStatus;

/**Generate machine-readable (multi-line-CVS with header) form for a segmentation summary.
 * A header line is included.
 * <p>
 * Lines are terminated with '\n'.
 * <p>
 * Stateless.
 */
public final class ETVPerHouseholdComputationSystemStatusSummaryCSV
        implements Function<List<ETVPerHouseholdComputationSystemStatus>,String>
    {
    /**Produce simple CVS format "houseID,controlDays,normalDays" eg "12345,8,55"; no leading/terminating comma, never null. */
    @Override
    public String apply(final List<ETVPerHouseholdComputationSystemStatus> rl)
        {
        final ETVPerHouseholdComputationResultToCSV s = new ETVPerHouseholdComputationResultToCSV();
        final StringBuilder sb = new StringBuilder();
        sb.append(headerCSV).append('\n');
        for(final ETVPerHouseholdComputationSystemStatus r : rl)
            {
            final int fE = Collections.frequency(r.getOptionalEnabledAndUsableFlagsByLocalDay().values(), SavingEnabledAndDataStatus.Enabled);
            final int fD = Collections.frequency(r.getOptionalEnabledAndUsableFlagsByLocalDay().values(), SavingEnabledAndDataStatus.Disabled);
            sb.append(r.getHouseID()).append(',').append(fD).append(',').append(fE).append(',').append('\n');
            }
        return(sb.toString());
        }

    /**SCV header line. */
    public static final String headerCSV = "houseID,controlDays,normalDays";
    }
