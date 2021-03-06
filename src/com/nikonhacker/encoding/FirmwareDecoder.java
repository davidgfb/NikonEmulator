package com.nikonhacker.encoding;

//<editor-fold defaultstate="collapsed" desc="imports">
import java.io.File;
import java.io.IOException;
import static java.lang.System.exit;
import static java.lang.System.out;
import java.security.NoSuchAlgorithmException;
import java.util.List;
//</editor-fold>


public class FirmwareDecoder {

    private static void usage() {
        out.println("Usage : " + FirmwareDecoder.class.getName() + " <infile> [<destdir>] ");
        exit(1);
    }

    /*
    public static void main(String[] args) {
        if (args.length < 1 ) {
            usage();
        }
        String destDir;
        if (args.length < 2) {
            destDir = ".";
        }
        else {
            destDir = args[1];
        }
        try {
            new FirmwareDecoder().decode(args[0], destDir, true);
        } catch (FirmwareFormatException e) {
            e.printStackTrace();
            usage();
        }
        System.out.println("Operation complete.");

    }
    */

    public void decode(String sourceFileName, String unpackDirName, boolean ignoreCrcErrors) throws FirmwareFormatException {
        File sourceFile = new File(sourceFileName);
        if (!sourceFile.exists()) {
            throw new FirmwareFormatException("Source file does not exist");
        } else {
            try {
                byte[] source = FirmwareUtils.load(sourceFile);
                int type = FirmwareUtils.tryXor(source);
                if (type == 0) {
                    throw new FirmwareFormatException("Unknown file type !!!");
                }

                List<FirmwareFileEntry> fileEntries = FirmwareUtils.unpack(source, type);
                File unpackDir = new File(unpackDirName);
                unpackDir.mkdirs();
                for (FirmwareFileEntry fileEntry : fileEntries) {
                    int computedCrc = FirmwareUtils.computeChecksum(fileEntry.getBuffer(), fileEntry.getOffset(), fileEntry.getLength());
                    if (computedCrc != fileEntry.getCheckSum()) {
                        String msg = "Warning : checksum not OK for " + fileEntry.getFileName() + ". Computed=" + computedCrc + ", stored=" + fileEntry.getCheckSum();
                        if (ignoreCrcErrors) {
                            System.err.println(msg);
                        }
                        else {
                            throw new FirmwareFormatException(msg);
                        }
                    }
                    FirmwareUtils.dumpFile(new File(unpackDir, fileEntry.getFileName()), fileEntry.getBuffer(), fileEntry.getOffset(), fileEntry.getLength());
                }
            } catch (FirmwareFormatException | IOException | NoSuchAlgorithmException e) {
                out.println("e: "+e);
                throw new FirmwareFormatException(e);
            }
        }
    }
}
