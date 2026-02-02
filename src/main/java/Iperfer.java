public class Iperfer {
    private static final int BYTES_PER_PKT = 1000;

    static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: missing or additional arguments");
            return;
        }

        String CS_flag = args[0];
        if (CS_flag.equals("-c")) {
            runClient(args);
        } else if (CS_flag.equals("-s")) {
            runServer(args);
        } else {
            System.err.println("Please specify a correct mode: Client (-c), Server (-s)");
        }
    }

    /**
     * Runs the client code for Iperfer
     *
     * @param args the command-line arguments
     */
    private static void runClient(String[] args) {
        // Capture the other args
        if (args.length != 7) {
            System.err.println("Error: missing or additional arguments");
            return;
        }

        String hostname = args[2];
        int port;
        try {
            port = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("Port must be an integer");
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[6]);
        } catch (NumberFormatException e) {
            System.err.println("Time must be an integer");
            return;
        }

        System.out.println("Connecting to server...");
        Client c = new Client(hostname, port, BYTES_PER_PKT);
        System.out.println("Connected! Sending payload...");
        long totalBytes = c.run(seconds);
        System.out.println("Done!");

        calculateTraffic(totalBytes, seconds);
    }

    /**
     * Runs the server code of Iperfer
     *
     * @param args the command-line arguments
     */
    private static void runServer(String[] args) {
        // Capture other args
        if (args.length != 3) {
            System.err.println("Error: missing or additional arguments");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("Port must be an integer");
            return;
        }

        System.out.println("Creating server instance...");
        Server server = new Server(port, BYTES_PER_PKT);
        System.out.println("Created! Listening on port " + port + "...");
        long[] output = server.run();
        System.out.println("Done!");

        calculateTraffic(output[0], output[1]);
    }

    /**
     * Calculates and prints the traffic for a singular connection in KB & Mbps
     *
     * @param totalBytes total number of bytes sent/received during connection
     * @param seconds total number of seconds elapsed during connection
     */
    private static void calculateTraffic(long totalBytes, long seconds) {
        // Get # KB. No rounding as we send 1KB/Pkt
        long KB = totalBytes / 1000;

        // Calculate Mbps
        float Mbps = ((float)(KB * 8) / 1000) / seconds;

        System.out.println("sent=" + KB + " KB  rate=" + Mbps + " Mbps");
    }
}