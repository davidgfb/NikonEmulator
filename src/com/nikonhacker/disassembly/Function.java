package com.nikonhacker.disassembly;

//<editor-fold defaultstate="collapsed" desc="imports">
import com.nikonhacker.Format;
import static com.nikonhacker.disassembly.Function.Type.STANDARD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//</editor-fold>


public class Function extends Symbol {

    public enum Type {/** The entry point */
                      MAIN, 
                      /** address referenced in interrupt vector */
                      INTERRUPT,
                      /** code reached by at least one static CALL */
                      STANDARD,
                      /** code not reached by any static CALL */
                      UNKNOWN}

    /**
     * List of Code Segments that compose the function
     */
    private List<CodeSegment> codeSegments;
    /**
     * List of jumps (JMP, Bcc) made from this function
     */
    private List<Jump> jumps;
    /**
     * List of calls to other functions made by this function
     */
    private List<Jump> calls;
    /**
     * List of calls to this function made by other functions
     * The map associates the call with the source function
     */
    private Map<Jump,Function> calledBy;

    /**
     * Function type
     * @see Type
     */
    private Type type = STANDARD;


    public Function(int address, String name, String comment, Type type) {
        super(address, name, comment);
        this.type = type;
        codeSegments = new ArrayList<CodeSegment>();
        jumps = new ArrayList<Jump>();
        calls = new ArrayList<Jump>();
        calledBy = new HashMap<Jump, Function>();
    }

    public Function(Symbol symbol) {
        super(symbol.address, symbol.name, symbol.comment);
    }


    public List<CodeSegment> getCodeSegments() {
        return codeSegments;
    }

    public List<Jump> getJumps() {
        return jumps;
    }

    public List<Jump> getCalls() {
        return calls;
    }

    public Map<Jump, Function> getCalledBy() {
        return calledBy;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getFillColor() {
        String fillColor="#AAAAAA";
        switch (type) {
            case INTERRUPT:
                fillColor= "#77FF77";
            case MAIN:
                fillColor= "#FFFF77";
            case STANDARD:
                fillColor= "#77FFFF";
        }
        return fillColor;
    }

    public String getBorderColor() {
        String borderColor = "#DDDDDD";
        switch (type) {
            case INTERRUPT:
                borderColor= "#007700";
            case MAIN:
                borderColor= "#777700";
            case STANDARD:
                borderColor= "#007777";
        }
        return borderColor;
    }

    @Override
    public String toString() {
        return getName() + "\n" + (getCalledBy().size()==0?"":("=> ")) + "0x" + Format.asHex(getAddress(), 8) + (getCalls().size()==0?"":(" =>"));
    }

    public String getSummary() {
        return getName() + " starting at 0x" + Integer.toHexString(getAddress()) + " [" + codeSegments.size() + " segment(s)]";
    }

    public String getTitleLine() {
        return name
                + "(" + (comment==null?"":comment) + ")"
                + (codeSegments.size()>1?(" [" + codeSegments.size() + " segment" + "s]"):"");
    }

}