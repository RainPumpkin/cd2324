import java.io.*;
import com.rabbitmq.client.Channel;
import spread.SpreadGroup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Leader {
    private String exchange;
    private String bindingkey;
    private Channel channel;

    Leader(String exchange, String bindingkey, Channel channel){
        this.exchange = exchange;
        this.bindingkey = bindingkey;
        this.channel = channel;
    }

    public void merge(String tipo){
        String resumefile = tipo + "/resume.txt";
        File file = new File(resumefile);
        FileWriter writer;
        try {
            if(file.exists()){
                if(!file.delete()) System.out.println("Failed to delete resume");
                else file = new File(resumefile);
            }
            if (!file.createNewFile()){
                System.out.println("A file that should not exist, exists");
            }
            writer = new FileWriter(file);
        } catch (IOException e) {
            System.out.println("ERROR CREATING FILE");
            throw new RuntimeException(e);
        }
        Set<String> files = listFilesUsingFilesList(tipo);
        for (String filename : files) {
            System.out.println("Reading file " +  filename);
            try {
                filename = tipo + "/" + filename;
                //skip resume file
                if (filename.contains("resume")) continue;
                File workerfile = new File(filename);
                BufferedReader reader = new BufferedReader(new FileReader(workerfile));
                while (true){
                    String line = reader.readLine();
                    if (line != null) writer.write(line + "\n");
                    else {
                        System.out.println("Reader closed\n");
                        reader.close();
                        break;
                    }
                }
                if(!workerfile.delete()) System.out.println("Error deleting workerfile: " + filename);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        notification(tipo);
    }

    public Set<String> listFilesUsingFilesList(String dir) {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void notification(String tipo){
        try {
            channel.basicPublish(exchange, bindingkey, true, null, tipo.getBytes());
        } catch (IOException e) {
            System.out.println("Failed to send message");
            throw new RuntimeException(e);
        }
    }

    public static boolean checkIfLeader(SpreadGroup[] members, String name){
        Stream<SpreadGroup> st = Arrays.stream(members).sorted();
        String first = st.findFirst().toString();
        System.out.println("First is=" + first);
        System.out.println("I am " + name);
        return name.equals(first);
    }
}
