package main;

import checker.Checker;
import checker.Checkstyle;
import common.Constants;
import fileio.ActionInputData;
import fileio.Input;
import fileio.InputLoader;
import fileio.MovieInputData;
import fileio.UserInputData;
import fileio.Writer;
import org.json.simple.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * The entry point to this homework. It runs the checker that tests your implentation.
 */
public final class Main {
    /**
     * for coding style
     */
    private Main() {
    }

    /**
     * Call the main checker and the coding style checker
     * @param args from command line
     * @throws IOException in case of exceptions to reading / writing
     */
    public static void main(final String[] args) throws IOException {
        File directory = new File(Constants.TESTS_PATH);
        Path path = Paths.get(Constants.RESULT_PATH);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        File outputDirectory = new File(Constants.RESULT_PATH);

        Checker checker = new Checker();
        checker.deleteFiles(outputDirectory.listFiles());

        for (File file : Objects.requireNonNull(directory.listFiles())) {

            String filepath = Constants.OUT_PATH + file.getName();
            File out = new File(filepath);
            boolean isCreated = out.createNewFile();
            if (isCreated) {
                action(file.getAbsolutePath(), filepath);
            }
        }

        checker.iterateFiles(Constants.RESULT_PATH, Constants.REF_PATH, Constants.TESTS_PATH);
        Checkstyle test = new Checkstyle();
        test.testCheckstyle();
    }

    /**
     * @param filePath1 for input file
     * @param filePath2 for output file
     * @throws IOException in case of exceptions to reading / writing
     */

