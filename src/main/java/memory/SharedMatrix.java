package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
        vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        
        this.vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            
            try {
                if (matrix[i] == null) {
                    throw new IllegalArgumentException("All Rows must be non-null");
                }
               
                if (i > 0 && matrix[i].length != matrix[i - 1].length) {
                    throw new IllegalArgumentException("Rows " + i + " and " + (i-1)  + " must have the same length");
                }

                SharedVector vec = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
                this.vectors[i] = vec;



            } catch (IllegalArgumentException e) {
                throw e;
            }
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        
        vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            vectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix

        vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {   
            vectors[i] = new SharedVector(matrix[i], VectorOrientation.COLUMN_MAJOR);
        }
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        double[][] result = new double[vectors.length][];
        for (int i = 0; i < vectors.length; i++) {
            vectors[i].readLock();
            result[i] = vectors[i].readRowMajor();
            vectors[i].readUnlock();
        }
        return result;
    }

    public SharedVector get(int index) {
        // TODO: Return vectors at index

        this.vectors[index].readLock();

        try {
            return this.vectors[index];
            
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        finally {
            this.vectors[index].readUnlock();
        }
    }

    public int length() {

        // return the length of the matrix
        return vectors.length;


    }

    public VectorOrientation getOrientation() {
            // reuturn the matrix orientation

            this.vectors[0].readLock();
            try {

                if (this.vectors[0].getOrientation() == VectorOrientation.ROW_MAJOR) {
                    return VectorOrientation.ROW_MAJOR;

                } else {
                    return VectorOrientation.COLUMN_MAJOR;
                }
            } finally {
                this.vectors[0].readUnlock();
            }

    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector

        for (SharedVector vec : vecs) {
            try {
                vec.readLock();
            } catch (Exception e) {
                releaseAllVectorReadLocks(vecs);
                throw e;
            }
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for (SharedVector vec : vecs) {
            try {
                vec.readUnlock();

            } catch (Exception e) {
                // ignore
                throw e;
            }
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for (SharedVector vec : vecs) {
            vec.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for (SharedVector vec : vecs) {
            vec.writeUnlock();
        }
    }
}
