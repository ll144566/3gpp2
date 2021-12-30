package com.quectel.jnitestexec.cdma2;

public class BitwiseOutputStream {

    // The byte array being written to, which will be grown as needed.
    private byte[] mBuf;

    // The current position offset, in bits, from the msb in byte 0.
    private int mPos;

    // The last bit offset, given the current buf length.
    private int mEnd;

    /**
     * An exception to report access problems.
     */
    public static class AccessException extends Exception {
        public AccessException(String s) {
            super("BitwiseOutputStream access failed: " + s);
        }
    }

    /**
     * Create object from hint at desired size.
     *
     * @param startingLength initial internal byte array length in bytes
     */
    public BitwiseOutputStream(int startingLength) {
        mBuf = new byte[startingLength];
        mEnd = startingLength << 3;
        mPos = 0;
    }

    /**
     * Return byte array containing accumulated data, sized to just fit.
     *
     * @return newly allocated byte array
     */
    public byte[] toByteArray() {
        int len = (mPos >>> 3) + ((mPos & 0x07) > 0 ? 1 : 0);  // &7==%8
        byte[] newBuf = new byte[len];
        System.arraycopy(mBuf, 0, newBuf, 0, len);
        return newBuf;
    }

    /**
     * Allocate a new internal buffer, if needed.
     *
     * @param bits additional bits to be accommodated
     */
    private void possExpand(int bits) {
        if ((mPos + bits) < mEnd) return;
        byte[] newBuf = new byte[(mPos + bits) >>> 2];
        System.arraycopy(mBuf, 0, newBuf, 0, mEnd >>> 3);
        mBuf = newBuf;
        mEnd = newBuf.length << 3;
    }

    /**
     * Write some data and increment the current position.
     *
     * The 8-bit limit on access to bitwise streams is intentional to
     * avoid endianness issues.
     *
     * @param bits the amount of data to write (gte 0, lte 8)
     * @param data to write, will be masked to expose only bits param from lsb
     */
    public void write(int bits, int data) throws AccessException {
        if ((bits < 0) || (bits > 8)) {
            throw new AccessException("illegal write (" + bits + " bits)");
        }
        possExpand(bits);
        data &= (-1 >>> (32 - bits));
        int index = mPos >>> 3;
        int offset = 16 - (mPos & 0x07) - bits;  // &7==%8
        data <<= offset;
        mPos += bits;
        mBuf[index] |= data >>> 8;
        if (offset < 8) mBuf[index + 1] |= data & 0xFF;
    }

    /**
     * Write data in bulk from a byte array and increment the current position.
     *
     * @param bits the amount of data to write
     * @param arr the byte array containing data to be written
     */
    public void writeByteArray(int bits, byte[] arr) throws AccessException {
        for (int i = 0; i < arr.length; i++) {
            int increment = Math.min(8, bits - (i << 3));
            if (increment > 0) {
                write(increment, (byte)(arr[i] >>> (8 - increment)));
            }
        }
    }

    /**
     * Increment the current position, implicitly writing zeros.
     *
     * @param bits the amount by which to increment the position
     */
    public void skip(int bits) {
        possExpand(bits);
        mPos += bits;
    }
}

