package com.nhnacademy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class scurl {

    public static void main(String[] args) throws MalformedURLException {
        Options options = new Options();
        options.addOption("v", "verbose, 요청, 응답 헤더를 출력한다.");
        options.addOption("H", true, "임의의 헤더를 서버로 전송한다.");
        options.addOption("d", true, "POST, PUT 등에 데이터를 전송한다.");
        options.addOption("X", true, "사용할 method를 지정한다. 지정되지 않은 경우, 기본값은 GET");
        options.addOption("L", "서버에 응답이 30x 계열이면 다음 응답을 따라 간다.");
        options.addOption("F", true,
            "multipart/form-data를 구성하여 전송한다. content 부분에 @filename을 사용할 수 있다.");

        String host = args[args.length - 1];
        URL url = new URL(host);
        boolean useL = false;
        int count = 0;
        do {
            CommandLineParser parser = new DefaultParser();
            BufferedReader br = null;
            BufferedWriter bw = null;
            try (Socket socket = new Socket(url.getHost(), 80)) {
                boolean verbose = false;

                String request = "";

                try {

                    String method = "GET ";
                    String location = url.getPath();
                    CommandLine cmd = parser.parse(options, args);

                    if (cmd.hasOption('X')) {
                        if (cmd.getOptionValue('X').equals("POST")) {
                            method = "POST ";
                        }
                    }

                    boolean isF = false;
                    String postData = "";
                    String boundary = "------------------------32ff7e12885ce3e2";
                    if (cmd.hasOption('F')) {
                        method = "POST ";
                        String filePath = cmd.getOptionValue('F');
                        String[] str = filePath.split("=");
                        filePath = str[1];
                        String fileName = str[0];

                        postData = "--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=" + fileName + "\r\n\r\n"
                            + filePath + "\r\n"
//                            + "--" + boundary + "\r\n"
//                            + "Content-Disposition: form-data; name=\"name\"" + "\r\n\r\n"
//                            + "Donny\r\n"
//                            + "--" + boundary + "\r\n"
//                            + "Content-Disposition: form-data; filename=\""
//                            + filePath + "\"" + "\r\n"
//                            + "Content-Type: image/png" + "\r\n\r\n"
//                            + "-a long string of image data-" + "\r\n"
                            + "--" + boundary + "--";
                        isF = true;

                    }

                    request =
                        method + location + " HTTP/1.1 \r\n" + "Host: " + url.getHost()
                            + "\r\n"
                            + "User-Agent: curl/8.1.2 \r\n"
                            + "Accept: */*\r\n"
                            + "Connection: close" + "\r\n";

                    if (cmd.hasOption('v')) {
                        verbose = true;
                    }

                    if (cmd.hasOption('H')) {
                        request += cmd.getOptionValue('H') + "\r\n";
                    }

                    if (cmd.hasOption('d')) {
                        request += "Content-Length: " + cmd.getOptionValue('d').length() + "\r\n";
                        request += "\r\n";
                        request += cmd.getOptionValue('d') + "\r\n";
                    }

                    if (cmd.hasOption('L')) {
                        useL = true;
                    }

                    if (isF) {
                        request +=
                            "Content-Length: " + postData.length() + "\r\n";
                        request +=
                            "Content-Type: multipart/form-data; boundary=" + boundary + "\r\n";

                        request += "\r\n";
                        request += postData;
                    }

                    request += "\r\n";

                    if (verbose) {
                        System.out.println(request);
                    }

                    bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    bw.write(request);
                    bw.flush();

                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line = "";
                    String responselocation = "";

                    while ((line = br.readLine()) != null) {
//                        if(!line.contains("FOUND"))
                        if (line.contains("OK") || line.contains("Bad Request")) {
                            useL = false;
                        }
                        if (line.contains("Location") || line.contains("location")) {
                            String[] str = line.split(" ");
                            responselocation = str[1];
                        }
                        System.out.println(line);

                    }
                    bw.write("Connection: close" + "\r\n\r\n");
                    bw.flush();

//                    socket.close();

                    url = new URL("http://" + url.getHost() + responselocation);
                    count++;
                } catch (ParseException e) {
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (useL && count != 5);
    }
}