import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static java.lang.System.exit;

/**
 * Contains the client connection for Iperfer. Establishes a
 * connection to the remote socket on construction. When {@link Client#run(int time)}
 * is called, a timer is established while a separate thread handles the output
 * stream. When the timer completes, the secondary thread is interrupted and the
 * socket connection is closed.
 */
public class Client {
    private static OutputStream CLIENT_OUT;
    private static Socket CLIENT_SOCKET;

    private static int BYTES_PER_PKT;

    public Client(String addr, int port, int pkt_bytes) {
        BYTES_PER_PKT = pkt_bytes;

        if (port < 1024 || port > 65535) {
            System.err.println("Error: port number must be in the range 1024 to 65535");
            exit(1);
        }

        // Create and connect socket
        try {
            CLIENT_SOCKET = new Socket(addr, port);
        } catch (IOException e) {
            System.err.println("Error: failed connection to "
                    + addr + " on port " + port);
            throw new RuntimeException(e);
        }

        // Establish I/O
        try {
            // We are sending raw bytes, so we just use the raw OutputStream
            CLIENT_OUT = CLIENT_SOCKET.getOutputStream();
        } catch (IOException e) {
            System.err.println("Error: successfully connected to socket but failed to establish I/O");
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs the client, sending 1000 bytes/sec for time seconds.
     *
     * @param time time in seconds for the client to run
     * @return the number of bytes sent
     */
    public long run(int time) {
        // Create the client runner
        ClientRunner runner = new ClientRunner();
        Timer t = new Timer(time, runner);

        // Run the timer on this thread
        t.run();
        try {
            CLIENT_SOCKET.close();
        } catch (IOException e) {
            System.err.println("Error when closing socket connection...");
            throw new RuntimeException(e);
        }

        return runner.getSentBytes();
    }

    /**
     * Runs a ClientRunner for a specified amount of time
     * Likely doesn't need to implement Runnable, but I don't feel like
     * changing it now :)
     */
    private static class Timer implements Runnable {
        private static int SECONDS;
        private static ClientRunner RUNNER;

        public Timer(int time, ClientRunner runner) {
            SECONDS = time;
            RUNNER = runner;
        }

        @Override
        public void run() {
            long startTime = System.nanoTime();
            // Spawn new ClientRunner thread
            Thread clientRunner = new Thread(RUNNER);
            clientRunner.start();

            while (elapsedTime(startTime) < SECONDS) {
                continue;
            }

            clientRunner.interrupt();
        }

        private long elapsedTime(long startTime) {
            return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
        }
    }

    /**
     * Sends the client payload to the server for as long as it can
     */
    private static class ClientRunner implements Runnable {
        private long sentBytes = 0;

        @Override
        public void run() {
            byte[] bytes = new byte[BYTES_PER_PKT];
            while (!Thread.interrupted()) {
                try {
                    CLIENT_OUT.write(bytes);
                    sentBytes += BYTES_PER_PKT;
                } catch (IOException e) {
                    System.err.println("Error: failed to send bytes to server");
                    throw new RuntimeException(e);
                }
            }
        }

        public long getSentBytes() {
            return sentBytes;
        }
    }
}
