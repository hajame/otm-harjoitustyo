package runit.ui;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import runit.dao.*;
import runit.dao.util.Database;
import runit.domain.*;

/**
 * Class responsible for application behavior and graphical user interface.
 * Main class for the application.
 */
public class GUI extends Application {

    private Logic logic;
    private Scene runitScene;
    private Scene summaryScene;
    private Scene newUserScene;
    private Scene loginScene;
    private VBox exerciseNodes;
    private Label menuLabel = new Label();

    /**
     * Initializes application Logic.
     * 
     * @throws Exception 
     */
    @Override
    public void init() throws Exception {
        Properties properties = getProperties();

        String databaseFileName = properties.getProperty("databaseFile");
        Database database = getDatabase(databaseFileName);

        UserDao userDao = new UserDao(database);
        ExerciseDao exerciseDao = new ExerciseDao(database);
        this.logic = new Logic(userDao, exerciseDao);
    }

    /**
     * Reads the name of the database file from configuration file. 
     * If not found creates a new configuration file "config.properties"
     * with a standard database name "database.db".
     * 
     * @return Properties including name of the database.
     * @throws Exception 
     */
    public Properties getProperties() throws Exception {
        Properties properties = new Properties();
        File config = new File("config.properties");

        if (!config.exists()) {
            config.createNewFile();
            Path path = Paths.get("config.properties");
            Files.write(path, Arrays.asList("databaseFile=database.db"), Charset.forName("UTF-8"));
        }
        properties.load(new FileInputStream("config.properties"));
        return properties;
    }

    /**
     * Creates a new Database object with the given database name. If the 
     * database does not exist, the init() method creates the database, tables 
     * and a test user.
     * 
     * @param databaseFileName name of the database file
     * @return Database object
     * @throws Exception 
     */
    public Database getDatabase(String databaseFileName) throws Exception {
        Database database = null;
        try {
            File file = new File(databaseFileName);
            database = new Database("jdbc:sqlite:" + file.getAbsolutePath());
            database.init();
        } catch (Exception e) {
            System.out.println("Incorrect database address. --- " + e);
        }
        return database;
    }

