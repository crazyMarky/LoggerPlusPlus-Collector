package com.nccgroup.loggerplusplus;

import com.coreyd97.BurpExtenderUtilities.PopOutPanel;
import com.nccgroup.loggerplusplus.about.AboutPanel;
import com.nccgroup.loggerplusplus.collector.CollectorController;
import com.nccgroup.loggerplusplus.collector.CollectorTab;
import com.nccgroup.loggerplusplus.help.HelpPanel;
import com.nccgroup.loggerplusplus.util.Globals;

import javax.swing.*;
import java.awt.*;

public class MainViewController {
    
    private final JTabbedPane tabbedPane;
    private final PopOutPanel popOutWrapper;
    private final CollectorController collectorController;

    public MainViewController(CollectorController collectorController) {
        this.collectorController = collectorController;
        this.tabbedPane = new JTabbedPane();
        LoggerPlusPlus loggerPlusPlus = LoggerPlusPlus.instance;
        tabbedPane.addTab("View Logs", null, loggerPlusPlus.getLogViewController().getLogViewPanel(), null);
        tabbedPane.addTab("Filter Library", null, loggerPlusPlus.getLibraryController().getFilterLibraryPanel(), null);
        tabbedPane.addTab("Grep Values", null, loggerPlusPlus.getGrepperController().getGrepperPanel(), null);
        tabbedPane.addTab("Options", null, loggerPlusPlus.getPreferencesController().getPreferencesPanel(), null);
        
        // Create and add Collector tab
        CollectorTab collectorTab = new CollectorTab(loggerPlusPlus.getPreferencesController().getPreferences(), collectorController);
        collectorController.setCollectorTab(collectorTab);
        tabbedPane.addTab("Collector", null, collectorTab, null);
        
        tabbedPane.addTab("About", null, new AboutPanel(loggerPlusPlus.getPreferencesController().getPreferences()), null);
        tabbedPane.addTab("Help", null, new HelpPanel(), null);
        this.popOutWrapper = new PopOutPanel(LoggerPlusPlus.montoya, tabbedPane, Globals.APP_NAME);
    }

    public Component getUiComponent() {
        return popOutWrapper;
    }

    public JTabbedPane getTabbedPanel(){
        return tabbedPane;
    }

    public PopOutPanel getPopOutWrapper() {
        return popOutWrapper;
    }
}
