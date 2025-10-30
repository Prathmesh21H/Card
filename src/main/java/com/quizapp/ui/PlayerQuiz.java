package com.quizapp.ui;

import com.quizapp.model.Question;
import com.quizapp.model.Option;
import com.quizapp.model.User;
import com.quizapp.db.Repository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Collections;

public class PlayerQuiz extends JFrame {

    private final User user;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;

    // UI Components
    private final JLabel titleLabel;
    private final JTextArea questionTextArea;
    private final JPanel optionsPanel;
    private final JButton nextButton;
    private final ButtonGroup optionsGroup;
    private final JPanel mainPanel;

    public PlayerQuiz(User user) {
        this.user = user;
        this.optionsGroup = new ButtonGroup();

        // --- Frame Setup ---
        setTitle("Quiz - Player: " + user.getUsername());
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        add(mainPanel);

        // --- Title (North) ---
        titleLabel = new JLabel("Loading Quiz...", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // --- Question Text (Center) ---
        questionTextArea = new JTextArea("Please wait while questions are loaded.");
        questionTextArea.setFont(new Font("Arial", Font.PLAIN, 22));
        questionTextArea.setWrapStyleWord(true);
        questionTextArea.setLineWrap(true);
        questionTextArea.setEditable(false);
        questionTextArea.setFocusable(false);
        questionTextArea.setOpaque(false); // Match background
        questionTextArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // --- Options Panel (Center) ---
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Center Panel to hold Question and Options
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(questionTextArea, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(optionsPanel), BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // --- Next Button (South) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        nextButton = new JButton("Next");
        nextButton.setFont(new Font("Arial", Font.BOLD, 18));
        nextButton.setBackground(new Color(40, 167, 69)); // Green
        nextButton.setForeground(Color.WHITE);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.setMargin(new Insets(8, 25, 8, 25));
        nextButton.setEnabled(false); // Disabled until questions load
        buttonPanel.add(nextButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        nextButton.addActionListener(e -> processAnswer());

        // --- Load Data ---
        loadQuizInBackground();
    }

    /**
     * Fetches questions from the DB on a background thread.
     */
    private void loadQuizInBackground() {
        // Show loading state
        mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Question>, Void>() {
            @Override
            protected List<Question> doInBackground() throws Exception {
                // Fetching all questions for now.
                // TODO: Allow category/difficulty selection
                return Repository.getQuestionsForPlayer(null, null);
            }

            @Override
            protected void done() {
                try {
                    questions = get();
                    if (questions == null || questions.isEmpty()) {
                        showError("No questions found in the database.", true);
                        return;
                    }
                    
                    Collections.shuffle(questions);
                    loadQuestion(); // Load the first question
                    nextButton.setEnabled(true);

                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Failed to load quiz: " + e.getMessage(), true);
                } finally {
                    mainPanel.setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    /**
     * Loads the question at the current index into the UI.
     */
    private void loadQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            finishQuiz();
            return;
        }

        Question q = questions.get(currentQuestionIndex);
        titleLabel.setText("Question " + (currentQuestionIndex + 1) + " of " + questions.size());
        questionTextArea.setText(q.getText());
        questionTextArea.setCaretPosition(0); // Scroll to top

        // Clear old options
        optionsPanel.removeAll();
        optionsGroup.clearSelection();

        // Add new options
        for (Option o : q.getOptions()) {
            JRadioButton rb = new JRadioButton("<html><body style='width: 500px;'>" + o.getText() + "</body></html>");
            rb.setFont(new Font("Arial", Font.PLAIN, 18));
            rb.setActionCommand(Boolean.toString(o.isCorrect())); // Store correctness
            rb.setOpaque(false);
            rb.setCursor(new Cursor(Cursor.HAND_CURSOR));
            optionsGroup.add(rb);
            optionsPanel.add(rb);
            optionsPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        }
        
        if (currentQuestionIndex == questions.size() - 1) {
            nextButton.setText("Finish");
        }

        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    /**
     * Checks the selected answer, updates score, and loads next question.
     */
    private void processAnswer() {
        ButtonModel selectedButton = optionsGroup.getSelection();
        
        if (selectedButton == null) {
            showError("Please select an answer.", false);
            return;
        }

        boolean isCorrect = Boolean.parseBoolean(selectedButton.getActionCommand());
        if (isCorrect) {
            score++;
        }

        currentQuestionIndex++;
        loadQuestion();
    }

    /**
     * Called when the quiz is finished. Saves score and shows results.
     */
    private void finishQuiz() {
        nextButton.setEnabled(false);
        
        // Save score in background
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // TODO: Pass actual category/difficulty
                Repository.saveScore(user.getId(), score, questions.size(), null, null);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for save errors
                } catch (Exception e) {
                    e.printStackTrace();
                    // Non-critical error, just log it
                    System.err.println("Failed to save score: " + e.getMessage());
                } finally {
                    // Always show score
                    String message = String.format("Quiz Finished!\nYour Score: %d / %d", score, questions.size());
                    JOptionPane.showMessageDialog(PlayerQuiz.this, message, "Quiz Complete", JOptionPane.INFORMATION_MESSAGE);
                    PlayerQuiz.this.dispose();
                }
            }
        }.execute();
    }

    private void showError(String message, boolean isFatal) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        if (isFatal) {
            this.dispose();
        }
    }
}
