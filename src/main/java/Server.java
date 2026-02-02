import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static java.lang.System.exit;

/**
 * Contains the server-side implementation of Iperfer. Creates a server socket
 * on construction that listens on a given port. When {@link Server#run()} is called,
 * the server will begin to wait for client connections. When a connection occurs,
 * the server tracks the amount of data consumed and for how long (until the connection
 * is closed).
 */
public class Server {
    private static int BYTES_PER_PKT;
    private static ServerSocket SERVER_SOCKET;

    public Server(int port, int bytes_per_pkt) {
        BYTES_PER_PKT = bytes_per_pkt;

        if (port < 1024 || port > 65535) {
            System.err.println("Error: port number must be in the range 1024 to 65535");
            exit(1);
        }

        // Create the ServerSocket instance
        try {
            SERVER_SOCKET = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Error: failed to instantiate server");
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs the server, listening for client connections and will terminate after the
     * connection closes.
     *
     * @return A long array containing the total bytes consumed and the time (s) elapsed
     */
    public long[] run() {
        // Listen for connections
        try {
            Socket clientSocket = SERVER_SOCKET.accept();
            System.out.println("Client connected: " + clientSocket);

            // We are only accepting one client at a time, so we will not handle on a new thread
            return handleClientConnection(clientSocket);
        } catch (IOException e) {
            System.err.println("Error: failed to handle client connection");
            throw new RuntimeException(e);
        }
    }

    /**
     * We are simply using a long array to deliver the connection information
     * because I couldn't be bothered to make a Record.
     *
     * @param clientSocket The client connection
     * @return A long array containing the total bytes consumed and the time (s) elapsed
     */
    private long[] handleClientConnection(Socket clientSocket) {
        BufferedInputStream clientInput;
        try {
            // Note that we use a BufferedInputStream so we can read in chunks
            clientInput = new BufferedInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("Error: failed to retrieve client input stream");
            throw new RuntimeException(e);
        }

        long totalBytes = 0;
        byte[] buffer = new byte[BYTES_PER_PKT];
        long startTime = System.nanoTime();
        try {
            while (clientInput.read(buffer) != -1) {
                totalBytes += BYTES_PER_PKT;
            }
        } catch (IOException e) {
            System.err.println("Error when reading input stream");
            throw new RuntimeException(e);
        }

        long endTime = System.nanoTime();

        return new long[]{totalBytes, TimeUnit.NANOSECONDS.toSeconds(endTime - startTime)};
    }
}
