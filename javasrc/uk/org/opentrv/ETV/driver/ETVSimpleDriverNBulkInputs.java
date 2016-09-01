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
package uk.org.opentrv.ETV.driver;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.org.opentrv.ETV.ETVHouseholdGroupSimpleSummaryStats;
import uk.org.opentrv.ETV.ETVHouseholdGroupSimpleSummaryStats.SummaryStats;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationInput;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationResult;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationSystemStatus;
import uk.org.opentrv.ETV.ETVPerHouseholdComputationSimpleImpl;
import uk.org.opentrv.ETV.filter.CommonSimpleResultFilters;
import uk.org.opentrv.ETV.filter.StatusSegmentation;
import uk.org.opentrv.ETV.output.ETVHouseholdGroupSimpleSummaryStatsToCSV;
import uk.org.opentrv.ETV.output.ETVPerHouseholdComputationResultsToCSV;
import uk.org.opentrv.ETV.output.ETVPerHouseholdComputationSystemStatusSummaryCSV;
import uk.org.opentrv.ETV.parse.NBulkInputs;
import uk.org.opentrv.ETV.parse.NBulkKWHParseByID;
import uk.org.opentrv.ETV.parse.OTLogActivityParse;
import uk.org.opentrv.hdd.HDDUtil;

/**Simple driver from N bulk and HDD data to output files.
 * An input directory is supplied,
 * and the data files are read from specific named files within that input directory.
 * <p>
 * An output directory is supplied
 * and the output files will be written to specific named files within that output directory.
 */
