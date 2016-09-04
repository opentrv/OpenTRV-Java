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

import java.util.function.Function;

import uk.org.opentrv.ETV.ETVHouseholdGroupSimpleSummaryStats.SummaryStats;

/**Generate machine-readable (single-line-CVS with header) form of simple summary stats.
 * A header line is included.
 * <p>
 * Lines are terminated with '\n'.
 * <p>
 * Stateless.
 */
public final class ETVHouseholdGroupSimpleSummaryStatsToCSV
        implements Function<SummaryStats,String>
    {
    /**Produce simple CVS format output with header row and one data row.
     * Precision is reduced to float for printing,
     * as anything beyond that is probably spurious.
     */
    @Override
    public String apply(final SummaryStats ss)
        {
        final StringBuilder sb = new StringBuilder();
        sb.append(headerCSV).append('\n');
        sb.append(ss.getAllHouseholdsCount()).append(',');
        sb.append(ss.getFinalHouseholdsCount()).append(',');
        sb.append(ss.getNormalDayCount()).append(',');
        sb.append((float) ss.getStatsOverRSquared().mean).append(',');
        sb.append((float) ss.getStatsOverRSquared().pSD).append(',');
        sb.append((float) ss.getStatsOverSlope().mean).append(',');
        sb.append((float) ss.getStatsOverSlope().pSD).append(',');
        sb.append((float) ss.getStatsOverEfficacy().mean).append(',');
        sb.append((float) ss.getStatsOverEfficacy().pSD);
            sb.append('\n');
        return(sb.toString());
        }

    /**CSV header row (no trailing line-ending). */
    public static final String headerCSV =
        "allHouseholdsCount,finalHouseholdsCount,normalDayCount,RsqMean,RsqSD,SlopeMean,SlopeSD,EfficacyMean,EfficacySD";
    }
