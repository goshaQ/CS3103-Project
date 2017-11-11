import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.LinkedList;

public class FileProxy {
    private BitSet pieces;

    private FileInfo fileInfo;
    private SeekableByteChannel byteChannel;

    public FileProxy(String location, FileInfo fileInfo, BitSet pieces) {
        this.pieces = (pieces != null) ?  pieces : new BitSet(fileInfo.pieceCount);
        this.fileInfo = fileInfo;

        // Obtain path for the up/down file
        String filePathName = location + File.separatorChar + fileInfo.fileName;
        Path filePath = Paths.get(filePathName);

        openStream(filePath);
        if (fileInfo.pieceHashes == null) {
            byte[][] pieceHashes = new byte[fileInfo.pieceCount][];

            for (short i = 0; i < fileInfo.pieceCount; i++) {
                byte[] piece = readPiece(i);
                pieceHashes[i] = FileInfo.SHA_1.digest(piece);
            }

            // Reinitialize variable in case of absence of piece hashes values at the beginning
            this.fileInfo = new FileInfo(fileInfo.fileName, fileInfo.size, fileInfo.pieceSize, fileInfo.pieceCount, pieceHashes);
        }
    }

    private void openStream(Path filePath) {
        try {
            byteChannel = Files.newByteChannel(filePath, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ,StandardOpenOption.WRITE));
        } catch (IOException e) {
            System.err.println("Can not open byte stream for the file.");
        }
    }

    public void closeStream() {
        try {
            byteChannel.close();
        } catch (IOException e) {
            System.err.println("Can not close byte stream for the file.");
        }
    }

    public byte[] readPiece(short index) {
        long position = index * fileInfo.pieceSize;
        ByteBuffer piece = ByteBuffer.allocate(getPieceSize(index));

        try {
            byteChannel.position(position);
            byteChannel.read(piece);
        } catch (IOException e) {
            System.err.println("Can not read piece with the index: " + index);
        }

        return piece.array();
    }

    public void writePiece(short index, byte[] buffer) {
        if (!verifyPiece(index, buffer)) {
            return;
        }

        long position = index * fileInfo.pieceSize;
        ByteBuffer piece = ByteBuffer.wrap(buffer);

        try {
            byteChannel.position(position);
            byteChannel.write(piece);
        } catch (IOException e) {
            System.err.println("Can not write piece with the index: " + index);
        }

        pieces.flip(index);
    }

    private boolean verifyPiece(short index, byte[] buffer) {
        return Arrays.equals(fileInfo.pieceHashes[index], FileInfo.SHA_1.digest(buffer));
    }

    private int getPieceSize(short index) {
        if (index == fileInfo.pieceCount - 1) {
            int remainder = (int) (fileInfo.size % fileInfo.pieceSize);
            if (remainder != 0) {
                return remainder;
            }
        }

        return fileInfo.pieceSize;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public BitSet getPieces() {
        return pieces;
    }

    public static FileProxy create(String location, String fileName) {
        // Open the up/down file to gather file statistics
        String filePathName = location + File.separatorChar + fileName;
        File file = new File(filePathName);

        long size = file.length();
        int pieceSize = getOptimalPieceSize(size);
        int pieceCount = (int) Math.ceil((double) size / pieceSize);

        BitSet pieces = new BitSet(pieceCount);
        pieces.flip(0, pieceCount);

        FileInfo fileInfo = new FileInfo(fileName, size, pieceSize, pieceCount, null);
        return new FileProxy(location, fileInfo, pieces);
    }

    private static int getOptimalPieceSize(long size) {
        final int OPTIMAL_PIECES_NUMBER = 1200;

        // Create a list containing possible sizes of pieces
        LinkedList<Integer> pieceSizes = new LinkedList<>();
        pieceSizes.add(16384);
        pieceSizes.add(65536);
        pieceSizes.add(262144);
        pieceSizes.add(1048576);

        // Find appropriate piece size that allows to split the file optimally
        for (int pieceSize : pieceSizes) {
            if (size / pieceSize < OPTIMAL_PIECES_NUMBER) {
                return pieceSize;
            }
        }

        return -1;
    }
}