public final class ETVSimpleDriverNBulkInputs
    {
    /**Name within input directory of simple daily HDD CSV ASCII7 file. */
    public static final String INPUT_FILE_HDD = "HDD.csv";
    /**Name within input directory of 'N' format kWh energy consumption CSV ASCII7 file. */
    public static final String INPUT_FILE_NKWH = "NkWh.csv";
    /**Name within input directory of per-household system state CSV ASCII7 file. */
    public static final String INPUT_FILE_STATUS = "status.csv";

    /**Name within output directory of basic per-household stats as ASCII7 CSV (no efficacy computation). */
    public static final String OUTPUT_STATS_FILE_BASIC = "10_basicStatsOut.csv";
    /**Name within output directory of filtered basic per-household stats as ASCII7 CSV (no efficacy computation).
     * The filtering removes those entries that are outliers
     * or have inadequate data points or no/poor correlation.
     */
    public static final String OUTPUT_STATS_FILE_FILTERED_BASIC = "20_basicFilteredStatsOut.csv";
    /**Name within output directory of pre-segmented per-household stats as ASCII7 CSV.
     * This consists of one line per house of houseID,controlDays,normalDays.
     */
    public static final String OUTPUT_STATS_FILE_PRESEGMENTED = "30_presegmentedStatsOut.csv";
    /**Name within output directory of segmented per-household stats as ASCII7 CSV including efficacy computation.
     * The HDD metrics are from the normal state, with energy-saving features enabled.
     */
    public static final String OUTPUT_STATS_FILE_SEGMENTED = "31_segmentedStatsOut.csv";
    /**Name within output directory of household group simple summary stats as ASCII7 CSV including efficacy computation.
     * The HDD metrics are from the normal state, with energy-saving features enabled.
     */
    public static final String OUTPUT_STATS_FILE_MULITHOUSEHOLD_SUMMARY = "90_multihouseholdSummaryStatsOut.csv";

    /**Gets a reader for the specified file; no checked exceptions. */
    private static Reader getReader(final File f)
        {
        try { return(new FileReader(f)); }
        catch(final IOException e) { throw new RuntimeException(e); }
        }

    /**Trivial command-line front-end. */
    public static void main(final String args[])
        {
        if(args.length < 2) { throw new IllegalArgumentException(); }
        try { doComputation(new File(args[0]), new File(args[1])); }
        catch(final IOException e) { throw new RuntimeException(e); }
        }

    /**Process from specified input to output directories; sort result by house ID for consistency.
     * The input and output directories can be the same if required;
     * the file names for input and output are all distinct.
     * <p>
     * Efficacy computation will be attempted if log files are present.
     * <p>
     * Note: currently assumes N-format bulk energy data and timezone.
     * <p>
     * If this is unable to complete a run
     * eg because there is insufficient/unsuitable data
     * then it will terminating with an exception
     * having generated what outputs that it can.
     *
     * @param inDir  directory containing input files, must exist and be readable; never null
     * @param outDir  directory for output files, must exist and be writeable; never null
     * @throws IOException in case of difficulty
     */
    public static void doComputation(final File inDir, final File outDir) throws IOException
        {
        if(null == inDir) { throw new IllegalArgumentException(); }
        if(null == outDir) { throw new IllegalArgumentException(); }
        if(!inDir.isDirectory()) { throw new IOException("Cannot open input directory " + inDir); }
        if(!outDir.isDirectory()) { throw new IOException("Cannot open output directory " + outDir); }

        // Gather raw kWh and HDD data; savings-measure status Map is null.
        final Map<String, ETVPerHouseholdComputationInput> mhi =
                NBulkInputs.gatherDataForAllHouseholds(
                        () -> getReader(new File(inDir, INPUT_FILE_NKWH)),
                        getReader(new File(inDir, INPUT_FILE_HDD)));

        // Compute and output basic results, no efficacy.
        final ETVPerHouseholdComputationSimpleImpl computationInstance = ETVPerHouseholdComputationSimpleImpl.getInstance();
        final List<ETVPerHouseholdComputationResult> rlBasic = mhi.values().stream().map(computationInstance).collect(Collectors.toList());
        Collections.sort(rlBasic, new ETVPerHouseholdComputation.ResultSortByHouseID());
        final String rlBasicCSV = (new ETVPerHouseholdComputationResultsToCSV()).apply(rlBasic);
        final File basicResultFile = new File(outDir, ETVSimpleDriverNBulkInputs.OUTPUT_STATS_FILE_BASIC);
//System.out.println(rlCSV);
        // Write output...
        try(final FileWriter w = new FileWriter(basicResultFile)) { w.write(rlBasicCSV); }

        // Compute and output basic *filtered* results.
        final List<ETVPerHouseholdComputationResult> rlBasicFiltered = rlBasic.stream().filter(CommonSimpleResultFilters.goodDailyDataResults).collect(Collectors.toList());
        Collections.sort(rlBasicFiltered, new ETVPerHouseholdComputation.ResultSortByHouseID());
        final String rlBasicFilteredCSV = (new ETVPerHouseholdComputationResultsToCSV()).apply(rlBasicFiltered);
        final File basicFilteredResultFile = new File(outDir, ETVSimpleDriverNBulkInputs.OUTPUT_STATS_FILE_FILTERED_BASIC);
//System.out.println(rlCSV);
        // Write output...
        try(final FileWriter w = new FileWriter(basicFilteredResultFile)) { w.write(rlBasicFilteredCSV); }

        // Stop if no candidates left after filtering.
        // Probably an error.
        if(rlBasicFiltered.isEmpty())
            { throw new UnsupportedOperationException("No candidate households left after filtering."); }

        // Test if log data is available for segmentation, else stop.
        // Not an error.
        if(!(new File(inDir, OTLogActivityParse.LOGDIR_PATH_TO_GROUPING_CSV)).exists())
            {
            System.out.println("No grouping file in input dir, so no segmentation attempted: " + OTLogActivityParse.LOGDIR_PATH_TO_GROUPING_CSV);
            return;
            }

        // Segment, and look for changes in energy efficiency.
        final Set<String> stage1FilteredHouseIDs = rlBasicFiltered.stream().map(e -> e.getHouseID()).collect(Collectors.toSet());
        final Map<String, ETVPerHouseholdComputationSystemStatus> byHouseholdSegmentation = OTLogActivityParse.loadAndParseAllOTLogs(HDDUtil.getDirSmartFileReader(inDir), NBulkKWHParseByID.DEFAULT_NB_TIMEZONE, stage1FilteredHouseIDs);

        // Output pre-segmented results per household
        // to give an indication of which (don't) have enough control and non-control days
        final List<ETVPerHouseholdComputationSystemStatus> rlPresegmented = new ArrayList<>(byHouseholdSegmentation.values());
//        Collections.sort(rlPresegmented, new ETVPerHouseholdComputation.ResultSortByHouseID());
        final String rlPresegmentedCSV = (new ETVPerHouseholdComputationSystemStatusSummaryCSV()).apply(rlPresegmented);
        final File presegmentedResultFile = new File(outDir, ETVSimpleDriverNBulkInputs.OUTPUT_STATS_FILE_PRESEGMENTED);
        try(final FileWriter w = new FileWriter(presegmentedResultFile)) { w.write(rlPresegmentedCSV); }

        // Filter out households with too little control and normal data left.
        final List<ETVPerHouseholdComputationSystemStatus> enoughControlAndNormal = byHouseholdSegmentation.values().stream().filter(CommonSimpleResultFilters.enoughControlAndNormal).collect(Collectors.toList());

        // Stop if no candidates left after attempting to segment.
        // Probably an error.
        if(enoughControlAndNormal.isEmpty())
            { throw new UnsupportedOperationException("No candidate households left after attempting to segment."); }

//System.out.println(enoughControlAndNormal.iterator().next().getOptionalEnabledAndUsableFlagsByLocalDay());

        // Analyse segmented data per household.
        // Inject the per-day savings-measures status into the input,
        // and use the split analysis.
        final List<ETVPerHouseholdComputationResult> rlSegmented = new ArrayList<>();
        for(final ETVPerHouseholdComputationSystemStatus statusByID : enoughControlAndNormal)
            {
            final String houseID = statusByID.getHouseID();
            final ETVPerHouseholdComputationInput input = mhi.get(houseID);
            if(null == input) { throw new Error("should not happen"); }
            final ETVPerHouseholdComputationInput inputWithStatus =
                StatusSegmentation.injectStatusInfo(input, statusByID);
            final ETVPerHouseholdComputationResult reanalysed = computationInstance.apply(inputWithStatus);
//System.out.println(reanalysed.getRatiokWhPerHDDNotSmartOverSmart());
            rlSegmented.add(reanalysed);
            }
        // Output segmented results per household.
        Collections.sort(rlSegmented, new ETVPerHouseholdComputation.ResultSortByHouseID());
        final String rlSegmentedCSV = (new ETVPerHouseholdComputationResultsToCSV()).apply(rlSegmented);
        final File segmentedResultFile = new File(outDir, ETVSimpleDriverNBulkInputs.OUTPUT_STATS_FILE_SEGMENTED);
        try(final FileWriter w = new FileWriter(segmentedResultFile)) { w.write(rlSegmentedCSV); }

        // Analyse across groups of households, with confidence estimate.
        final SummaryStats summaryStats = ETVHouseholdGroupSimpleSummaryStats.computeSummaryStats(mhi.size(), rlSegmented);
        final String summaryCSV = (new ETVHouseholdGroupSimpleSummaryStatsToCSV()).apply(summaryStats);
        final File summaryResultFile = new File(outDir, ETVSimpleDriverNBulkInputs.OUTPUT_STATS_FILE_MULITHOUSEHOLD_SUMMARY);
        try(final FileWriter w = new FileWriter(summaryResultFile)) { w.write(summaryCSV); }


        // TODO

// Generate and write report(s).


        }
    }
