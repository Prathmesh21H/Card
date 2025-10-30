package com.quizapp.model;

import java.util.ArrayList;
import java.util.List;

public class Question {
    private int id;
    private String text;
    private List<Option> options = new ArrayList<>();
    
    // [NEW] Added fields for admin panel
    private String category;
    private String difficulty;

    public Question(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void addOption(Option o) {
        options.add(o);
    }

    public java.util.List<Option> getOptions() {
        return options;
    }
    
    // --- [NEW] Getters and Setters for meta-data ---

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
