package com.nikonhacker.disassembly.arm;

import com.nikonhacker.disassembly.CodeAnalyzer;
import com.nikonhacker.disassembly.CodeStructure;
import com.nikonhacker.disassembly.Range;
import com.nikonhacker.disassembly.Symbol;
import com.nikonhacker.disassembly.OutputOption;
import com.nikonhacker.disassembly.Function;
import com.nikonhacker.disassembly.Statement;
import com.nikonhacker.emu.memory.Memory;
import com.nikonhacker.Format;

import java.io.PrintWriter;
import java.util.SortedSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

public class ArmCodeAnalyzer extends CodeAnalyzer {

    public ArmCodeAnalyzer(CodeStructure codeStructure, SortedSet<Range> ranges, Memory memory, Map<Integer, Symbol> symbols, Map<Integer, List<Integer>> jumpHints, Set<OutputOption> outputOptions, PrintWriter debugPrintWriter) {
        super(codeStructure, ranges, memory, symbols, jumpHints, outputOptions, debugPrintWriter);
    }

    protected final int[] getJmpTableAddressSize(int address) {
        return null;
    }

    protected final List<Integer> getCallTableEntrys(Function currentFunction, int address, Statement statement) {
        debugPrintWriter.println("WARNING : Cannot determine dynamic target of CALL. Add -j 0x" + Format.asHex(address, 8) + "=addr1[, addr2[, ...]] to specify targets");
        return null;
    }
}