package krati.core.array.basic;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import krati.array.IntArray;
import krati.core.array.entry.EntryIntFactory;
import krati.core.array.entry.EntryValueInt;

/**
 * StaticIntArray: Fixed-Size Persistent IntArray Implementation.
 * 
 * This class is not thread-safe by design. It is expected that the conditions below hold within one JVM.
 * <pre>
 *    1. There is one and only one instance of StaticIntArray for a given home directory.
 *    2. There is one and only one thread is calling the setData method at any given time. 
 * </pre>
 * 
 * It is expected that this class is used in the case of multiple readers and single writer.
 * 
 * @author jwu
 *
 */
public class StaticIntArray extends AbstractRecoverableArray<EntryValueInt> implements IntArray {
    private static final Logger _log = Logger.getLogger(StaticIntArray.class);
    private int[] _internalArray;
    
    /**
     * Create a fixed-length persistent integer array.
     * 
     * @param length
     *            the length of this array
     * @param entrySize
     *            the size of redo entry (i.e., batch size)
     * @param maxEntries
     *            the number of redo entries required for updating the
     *            underlying array file
     * @param homeDirectory
     *            the home directory of this array
     * @throws Exception
     *             if this array cannot be created.
     */
    public StaticIntArray(int length, int entrySize, int maxEntries, File homeDirectory) throws Exception {
        super(length, 4 /* elementSize */, entrySize, maxEntries, homeDirectory, new EntryIntFactory());
    }

    @Override
    protected void loadArrayFileData() {
        long maxScn = 0;
        
        try {
            maxScn = _arrayFile.getLwmScn();
            _internalArray = _arrayFile.loadIntArray();
            if (_internalArray.length != _length) {
                maxScn = 0;
                _internalArray = new int[_length];
                clear();

                _log.warn("Allocated _internalArray due to invalid length");
            } else {
                _log.info("Data loaded successfully from file " + _arrayFile.getName());
            }
        } catch (Exception e) {
            maxScn = 0;
            _internalArray = new int[_length];
            clear();

            _log.warn("Allocated _internalArray due to a thrown exception: " + e.getMessage());
        }
        
        _entryManager.setWaterMarks(maxScn, maxScn);
    }
    
    /**
     * Sync-up the high water mark to a given value.
     * 
     * @param endOfPeriod
     */
    @Override
    public void saveHWMark(long endOfPeriod) {
        if (getHWMark() < endOfPeriod) {
            try {
                set(0, get(0), endOfPeriod);
            } catch (Exception e) {
                _log.error(e);
            }
        }
    }
    
    @Override
    public void clear() {
        if (_internalArray != null) {
            for (int i = 0; i < _internalArray.length; i++) {
                _internalArray[i] = 0;
            }
        }
        
        // Clear the entry manager
        _entryManager.clear();
        
        // Clear the underly array file
        try {
            _arrayFile.reset(_internalArray, _entryManager.getLWMark());
        } catch (IOException e) {
            _log.error(e.getMessage(), e);
        }
    }
    
    public int get(int index) {
        return _internalArray[index];
    }
    
    public void set(int index, int value, long scn) throws Exception {
        _internalArray[index] = value;
        _entryManager.addToPreFillEntryInt(index, value, scn);
    }
    
    @Override
    public int[] getInternalArray() {
        return _internalArray;
    }
}
