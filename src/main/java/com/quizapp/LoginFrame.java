package com.quizapp;

import com.quizapp.admin.QuizAdminPanel;
import com.quizapp.db.Repository;
import com.quizapp.model.User;
import com.quizapp.ui.PlayerQuiz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private final JTextField userField;
    private final JPasswordField passField;
    private final JButton loginButton;
    private final JButton signupButton;

    public LoginFrame() {
        setTitle("QuizApp - Login");
        setSize(450, 350); // Increased size for better layout
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        add(mainPanel);

        // Header
        JLabel headerLabel = new JLabel("Welcome to QuizApp", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        // Username row
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        userField = new JTextField(20);
        userField.setFont(new Font("Arial", Font.PLAIN, 16));
        userPanel.add(userLabel);
        userPanel.add(userField);

        // Password row
        JPanel passPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        passField = new JPasswordField(20);
        passField.setFont(new Font("Arial", Font.PLAIN, 16));
        passPanel.add(passLabel);
        passPanel.add(passField);

        // Align labels
        userLabel.setPreferredSize(passLabel.getPreferredSize());

        formPanel.add(Box.createVerticalGlue());
        formPanel.add(userPanel);
        formPanel.add(passPanel);
        formPanel.add(Box.createVerticalGlue());

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        loginButton = new JButton("Login");
        stylePrimaryButton(loginButton);
        signupButton = new JButton("Sign Up");
        styleSecondaryButton(signupButton);

        buttonPanel.add(loginButton);
        buttonPanel.add(signupButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---

        // Add action listener for pressing Enter in password field
        passField.addActionListener(this::performLogin);
        loginButton.addActionListener(this::performLogin);

        signupButton.addActionListener(e -> {
            SignupDialog dialog = new SignupDialog(this);
            dialog.setVisible(true);
        });
    }

    private void stylePrimaryButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(new Color(60, 139, 219)); // Blue
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 20, 8, 20));
    }

    private void styleSecondaryButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(new Color(108, 117, 125)); // Gray
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 20, 8, 20));
    }

    /**
     * Handles the login logic, called by button or Enter key.
     */
    private void performLogin(ActionEvent e) {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password cannot be empty.");
            return;
        }

        // Disable UI during login attempt
        setUIEnabled(false);

        // Run authentication in a background thread
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return Repository.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user == null) {
                        showError("Invalid username or password.");
                        return;
                    }

                    // Login successful
                    if (user.isAdmin()) {
                        new QuizAdminPanel().setVisible(true);
                    } else {
                        new PlayerQuiz(user).setVisible(true);
                    }
                    LoginFrame.this.dispose(); // Close login window

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Database error: " + ex.getMessage());
                } finally {
                    // Re-enable UI regardless of outcome
                    setUIEnabled(true);
                }
            }
        }.execute();
    }

    private void setUIEnabled(boolean enabled) {
        userField.setEnabled(enabled);
        passField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        signupButton.setEnabled(enabled);
        loginButton.setText(enabled ? "Login" : "Logging in...");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Login Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * A proper dialog for user signup.
     */
    private static class SignupDialog extends JDialog {
        private final JTextField userField;
        private final JPasswordField passField;
        private final JPasswordField confirmPassField;

        SignupDialog(Frame owner) {
            super(owner, "Sign Up", true);
            setSize(400, 250);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10, 10));
            JPanel mainPanel = new JPanel();
            mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            userField = new JTextField(20);
            passField = new JPasswordField(20);
            confirmPassField = new JPasswordField(20);

            mainPanel.add(createFieldRow("Username:", userField));
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            mainPanel.add(createFieldRow("Password:", passField));
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            mainPanel.add(createFieldRow("Confirm Password:", confirmPassField));

            JButton signupButton = new JButton("Create Account");
            signupButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(signupButton);

            add(mainPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);

            signupButton.addActionListener(e -> performSignup());
        }

        private JPanel createFieldRow(String labelText, JComponent field) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel label = new JLabel(labelText);
            label.setPreferredSize(new Dimension(120, 25));
            panel.add(label);
            panel.add(field);
            return panel;
        }

        private void performSignup() {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            String confirmPassword = new String(confirmPassField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                showError("Username and password are required.");
                return;
            }
            if (!password.equals(confirmPassword)) {
                showError("Passwords do not match.");
                return;
            }

            // Run signup in background
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Repository.createUser(username, password, false);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Check for exceptions
                        showInfo("User created successfully. You can now login.");
                        dispose(); // Close signup dialog
                    } catch (Exception ex) {
                        if (ex.getMessage().contains("users_username_key")) {
                            showError("Failed to create user: Username already exists.");
                        } else {
                            ex.printStackTrace();
                            showError("Failed to create user: " + ex.getMessage());
                        }
                    }
                }
            }.execute();
        }
        
        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Signup Error", JOptionPane.ERROR_MESSAGE);
        }

        private void showInfo(String message) {
            JOptionPane.showMessageDialog(this, message, "Signup Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
