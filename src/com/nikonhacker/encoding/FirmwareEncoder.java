package com.nikonhacker.encoding;

//<editor-fold defaultstate="collapsed" desc="imports">
import java.io.File;
import static java.lang.System.exit;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.List;
//</editor-fold>


public class FirmwareEncoder {
    private static void usage() {
        out.println("Usage : " + FirmwareEncoder.class.getName() + " <infile1> [ <infile2> [...]] <outfile> ");
        exit(1);
    }

    /*
    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
        }

        String outFilename = args[args.length - 1];
        List<String> inFilenames = new ArrayList<String>();
        inFilenames.addAll(Arrays.asList(args).subList(0, args.length - 1));

        try {
            new FirmwareEncoder().encode(inFilenames, outFilename);
        } catch (FirmwareFormatException e) {
            System.err.println(e.getMessage());
            usage();
        }
        System.out.println("Operation complete.");

    }
    */

    public void encode(List<String> inputFilenames, String outFilename) throws FirmwareFormatException {
        try {
            File outFile = new File(outFilename),
                 outDir = outFile.getParentFile();
            if (outDir!=null)
                outDir.mkdirs();

            List<FirmwareFileEntry> entries = new ArrayList<FirmwareFileEntry>();
            for (String inputFilename : inputFilenames) {
                File f = new File(inputFilename);
                if (!f.exists()) {
                    throw new FirmwareFormatException("File '" + inputFilename + "' does not exist");
                }
                byte[] b = FirmwareUtils.load(f);
                entries.add(new FirmwareFileEntry(f.getName(), b, 0, (int) f.length(), FirmwareUtils.computeChecksum(b, 0, (int) f.length())));
            }

            byte[] packed = FirmwareUtils.pack(entries);
            byte[] encrypted = FirmwareUtils.xor(packed, FirmwareUtils.STANDARD);

            FirmwareUtils.dumpFile(outFile, encrypted, 0, encrypted.length);

        } catch (Exception e) {
            throw new FirmwareFormatException(e);
        }
    }
}