    /**
     * Creates a Node object consisting of the exercise's string format and 
     * a "delete" button.
     * 
     * @param exercise
     * @return Node containing exercise in String format and a "delete" button.
     */
    public Node createExerciseNode(Exercise exercise) {
        HBox box = new HBox(10);
        Label label = new Label(exercise.toString());
        label.setMinHeight(28);
        Button button = new Button("delete");
        button.setOnAction(e -> {
            logic.deleteExercise(exercise);
            redrawExerciseList();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        box.setPadding(new Insets(1, 1, 1, 0));
        box.getChildren().addAll(label, spacer, button);
        return box;
    }

    /**
     * Updates the Exercise list. Clears the VBox exerciseNodes, retrieves
     * exercise history from logic, turns exercises into Nodes, and adds them 
     * into exerciseNodes.
     */
    public void redrawExerciseList() {
        if (logic.getUser() == null) {
            return;
        }
        exerciseNodes.getChildren().clear();
        List<Exercise> exercises = logic.getHistory();
        exercises.forEach(oneExercise -> {
            exerciseNodes.getChildren().add(createExerciseNode(oneExercise));
        });
    }

    /**
     * Application graphical user interface elements and UI logic for buttons.
     * 
     * @param primaryStage 
     */
    @Override
    public void start(Stage primaryStage) {
        // login scene
        VBox loginPane = new VBox(10);
        VBox inputPane = new VBox(10);
        loginPane.setPadding(new Insets(10));
        Label userLabel = new Label("username");
        TextField userInput = new TextField("test");
        userInput.setPromptText("test");
        Label passLabel = new Label("password");
        TextField passInput = new TextField("pass");
        passInput.setPromptText("pass");

        inputPane.getChildren().addAll(userLabel, userInput, passLabel, passInput);
        Label loginMessage = new Label();

        Button loginButton = new Button("login");
        Button createButton = new Button("create new user");
        loginButton.setOnAction(e -> {
            String username = userInput.getText();
            String password = passInput.getText();
            menuLabel.setText(username + " logged in...");
            if (logic.loginUser(username, password).equals("Login successful")) {
                loginMessage.setText("");
                redrawExerciseList();
                userInput.setText("");
                passInput.setText("");
                primaryStage.setScene(runitScene);
            } else {
                loginMessage.setText("User does not exist");
                loginMessage.setTextFill(Color.RED);
            }
        });

        createButton.setOnAction(e -> {
            userInput.setText("");
            passInput.setText("");
            primaryStage.setScene(newUserScene);
        });
        loginPane.getChildren().addAll(loginMessage, inputPane, loginButton, createButton);
        loginScene = new Scene(loginPane, 480, 250);

        // newUserScene
        VBox newUserPane = new VBox(10);

        HBox newUsernamePane = new HBox(10);
        newUsernamePane.setPadding(new Insets(10));
        TextField newUsernameInput = new TextField();
        Label newUsernameLabel = new Label("username");
        newUsernameLabel.setPrefWidth(100);
        newUsernamePane.getChildren().addAll(newUsernameLabel, newUsernameInput);

        HBox newPassPane = new HBox(10);
        newPassPane.setPadding(new Insets(10));
        TextField newPassInput = new TextField();
        Label newPassLabel = new Label("password");
        newPassLabel.setPrefWidth(100);
        newPassPane.getChildren().addAll(newPassLabel, newPassInput);

        Label userCreationMessage = new Label();

        Button createNewUserButton = new Button("create");
        createNewUserButton.setPadding(new Insets(10));

        createNewUserButton.setOnAction(e -> {
            String username = newUsernameInput.getText();
            String pass = newPassInput.getText();

            if (username.length() <= 2 || pass.length() < 2) {
                userCreationMessage.setText("username or password too short");
                userCreationMessage.setTextFill(Color.RED);
            } else if (username.length() > 32 || pass.length() > 32) {
                userCreationMessage.setText("username or password too long");
                userCreationMessage.setTextFill(Color.RED);
            }
            String result = logic.signupUser(username, pass);
            if (result.equals("Login successful")) {
                userCreationMessage.setText("");
                newUsernameInput.setText("");
                newPassInput.setText("");
                loginMessage.setText("new user created");
                loginMessage.setTextFill(Color.GREEN);
                primaryStage.setScene(loginScene);
            } else if (result.equals("Username taken")) {
                userCreationMessage.setText("username taken");
                userCreationMessage.setTextFill(Color.RED);
            }
        });

        newUserPane.getChildren().addAll(userCreationMessage, newUsernamePane, 
                newPassPane, createNewUserButton);
        newUserScene = new Scene(newUserPane, 480, 250);

        // summary scene
        VBox infoPane = new VBox();
        BorderPane summaryPane = new BorderPane(infoPane);
        summaryScene = new Scene(summaryPane, 880, 450);
        Region summaryMenuSpacer = new Region();
        HBox.setHgrow(summaryMenuSpacer, Priority.ALWAYS);
        HBox summaryMenu = new HBox(10);
        Label summaryLabel = new Label("Summary");
        Button summaryLogoutButton = new Button("logout");
        Button exercisesView = new Button("exercises");
        Label totalExercises = new Label("Total exercises: \t" + 0);
        totalExercises.setMinWidth(30);
        Label totalDistance = new Label("Total distance: \t" + 0 +" km");
        totalDistance.setMinWidth(30);
        Label totalDuration = new Label("Total duration: \t" + 0);
        totalDuration.setMinWidth(30);
        Label avgSpeed = new Label("Average speed: \t" + 0 + " km/h");
        avgSpeed.setMinWidth(30);
        Label avgDuration = new Label("Average duration: \t" + 0);
        avgDuration.setMinWidth(30);
        Label avgDistance = new Label("Average distance: \t" + 0 + " km");
        avgDistance.setMinWidth(30);
        summaryMenu.getChildren().addAll(summaryLabel, summaryMenuSpacer, 
                exercisesView, summaryLogoutButton);

        exercisesView.setOnAction(e -> {
            primaryStage.setScene(runitScene);
        });
        summaryLogoutButton.setOnAction(e -> {
            logic.logout();
            primaryStage.setScene(loginScene);
        });

        infoPane.getChildren().addAll(totalExercises, totalDistance, totalDuration, 
                avgSpeed, avgDuration, avgDistance);
        summaryPane.setTop(summaryMenu);

        // runitScene (main scene)
        ScrollPane exerciseScrollbar = new ScrollPane();
        BorderPane mainPane = new BorderPane(exerciseScrollbar);
        runitScene = new Scene(mainPane, 880, 450);

        HBox menuPane = new HBox(10);
        Region menuSpacer = new Region();
        HBox.setHgrow(menuSpacer, Priority.ALWAYS);
        Button logoutButton = new Button("logout");
        Button summaryButton = new Button("summary");

        summaryButton.setOnAction(e -> {
            totalExercises.setText("Total exercises: \t"
                    + logic.getStatistics().getTotalExercises());
            totalDistance.setText("Total distance: \t"
                    + logic.getStatistics().getTotalDistance() + " km");
            totalDuration.setText("Total duration: \t"
                    + logic.getStatistics().getTotalDuration());
            avgSpeed.setText("Average speed: \t"
                    + logic.getStatistics().getAvgExercise().getAvgSpeed() + " km/h");
            avgDuration.setText("Average duration: \t"
                    + logic.getStatistics().getAvgExercise().durationToString());
            avgDistance.setText("Average distance: \t"
                    + logic.getStatistics().getAvgExercise().getDistance() + " km");
            primaryStage.setScene(summaryScene);
        });
        logoutButton.setOnAction(e -> {
            logic.logout();
            primaryStage.setScene(loginScene);
        });

        menuPane.getChildren().addAll(menuLabel, menuSpacer, summaryButton, logoutButton);
        
        DateTimeFormatter yearMonthDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter hoursMinutes = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();

        HBox createForm = new HBox(30);
        Button createExercise = new Button("add");
        createExercise.setMinWidth(60);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label dateLabel = new Label("date");
        dateLabel.setMinWidth(30);
        TextField date = new TextField(yearMonthDate.format(now));
        date.setPromptText(yearMonthDate.format(now));
        date.setMaxWidth(100);
        Label timeLabel = new Label("time");
        timeLabel.setMinWidth(30);
        TextField time = new TextField(hoursMinutes.format(now));
        time.setPromptText(hoursMinutes.format(now));
        time.setMaxWidth(60);
        Label durationLabel = new Label("duration");
        durationLabel.setMinWidth(55);
        TextField duration = new TextField("00:30:00");
        duration.setPromptText("00:30:00");
        duration.setMaxWidth(100);
        Label distanceLabel = new Label("distance (km)");
        distanceLabel.setMinWidth(60);
        TextField distance = new TextField("5.00");
        distance.setPromptText("5.00");
        distance.setMaxWidth(60);
        createForm.getChildren().addAll(dateLabel, date, timeLabel, time, 
                durationLabel, duration, distanceLabel, distance, spacer, createExercise);

        exerciseNodes = new VBox(10);
        exerciseNodes.setMaxWidth(820);
        exerciseNodes.setMinWidth(820);
        redrawExerciseList();

        exerciseScrollbar.setContent(exerciseNodes);
        mainPane.setBottom(createForm);
        mainPane.setTop(menuPane);

        createExercise.setOnAction(e -> {
            Timestamp timestamp = logic.createTimestamp(date.getText() + 
                    " " + time.getText());
            int seconds = logic.createDuration(duration.getText());
            logic.addExercise(new Exercise(timestamp, seconds, Double.parseDouble(distance.getText())));
            date.setText(yearMonthDate.format(LocalDateTime.now()));
            time.setText(hoursMinutes.format(LocalDateTime.now()));
            duration.setText("00:30:00");
            distance.setText("5.00");
            redrawExerciseList();
        });

        // setup primary stage
        primaryStage.setTitle("runIT");
        primaryStage.setScene(loginScene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            if (logic.getUser() != null) {
                String name = logic.getUser().toString();
                logic.logout();
                System.out.println("logged out user '" + name + "'");
            }
            System.out.println("closing");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
