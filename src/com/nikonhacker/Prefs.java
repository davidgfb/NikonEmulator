package com.nikonhacker;

//<editor-fold defaultstate="collapsed" desc="imports">
import static com.nikonhacker.ApplicationInfo.getName;
import static com.nikonhacker.Constants.CHIP_LABEL;
import static com.nikonhacker.Prefs.EepromInitMode.LAST_LOADED;
import com.nikonhacker.disassembly.CPUState;
import com.nikonhacker.disassembly.OutputOption;
import com.nikonhacker.disassembly.Register32;
import com.nikonhacker.disassembly.WriteListenerRegister32;
import com.nikonhacker.disassembly.tx.NullRegister32;
import com.nikonhacker.emu.AddressRange;
import com.nikonhacker.emu.EmulationFramework;
import com.nikonhacker.emu.EmulationFramework.ExecutionMode;
import static com.nikonhacker.emu.EmulationFramework.ExecutionMode.RUN;
import com.nikonhacker.emu.trigger.BreakTrigger;
import com.nikonhacker.gui.EmulatorUI;
import com.nikonhacker.gui.component.memoryHexEditor.MemoryWatch;
import com.thoughtworks.xstream.XStream;

import java.io.File;
import static java.io.File.separator;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import java.util.HashMap;
import java.util.List;
import java.util.EnumSet;
import java.util.Map;
import java.util.ArrayList;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.noneOf;
import java.util.Set;
//</editor-fold>

public class Prefs {
    
    //<editor-fold defaultstate="collapsed" desc="vars">
    public enum EepromInitMode {BLANK, 
                                PERSISTENT, 
                                LAST_LOADED}

    private static final String KEY_WINDOW_MAIN = "MAIN";

    // Common
    private String                          buttonSize            = EmulatorUI.getBUTTON_SIZE_SMALL(),
                                            frontPanelName;
    private boolean                         closeAllWindowsOnStop = false,
                                            dividerKeepHidden,
                                            syncPlay = true;
    private HashMap<String, WindowPosition> windowPositionMap,
                                            windowSizeMap;
    private int                             dividerLocation,
                                            lastDividerLocation;
    
    // Per chip
    private List<BreakTrigger>[]         triggers;
    private List<AddressRange>[]         disassemblyAddressRanges;
    private List<MemoryWatch>[]          memoryWatches;
    private EnumSet<OutputOption>[]      outputOptions;
    private String[]                     codeStructureGraphOrientation,
                                         firmwareFilename;
    private boolean[]                    autoUpdateITronObjectWindow,
                                         callStackHideJumps,
                                         writeDisassemblyToFile,
                                         sourceCodeFollowsPc,
                                         firmwareWriteProtected,
                                         dmaSynchronous,
                                         autoEnableTimers,
                                         logMemoryMessages,
                                         logSerialMessages,
                                         logPinMessages,
                                         logRegisterMessages,
                                         adValueFromList;
    private Map<String, List<Integer>>[] adValueListMap;
    private Map<String, Integer>[]       adValueMap,
                                         ioValueOverrideMap;
    private EepromInitMode               eepromInitMode;
    private byte[]                       lastEepromContents;
    private String                       lastEepromFileName;
    
    private int[]                        serialInterfaceFrameSelectedTab,
                                         genericSerialFrameSelectedTab,
                                         ioPortsFrameSelectedTab,
                                         sleepTick;
    private ExecutionMode[]              altExecutionModeForSyncedCpuUponDebug,
                                         altExecutionModeForSyncedCpuUponStep;
    private Map<String, Integer>         buttonsState;
    

