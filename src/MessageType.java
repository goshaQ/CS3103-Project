public enum MessageType {
    DirectoryListingRequest(0),
    Handshake(1),
    AvailablePieces(2);


    private int value;
    MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MessageType fromInteger(int value) {
        switch(value) {
            case 0:
                return DirectoryListingRequest;
            case 1:
                return Handshake;
            case 2:
                return AvailablePieces;
        }
        return null;
    }
}
