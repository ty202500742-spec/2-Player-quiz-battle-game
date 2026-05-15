package quizbattle;

public class Question {
    String text;
    String[] options;
    int correctIndex;
    String difficulty;
    String topic;

    public Question(String text, String[] options, int correctIndex, String difficulty, String topic) {
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
        this.difficulty = difficulty;
        this.topic = topic;
    }
}