    // Note: that field has a name that does not reflect its current use but it's kept for prefs backwards compatibility
    private int screenEmulatorRefreshIntervalMs;
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="getters">
    public static XStream getPrefsXStreamIn() {
        XStream xStream = XStreamUtils.getBaseXStream();
        xStream.omitField(BreakTrigger.class, "function");
        xStream.alias("wpos", WindowPosition.class);
        xStream.alias("r32", Register32.class);
        xStream.alias("nr32", NullRegister32.class);
        xStream.alias("wlr32", WriteListenerRegister32.class);
        xStream.useAttributeFor(Register32.class, "value");
        xStream.aliasField("v", Register32.class, "value");
        xStream.aliasField("r", CPUState.class, "regValue");
        return xStream;
    }

    public static XStream getPrefsXStream() {
        XStream xStream = getPrefsXStreamIn();
        xStream.omitField(Prefs.class, "altModeForSyncedCpuUponStep");
        xStream.omitField(Prefs.class, "altModeForSyncedCpuUponDebug");
        return xStream;
    }
    public String getButtonSize() {
        return buttonSize;
    }
    
    public int getSleepTick(int chip) {
        if (sleepTick == null || sleepTick.length != 2) {
            sleepTick = new int[2];
        }
        return sleepTick[chip];
    }
    
    public Set<OutputOption> getOutputOptions(int chip) {
        if (outputOptions == null) {
            outputOptions = new EnumSet[2];
        }
        if (outputOptions[chip]==null) {
            // Prepare a new outputOptions containing only default values
            outputOptions[chip] = noneOf(OutputOption.class);
            for (OutputOption option : allOf(OutputOption.class)) {
                if (option.isDefaultValue()) {
                    outputOptions[chip].add(option);
                }
            }
        }
        return outputOptions[chip];
    }
    
    public List<BreakTrigger> getTriggers(int chip) {
        if (triggers == null) {
            triggers = new List[2];
        }
        if (triggers[chip] == null) {
            triggers[chip] = new ArrayList<BreakTrigger>();
        }
        return triggers[chip];
    }
    
    public List<AddressRange> getDisassemblyAddressRange(int chip) {
        if (disassemblyAddressRanges == null) {
            disassemblyAddressRanges = new List[2];
        }
        if (disassemblyAddressRanges[chip] == null) {
            disassemblyAddressRanges[chip] = new ArrayList<AddressRange>();
        }
        return disassemblyAddressRanges[chip];
    }
    
    public List<MemoryWatch> getWatches(int chip) {
        if (memoryWatches == null) {
            memoryWatches = new List[2];
        }
        if (memoryWatches[chip] == null) {
            memoryWatches[chip] = new ArrayList<MemoryWatch>();
        }
        return memoryWatches[chip];
    }
    
    private String getKey(String windowName, int chip) {
        return windowName + "_" + CHIP_LABEL[chip];
    }
    
    public int getWindowPositionX(String windowName, int chip) {
        if (windowPositionMap==null) {
            windowPositionMap = new HashMap<String, WindowPosition>();
        }
        WindowPosition windowPosition = windowPositionMap.get(getKey(windowName, chip));
        return (windowPosition==null)?0:windowPosition.getX();
    }
    
    public int getWindowPositionY(String windowName, int chip) {
        if (windowPositionMap==null) {
            windowPositionMap = new HashMap<String, WindowPosition>();
        }
        WindowPosition windowPosition = windowPositionMap.get(getKey(windowName, chip));
        return (windowPosition==null)?0:windowPosition.getY();
    }
    
    public int getMainWindowPositionX() {
        if (windowPositionMap==null) {
            windowPositionMap = new HashMap<String, WindowPosition>();
        }
        WindowPosition windowPosition = windowPositionMap.get(KEY_WINDOW_MAIN);
        return (windowPosition==null)?0:windowPosition.getX();
    }
    
    public int getMainWindowPositionY() {
        if (windowPositionMap==null) {
            windowPositionMap = new HashMap<String, WindowPosition>();
        }
        WindowPosition windowPosition = windowPositionMap.get(KEY_WINDOW_MAIN);
        return (windowPosition==null)?0:windowPosition.getY();
    }
    
