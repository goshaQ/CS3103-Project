public enum MessageType {
    DirectoryListingRequest(0),
    DirectoryListingReply(1),
    Handshake(2),
    AvailablePieces(3),
    DataRequest(4),
    DataPackage(5),
    PieceUpdate(6),
    AnnounceRequest(7),
    AnnounceReply(8),
    ConnectRequest(9),
    ConnectReply(10),
    Exit(11);

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
            case 7:
                return AnnounceRequest;
            case 8:
                return AnnounceReply;
            case 9:
                return ConnectRequest;
            case 10:
                return ConnectReply;
            case 11:
                return Exit;
        }
        return null;
    }
}
