package com.nikonhacker.disassembly;

//<editor-fold defaultstate="collapsed" desc="imports">
import static java.lang.Integer.toHexString;
//</editor-fold>

public class CodeSegment {
    
    //<editor-fold defaultstate="collapsed" desc="vars">
    private int start,
                end;
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="constructors">
    public CodeSegment() {
    }

    public CodeSegment(int start, int end) {
        this.start = start;
        this.end = end;
    }
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="getters">
    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="setters">
    public void setStart(int start) {
        this.start = start;
    }
    
    public void setEnd(int end) {
        this.end = end;
    }
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="tostring">
    @Override
    public String toString() {
        return "Code Segment from 0x" + toHexString(start) + " to 0x" + toHexString(end);
    }
//</editor-fold>
}