    public int getWindowSizeX(String windowName, int chip) {
        if (windowSizeMap==null) {
            windowSizeMap = new HashMap<String, WindowPosition>();
        }
        WindowPosition windowSize = windowSizeMap.get(getKey(windowName, chip));
        return (windowSize==null)?0:windowSize.getX();
    }

    public int getWindowSizeY(String windowName, int chip) {
        if (windowSizeMap==null) {
            windowSizeMap = new HashMap<String, WindowPosition>();
        }
        WindowPosition windowSize = windowSizeMap.get(getKey(windowName, chip));
        return (windowSize==null)?0:windowSize.getY();
    }
    
    public int getMainWindowSizeX() {
        if (windowSizeMap==null) {
            windowSizeMap = new HashMap<String, WindowPosition>();
        }
        WindowPosition windowSize = windowSizeMap.get(KEY_WINDOW_MAIN);
        return (windowSize==null)?0:windowSize.getX();
    }

    public int getMainWindowSizeY() {
        if (windowSizeMap==null) {
            windowSizeMap = new HashMap<String, WindowPosition>();
        }
        WindowPosition windowSize = windowSizeMap.get(KEY_WINDOW_MAIN);
        return (windowSize==null)?0:windowSize.getY();
    }
    
    public String getCodeStructureGraphOrientation(int chip) {
        if (codeStructureGraphOrientation == null || codeStructureGraphOrientation.length != 2) {
            codeStructureGraphOrientation = new String[2];
        }
        return codeStructureGraphOrientation[chip];
    }
    
    public int getDividerLocation() {
        return dividerLocation;
    }

    public int getLastDividerLocation() {
        return lastDividerLocation;
    }
    public int getSerialInterfaceFrameSelectedTab(int chip) {
        if (this.serialInterfaceFrameSelectedTab == null || this.serialInterfaceFrameSelectedTab.length != 2) {
            this.serialInterfaceFrameSelectedTab = new int[]{0, 0};
        }
        return serialInterfaceFrameSelectedTab[chip];
    }
    public int getGenericSerialFrameSelectedTab(int chip) {
        if (this.genericSerialFrameSelectedTab == null || this.genericSerialFrameSelectedTab.length != 2) {
            this.genericSerialFrameSelectedTab = new int[]{0, 0};
        }
        return genericSerialFrameSelectedTab[chip];
    }
    public int getIoPortsFrameSelectedTab(int chip) {
        if (this.ioPortsFrameSelectedTab == null || this.ioPortsFrameSelectedTab.length != 2) {
            this.ioPortsFrameSelectedTab = new int[]{0, 0};
        }
        return ioPortsFrameSelectedTab[chip];
    }
    public List<Integer> getAdValueList(int chip, String channelKey) {
        if (adValueListMap == null || adValueListMap.length != 2) {
            adValueListMap = new Map[]{new HashMap<String, ArrayList<Integer>>(), new HashMap<String, ArrayList<Integer>>()};
        }
        return adValueListMap[chip].get(channelKey);
    }
    public int getAdValue(int chip, String channelKey) {        
        Integer value = adValueMap[chip].get(channelKey);

        if (adValueMap == null || adValueMap.length != 2) {
            adValueMap = new Map[]{new HashMap<String, Integer>(), new HashMap<String, Integer>()};
        }
        return (value == null?0:value);
    }
    public EepromInitMode getEepromInitMode() {
        EepromInitMode initMode;
        if (eepromInitMode == null) {
            initMode= LAST_LOADED;
        }
        else {
            initMode= eepromInitMode;
        }
        return initMode;
    }
    public byte[] getLastEepromContents() {
        return lastEepromContents;
    }
    public String getLastEepromFileName() {
        return lastEepromFileName;
    }
    public Integer getPortInputValueOverride(int chip, int portNumber, int bitNumber) {
        if (ioValueOverrideMap == null || ioValueOverrideMap.length != 2) {
            ioValueOverrideMap = new Map[]{new HashMap<String, Integer>(), new HashMap<String, Integer>()};
        }
        return ioValueOverrideMap[chip].get(portNumber + "-" + bitNumber);
    }
    public EmulationFramework.ExecutionMode getAltExecutionModeForSyncedCpuUponDebug(int chip) {
        if (this.altExecutionModeForSyncedCpuUponDebug == null || this.altExecutionModeForSyncedCpuUponDebug.length != 2) {
            this.altExecutionModeForSyncedCpuUponDebug = new EmulationFramework.ExecutionMode[]{EmulationFramework.ExecutionMode.RUN, EmulationFramework.ExecutionMode.RUN};
        }
        return altExecutionModeForSyncedCpuUponDebug[chip];
    }
    
