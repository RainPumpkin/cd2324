package imageserver;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.github.dockerjava.core.DockerClientBuilder.getInstance;

public class DockerAPI {
    DockerClient dockerclient;
    HostConfig hostConfig;

    public DockerAPI(String HOST_URI) {
        String pathVolDir = "/home/CD2324-G09/images";
        dockerclient = getInstance()
                .withDockerHttpClient(
                        new ApacheDockerHttpClient.Builder()
                                .dockerHost(URI.create(HOST_URI)).build()
                )
                .build();
        hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(pathVolDir, new Volume("/usr/images")));
    }

    public void executeContainer(String containerName, List<String> command) {
        try {
            System.out.println(command);
            String imageName = "g09/markapp";
            CreateContainerResponse containerResponse = dockerclient
                    .createContainerCmd(imageName)
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .withCmd(command)       // [N argumentos Do Jar] (ADD to RUN command)
                    .exec();
            System.out.println("ID:" + containerResponse.getId());
            dockerclient.startContainerCmd(containerResponse.getId()).exec();
            dockerclient.waitContainerCmd(containerResponse.getId()).exec(new WaitContainerResultCallback()).awaitStatusCode();
            dockerclient.waitContainerCmd(containerResponse.getId()).exec(new WaitContainerResultCallback()).awaitStatusCode();
            InspectContainerResponse inspResp = dockerclient
                    .inspectContainerCmd(containerName).exec();
            System.out.println(inspResp.getState().getStatus());
            dockerclient.removeContainerCmd(containerName).exec();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