    @SuppressWarnings({"unchecked"})
    public static void action(final String filePath1,
                              final String filePath2) throws IOException {
        InputLoader inputLoader = new InputLoader(filePath1);
        Input input = inputLoader.readData();

        Writer fileWriter = new Writer(filePath2);
        JSONArray arrayResult = new JSONArray();

        //TODO add here the entry point to your implementation
        List<ActionInputData> actions = input.getCommands();
        List<UserInputData> users = input.getUsers();
        List<MovieInputData> movies = input.getMovies();

        UserInputData user = null;
        MovieInputData movie = null;

        for (int i = 0; i < actions.size(); i++) {
            /* Cautam user-ul cu username-ul care ne intereseaza */
            for (UserInputData iterator : users) {
                if (iterator.getUsername().equals(input.getCommands().get(i).getUsername())) {
                    user = iterator;
                }
            }
            /* Cautam filmul cu titlul care ne intereseaza */
            for (MovieInputData iterator : movies) {
                if (iterator.getTitle().equals(input.getCommands().get(i).getTitle())) {
                    movie = iterator;
                }
            }
            /* Daca comanda este favorite, vom verifica daca video-ul se afla deja in lista
               de favorite si daca acesta a fost vazut sau nu de user */
            if (input.getCommands().get(i).getActionType().equals("command")) {
                if (input.getCommands().get(i).getType().equals("favorite")) {
                    assert user != null;
                    if (user.getHistory().containsKey(input.getCommands().get(i).getTitle())) {
                        if (user.getFavoriteMovies().contains(
                                input.getCommands().get(i).getTitle())) {
                            arrayResult.add(fileWriter.writeFile(input.
                                            getCommands().get(i).getActionId(),
                                    "", "error -> "
                                            + input.getCommands().get(i).getTitle()
                                            + " is already in favourite list"));
                        } else {
                            user.getFavoriteMovies().add(input.getCommands().get(i).getTitle());
                            arrayResult.add(fileWriter.writeFile(input.getCommands().
                                            get(i).getActionId(),
                                    "", "success -> " + input.getCommands().get(i).getTitle()
                                            + " was added as favourite"));
                        }
                    } else {
                        arrayResult.add(fileWriter.writeFile(input.getCommands().
                                        get(i).getActionId(),
                                        "", "error -> " + input.getCommands().get(i).getTitle()
                                        + " is not seen"));
                    }
                }
                /* Daca comanda este view vom verifica daca video-ul a mai fost vazut si vom
                   printa de cate ori a fost vazut. Daca acesta nu a fost vazut pana acum,
                   adaugam in history titlul video-ului si numarul de view-uri (adica 1) */
                if (input.getCommands().get(i).getType().equals("view")) {
                    int value;
                    assert user != null;
                    if (user.getHistory().containsKey(input.getCommands().get(i).getTitle())) {
                        value = user.getHistory().get(input.getCommands().get(i).getTitle()) + 1;
                        user.getHistory().remove(input.getCommands().get(i).getTitle());
                        user.getHistory().put(input.getCommands().get(i).getTitle(), value);
                        arrayResult.add(fileWriter.writeFile(input.getCommands().
                                        get(i).getActionId(),
                                        "", "success -> " + input.getCommands().get(i).getTitle()
                                        + " was viewed with total views of " + value));
                    } else {
                        user.getHistory().put(input.getCommands().get(i).getTitle(), 1);
                        arrayResult.add(fileWriter.writeFile(input.getCommands().
                                        get(i).getActionId(),
                                "", "success -> " + input.getCommands().get(i).getTitle()
                                        + " was viewed with total views of " + 1));
                    }
                }
                /* Daca comanda este rating, verificam daca video-ul a fost vazut sau nu.
                   Daca acesta nu a fost vazut, utilizatorul nu are voie sa dea rating la
                    video-ul respectiv*/
                if (input.getCommands().get(i).getType().equals("rating")) {
                    if (user != null) {
                        if (user.getHistory().containsKey(input.getCommands().get(i).getTitle())) {
                            double ratingVal = input.getCommands().get(i).getGrade();

                            arrayResult.add(fileWriter.writeFile(input.getCommands().
                                            get(i).getActionId(),
                                    "", "success -> "
                                            + input.getCommands().get(i).getTitle()
                                            + " was rated with " + ratingVal
                                            + " by " + user.getUsername()));

                            if (movie != null) {
                                movie.setRating((movie.getRating() * movie.getNumRatings()
                                        + ratingVal) / (movie.getNumRatings() + 1));
                                movie.setNumRatings(movie.getNumRatings() + 1);
                            }
                        } else {
                            arrayResult.add(fileWriter.writeFile(input.getCommands().
                                            get(i).getActionId(),
                                    "", "error -> " + input.getCommands().get(i).getTitle()
                                            + " is not seen"));
                        }
                    }
                }
            } else if (input.getCommands().get(i).getActionType().equals("recommendation")) {
                if (input.getCommands().get(i).getType().equals("standard")) {
                    for (MovieInputData iterator : movies) {
                        if (user != null && !user.getHistory().containsKey(iterator.getTitle())) {
                            arrayResult.add(fileWriter.writeFile(input.getCommands().
                                            get(i).getActionId(),
                                    "", "StandardRecommendation result: "
                                            + iterator.getTitle()));
                        }
                    }
                } else if (input.getCommands().get(i).getType().equals("best_unseen")) {
                    movies.sort((movie1, movie2) -> {
                        if (movie1.getRating() != movie2.getRating()) {
                            return Double.compare(movie1.getRating(), movie2.getRating());
                        } else {
                            return movie1.getTitle().compareTo(movie2.getTitle());
                        }
                    });
                    for (MovieInputData iterator : movies) {
                        if (user != null && !user.getHistory().containsKey(iterator.getTitle())) {
                            movie = iterator;
                        }
                    }
                    if (movie != null) {
                        arrayResult.add(fileWriter.writeFile(input.getCommands().get(i).
                                        getActionId(), "", "BestRatedUnseenRecommendation result: "
                                        + movie.getTitle()));
                    }

                }
            }
        }
        fileWriter.closeJSON(arrayResult);
    }

}