    public EmulationFramework.ExecutionMode getAltExecutionModeForSyncedCpuUponStep(int chip) {
        if (this.altExecutionModeForSyncedCpuUponStep == null || this.altExecutionModeForSyncedCpuUponStep.length != 2) {
            this.altExecutionModeForSyncedCpuUponStep = new EmulationFramework.ExecutionMode[]{EmulationFramework.ExecutionMode.RUN, EmulationFramework.ExecutionMode.RUN};
        }
        return altExecutionModeForSyncedCpuUponStep[chip];
    }
    
    public Integer getButtonState(String key) {
        if (buttonsState == null) {
            buttonsState = new HashMap<>();
        }
        return buttonsState.get(key);
    }

    public String getFirmwareFilename(int chip) {
        if (this.firmwareFilename == null || this.firmwareFilename.length != 2) {
            this.firmwareFilename = new String[2];
        }
        return firmwareFilename[chip];
    }
    public int getRefreshIntervalMs() {
        // Note: that field has a name that does not reflect its current use but it's kept for prefs backwards compatibility
        if (screenEmulatorRefreshIntervalMs < 10 || screenEmulatorRefreshIntervalMs > 10000) {
            screenEmulatorRefreshIntervalMs = 1000;
        }
        return screenEmulatorRefreshIntervalMs;
    }

    public String getFrontPanelName() {
        return frontPanelName;
    }
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="setters">
    public void setButtonSize(String buttonSize) {
        this.buttonSize = buttonSize;
    }
    
    public void setCloseAllWindowsOnStop(boolean closeAllWindowsOnStop) {
        this.closeAllWindowsOnStop = closeAllWindowsOnStop;
    }
    
    public void setWriteDisassemblyToFile(int chip, boolean value) {
        if (writeDisassemblyToFile == null || writeDisassemblyToFile.length != 2) {
            writeDisassemblyToFile = new boolean[2];
        }
        this.writeDisassemblyToFile[chip] = value;
    }
    
    public void setSourceCodeFollowsPc(int chip, boolean value) {
        if (sourceCodeFollowsPc == null || sourceCodeFollowsPc.length != 2) {
            sourceCodeFollowsPc = new boolean[]{true, true};
        }
        this.sourceCodeFollowsPc[chip] = value;
    }
    
    public void setSleepTick(int chip, int value) {
        if (sleepTick == null || sleepTick.length != 2) {
            sleepTick = new int[2];
        }
        this.sleepTick[chip] = value;
    }
    
    public void setOutputOption(int chip, OutputOption outputOption, boolean value) {
        getOutputOptions(chip);
        if (value) {
            outputOptions[chip].add(outputOption);
        }
        else {
            outputOptions[chip].remove(outputOption);
        }
    }
    
    public void setWindowPosition(String windowName, int chip, int x, int y) {
        if (windowPositionMap==null) {
            windowPositionMap = new HashMap<String, WindowPosition>();
        }
        windowPositionMap.put(getKey(windowName, chip), new WindowPosition(x, y));
    }

