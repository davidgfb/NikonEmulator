package com.nikonhacker.util;

//<editor-fold defaultstate="collapsed" desc="imports">
import static com.nikonhacker.Format.asHex;
import static com.nikonhacker.Format.parseUnsigned;
import com.nikonhacker.disassembly.ParsingException;

import java.io.FileInputStream;
import java.io.IOException;
import static java.lang.System.err;
import static java.lang.System.exit;
import static java.lang.System.out;
//</editor-fold>


public class BinCompare {
    //main
    /*
    public static void main(String[] args) throws IOException, ParsingException {
        FileInputStream fis2;
        
        if (args.length < 6) {
            err.println("Usage BinCompare <file1> <file2> <start1> <start2> <skip1> <skip2>");
            exit(-1);
        }
        
        try (FileInputStream fis1 = new FileInputStream(args[0])) {
            int start1 = parseUnsigned(args[2]),
                start2 = parseUnsigned(args[3]),
                skip1 = parseUnsigned(args[4]),
                skip2 = parseUnsigned(args[5]),
                i = 0;
            boolean condition = true;
            
            fis2 = new FileInputStream(args[1]);
            
            fis1.skip(skip1);
            fis2.skip(skip2);
            //break
            
            while(condition) {
                int b1 = fis1.read(),
                    b2 = fis2.read();
                
                if (b1 == -1) {
                    if (b2 == -1) {
                        out.println("Comparison complete");
                        break;
                    } else {
                        out.println("End of file1 reached");
                        break;
                    }
                }
                if (b2 == -1) {
                    out.println("End of file2 reached");
                    break;
                }
                if (b1 != b2) {
                    out.println("Difference : at 0x" + asHex(start1 + skip1 + i, 8) + " file1 : 0x" + asHex(b1, 2) + " - file2 - 0x" + asHex(b2, 2));
                }
                i++;
            }
        }
        fis2.close();
    }
    */
}
