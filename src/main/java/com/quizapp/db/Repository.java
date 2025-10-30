package com.quizapp.db;

import com.quizapp.model.User;
import com.quizapp.model.Question;
import com.quizapp.model.Option;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.InputStream;

public class Repository {
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASS;

    static {
        // Load database configuration from properties file
        try (InputStream in = Repository.class.getResourceAsStream("/application.properties")) {
            if (in == null) {
                throw new RuntimeException("Cannot find application.properties on the classpath");
            }
            Properties p = new Properties();
            p.load(in);
            DB_URL = System.getProperty("db.url", p.getProperty("db.url"));
            DB_USER = System.getProperty("db.user", p.getProperty("db.user"));
            DB_PASS = System.getProperty("db.pass", p.getProperty("db.pass"));
        } catch (Exception e) {
            System.err.println("Warning: Could not load application.properties. Using default fallback credentials.");
            e.printStackTrace();
            DB_URL = System.getProperty("db.url", "jdbc:postgresql://localhost:5432/quiz_db");
            DB_USER = System.getProperty("db.user", "postgres");
            DB_PASS = System.getProperty("db.pass", "password");
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // --- Authentication ---

    public static User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT id, username, password_hash, is_admin FROM users WHERE username = ?";
        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    String hash = r.getString("password_hash");
                    // Check password
                    if (BCrypt.checkpw(password, hash)) {
                        return new User(r.getInt("id"), r.getString("username"), r.getBoolean("is_admin"));
                    }
                }
            }
        }
        return null;
    }

    public static void createUser(String username, String password, boolean isAdmin) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, is_admin) VALUES (?, ?, ?)";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, hash);
            p.setBoolean(3, isAdmin);
            p.executeUpdate();
        }
    }
    
    // --- Category Management ---

    public static List<String> getAllCategoryNames() throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT name FROM categories ORDER BY name";
        try (Connection c = getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                categories.add(r.getString("name"));
            }
        }
        return categories;
    }

    /**
     * Helper to get a category ID from its name.
     */
    private static Integer getCategoryIdByName(Connection c, String name) throws SQLException {
        String sql = "SELECT id FROM categories WHERE name = ?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, name);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    return r.getInt("id");
                }
            }
        }
        return null; // Or throw exception
    }

    // --- Player Quiz ---

    /**
     * [OPTIMIZED] Fetches questions and options for the player in a single query.
     * Fixes the N+1 query problem.
     */
    public static List<Question> getQuestionsForPlayer(Integer categoryId, String difficulty) throws SQLException {
        Map<Integer, Question> questionMap = new HashMap<>();
        
        // Base SQL with JOIN to get questions and options
        StringBuilder sql = new StringBuilder(
            "SELECT q.id, q.question_text, o.option_text, o.is_correct " +
            "FROM questions q " +
            "JOIN options o ON q.id = o.question_id "
        );
        
        // Build WHERE clause dynamically
        List<Object> params = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder();

        if (categoryId != null) {
            whereClause.append("q.category_id = ?");
            params.add(categoryId);
        }
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            if (whereClause.length() > 0) whereClause.append(" AND ");
            whereClause.append("q.difficulty = ?");
            params.add(difficulty);
        }

        if (whereClause.length() > 0) {
            sql.append(" WHERE ").append(whereClause);
        }
        sql.append(" ORDER BY q.id"); // Order is crucial for mapping logic

        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql.toString())) {
            
            // Set dynamic parameters
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    int id = r.getInt("id");
                    Question q = questionMap.get(id);
                    
                    // If first time seeing this question, create it
                    if (q == null) {
                        q = new Question(id, r.getString("question_text"));
                        questionMap.put(id, q);
                    }
                    
                    // Add the option to this question
                    q.addOption(new Option(r.getString("option_text"), r.getBoolean("is_correct")));
                }
            }
        }
        return new ArrayList<>(questionMap.values());
    }

    public static void saveScore(int userId, int score, int total, Integer categoryId, String difficulty) throws SQLException {
        String sql = "INSERT INTO scores (user_id, score, total, category_id, difficulty) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, userId);
            p.setInt(2, score);
            p.setInt(3, total);
            if (categoryId != null) p.setInt(4, categoryId);
            else p.setNull(4, Types.INTEGER);
            p.setString(5, difficulty);
            p.executeUpdate();
        }
    }

    // --- Admin Panel - Question Management ---

    /**
     * [NEW] Gets all questions with category and difficulty for the admin table.
     */
    public static List<Question> getAllQuestionsWithMeta() throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT q.id, q.question_text, c.name AS category_name, q.difficulty " +
                     "FROM questions q " +
                     "LEFT JOIN categories c ON q.category_id = c.id " +
                     "ORDER BY q.id";
        
        try (Connection c = getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            
            while (r.next()) {
                Question q = new Question(r.getInt("id"), r.getString("question_text"));
                q.setCategory(r.getString("category_name"));
                q.setDifficulty(r.getString("difficulty"));
                questions.add(q);
            }
        }
        return questions;
    }

    /**
     * [NEW] Gets a single question's metadata (no options) by ID.
     */
    public static Question getQuestionById(int questionId) throws SQLException {
        String sql = "SELECT q.id, q.question_text, c.name AS category_name, q.difficulty " +
                     "FROM questions q " +
                     "LEFT JOIN categories c ON q.category_id = c.id " +
                     "WHERE q.id = ?";
        
        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, questionId);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    Question q = new Question(r.getInt("id"), r.getString("question_text"));
                    q.setCategory(r.getString("category_name"));
                    q.setDifficulty(r.getString("difficulty"));
                    return q;
                }
            }
        }
        return null; // Not found
    }

    /**
     * [NEW] Gets all options for a specific question ID.
     */
    public static List<Option> getOptionsForQuestion(int questionId) throws SQLException {
        List<Option> options = new ArrayList<>();
        String sql = "SELECT option_text, is_correct FROM options WHERE question_id = ? ORDER BY id";
        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, questionId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    options.add(new Option(r.getString("option_text"), r.getBoolean("is_correct")));
                }
            }
        }
        return options;
    }
    
    /**
     * [NEW] Adds a new question and its options in a transaction.
     */
    public static void addQuestion(String questionText, List<Option> options, String categoryName, String difficulty) throws SQLException {
        String sqlQuestion = "INSERT INTO questions (question_text, category_id, difficulty) VALUES (?, ?, ?)";
        String sqlOption = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?)";
        
        Connection c = null;
        try {
            c = getConnection();
            c.setAutoCommit(false); // Start transaction
            
            // Get category ID
            Integer categoryId = getCategoryIdByName(c, categoryName);
            if (categoryId == null) {
                throw new SQLException("Invalid category name: " + categoryName);
            }

            // Insert question and get generated ID
            int newQuestionId;
            try (PreparedStatement p = c.prepareStatement(sqlQuestion, Statement.RETURN_GENERATED_KEYS)) {
                p.setString(1, questionText);
                p.setInt(2, categoryId);
                p.setString(3, difficulty);
                p.executeUpdate();
                try (ResultSet rs = p.getGeneratedKeys()) {
                    if (rs.next()) {
                        newQuestionId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to create question, no ID obtained.");
                    }
                }
            }
            
            // Insert options
            try (PreparedStatement p = c.prepareStatement(sqlOption)) {
                for (Option opt : options) {
                    p.setInt(1, newQuestionId);
                    p.setString(2, opt.getText());
                    p.setBoolean(3, opt.isCorrect());
                    p.addBatch();
                }
                p.executeBatch();
            }
            
            c.commit(); // Commit transaction
            
        } catch (SQLException e) {
            if (c != null) c.rollback(); // Rollback on error
            throw e;
        } finally {
            if (c != null) {
                c.setAutoCommit(true);
                c.close();
            }
        }
    }

    /**
     * [NEW] Updates a question and its options in a transaction.
     */
    public static void updateQuestion(int questionId, String questionText, List<Option> options, String categoryName, String difficulty) throws SQLException {
        String sqlUpdateQuestion = "UPDATE questions SET question_text = ?, category_id = ?, difficulty = ? WHERE id = ?";
        String sqlDeleteOptions = "DELETE FROM options WHERE question_id = ?";
        String sqlInsertOption = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?)";
        
        Connection c = null;
        try {
            c = getConnection();
            c.setAutoCommit(false); // Start transaction

            // Get category ID
            Integer categoryId = getCategoryIdByName(c, categoryName);
            if (categoryId == null) {
                throw new SQLException("Invalid category name: " + categoryName);
            }
            
            // 1. Update Question text
            try (PreparedStatement p = c.prepareStatement(sqlUpdateQuestion)) {
                p.setString(1, questionText);
                p.setInt(2, categoryId);
                p.setString(3, difficulty);
                p.setInt(4, questionId);
                p.executeUpdate();
            }
            
            // 2. Delete all old options
            try (PreparedStatement p = c.prepareStatement(sqlDeleteOptions)) {
                p.setInt(1, questionId);
                p.executeUpdate();
            }
            
            // 3. Insert new options
            try (PreparedStatement p = c.prepareStatement(sqlInsertOption)) {
                for (Option opt : options) {
                    p.setInt(1, questionId);
                    p.setString(2, opt.getText());
                    p.setBoolean(3, opt.isCorrect());
                    p.addBatch();
                }
                p.executeBatch();
            }
            
            c.commit(); // Commit transaction

        } catch (SQLException e) {
            if (c != null) c.rollback();
            throw e;
        } finally {
            if (c != null) {
                c.setAutoCommit(true);
                c.close();
            }
        }
    }
    
    /**
     * [NEW] Deletes a question. Options are deleted automatically by "ON DELETE CASCADE".
     */
    public static void deleteQuestion(int questionId) throws SQLException {
        String sql = "DELETE FROM questions WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, questionId);
            p.executeUpdate();
        }
    }
}
