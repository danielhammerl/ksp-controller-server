package de.danielhammerl;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.KRPC;

import java.io.IOException;
import java.net.Inet4Address;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import krpc.client.services.SpaceCenter;


public class Main extends Application {
    int maxFps = 15;
    static volatile boolean connected = false;
    static volatile boolean connectionStarted = false;
    Font mainFont;
    BusinessCode businessCode = new BusinessCode();

    Subsystem[] subsystems = new Subsystem[]{
            new Subsystem(1, 12800),
            new Subsystem(2, 12801),
            new Subsystem(3, 12802),
            new Subsystem(4, 12803)
    };

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(1024, 768);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        mainFont = Font.loadFont(getClass().getResourceAsStream("/Quantico-Regular.ttf"), 58);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis((double) 1000 / maxFps), _ -> {
                    update();
                    render(gc);
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        stage.setOnCloseRequest(_ -> System.exit(0));

        Scene scene = new Scene(new StackPane(canvas));

        scene.widthProperty().addListener((_, _, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            render(gc);
        });

        scene.heightProperty().addListener((_, _, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            render(gc);
        });

        stage.setScene(scene);
        stage.setTitle("KSP Controller - System overview");
        stage.show();
    }

    private void startConnectionThread() {
        new Thread(() -> {
            try {
                try (Connection connection = Connection.newInstance("KSP-Server", Inet4Address.getByName("127.0.0.1"), 50000, 50001)) {
                    KRPC krpc = KRPC.newInstance(connection);
                    connected = true;

                    while (true) {
                        if (krpc.getCurrentGameScene().getValue() == KRPC.GameScene.FLIGHT.getValue()) {
                            businessCode.update(connection, subsystems);
                        }
                        Thread.sleep(10);
                    }

                } catch (IOException | RPCException e) {
                    System.err.println("Error in connection thread" + e);
                };
                connected = false;
                connectionStarted = false;
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        }).start();
    }

    private void update() {
        if (!connected && !connectionStarted) {
            connectionStarted = true;
            startConnectionThread();
        }

        for (Subsystem subsystem : subsystems) {
            subsystem.update();
        }
    }

    private void render(GraphicsContext gc) {
        double canvasWidth = gc.getCanvas().getWidth();
        double canvasHeight = gc.getCanvas().getHeight();

        // Background
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        // Main connection text
        gc.setFill(connected ? Color.WHITE : Color.RED);
        gc.setFont(mainFont);

        double connectedTextWidth;
        String connectedText = connected ? "CONNECTED" : "NOT CONNECTED";
        {
            Text temp = new Text(connectedText);
            temp.setFont(mainFont);
            connectedTextWidth = temp.getLayoutBounds().getWidth();
        }

        gc.fillText(connectedText, (canvasWidth - connectedTextWidth) / 2, canvasHeight * 0.15);
        // End main connection text

        // Quadrat-Einstellungen
        double squareSize = canvasWidth * 0.10;
        double spacing = 40; // mehr Abstand zwischen Quadraten
        double strokeWidth = 10;
        double y = canvasHeight * 0.5;

        // Start-X (zentriert)
        double totalWidth = subsystems.length * squareSize + (subsystems.length - 1) * spacing;
        double startX = (canvasWidth - totalWidth) / 2;

        gc.setLineWidth(strokeWidth);

        for (int i = 0; i < subsystems.length; i++) {
            double x = startX + i * (squareSize + spacing);
            subsystems[i].draw(gc, x, y, squareSize);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}