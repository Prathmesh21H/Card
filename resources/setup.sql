-- merged setup with users and admin user
DROP TABLE IF EXISTS scores;
DROP TABLE IF EXISTS options;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

CREATE TABLE categories (id SERIAL PRIMARY KEY, name TEXT UNIQUE NOT NULL);
CREATE TABLE questions (
  id SERIAL PRIMARY KEY,
  question_text TEXT NOT NULL,
  category_id INT REFERENCES categories(id) ON DELETE SET NULL,
  difficulty VARCHAR(20) DEFAULT 'medium'
);
CREATE TABLE options (
  id SERIAL PRIMARY KEY,
  question_id INT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  option_text TEXT NOT NULL,
  is_correct BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  is_admin BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE scores (id SERIAL PRIMARY KEY, user_id INT REFERENCES users(id), score INT, total INT, taken_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, category_id INT, difficulty VARCHAR(20));

-- sample categories
INSERT INTO categories (name) VALUES ('Java'), ('SQL'), ('Geography'), ('General Knowledge');

-- sample question
INSERT INTO questions (question_text, category_id, difficulty) VALUES ('Which keyword is used to define a constant in Java?', 1, 'easy');
INSERT INTO options (question_id, option_text, is_correct) VALUES (1,'const',false),(1,'static',false),(1,'final',true),(1,'let',false);

-- create an admin user: username admin, password admin123 (bcrypt hash)
-- Replace below hash if you want a different password. This hash corresponds to "admin123".
INSERT INTO users (username, password_hash, is_admin) VALUES ('admin', '$2a$10$KIX/8h5eV7dqkYg0qQ2P/Oc1zYx4k0cJfKMq5bV8rFLxI1f0h8e3a', true);
