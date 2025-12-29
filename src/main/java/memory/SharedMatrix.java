package memory;


public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        loadMatrix(matrix, VectorOrientation.ROW_MAJOR); // שימוש בפונקציית עזר למניעת שכפול קוד
    }

    // פונקציית עזר פרטית לטעינת מטריצה (מונעת שכפול קוד בין הבנאי ל-loadRowMajor)
    private void loadMatrix(double[][] matrix, VectorOrientation orientation) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must be non-null");
        }

        SharedVector[] newvectors = new SharedVector[matrix.length];

        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] == null) {
                throw new IllegalArgumentException("All Rows must be non-null");
            }
            if (i > 0 && matrix[i].length != matrix[i - 1].length) {
                throw new IllegalArgumentException(
                    "Rows " + i + " and " + (i - 1) + " must have the same length"
                );
            }

            double[] rowCopy = matrix[i].clone();
            newvectors[i] = new SharedVector(rowCopy, orientation);
        }
        
        this.vectors = newvectors;
    }

    public void loadRowMajor(double[][] matrix) {
        loadMatrix(matrix, VectorOrientation.ROW_MAJOR);
    }

    public void loadColumnMajor(double[][] matrix) {
        loadMatrix(matrix, VectorOrientation.COLUMN_MAJOR);
    }

    public double[][] readRowMajor() {
        SharedVector[] currentVectors = this.vectors; // העתק מקומי למניעת בעיות אם vectors מתחלף באמצע
        double[][] result = new double[currentVectors.length][];
        
        for (int i = 0; i < currentVectors.length; i++) {
            SharedVector vec = currentVectors[i];
            vec.readLock(); // נועלים רק לקריאה
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
            return VectorOrientation.ROW_MAJOR; // ברירת מחדל למטריצה ריקה
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
            // שחרור רק של מה שהצלחנו לנעול עד כה
            for (int j = 0; j < i; j++) {
                try { vecs[j].readUnlock(); } catch (Exception ignored) {}
            }
            throw e;
        }
    }
    
    // בשיטות השחרור אין בעיה, כי משחררים הכל בסוף פעולה תקינה
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
            // שחרור רק של מה שהצלחנו לנעול עד כה
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