    public void setMainWindowPosition(int x, int y) {
        if (windowPositionMap==null) {
            windowPositionMap = new HashMap<String, WindowPosition>();
        }
        windowPositionMap.put(KEY_WINDOW_MAIN, new WindowPosition(x, y));
    }

    public void setWindowSize(String windowName, int chip, int x, int y) {
        if (windowSizeMap==null) {
            windowSizeMap = new HashMap<String, WindowPosition>();
        }
        windowSizeMap.put(getKey(windowName, chip), new WindowPosition(x, y));
    }

    public void setMainWindowSize(int x, int y) {
        if (windowSizeMap==null) {
            windowSizeMap = new HashMap<String, WindowPosition>();
        }
        windowSizeMap.put(KEY_WINDOW_MAIN, new WindowPosition(x, y));
    }

    public void setCodeStructureGraphOrientation(int chip, String value) {
        if (codeStructureGraphOrientation == null || codeStructureGraphOrientation.length != 2) {
            codeStructureGraphOrientation = new String[2];
        }
        this.codeStructureGraphOrientation[chip] = value;
    }

    public void setFirmwareWriteProtected(int chip, boolean isFirmwareWriteProtected) {
        if (firmwareWriteProtected == null || firmwareWriteProtected.length != 2) {
            firmwareWriteProtected = new boolean[2];
        }
        this.firmwareWriteProtected[chip] = isFirmwareWriteProtected;
    }
    
    public void setAutoUpdateITronObjects(int chip, boolean autoUpdateITronObjects) {
        if (autoUpdateITronObjectWindow == null) {
            autoUpdateITronObjectWindow = new boolean[2];
        }
        this.autoUpdateITronObjectWindow[chip] = autoUpdateITronObjects;
    }
    
    public void setCallStackHideJumps(int chip, boolean value) {
        if (callStackHideJumps == null) {
            callStackHideJumps = new boolean[2];
        }
        this.callStackHideJumps[chip] = value;
    }
    
    public void setDividerLocation(int dividerLocation) {
        this.dividerLocation = dividerLocation;
    }
    
    public void setLastDividerLocation(int lastDividerLocation) {
        this.lastDividerLocation = lastDividerLocation;
    }
    
    public void setDividerKeepHidden(boolean dividerKeepHidden) {
        this.dividerKeepHidden = dividerKeepHidden;
    }
    
    public void setDmaSynchronous(int chip, boolean isDmaSynchronous) {
        if (dmaSynchronous == null || dmaSynchronous.length != 2) {
            dmaSynchronous = new boolean[]{true, true};
        }
        this.dmaSynchronous[chip] = isDmaSynchronous;
    }
    
    public void setAutoEnableTimers(int chip, boolean isAutoEnableTimers) {
        if (autoEnableTimers == null || autoEnableTimers.length != 2) {
            autoEnableTimers = new boolean[]{true, true};
        }
        this.autoEnableTimers[chip] = isAutoEnableTimers;
    }

    public void setLogMemoryMessages(int chip, boolean isLogMemoryMessages) {
        if (logMemoryMessages == null || logMemoryMessages.length != 2) {
            logMemoryMessages = new boolean[]{false, false};
        }
        this.logMemoryMessages[chip] = isLogMemoryMessages;
    }
    
    public void setLogSerialMessages(int chip, boolean isLogSerialMessages) {
        if (logSerialMessages == null || logSerialMessages.length != 2) {
            logSerialMessages = new boolean[]{false, false};
        }
        this.logSerialMessages[chip] = isLogSerialMessages;
    }    
    
