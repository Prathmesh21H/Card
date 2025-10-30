package com.quizapp;

import com.formdev.flatlaf.FlatDarculaLaf;
import javax.swing.*;

public class Launcher {
    public static void main(String[] args) {
        // Set the modern Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to initialize FlatLaf look and feel.");
            e.printStackTrace();
        }

        // Run the application on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
