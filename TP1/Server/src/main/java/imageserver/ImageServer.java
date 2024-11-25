package imageserver;

import clientserverservice.*;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ImageServer extends ClientServerServiceGrpc.ClientServerServiceImplBase {
    DockerAPI docker;
    //key -> ImageId
    Map<String, ImageState> imageStates = new HashMap<>();
    int MAXBYTES = 32000;

    public ImageServer() {
        docker = new DockerAPI("unix:///var/run/docker.sock");
    }

    class ImageStreamObserver implements StreamObserver<Image> {
        LinkedList<ByteString> image = new LinkedList<>();

        StreamObserver<ImageId> responseObs;
        String imageName;

        public ImageStreamObserver(StreamObserver<ImageId> responseObs) {
            this.responseObs = responseObs;
        }

        @Override
        public void onNext(Image image) {
            this.image.add(image.getData());
            System.out.printf(image.getName());
            this.imageName = image.getName();
        }

        @Override
        public void onError(Throwable throwable) {
            responseObs.onError(throwable);
        }

        @Override
        public void onCompleted() {
            System.out.println("OnCompleted");
            try {
                File file = new File("/home/CD2324-G09/images/" + imageName);
                try {
                    if (!file.createNewFile()) System.out.println("File exists already.");
                } catch (IOException e) {
                    System.out.println("ERROR CREATING FILE");
                    throw new RuntimeException(e);
                }
                DataOutputStream dt = new DataOutputStream(new FileOutputStream(file));
                UUID id = UUID.randomUUID();
                for (ByteString s: image) {
                    dt.write(s.toByteArray());
                }
                dt.close();
                responseObs.onNext(
                        ImageId
                                .newBuilder()
                                .setId(id.toString())
                                .build()
                );
                imageStates.put(id.toString(), new ImageState(id.toString(), imageName));
                responseObs.onCompleted();
                System.out.println("onCompletedDone");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public StreamObserver<Image> processImage(StreamObserver<ImageId> responseObserver) {
        System.out.println("Receiving Image");
        ImageStreamObserver obs = new ImageStreamObserver(responseObserver);
        return obs;
    }

    @Override
    public void checkImageStatus(ImageId request, StreamObserver<ImageStatus> responseObserver) {
        ImageState img = imageStates.get(request.getId());
        responseObserver.onNext(
                ImageStatus
                .newBuilder()
                .setStatus(img.state == State.DONE)
                .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void getProcessedImage(ImageId request, StreamObserver<Image> responseObserver) {
        String[] aux = imageStates.get(request.getId()).imageName.split("\\.");
        String imageName = aux[0] + "-marked." + aux[1];
        try {
            File f = Path.of("/home/CD2324-G09/images/"+imageName).toFile();
            DataInputStream dt = new DataInputStream(new FileInputStream(f));
            byte[] read;
            while (true){
                read = dt.readNBytes(MAXBYTES);
                responseObserver.onNext(
                        Image.newBuilder()
                                .setData(ByteString.copyFrom(read))
                                .setName(imageName)
                                .build()
                );
                if (read.length<MAXBYTES) break;
            }
            dt.close();
            responseObserver.onCompleted();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void keywordList(Keywords request, StreamObserver<Confirmation> reply) {
        String [] aux = imageStates.get(request.getId()).imageName.split("\\.");
        String imageName = "/usr/images/" + aux[0];
        List<String> cmd = Arrays.asList(imageName + "." + aux[1], imageName + "-marked." + aux[1]);
        LinkedList<String> command = new LinkedList<>(cmd);
        command.addAll(request.getKeywordsList());
        reply.onNext(
                Confirmation
                        .newBuilder()
                        .setValid(true)
                        .setError("")
                        .build()
        );
        reply.onCompleted();
        docker.executeContainer(request.getId(), command);
        imageStates.get(request.getId()).updateState();
    }

    @Override
    public void ping(Nada request, StreamObserver<Nada> reply){
        reply.onNext(
                Nada
                        .newBuilder()
                        .setId("I'm alive")
                        .build()
        );
        reply.onCompleted();
    }
}