    public void setLogPinMessages(int chip, boolean isLogPinMessages) {
        if (logPinMessages == null || logPinMessages.length != 2) {
            logPinMessages = new boolean[]{true, true};
        }
        this.logPinMessages[chip] = isLogPinMessages;
    }
    public void setLogRegisterMessages(int chip, boolean isLogRegisterMessages) {
        if (logRegisterMessages == null || logRegisterMessages.length != 2) {
            logRegisterMessages = new boolean[]{true, true};
        }
        this.logRegisterMessages[chip] = isLogRegisterMessages;
    }
    public void setAdValueFromList(int chip, boolean isAdValueFromList) {
        if (adValueFromList == null || adValueFromList.length != 2) {
            adValueFromList = new boolean[]{true, true};
        }
        this.adValueFromList[chip] = isAdValueFromList;
    }
    public void setSerialInterfaceFrameSelectedTab(int chip, int serialInterfaceFrameSelectedTab) {
        if (this.serialInterfaceFrameSelectedTab == null || this.serialInterfaceFrameSelectedTab.length != 2) {
            this.serialInterfaceFrameSelectedTab = new int[]{0, 0};
        }
        this.serialInterfaceFrameSelectedTab[chip] = serialInterfaceFrameSelectedTab;
    }

    public void setGenericSerialFrameSelectedTab(int chip, int genericSerialFrameSelectedTab) {
        if (this.genericSerialFrameSelectedTab == null || this.genericSerialFrameSelectedTab.length != 2) {
            this.genericSerialFrameSelectedTab = new int[]{0, 0};
        }
        this.genericSerialFrameSelectedTab[chip] = genericSerialFrameSelectedTab;
    }
    public void setIoPortsFrameSelectedTab(int chip, int ioPortsFrameSelectedTab) {
        if (this.ioPortsFrameSelectedTab == null || this.ioPortsFrameSelectedTab.length != 2) {
            this.ioPortsFrameSelectedTab = new int[]{0, 0};
        }
        this.ioPortsFrameSelectedTab[chip] = ioPortsFrameSelectedTab;
    }
    public List<Integer> setAdValueList(int chip, String channelKey, List<Integer> values) {
        if (adValueListMap == null || adValueListMap.length != 2) {
            adValueListMap = new Map[]{new HashMap<String, ArrayList<Integer>>(), new HashMap<String, ArrayList<Integer>>()};
        }
        return adValueListMap[chip].put(channelKey, values);
    }
    
    public void setEepromInitMode(EepromInitMode eepromInitMode) {
        this.eepromInitMode = eepromInitMode;
    }
    public void setLastEepromContents(byte[] lastEepromContents) {
        this.lastEepromContents = lastEepromContents;
    }
    public void setLastEepromFileName(String lastEepromFileName) {
        this.lastEepromFileName = lastEepromFileName;
    }
    public void setSyncPlay(boolean syncPlay) {
        this.syncPlay = syncPlay;
    }
    
    public void setAltExecutionModeForSyncedCpuUponDebug(int chip, EmulationFramework.ExecutionMode altExecutionModeForSyncedCpuUponDebug) {
        if (this.altExecutionModeForSyncedCpuUponDebug == null || this.altExecutionModeForSyncedCpuUponDebug.length != 2) {
            this.altExecutionModeForSyncedCpuUponDebug = new EmulationFramework.ExecutionMode[]{EmulationFramework.ExecutionMode.RUN, EmulationFramework.ExecutionMode.RUN};
        }
        this.altExecutionModeForSyncedCpuUponDebug[chip] = altExecutionModeForSyncedCpuUponDebug;
    }
    
    public void setAltExecutionModeForSyncedCpuUponStep(int chip, EmulationFramework.ExecutionMode altExecutionModeForSyncedCpuUponStep) {
        if (this.altExecutionModeForSyncedCpuUponStep == null || this.altExecutionModeForSyncedCpuUponStep.length != 2) {
            this.altExecutionModeForSyncedCpuUponStep = new EmulationFramework.ExecutionMode[]{EmulationFramework.ExecutionMode.RUN, EmulationFramework.ExecutionMode.RUN};
        }
        this.altExecutionModeForSyncedCpuUponStep[chip] = altExecutionModeForSyncedCpuUponStep;
    }

