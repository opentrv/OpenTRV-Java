package uk.org.opentrv.ETV.driver;

import java.io.File;
import java.io.IOException;

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

    public static void doComputation(final File inDir, final File outDir) throws IOException
        {
        if(null == inDir) { throw new IllegalArgumentException(); }
        if(null == outDir) { throw new IllegalArgumentException(); }
        if(!inDir.isDirectory()) { throw new IOException("Cannot open input directory " + inDir); }
        if(!outDir.isDirectory()) { throw new IOException("Cannot open output directory " + outDir); }
// TODO

        }
    }
