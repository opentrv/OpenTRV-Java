package uk.org.opentrv.ETV.driver;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.function.Supplier;

import uk.org.opentrv.ETV.ETVPerHouseholdComputation.ETVPerHouseholdComputationInput;
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






        // TODO

        }
    }
