package com.quizapp.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import com.quizapp.admin.editor.QuestionEditor;
import com.quizapp.db.Repository;
import com.quizapp.model.Question;
import java.util.ArrayList;

/**
 * Main Admin Panel for managing questions.
 * This class replaces the old QuestionManager and the simple QuizAdminPanel.
 */
public class QuizAdminPanel extends JFrame {
    
    private final QuestionTableModel tableModel;
    private final JTable table;
    private final JLabel statusBarLabel;

    public QuizAdminPanel() {
        setTitle("Quiz Admin Panel - Question Manager");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Don't exit, just dispose this window

        // Main panel with border layout and padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        setContentPane(mainPanel);

        // --- Header (North) ---
        JLabel headerLabel = new JLabel("Question Manager", SwingConstants.LEFT);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // --- Table (Center) ---
        tableModel = new QuestionTableModel();
        table = new JTable(tableModel);
        styleTable();
        
        JScrollPane scrollPane = new JScrollPane(table);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Button Panel (South) ---
        JPanel buttonPanel = new JPanel(new BorderLayout());
        
        // Status bar on the left
        statusBarLabel = new JLabel("Loading...");
        statusBarLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        statusBarLabel.setBorder(new EmptyBorder(10, 5, 10, 10));
        buttonPanel.add(statusBarLabel, BorderLayout.WEST);
        
        // Action buttons on the right
        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton addButton = new JButton("Add New Question");
        styleButton(addButton, new Color(40, 167, 69)); // Green
        
        JButton editButton = new JButton("Edit Selected");
        styleButton(editButton, new Color(255, 193, 7)); // Yellow
        
        JButton deleteButton = new JButton("Delete Selected");
        styleButton(deleteButton, new Color(220, 53, 69)); // Red

        actionButtonPanel.add(addButton);
        actionButtonPanel.add(editButton);
        actionButtonPanel.add(deleteButton);
        buttonPanel.add(actionButtonPanel, BorderLayout.EAST);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        addButton.addActionListener(e -> openEditor(-1));
        
        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                showError("Please select a question from the table to edit.");
                return;
            }
            // Convert view row to model row in case of sorting
            int modelRow = table.convertRowIndexToModel(selectedRow); 
            int questionId = (int) tableModel.getValueAt(modelRow, 0);
            openEditor(questionId);
        });

        deleteButton.addActionListener(e -> deleteSelectedQuestion());
        
        // --- Load Data ---
        loadQuestions();
    }

    private void styleTable() {
        table.setFont(new Font("Arial", Font.PLAIN, 16));
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(184, 207, 229));
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 18));
        header.setBackground(new Color(240, 240, 240));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
        
        // Set initial column widths
        table.getColumnModel().getColumn(0).setMaxWidth(100); // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(600); // Text
        table.getColumnModel().getColumn(2).setPreferredWidth(150); // Category
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // Difficulty
    }
    
    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 18, 8, 18));
    }

    /**
     * Loads questions from the repository using a background thread.
     */
    private void loadQuestions() {
        statusBarLabel.setText("Loading questions from database...");
        table.setEnabled(false);

        new SwingWorker<List<Question>, Void>() {
            @Override
            protected List<Question> doInBackground() throws Exception {
                return Repository.getAllQuestionsWithMeta();
            }

            @Override
            protected void done() {
                try {
                    List<Question> questions = get();
                    tableModel.setQuestions(questions);
                    statusBarLabel.setText(questions.size() + " questions loaded successfully.");
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Failed to load questions: " + e.getMessage());
                    statusBarLabel.setText("Error loading questions.");
                } finally {
                    table.setEnabled(true);
                }
            }
        }.execute();
    }

    /**
     * Opens the QuestionEditor dialog to add (id = -1) or edit a question.
     */
    private void openEditor(int questionId) {
        QuestionEditor editor = new QuestionEditor(this, questionId);
        // Load data in background *after* creating the dialog
        editor.loadDataAsync(); 
        editor.setVisible(true); // This will block until the dialog is closed
        
        // After dialog is closed, refresh the table
        loadQuestions();
    }

    /**
     * Handles deletion of the selected question.
     */
    private void deleteSelectedQuestion() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a question from the table to delete.");
            return;
        }
        
        int modelRow = table.convertRowIndexToModel(selectedRow);
        int questionId = (int) tableModel.getValueAt(modelRow, 0);
        String questionText = (String) tableModel.getValueAt(modelRow, 1);

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this question?\n\nID: " + questionId + "\nQuestion: " + questionText,
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            // Run deletion in background
            statusBarLabel.setText("Deleting question " + questionId + "...");
            table.setEnabled(false);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Repository.deleteQuestion(questionId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Check for exceptions
                        statusBarLabel.setText("Question " + questionId + " deleted.");
                        loadQuestions(); // Refresh the table
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Failed to delete question: " + e.getMessage());
                        statusBarLabel.setText("Error deleting question.");
                    } finally {
                        table.setEnabled(true);
                    }
                }
            }.execute();
        }
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // --- Custom Table Model ---
    
    static class QuestionTableModel extends AbstractTableModel {
        private final String[] columnNames = {"ID", "Question Text", "Category", "Difficulty"};
        private List<Question> questions = new ArrayList<>();

        public void setQuestions(List<Question> questions) {
            this.questions = questions;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return questions.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Question q = questions.get(rowIndex);
            switch (columnIndex) {
                case 0: return q.getId();
                case 1: return q.getText();
                case 2: return q.getCategory() == null ? "N/A" : q.getCategory();
                case 3: return q.getDifficulty() == null ? "N/A" : q.getDifficulty();
                default: return null;
            }
        }
        
        @Override
        public Class<?> getColumnClass(int c) {
            return (c == 0) ? Integer.class : String.class;
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
