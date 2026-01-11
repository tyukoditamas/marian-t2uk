package org.app.ui;

import org.app.excel.ExcelReader;
import org.app.model.ExcelData;
import org.app.xml.XmlGenerator;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

public class GeneratorFrame extends JFrame {
    private final ExcelReader excelReader;
    private final XmlGenerator xmlGenerator;

    private final JTextField excelField = new JTextField(28);
    private final JTextField lrnField = new JTextField(28);
    private final JButton generateButton = new JButton("Generate XML");
    private final JLabel statusLabel = new JLabel(" ");

    private File selectedFile;

    public GeneratorFrame(ExcelReader excelReader, XmlGenerator xmlGenerator) {
        super("T2 XML Generator");
        this.excelReader = excelReader;
        this.xmlGenerator = xmlGenerator;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(buildContent());
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel excelLabel = new JLabel("Excel file:");
        excelField.setEditable(false);
        JButton browseButton = new JButton("Browse...");

        JLabel lrnLabel = new JLabel("LRN:");

        generateButton.setEnabled(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(excelLabel, gbc);

        gbc.gridx = 1;
        panel.add(excelField, gbc);

        gbc.gridx = 2;
        panel.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(lrnLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(lrnField, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(generateButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        panel.add(statusLabel, gbc);

        Runnable updateGenerateState = () -> {
            boolean hasFile = selectedFile != null && selectedFile.isFile();
            boolean hasLrn = !lrnField.getText().trim().isEmpty();
            generateButton.setEnabled(hasFile && hasLrn);
        };

        DocumentListener validator = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateGenerateState.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateGenerateState.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateGenerateState.run();
            }
        };

        lrnField.getDocument().addDocumentListener(validator);

        browseButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Excel files (*.xls, *.xlsx)", "xls", "xlsx"));
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = chooser.getSelectedFile();
                excelField.setText(selectedFile.getAbsolutePath());
                statusLabel.setText(" ");
                updateGenerateState.run();
            }
        });

        generateButton.addActionListener(event -> handleGenerate());

        return panel;
    }

    private void handleGenerate() {
        if (selectedFile == null || !selectedFile.isFile()) {
            JOptionPane.showMessageDialog(this, "Please choose a valid Excel file.", "Missing file",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String lrn = lrnField.getText().trim();
        if (lrn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the LRN.", "Missing LRN",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            ExcelData data = excelReader.read(selectedFile);
            if (data.getItems().isEmpty()) {
                JOptionPane.showMessageDialog(this, "No valid rows found in the Excel file.", "No data",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            File outputFile = xmlGenerator.buildOutputFile(selectedFile.getParentFile());
            if (outputFile.exists()) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "File already exists:\n" + outputFile.getName() + "\nOverwrite?",
                        "Confirm overwrite", JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            xmlGenerator.generate(outputFile, lrn, data);
            statusLabel.setText("Created: " + outputFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "XML created:\n" + outputFile.getAbsolutePath(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
