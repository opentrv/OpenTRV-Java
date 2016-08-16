package uk.org.opentrv.ETV.driver;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationInput;
import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationResult;
import uk.org.opentrv.ETV.ETVPerHouseholdComputationSimpleImpl;
import uk.org.opentrv.ETV.output.ETVPerHouseholdComputationResultsToCSV;
import uk.org.opentrv.ETV.parse.NBulkInputs;

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

    /**Name within output directory of basic per-household stats as ASCII7 CSV (no efficacy computation). */
    public static final String OUTPUT_FILE_BASIC_STATS = "basicStatsOut.csv";

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

    /**Process from specified input to output directories; sort result by house ID for consistency. */
    public static void doComputation(final File inDir, final File outDir) throws IOException
        {
        if(null == inDir) { throw new IllegalArgumentException(); }
        if(null == outDir) { throw new IllegalArgumentException(); }
        if(!inDir.isDirectory()) { throw new IOException("Cannot open input directory " + inDir); }
        if(!outDir.isDirectory()) { throw new IOException("Cannot open output directory " + outDir); }

        final Map<String, ETVPerHouseholdComputationInput> mhi =
                NBulkInputs.gatherDataForAllHouseholds(
                        () -> getReader(new File(inDir, INPUT_FILE_NKWH)),
                        getReader(new File(inDir, INPUT_FILE_HDD)));

        final List<ETVPerHouseholdComputationResult> rl = mhi.values().stream().map(ETVPerHouseholdComputationSimpleImpl.getInstance()).collect(Collectors.toList());
        Collections.sort(rl, new ETVPerHouseholdComputation.ResultSortByHouseID());
        final String rlCSV = (new ETVPerHouseholdComputationResultsToCSV()).apply(rl);
//      System.out.println(rlCSV);
//        assertEquals(
//              "\"house ID\",\"slope energy/HDD\",\"baseload energy\",\"R^2\",\"n\",\"efficiency gain if computed\"\n" +
//              "\"1234\",1.2,5.4,0.8,63,\n" +
//              "\"56\",7.89,0.1,0.6,532,1.23\n",
//              rlCSV);


        // TODO

        }
    }
