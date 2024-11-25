package imageserver;

enum State{ PROCESSING, DONE}

public class ImageState {
    String imageId;
    State state;
    String imageName;

    public ImageState(String imageId, String imageName) {
        this.imageId = imageId;
        this.state = State.PROCESSING;
        this.imageName = imageName;
    }

    public void updateState() {
        state = State.DONE;
    }
}
