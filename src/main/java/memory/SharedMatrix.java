package memory;


public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        loadRowMajor(matrix);
    }

    public void loadRowMajor(double[][] matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must be non-null");
        }
        
        // Handle empty matrix
        //
        if (matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }

        int rows = matrix.length;
        
        // Initialize internal array to hold 'rows' number of vectors
        SharedVector[] newvectors = new SharedVector[rows];

        for (int r = 0; r < rows; r++) {
            if (matrix[r] == null) {
                throw new IllegalArgumentException("All rows must be non-null");
            }
            if (r > 0 && matrix[r].length != matrix[r-1].length) {
                throw new IllegalArgumentException("All rows must have the same length");
            }

            // Simple copy: The row in the 2D array becomes the vector
            double[] rowCopy = matrix[r].clone();
            newvectors[r] = new SharedVector(rowCopy, VectorOrientation.ROW_MAJOR);
        }
        this.vectors = newvectors;
    }

    public void loadColumnMajor(double[][] matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must be non-null");
        }
        
        // Handle empty matrix case
        if (matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }

        int rows = matrix.length;
        int cols = matrix[0].length;

        // Initialize the internal array to hold 'cols' number of vectors
        // (since we are storing columns)
        SharedVector[] newvectors = new SharedVector[cols];

        for (int c = 0; c < cols; c++) {
            double[] columnData = new double[rows];

            // Harvest data for the current column from all rows
            for (int r = 0; r < rows; r++) {
                if (matrix[r] == null || matrix[r].length != cols) {
                    throw new IllegalArgumentException("All rows must be non-null and have the same length");
                }
                // Transpose logic: matrix[row][col] -> columnData[row]
                columnData[r] = matrix[r][c];
            }

            // Create the vector with the correct orientation
            newvectors[c] = new SharedVector(columnData, VectorOrientation.COLUMN_MAJOR);
        }
        this.vectors = newvectors;
    }

    public double[][] readRowMajor() {
        SharedVector[] currentVectors = this.vectors; // snapshot of current vectors
        double[][] result = new double[currentVectors.length][];
        
        for (int i = 0; i < currentVectors.length; i++) {
            SharedVector vec = currentVectors[i];
            vec.readLock();
            try {
                
                double[] vecData = new double[vec.length()];
                for(int j=0; j<vec.length(); j++){
                    vecData[j] = vec.get(j);
                }
                result[i] = vecData; 
            } finally {
                vec.readUnlock();
            }
        }
        return result;
    }

    public SharedVector get(int index) {
        if (index < 0 || index >= vectors.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds");
        }
        return vectors[index];
    }

    public int length() {
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        if (vectors.length == 0) {
            return VectorOrientation.ROW_MAJOR; // default orientation for empty matrix
        }
        return vectors[0].getOrientation();
    }


    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        int i = 0;
        try {
            for (; i < vecs.length; i++) {
                vecs[i].readLock();
            }
        } catch (Exception e) {
            // If an exception occurs, release all previously acquired locks
            for (int j = 0; j < i; j++) {
                try { vecs[j].readUnlock(); } catch (Exception ignored) {}
            }
            throw e;
        }
    }
    
    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        for (SharedVector vec : vecs) {
            vec.readUnlock();
        }
    }
    
    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
          int i = 0;
        try {
            for (; i < vecs.length; i++) {
                vecs[i].writeLock();
            }
        } catch (Exception e) {
            // If an exception occurs, release all previously acquired locks
            for (int j = 0; j < i; j++) {
                try { vecs[j].writeUnlock(); } catch (Exception ignored) {}
            }
            throw e;
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for (SharedVector vec : vecs) {
            vec.writeUnlock();
        }
    }
}