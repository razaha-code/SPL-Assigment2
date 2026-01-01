import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import memory.SharedMatrix;
import memory.SharedVector;
import memory.VectorOrientation;

public class TestSharedMatrix {

    private SharedMatrix matrix;

    @BeforeEach
    public void setUp() {
        matrix = new SharedMatrix();
    }

    @AfterEach
    public void tearDown() {
        matrix = null;
    }

    // ---------------------------
    // 1. Constructors & בסיס
    // ---------------------------

    @Test
    public void testDefaultConstructorCreatesEmptyMatrix() {
        assertNotNull(matrix, "Matrix should not be null");
        assertEquals(0, matrix.length(), "Default matrix should have length 0");
        assertEquals(VectorOrientation.ROW_MAJOR,
                matrix.getOrientation(),
                "Empty matrix orientation should default to ROW_MAJOR");
    }

    @Test
    public void testConstructorWithRowMajorMatrix() {
        double[][] data = {
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0}
        };

        SharedMatrix m = new SharedMatrix(data);

        assertEquals(2, m.length(), "Matrix should have 2 row vectors");
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());

        SharedVector row0 = m.get(0);
        SharedVector row1 = m.get(1);

        assertEquals(3, row0.length());
        assertEquals(3, row1.length());

   
    }

    // מקרה קצה – מטריצה ריקה בבנאי
    @Test
    public void testConstructorWithEmptyMatrix() {
        double[][] data = new double[0][0];

        SharedMatrix m = new SharedMatrix(data);

        assertEquals(0, m.length());
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
        double[][] out = m.readRowMajor();
        assertNotNull(out);
        assertEquals(0, out.length);
    }

    // ---------------------------
    // 2. loadRowMajor
    // ---------------------------

    @Test
    public void testLoadRowMajorValidMatrix() {
        double[][] data = {
                {1.0, 2.0},
                {3.0, 4.0}
        };

        matrix.loadRowMajor(data);

        assertEquals(2, matrix.length());
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());

        SharedVector row0 = matrix.get(0);
        SharedVector row1 = matrix.get(1);

        assertEquals(2, row0.length());
        assertEquals(2, row1.length());

        assertEquals(1.0, row0.get(0), 1e-9);
        assertEquals(2.0, row0.get(1), 1e-9);
        assertEquals(3.0, row1.get(0), 1e-9);
        assertEquals(4.0, row1.get(1), 1e-9);
    }

    // דוגמה נוספת – מטריצה 1x3
    @Test
    public void testLoadRowMajorSingleRow() {
        double[][] data = {
                {10.0, 20.0, 30.0}
        };

        matrix.loadRowMajor(data);

        assertEquals(1, matrix.length());
        SharedVector row0 = matrix.get(0);
        assertEquals(3, row0.length());
        
    }

    // מקרה קצה – null או שורות לא תקינות
    @Test
    public void testLoadRowMajorNullMatrixThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> matrix.loadRowMajor(null));
    }

    @Test
    public void testLoadRowMajorWithNullRowThrows() {
        double[][] data = {
                {1.0, 2.0},
                null
        };

        assertThrows(IllegalArgumentException.class,
                () -> matrix.loadRowMajor(data));
    }

    @Test
    public void testLoadRowMajorWithJaggedRowsThrows() {
        double[][] data = {
                {1.0, 2.0},
                {3.0}
        };

        assertThrows(IllegalArgumentException.class,
                () -> matrix.loadRowMajor(data));
    }

    // ---------------------------
    // 3. loadColumnMajor
    // ---------------------------

    @Test
    public void testLoadColumnMajorValidMatrix() {
        double[][] data = {
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0}
        };

        matrix.loadColumnMajor(data);

        // מאחסנים וקטורים של עמודות
        assertEquals(3, matrix.length(), "Should have 3 column vectors");
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());

        SharedVector col0 = matrix.get(0);
        SharedVector col1 = matrix.get(1);
        SharedVector col2 = matrix.get(2);

        assertEquals(2, col0.length());
        assertEquals(2, col1.length());
        assertEquals(2, col2.length());

        assertEquals(1.0, col0.get(0), 1e-9);
        assertEquals(4.0, col0.get(1), 1e-9);

        assertEquals(2.0, col1.get(0), 1e-9);
        assertEquals(5.0, col1.get(1), 1e-9);

        assertEquals(3.0, col2.get(0), 1e-9);
        assertEquals(6.0, col2.get(1), 1e-9);
    }

    // דוגמה נוספת – מטריצה 3x1 (עמודה בודדת)
    @Test
    public void testLoadColumnMajorSingleColumn() {
        double[][] data = {
                {7.0},
                {8.0},
                {9.0}
        };

        matrix.loadColumnMajor(data);

        assertEquals(1, matrix.length(), "Should have 1 column vector");
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());

        SharedVector col0 = matrix.get(0);
        assertEquals(3, col0.length());
        assertEquals(7.0, col0.get(0), 1e-9);
        assertEquals(8.0, col0.get(1), 1e-9);
        assertEquals(9.0, col0.get(2), 1e-9);
    }

    // מקרי קצה/שגיאה ב-loadColumnMajor
    @Test
    public void testLoadColumnMajorNullMatrixThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> matrix.loadColumnMajor(null));
    }

    @Test
    public void testLoadColumnMajorWithNullOrJaggedRowsThrows() {
        double[][] data = {
                {1.0, 2.0},
                {3.0}    // אורך שונה
        };

        assertThrows(IllegalArgumentException.class,
                () -> matrix.loadColumnMajor(data));
    }

    @Test
    public void testLoadColumnMajorEmptyMatrix() {
        double[][] data = new double[0][0];

        matrix.loadColumnMajor(data);

        assertEquals(0, matrix.length());
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation(),
                "Empty matrix orientation defaults to ROW_MAJOR");
    }

    // ---------------------------
    // 4. readRowMajor
    // ---------------------------

    @Test
    public void testReadRowMajorAfterLoadRowMajor() {
        double[][] data = {
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0}
        };

        matrix.loadRowMajor(data);
        double[][] out = matrix.readRowMajor();

        assertNotNull(out);
        assertEquals(2, out.length);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, out[0], 1e-9);
        assertArrayEquals(new double[]{4.0, 5.0, 6.0}, out[1], 1e-9);
    }

    // דוגמה נוספת – שורה אחת
    @Test
    public void testReadRowMajorSingleRow() {
        double[][] data = {
                {10.0, 20.0}
        };

        matrix.loadRowMajor(data);
        double[][] out = matrix.readRowMajor();

        assertEquals(1, out.length);
        assertArrayEquals(new double[]{10.0, 20.0}, out[0], 1e-9);
    }

    // מקרה קצה – מטריצה ריקה
    @Test
    public void testReadRowMajorOnEmptyMatrix() {
        double[][] out = matrix.readRowMajor();

        assertNotNull(out);
        assertEquals(0, out.length);
    }

    // ---------------------------
    // 5. get() & length() & orientation()
    // ---------------------------

    @Test
    public void testGetValidIndex() {
        double[][] data = {
                {1.0, 2.0},
                {3.0, 4.0}
        };

        matrix.loadRowMajor(data);

        SharedVector row0 = matrix.get(0);
        SharedVector row1 = matrix.get(1);

        assertEquals(2, row0.length());
        assertEquals(2, row1.length());
    }

    // מקרה קצה – get עם אינדקס לא חוקי
    @Test
    public void testGetInvalidIndexThrows() {
        double[][] data = {
                {1.0, 2.0},
                {3.0, 4.0}
        };

        matrix.loadRowMajor(data);

        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(2));
    }

    @Test
    public void testLengthAndOrientationUpdates() {
        double[][] data = {
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0}
        };

        matrix.loadRowMajor(data);
        assertEquals(2, matrix.length());
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());

        matrix.loadColumnMajor(data);
        assertEquals(3, matrix.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }
}