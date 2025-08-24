package de.danielhammerl;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Subsystem {
    private final int number;
    private final int port;
    private boolean state = false;
    PersistentTcpServer tcpClient;
    private PersistentTcpServer.DataListener listener;


    public Subsystem(int number, int port) {
        this.number = number;
        this.port = port;

        tcpClient = new PersistentTcpServer(port, listener);
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

    public void draw(GraphicsContext gc, double x, double y, double size) {
        Color color = state ? Color.WHITE : Color.RED;

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
