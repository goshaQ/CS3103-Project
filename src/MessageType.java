public enum MessageType {
    DirectoryListingRequest(0),
    DirectoryListingReply(1),
    Handshake(2),
    AvailablePieces(3),
    DataRequest(4),
    DataPackage(5),
    PieceUpdate(6);


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
                return DirectoryListingReply;
            case 2:
                return Handshake;
            case 3:
                return AvailablePieces;
            case 4:
                return DataRequest;
            case 5:
                return DataPackage;
            case 6:
                return PieceUpdate;
        }
        return null;
    }
}
