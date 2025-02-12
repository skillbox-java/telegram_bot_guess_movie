package bot;

import game.GameManager;
import game.GameSession;
import game.Movie;
import game.Question;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class GuessMovieBot implements LongPollingSingleThreadUpdateConsumer {

    public static final int ANSWER_COLUMNS_COUNT = 3;
    private final TelegramClient telegramClient;
    private final GameManager gameManager;
    private final List<Movie> movies;

    public GuessMovieBot(TelegramClient telegramClient, GameManager gameManager, List<Movie> movies) {
        this.telegramClient = telegramClient;
        this.gameManager = gameManager;
        this.movies = movies;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            System.out.println("В сообщение пользователя нет текста!");
            return;
        }

        Message message = update.getMessage();
        String chatId = message.getChatId().toString();
        String userMessageText = message.getText();

        if (userMessageText.equalsIgnoreCase("/start")) {
            startNewGame(chatId);
        } else {
            handleUserGuess(chatId, userMessageText);
        }
    }

    private void handleUserGuess(String chatId, String userMessageText) {
        GameSession gameSession = gameManager.findGameSession(chatId);

        if (gameSession == null) {
            sendMessage(chatId, "Игра не начата! Для старта выполните команду /start");
            return;
        }

        Optional<Question> optLastQuestion = gameSession.getLastQuestion();
        if (optLastQuestion.isEmpty()) {
            sendMessage(chatId, "Вопросы закончились! Ваш счет: %d".formatted(gameSession.getScore()));
            gameManager.endGame(chatId);
            return;
        }

        Question lastQuestion = optLastQuestion.get();

        if (lastQuestion.isRightAnswer(userMessageText)) {
            gameSession.incrementScore();
            sendMessage(chatId, "Правильно! Ваш текущий счет:%d".formatted(gameSession.getScore()));
        } else {
            sendMessage(chatId, "К сожалению ответ неверный! Правильный ответ: %s"
                    .formatted(lastQuestion.secretMovie().title()));
        }

        if (gameSession.isGameFinished()) {
            sendMessage(chatId, "Игра окончена! Ваш итоговый счет: %d".formatted(gameSession.getScore()));
            return;
        }

        sendNextMovie(chatId, gameSession);
    }


    private void startNewGame(String chatId) {
        GameSession gameSession = gameManager.startNewGame(movies, chatId);
        sendNextMovie(chatId, gameSession);
    }

    private void sendNextMovie(String chatId, GameSession gameSession) {
        gameSession.getNextQuestion()
                .ifPresent(question -> sendPhoto(chatId, question.secretMovie().pathToImage(),
                        "Угадай фильм!", new KeyboardBuilder().build(question.answerOptions(), ANSWER_COLUMNS_COUNT)));
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhoto(String chatId, String imagePath, String caption, ReplyKeyboard keyboard) {
        try (InputStream inputStream = getClass().getResourceAsStream(imagePath)) {

            if (inputStream == null) {
                System.out.println("Изображение %s не найдено".formatted(imagePath));
                return;
            }

            InputFile inputFile = new InputFile(inputStream, imagePath);

            SendPhoto.SendPhotoBuilder photo = SendPhoto.builder()
                    .chatId(chatId)
                    .photo(inputFile)
                    .caption(caption);

            if(keyboard != null){
                photo.replyMarkup(keyboard);
            }

            telegramClient.execute(photo.build());
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
        }
    }
}
