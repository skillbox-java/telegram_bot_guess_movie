package game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    @Test
    void whenOptionsContainsSecretMovie_thenQuestionIsCreated(){
        var question = new Question(
                new Movie("a", "p"),
                List.of("a", "b")
        );

        assertNotEquals(null, question);
    }

}