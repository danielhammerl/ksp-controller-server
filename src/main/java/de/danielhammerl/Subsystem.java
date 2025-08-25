package de.danielhammerl;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;

public class Subsystem {
    private final int number;
    private final int port;
    private boolean state = false;
    PersistentTcpServer tcpClient;


    public Subsystem(int number, int port) {
        this.number = number;
        this.port = port;

        tcpClient = new PersistentTcpServer(port, this::onDataReceived);
    }

    public void onDataReceived(PersistentTcpServer.ClientHandler client, byte[] data, int length) {
        String msg = new String(data, 0, length);
        try {
            client.send(("Echo from subsystem " + number + ": " + msg).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public boolean getState() {
        return state;
    }

    public void update() {
        state = tcpClient.getConnected();
    }

    // statisch, daher über alle instanzen genutzt, daher synchones blinken! :)
    public static boolean isBlinkOn() {
        long now = System.currentTimeMillis();
        return (now / 500) % 2 == 0;
    }

    public void draw(GraphicsContext gc, double x, double y, double size) {
        Color color;
        if (state) {
            color = Color.WHITE;
        } else {
            boolean blinkOn = Subsystem.isBlinkOn();
            color = blinkOn ? Color.RED : Color.TRANSPARENT;
        }

        // Quadrat-Rahmen
        gc.setStroke(color);
        gc.strokeRect(x, y, size, size);

        // Text
        String text = String.valueOf(number);
        Font font = Font.loadFont(getClass().getResourceAsStream("/Quantico-Regular.ttf"), size * 0.6);
        gc.setFont(font);
        gc.setFill(color);

        Text temp = new Text(text);
        temp.setFont(font);
        double textWidth = temp.getLayoutBounds().getWidth();
        double textHeight = temp.getLayoutBounds().getHeight();

        // Horizontal & vertikal zentrieren
        double textX = x + (size - textWidth) / 2;
        double textY = y + size / 2 + textHeight / 4; // ungefähr vertikal zentriert

        gc.fillText(text, textX, textY);
    }
}