    public void setButtonState(String key, Integer state) {
        if (buttonsState == null) {
            buttonsState = new HashMap<>();
        }
        buttonsState.put(key, state);
    }
    public void setFirmwareFilename(int chip, String firmwareFilename) {
        if (this.firmwareFilename == null || this.firmwareFilename.length != 2) {
            this.firmwareFilename = new String[2];
        }
        this.firmwareFilename[chip] = firmwareFilename;
    }
    public void setRefreshIntervalMs(int refreshIntervalMs) {
        // Note: that field has a name that does not reflect its current use but it's kept for prefs backwards compatibility
        this.screenEmulatorRefreshIntervalMs = refreshIntervalMs;
    }
    public void setFrontPanelName(String frontPanelName) {
        this.frontPanelName = frontPanelName;
    }
    
    public void setAdValue(int chip, String channelKey, int value) {
        if (adValueMap == null || adValueMap.length != 2) {
            adValueMap = new Map[]{new HashMap<String, Integer>(), new HashMap<String, Integer>()};
        }
        adValueMap[chip].put(channelKey, value);
    }
    public void setPortInputValueOverride(int chip, int portNumber, int bitNumber, int value) {
        if (ioValueOverrideMap == null || ioValueOverrideMap.length != 2) {
            ioValueOverrideMap = new Map[]{new HashMap<String, Integer>(), new HashMap<String, Integer>()};
        }
        ioValueOverrideMap[chip].put(portNumber + "-" + bitNumber, value);
    }
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="functions">
    private static File getPreferenceFile() {
        return new File(getProperty("user.home") + separator + "." + getName());
    }
    
    public static Prefs load() {
        File preferenceFile = getPreferenceFile(),
             lastKnownGoodFile = new File(preferenceFile.getAbsolutePath() + ".lastKnownGood"),
             corruptFile = new File(preferenceFile.getAbsolutePath() + ".corrupt");
        Prefs p = new Prefs();
        
        try {
            p= loadFile(preferenceFile, lastKnownGoodFile);
        }
        catch (IOException e) {
            out.println(e);
            out.println("Could not load preferences file. Attempting a rename to " + corruptFile.getName() + 
                        " and trying to revert to " + lastKnownGoodFile.getName() + " instead...");
            preferenceFile.renameTo(corruptFile);
            
            try {
                p= loadFile(lastKnownGoodFile, preferenceFile);
            } catch (IOException e1) {
                out.println(e);
                out.println("Could not load " + lastKnownGoodFile.getName() + ". Starting with a blank preference file...");
            }
        }
        return p;
    }
    private static Prefs loadFile(File file, File backupTargetFile) throws IOException {
        Prefs prefs = new Prefs();
        
        if (file.exists()) {
            XStream prefsXStream;
            
            try (FileInputStream inputStream = new FileInputStream(file)) {
                prefsXStream = getPrefsXStreamIn();
                prefs = (Prefs) XStreamUtils.load(inputStream, prefsXStream);
            }
            
            convertExecutionMode(prefs);
            
            if (backupTargetFile != null) {
                try ( // Parsing was OK. Back-up config
                    FileOutputStream outputStream = new FileOutputStream(backupTargetFile)) {
                    XStreamUtils.save(prefs, outputStream, prefsXStream);
                }
            }
        } 
        
        return prefs;
    }
    public boolean isCloseAllWindowsOnStop() {
        return closeAllWindowsOnStop;
    }

    public boolean isWriteDisassemblyToFile(int chip) {
        if (writeDisassemblyToFile == null  || writeDisassemblyToFile.length != 2) {
            writeDisassemblyToFile = new boolean[2];
        }
        return writeDisassemblyToFile[chip];
    }

    public boolean isSourceCodeFollowsPc(int chip) {
        if (sourceCodeFollowsPc == null || sourceCodeFollowsPc.length != 2) {
            sourceCodeFollowsPc = new boolean[]{true, true};
        }
        return sourceCodeFollowsPc[chip];
    }

