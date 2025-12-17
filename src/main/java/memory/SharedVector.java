package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        // TODO: return element at index (read-locked)
        lock.readLock().lock();
        try {
            return vector[index];
        } finally {
            lock.readLock().unlock();
        }
    }

    public int length() {
        // TODO: return vector length
        return vector.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        return orientation;
    }

    public void writeLock() {
        // TODO: acquire write lock
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        // TODO: release write lock
        lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock
        lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector
        if (orientation == VectorOrientation.ROW_MAJOR) {
            orientation = VectorOrientation.COLUMN_MAJOR;
        } 
        else {
            orientation = VectorOrientation.ROW_MAJOR;
        }
    }

    public void add(SharedVector other) {
        // TODO: add two vectors
        if (this.length() != other.length()) {
            throw new IllegalArgumentException("Vectors must be of the same length to add");
        }
        for (int i = 0 ; i < this.length() ; i++) {
            this.vector[i] = this.vector[i] +other.get(i);
        }
    }

    public void negate() {
        // TODO: negate vector
        for (int i = 0 ; i < this.length() ; i++) {
            this.vector[i] = this.vector[i] * (-1);
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        if (this.length() != other.length()) {
            throw new IllegalArgumentException("Vectors must be of the same length to use the dot method");
        }
        if (this.orientation != VectorOrientation.ROW_MAJOR || other.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException("First vector must be a row vector and second vector must be a column vector to use the dot method");
        }
        double result = 0;
        for (int i = 0 ; i < this.length() ; i++) {
            result += this.vector[i] * other.get(i);
        }
        return result;
    }

    public void vecMatMul(SharedMatrix matrix) {
        if (this.orientation != VectorOrientation.ROW_MAJOR) {
            throw new IllegalArgumentException("Vector must be a row vector to use vecMatMul");
        }
        VectorOrientation matOrientation = matrix.getOrientation();
        double[] result;
        // CASE 1: Matrix is composed of rows (Row Major)
        // Structure: Array of N rows, each of length M
        if (matOrientation == VectorOrientation.ROW_MAJOR) {
            if (this.length() != matrix.length()) {
                throw new IllegalArgumentException("Vector length (" + this.length() + 
                    ") must match matrix row count (" + matrix.length() + ")");
            }
            int resultLength = matrix.get(0).length();
            result = new double[resultLength];
            for (int j = 0; j < resultLength; j++) {
                double sum = 0;
                for (int i = 0; i < this.length(); i++) {
                    // matrix.get(i) gives the i-th row
                    // .get(j) gives the j-th element in that row (the column index)
                    sum += this.get(i) * matrix.get(i).get(j);
                }
                result[j] = sum;
            }
        } 
        
        // CASE 2: Matrix is composed of columns (Column Major)
        // Structure: Array of M columns, each of length N
        else {
            if (this.length() != matrix.get(0).length()) {
                throw new IllegalArgumentException("Vector length (" + this.length() + 
                    ") must match column height (" + matrix.get(0).length() + ")");
            }
            int resultLength = matrix.length(); // Number of columns
            result = new double[resultLength];
            // Compute: Each element in the result is the dot product of 'this' vector and the column vector
            for (int j = 0; j < resultLength; j++) {
                SharedVector column = matrix.get(j);
                result[j] = this.dot(column);
            }
        }
        this.vector = result;
    }
}
