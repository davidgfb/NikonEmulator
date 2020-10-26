/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nikonhacker;

/**
 *
 * @author David
 */

/**
 * This is basically just a structure with an X Y value.
 * Now used for position but also for size
 * java.awt.Point looks similar but has double as getters :-(. Moreover, it would break current config files
 */
//nueva clase
public class WindowPosition {

    //<editor-fold defaultstate="collapsed" desc="vars">
    int x = 0,
        y = 0;
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="constructor">
    public WindowPosition(int x, int y) {
        setX(x);
        setY(y); 
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="setters">
    private void setX(int x) {
        this.x = x;
    }

    private void setY(int y) {
        this.y = y;
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="getters">
    //necesario?
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
//</editor-fold>
}