    public boolean isFirmwareWriteProtected(int chip) {
        if (firmwareWriteProtected == null || firmwareWriteProtected.length != 2) {
            firmwareWriteProtected = new boolean[2];
        }
        return firmwareWriteProtected[chip];
    }

    public boolean isAutoUpdateITronObjects(int chip) {
        if (autoUpdateITronObjectWindow == null) {
            autoUpdateITronObjectWindow = new boolean[2];
        }
        return autoUpdateITronObjectWindow[chip];
    }

    public boolean isCallStackHideJumps(int chip) {
        if (callStackHideJumps == null) {
            callStackHideJumps = new boolean[2];
        }
        return callStackHideJumps[chip];
    }
    
    public boolean isDividerKeepHidden() {
        return dividerKeepHidden;
    }

    public boolean isDmaSynchronous(int chip) {
        if (dmaSynchronous == null || dmaSynchronous.length != 2) {
            dmaSynchronous = new boolean[]{true, true};
        }
        return dmaSynchronous[chip];
    }
    
    public boolean isAutoEnableTimers(int chip) {
        if (autoEnableTimers == null || autoEnableTimers.length != 2) {
            autoEnableTimers = new boolean[]{true, true};
        }
        return autoEnableTimers[chip];
    }
    
    public boolean isLogMemoryMessages(int chip) {
        if (logMemoryMessages == null || logMemoryMessages.length != 2) {
            logMemoryMessages = new boolean[]{false, false};
        }
        return logMemoryMessages[chip];
    }
    
    public boolean isLogSerialMessages(int chip) {
        if (logSerialMessages == null || logSerialMessages.length != 2) {
            logSerialMessages = new boolean[]{false, false};
        }
        return logSerialMessages[chip];
    }
    public boolean isLogPinMessages(int chip) {
        if (logPinMessages == null || logPinMessages.length != 2) {
            logPinMessages = new boolean[]{false, false};
        }
        return logPinMessages[chip];
    }
    public boolean isLogRegisterMessages(int chip) {
        if (logRegisterMessages == null || logRegisterMessages.length != 2) {
            logRegisterMessages = new boolean[]{true, true};
        }
        return logRegisterMessages[chip];
    }

    public boolean isAdValueFromList(int chip) {
        if (adValueFromList == null || adValueFromList.length != 2) {
            adValueFromList = new boolean[]{true, true};
        }
        return adValueFromList[chip];
    }
    
    public boolean isSyncPlay() {
        return syncPlay;
    }
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="procedimientos">
    public static void save(Prefs prefs) {
        try {
            XStreamUtils.save(prefs, new FileOutputStream(getPreferenceFile()), getPrefsXStream());
        } catch (FileNotFoundException e) {
            out.println(e);
        }
    }
    /** @deprecated this is a temporary migration process */
    private static void convertExecutionMode(Prefs prefs) {
        if (prefs.altExecutionModeForSyncedCpuUponDebug == null || prefs.altExecutionModeForSyncedCpuUponDebug.length != 2) {
            prefs.altExecutionModeForSyncedCpuUponDebug = new ExecutionMode[]{RUN, RUN};
        }
        if (prefs.altExecutionModeForSyncedCpuUponStep == null || prefs.altExecutionModeForSyncedCpuUponStep.length != 2) {
            prefs.altExecutionModeForSyncedCpuUponStep = new ExecutionMode[]{RUN, RUN};
        }
    }
    public void removePortInputValueOverride(int chip, int portNumber, int bitNumber) {
        if (ioValueOverrideMap == null || ioValueOverrideMap.length != 2) {
            ioValueOverrideMap = new Map[]{new HashMap<String, Integer>(), new HashMap<String, Integer>()};
        }
        ioValueOverrideMap[chip].remove(portNumber + "-" + bitNumber);
    }
//</editor-fold>
}
