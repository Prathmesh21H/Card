package com.quizapp.admin.editor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import com.quizapp.db.Repository;
import com.quizapp.model.Option;
import com.quizapp.model.Question;
import java.sql.SQLException;

public class QuestionEditor extends JDialog {

    private final int questionId;
    private boolean isLoading = true;

    // UI Components
    private final JTextArea questionTextArea;
    private final JComboBox<String> categoryComboBox;
    private final JComboBox<String> difficultyComboBox;
    private final JTextField[] optionFields = new JTextField[4];
    private final JRadioButton[] radioButtons = new JRadioButton[4];
    private final ButtonGroup optionsGroup = new ButtonGroup();
    private final JButton saveButton;

    public QuestionEditor(Frame owner, int questionId) {
        super(owner, "Question Editor", true);
        this.questionId = questionId;
        
        String title = (questionId == -1) ? "Add New Question" : "Edit Question (ID: " + questionId + ")";
        setTitle(title);

        setSize(750, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // Main form panel with padding
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // --- Question Text Area ---
        questionTextArea = new JTextArea(4, 40);
        questionTextArea.setFont(new Font("Arial", Font.PLAIN, 16));
        questionTextArea.setLineWrap(true);
        questionTextArea.setWrapStyleWord(true);
        formPanel.add(createFieldRow("Question:", new JScrollPane(questionTextArea)));
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- Category ComboBox ---
        categoryComboBox = new JComboBox<>();
        categoryComboBox.setFont(new Font("Arial", Font.PLAIN, 16));
        formPanel.add(createFieldRow("Category:", categoryComboBox));
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- Difficulty ComboBox ---
        difficultyComboBox = new JComboBox<>(new String[]{"easy", "medium", "hard"});
        difficultyComboBox.setFont(new Font("Arial", Font.PLAIN, 16));
        formPanel.add(createFieldRow("Difficulty:", difficultyComboBox));
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- Options Panel ---
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options (Select one as correct)"));
        
        for (int i = 0; i < 4; i++) {
            radioButtons[i] = new JRadioButton("Option " + (i + 1));
            radioButtons[i].setFont(new Font("Arial", Font.PLAIN, 16));
            optionsGroup.add(radioButtons[i]);
            
            optionFields[i] = new JTextField(35);
            optionFields[i].setFont(new Font("Arial", Font.PLAIN, 16));
            
            JPanel optionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            optionRow.add(radioButtons[i]);
            optionRow.add(optionFields[i]);
            optionsPanel.add(optionRow);
        }
        radioButtons[0].setSelected(true); // Default selection
        formPanel.add(optionsPanel);
        
        add(new JScrollPane(formPanel), BorderLayout.CENTER);

        // --- Save Button (South) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(0, 20, 10, 20));
        saveButton = new JButton("Save");
        saveButton.setFont(new Font("Arial", Font.BOLD, 18));
        saveButton.setBackground(new Color(40, 167, 69)); // Green
        saveButton.setForeground(Color.WHITE);
        saveButton.setMargin(new Insets(8, 25, 8, 25));
        saveButton.setEnabled(false); // Disabled until data is loaded
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        saveButton.addActionListener(e -> saveQuestion());
    }

    /**
     * Helper to create a standardized form row.
     */
    private JPanel createFieldRow(String labelText, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setPreferredSize(new Dimension(100, 30));
        panel.add(label, BorderLayout.WEST);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Loads categories and (if editing) question data in a background thread.
     */
    public void loadDataAsync() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<EditorData, Void>() {
            @Override
            protected EditorData doInBackground() throws Exception {
                // Always fetch categories
                List<String> categories = Repository.getAllCategoryNames();
                
                // If editing, fetch question details
                if (questionId != -1) {
                    Question q = Repository.getQuestionById(questionId);
                    List<Option> options = Repository.getOptionsForQuestion(questionId);
                    return new EditorData(categories, q, options);
                }
                
                return new EditorData(categories, null, null);
            }

            @Override
            protected void done() {
                try {
                    EditorData data = get();
                    
                    // Populate categories
                    categoryComboBox.removeAllItems();
                    if (data.categories != null) {
                        for (String cat : data.categories) {
                            categoryComboBox.addItem(cat);
                        }
                    }

                    // If editing, populate form fields
                    if (data.question != null && data.options != null) {
                        questionTextArea.setText(data.question.getText());
                        difficultyComboBox.setSelectedItem(data.question.getDifficulty());
                        categoryComboBox.setSelectedItem(data.question.getCategory());
                        
                        for (int i = 0; i < data.options.size() && i < 4; i++) {
                            Option opt = data.options.get(i);
                            optionFields[i].setText(opt.getText());
                            if (opt.isCorrect()) {
                                radioButtons[i].setSelected(true);
                            }
                        }
                    }
                    
                    isLoading = false;
                    saveButton.setEnabled(true);
                    saveButton.setText("Save");

                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Failed to load question data: " + e.getMessage());
                    // Close dialog if loading fails fatally
                    dispose();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    /**
     * Validates form and saves data to the repository.
     */
    private void saveQuestion() {
        if (isLoading) return;

        // --- Validate ---
        String questionText = questionTextArea.getText().trim();
        if (questionText.isEmpty()) {
            showError("Question text is required.");
            return;
        }

        List<Option> options = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String optionText = optionFields[i].getText().trim();
            if (optionText.isEmpty()) {
                showError("All four option fields are required.");
                return;
            }
            options.add(new Option(optionText, radioButtons[i].isSelected()));
        }

        String category = (String) categoryComboBox.getSelectedItem();
        String difficulty = (String) difficultyComboBox.getSelectedItem();
        
        if (category == null) {
            showError("A category must be selected.");
            return;
        }

        // --- Save (in background) ---
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (questionId == -1) {
                    Repository.addQuestion(questionText, options, category, difficulty);
                } else {
                    Repository.updateQuestion(questionId, questionText, options, category, difficulty);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    showInfo("Question saved successfully!");
                    dispose(); // Close dialog on success
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Failed to save question: " + ex.getMessage());
                    saveButton.setEnabled(true);
                    saveButton.setText("Save");
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Helper class to hold data fetched in the background worker.
     */
    private static class EditorData {
        final List<String> categories;
        final Question question;
        final List<Option> options;

        EditorData(List<String> categories, Question question, List<Option> options) {
            this.categories = categories;
            this.question = question;
            this.options = options;
        }
    }
}
