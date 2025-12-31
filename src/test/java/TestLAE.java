import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import parser.ComputationNode;
import parser.ComputationNodeType;
import spl.lae.LinearAlgebraEngine;

public class TestLAE {

    // Helper – compare matrices
    private void assertMatrixEquals(double[][] expected, double[][] actual, double delta) {
        assertEquals(expected.length, actual.length, "Row count mismatch");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].length, actual[i].length, "Column count mismatch at row " + i);
            for (int j = 0; j < expected[i].length; j++) {
                assertEquals(expected[i][j], actual[i][j], delta);
            }
        }
    }

    @Test
    void test_ADD_simple() {
        double[][] A = { {1,2}, {3,4} };
        double[][] B = { {5,6}, {7,8} };

        ComputationNode root = new ComputationNode(ComputationNodeType.ADD,
                List.of(new ComputationNode(A), new ComputationNode(B))
        );

        LinearAlgebraEngine lae = new LinearAlgebraEngine(4);
        ComputationNode resultNode = lae.run(root);
        double[][] result = resultNode.getMatrix();

        double[][] expected = { {6,8}, {10,12} };
        assertMatrixEquals(expected, result, 1e-9);
    }

    @Test
    void test_NEGATE() {
        double[][] A = { {3, -1}, {0, 2} };
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, List.of(new ComputationNode(A)));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);
        ComputationNode resultNode = lae.run(root);
        double[][] result = resultNode.getMatrix();

        double[][] expected = { {-3, 1}, {0, -2} };
        assertMatrixEquals(expected, result, 1e-9);
    }

    @Test
    void test_MULTIPLY_simple() {
        // A: 2×3, B: 3×2
        double[][] A = { {1,2,3}, {4,5,6} };
        double[][] B = { {7,8}, {9,10}, {11,12} };

        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY,
                List.of(new ComputationNode(A), new ComputationNode(B))
        );

        LinearAlgebraEngine lae = new LinearAlgebraEngine(4);
        ComputationNode resultNode = lae.run(root);
        double[][] result = resultNode.getMatrix();

        double[][] expected = {
                {1*7 + 2*9 + 3*11, 1*8 + 2*10 + 3*12},
                {4*7 + 5*9 + 6*11, 4*8 + 5*10 + 6*12}
        };
        assertMatrixEquals(expected, result, 1e-9);
    }

    @Test
    void test_TRANSPOSE() {
        double[][] A = { {1,2,3}, {4,5,6} };

        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, List.of(new ComputationNode(A)));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(3);
        ComputationNode resultNode = lae.run(root);
        double[][] result = resultNode.getMatrix();

        double[][] expected = { {1,4}, {2,5}, {3,6} };
        assertMatrixEquals(expected, result, 1e-9);
    }

    @Test
    void test_ComplexTree_LEFT_ASSOCIATION() {
        // Expression: (A + B) * C

        double[][] A = { {1,1}, {1,1} };
        double[][] B = { {1,2}, {3,4} };
        double[][] C = { {2,0}, {1,2} };

        // ADD(A,B)
        ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD,
                List.of(new ComputationNode(A), new ComputationNode(B))
        );

        // MULTIPLY( ADD(A,B), C )
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY,
                List.of(addNode, new ComputationNode(C))
        );

        LinearAlgebraEngine lae = new LinearAlgebraEngine(4);
        ComputationNode resultNode = lae.run(root);
        double[][] result = resultNode.getMatrix();

        // First A+B:
        double[][] addExpected = { {2,3}, {4,5} };
        // Then (A+B)*C:
        double[][] expected = {
                { 2*2 + 3*1, 2*0 + 3*2 },
                { 4*2 + 5*1, 4*0 + 5*2 }
        };

        // check final only
        assertMatrixEquals(expected, result, 1e-9);
    }
    
    @Test
    void test_ComplexExpression_Add_Transpose_Multiply() {
        // Expression: A + (Transpose(B) * C)

        double[][] A = {
                {1, 0},
                {0, 1}
        };

        // B is 3x2
        double[][] B = {
                {1, 2},
                {3, 4},
                {5, 6}
        };

        // C is 3x2
        double[][] C = {
                {1, 0},
                {0, 1},
                {1, 1}
        };

        // Nodes
        ComputationNode nodeA = new ComputationNode(A);
        ComputationNode nodeB = new ComputationNode(B);
        ComputationNode nodeC = new ComputationNode(C);

        // transpose(B)
        ComputationNode transposeB =
                new ComputationNode(ComputationNodeType.TRANSPOSE, List.of(nodeB));

        // transpose(B) * C
        ComputationNode multNode =
                new ComputationNode(ComputationNodeType.MULTIPLY, List.of(transposeB, nodeC));

        // A + (transpose(B) * C)
        ComputationNode root =
                new ComputationNode(ComputationNodeType.ADD, List.of(nodeA, multNode));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(4);
        ComputationNode resultNode = lae.run(root);
        double[][] result = resultNode.getMatrix();

        double[][] expected = {
                {7, 8},
                {8, 11}
        };

        assertMatrixEquals(expected, result, 1e-9);
    }

}
