package de.danielhammerl;

import de.danielhammerl.datastructs.DataStruct;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.Optional;

import static de.danielhammerl.Util.bytesToHex;

// highly compressed shit with pressure of around 495 giga-decimals. Harry would be proud
public class Subsystem {
    private final int number;
    private final int port;
    private boolean state = false;
    public PersistentTcpServer tcpClient;

    public Subsystem(int number, int port) {
        this.number = number;
        this.port = port;

        tcpClient = new PersistentTcpServer(port);
    }

    public Optional<byte[]> getData() {
        try {
            var data = Optional.ofNullable(tcpClient.waitForData(2000));
            if(data.isPresent()) {
                System.out.println("Received data from " + number + " with data: " + bytesToHex(data.get().data));
                return Optional.of(data.get().data);
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            System.out.println("Could not receive data from subsystem " + number);
            return Optional.empty();
        }
    }

    public void setData(DataStruct data) {
        try {
            tcpClient.sendData(data.toBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
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
