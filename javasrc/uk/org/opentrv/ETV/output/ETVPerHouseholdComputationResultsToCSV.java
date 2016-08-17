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

import java.util.List;
import java.util.function.Function;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationResult;

/**Generate machine-readable (multi-line-CVS with header) form for a result list.
 * A header line is included.
 * <p>
 * Lines are terminated with '\n'.
 * <p>
 * Stateless.
 */
public final class ETVPerHouseholdComputationResultsToCSV
        implements Function<List<ETVPerHouseholdComputationResult>,String>
    {
    /**Produce simple CVS format "house ID,slope,baseload,R^2,n,efficiency gain" eg "12345,1.2,3.5,0.73,156,1.1"; no leading/terminating comma, never null. */
    public String apply(List<ETVPerHouseholdComputationResult> rl)
        {
        final ETVPerHouseholdComputationResultToCSV s = new ETVPerHouseholdComputationResultToCSV();
        final StringBuilder sb = new StringBuilder();
        sb.append(ETVPerHouseholdComputationResultToCSV.headerCSV()).append('\n');
        for(final ETVPerHouseholdComputationResult r : rl)
            { sb.append(s.apply(r)).append('\n'); }
        return(sb.toString());
        }
    }
