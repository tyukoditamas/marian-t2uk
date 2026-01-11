package org.app;

import org.app.excel.ExcelReader;
import org.app.ui.GeneratorFrame;
import org.app.xml.XmlGenerator;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GeneratorFrame frame = new GeneratorFrame(new ExcelReader(), new XmlGenerator());
            frame.setVisible(true);
        });
    }
}
