/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Main;

import static java.lang.System.exit;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 *
 * @author David
 */
public class UEHandler implements UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        exit(1);
    }
}
