package com.quizapp.model;

public class Option {
    private String text;
    private boolean correct;

    public Option(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }
}
