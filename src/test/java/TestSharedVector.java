import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import memory.SharedVector;
import memory.VectorOrientation;

public class TestSharedVector {

    private SharedVector sharedVector;

    @BeforeEach
    public void setUp() {
        sharedVector = new SharedVector(new double[]{}, VectorOrientation.ROW_MAJOR);
    }

    @AfterEach
    public void tearDown() {
        sharedVector = null;
    }

    // ---------------------------
    // 1. Vector Initialization
    // ---------------------------

    @Test
    public void testVectorInitialization() {
        assertNotNull(sharedVector, "SharedVector should not be null");
    }

    // דוגמה נוספת – וקטור עם ערכים
    @Test
    public void testVectorInitializationWithValues() {
        SharedVector v = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);

        assertNotNull(v, "SharedVector with values should not be null");
        assertEquals(3, v.length());
        assertEquals(1.0, v.get(0), 0.0);
        assertEquals(2.0, v.get(1), 0.0);
        assertEquals(3.0, v.get(2), 0.0);
    }

    // מקרה קצה – וקטור ריק עם COLUMN_MAJOR
    @Test
    public void testVectorInitializationEmptyColumnMajor() {
        SharedVector v = new SharedVector(new double[]{}, VectorOrientation.COLUMN_MAJOR);

        assertNotNull(v, "Empty SharedVector should not be null");
        assertEquals(0, v.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
    }

    // ---------------------------
    // 2. Transpose
    // ---------------------------

    @Test
    public void testTranspose() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        v.transpose();

        // orientation מתהפך
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());

        // הערכים נשארים זהים באותו סדר
        assertEquals(1.0, v.get(0), 0.0);
        assertEquals(2.0, v.get(1), 0.0);
        assertEquals(3.0, v.get(2), 0.0);
    }

    // דוגמה נוספת – איבר בודד
    @Test
    public void testTransposeSingleElement() {
        SharedVector v = new SharedVector(new double[]{42}, VectorOrientation.ROW_MAJOR);
        v.transpose();

        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        assertEquals(1, v.length());
        assertEquals(42.0, v.get(0), 0.0);
    }

    // מקרה קצה – וקטור ריק
    @Test
    public void testTransposeEmptyVector() {
        SharedVector v = new SharedVector(new double[]{}, VectorOrientation.ROW_MAJOR);
        v.transpose();

        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        assertEquals(0, v.length());
    }

    // ---------------------------
    // 3. Negate
    // ---------------------------

    @Test
    public void testNegate() {
        SharedVector neg = new SharedVector(new double[]{3, -1, 0}, VectorOrientation.ROW_MAJOR);
        neg.negate();

        assertEquals(-3.0, neg.get(0), 0.0);
        assertEquals(1.0,  neg.get(1), 0.0);
        assertEquals(0.0,  neg.get(2), 0.0);
    }

    // דוגמה נוספת – רק ערכים חיוביים
    @Test
    public void testNegatePositiveValues() {
        SharedVector v = new SharedVector(new double[]{1.5, 2.5}, VectorOrientation.ROW_MAJOR);
        v.negate();

        assertEquals(-1.5, v.get(0), 0.0);
        assertEquals(-2.5, v.get(1), 0.0);
    }

    // מקרה קצה – וקטור ריק
    @Test
    public void testNegateEmptyVector() {
        SharedVector v = new SharedVector(new double[]{}, VectorOrientation.ROW_MAJOR);
        v.negate();  // לא אמור לקרוס

        assertEquals(0, v.length());
    }

    // ---------------------------
    // 4. Add
    // ---------------------------

    @Test
    public void testAdd() {
        SharedVector a = new SharedVector(new double[]{2, 4, 6}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{1, 1, 1}, VectorOrientation.ROW_MAJOR);

        a.add(b);

        assertEquals(3, a.length());
        assertEquals(3.0, a.get(0), 0.0);
        assertEquals(5.0, a.get(1), 0.0);
        assertEquals(7.0, a.get(2), 0.0);
    }

    // דוגמה נוספת – חיבור עם ערכים שליליים
    @Test
    public void testAddWithNegativeValues() {
        SharedVector a = new SharedVector(new double[]{2, -4, 6}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{-1, 2, -3}, VectorOrientation.ROW_MAJOR);

        a.add(b);

        assertEquals(3, a.length());
        assertEquals(1.0,  a.get(0), 0.0);
        assertEquals(-2.0, a.get(1), 0.0);
        assertEquals(3.0,  a.get(2), 0.0);
    }

    // מקרה קצה – חיבור וקטור עם וקטור אפסים
    @Test
    public void testAddWithZeroVector() {
        SharedVector a = new SharedVector(new double[]{5, -2, 0}, VectorOrientation.ROW_MAJOR);
        SharedVector zeros = new SharedVector(new double[]{0, 0, 0}, VectorOrientation.ROW_MAJOR);

        a.add(zeros);

        assertEquals(3, a.length());
        assertEquals(5.0,  a.get(0), 0.0);
        assertEquals(-2.0, a.get(1), 0.0);
        assertEquals(0.0,  a.get(2), 0.0);
    }

    // ---------------------------
    // 5. Dot Product
    // ---------------------------

    @Test
    public void testDotProduct() {
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);

        double result = row.dot(col);

        // 1*4 + 2*5 + 3*6 = 32
        assertEquals(32.0, result, 1e-9);
    }

    // דוגמה נוספת – וקטורים אורתוגונליים (תוצאה 0)
    @Test
    public void testDotProductOrthogonal() {
        SharedVector row = new SharedVector(new double[]{1, 0}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{0, 1}, VectorOrientation.COLUMN_MAJOR);

        double result = row.dot(col);

        assertEquals(0.0, result, 1e-9);
    }

    // מקרה קצה – אחד הוקטורים הוא וקטור אפסים
    @Test
    public void testDotProductWithZeroVector() {
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector zeros = new SharedVector(new double[]{0, 0, 0}, VectorOrientation.COLUMN_MAJOR);

        double result = row.dot(zeros);

        assertEquals(0.0, result, 1e-9);
    }
}
