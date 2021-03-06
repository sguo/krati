package krati.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * MappedWriter
 * 
 * @author jwu
 * 
 */
public class MappedWriter implements DataWriter {
    private final File _file;
    private RandomAccessFile _raf;
    private MappedByteBuffer _mmapBuffer;
    
    public MappedWriter(File file) {
        this._file = file;
    }
    
    @Override
    public File getFile() {
        return _file;
    }
    
    @Override
    public void open() throws IOException {
        if(!_file.exists()) {
            File dir = _file.getParentFile();
            if(dir.exists())  _file.createNewFile();
            else if(dir.mkdirs()) _file.createNewFile();
            else throw new IOException("Failed to create file " + _file.getAbsolutePath());
        }
        
        if(_file.isDirectory()) {
            throw new IOException("Cannot open directory " + _file.getAbsolutePath());
        }
        
        _raf = new RandomAccessFile(_file, "rw");
        _mmapBuffer = _raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, _raf.length());
    }
    
    @Override
    public void close() throws IOException {
        try {
            _mmapBuffer.force();
            _raf.close();
        } finally {
            _mmapBuffer = null;
            _raf = null;
        }
    }
    
    @Override
    public void flush() throws IOException {
        _mmapBuffer.force();
    }
    
    @Override
    public void writeInt(int value) throws IOException {
        _mmapBuffer.putInt(value);
    }
    
    @Override
    public void writeLong(long value) throws IOException {
        _mmapBuffer.putLong(value);
    }
    
    @Override
    public void writeShort(short value) throws IOException {
        _mmapBuffer.putShort(value);
    }
    
    @Override
    public void writeInt(long position, int value) throws IOException {
        _mmapBuffer.putInt((int)position, value);
    }
    
    @Override
    public void writeLong(long position, long value) throws IOException {
        _mmapBuffer.putLong((int)position, value);
    }
    
    @Override
    public void writeShort(long position, short value) throws IOException {
        _mmapBuffer.putShort((int)position, value);
    }
    
    @Override
    public long position() throws IOException {
        return _mmapBuffer.position();
    }
    
    @Override
    public void position(long newPosition) throws IOException {
        _mmapBuffer.position((int)newPosition);
    }
}
