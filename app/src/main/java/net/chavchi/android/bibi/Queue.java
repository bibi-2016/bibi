package net.chavchi.android.bibi;

class Queue {
    private static final Object data_lock = new Object() {};
    private byte[] data;
    private int size;  // size of array
    private int head;  // index of first empty byte
    private int tail;  // index of first unread byte
    private int count; // number of elements in queue
    private boolean overwrite; // should the data in queue be overwriten when full

    // ctor
    //   - create a queue of size s and overwrite mode o
    public Queue(int s) {
        size = s;
        head = 0;
        tail = 0;
        count = 0;
        overwrite = false;
        data = new byte[size];
    }

    // setOverwrite
    //   - set the overwrite flag
    public void setOverwrite(boolean o) {
        overwrite = o;
    }

    // hasData
    //   - return true if there are any data to read
    public boolean hasData() {
        return count > 0;
    }

    // getCount
    //	 - returns number of bytes in the queue
    public int getCount() {
        return count;
    }

    // getFreeCount
    //   - return number of free bytes in the queue
    public int getFreeCount() {
        return size - count;
    }

    // clear
    //   - resets the queue to the initial state - empty
    public void clear() {
        synchronized (data_lock) {
            head = 0;
            tail = 0;
            count = 0;
        }
    }

    // write
    //   - write len bytes from buffer starting at position pos
    //   - returns number of bytes written to queue
    public int write(byte[] buff, int pos, int len) {
        synchronized (data_lock) {
            int cnt = 0;

            if (overwrite == true) {
                boolean move_tail = ((size - count) - len) < 0; // see if input len is larger than free space in queue

                while (len > 0) {
                    int cnt0 = Math.min(size - head, len);

                    System.arraycopy(buff, pos, data, head, cnt0); // copy first part of data
                    head = (head + cnt0) % size; // move head, make sure not to overflow
                    pos += cnt0; // move source pos
                    len -= cnt0; // dec number of elements to write

                    cnt += cnt0;
                }

                if (move_tail == true) {
                    tail = (head + 1) % size;
                }

                count =  Math.min(size, count + cnt); // increase number of elements in queue

            } else {
                if (size - count > 0) { // have some free space
                    if (tail > head) {  // have space from head to tail
                        cnt = Math.min(tail - head, len);
                        System.arraycopy(buff, pos, data, head, cnt);
                        head += cnt; // move head
                    } else {  // have space from head to end and from start to tail
                        cnt = Math.min(size - head, len);

                        System.arraycopy(buff, pos, data, head, cnt); // copy first part of data
                        head = (head + cnt) % size; // move head, make sure not to overflow
                        pos += cnt; // move source pos
                        len -= cnt; // dec number of elements to write

                        if (len > 0) { // have more data to write ... write from head (which should be equal to size) to tail
                            int cnt1 = Math.min(tail, len);
                            System.arraycopy(buff, pos, data, head, cnt1);

                            head = cnt1; // first empty byte
                            cnt += cnt1; // add the second pass bytes to count
                        }

                    }
                }

                count += cnt; // increase number of elements in queue
            }

            return cnt;
        }
    }

    // read
    //   - reads len bytes from queue into buffer starting at position pos
    //   - returns number of bytes read
    public int read(byte[] buff, int pos, int len) {
        synchronized (data_lock) {
            int cnt = 0;

            if (count > 0) {
                if (head > tail) { // have data from tail to head
                    cnt = Math.min(head - tail, len);
                    System.arraycopy(data, tail, buff, pos, cnt);
                    tail += cnt; // move
                } else { // have data from tail to size and from 0 to head
                    cnt = Math.min(size - tail, len);
                    System.arraycopy(data, tail, buff, pos, cnt);
                    tail = (tail + cnt) % size;
                    pos += cnt;
                    len -= cnt;

                    if (len > 0) { // have more data to read ... read from 0 to head
                        int cnt1 = Math.min(head, len);
                        System.arraycopy(data, tail, buff, pos, cnt1);
                        tail = cnt1;
                        cnt += cnt1;
                    }
                }
            }

            count -= cnt;
            return cnt;
        }
    }

    // print
    //   - print queue values in hex string
    public String print() {
        String s = "";
        for (int i = 0; i < size; i++) {
            s += Integer.toHexString(data[i]);
        }
        return s;
    }
}
