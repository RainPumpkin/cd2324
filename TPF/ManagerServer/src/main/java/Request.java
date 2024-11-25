public class Request {
    private Boolean hasToken;

    public Request() {
        hasToken = false;
    }

    public void receiveToken() {
        hasToken = true;
    }

    public void releaseToken() {
        hasToken = false;
    }

    public Boolean getHasToken() {
        return hasToken;
    }
}