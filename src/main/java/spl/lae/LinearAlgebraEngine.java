package spl.lae;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import memory.SharedMatrix;
import memory.SharedVector;
import parser.ComputationNode;
import parser.ComputationNodeType;
import scheduling.TiredExecutor;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count

        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
    // If the root is already a concrete matrix, no computation is needed
        if (computationRoot.getNodeType() == ComputationNodeType.MATRIX) {
            return computationRoot;
        }
        while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {
            // Find the next node ready for computation (all children are matrices)
            ComputationNode resolver = computationRoot.findResolvable();
            if (resolver == null) {
                break; 
            }
            // Handle operations with >2 operands (e.g., A+B+C -> (A+B)+C)
            resolver.associativeNesting();
            // Check if the node is still resolvable after nesting.
            // Nesting might have introduced a new operator node as a child.
            boolean isStillResolvable = true;
            for (ComputationNode child : resolver.getChildren()) {
                if (child.getNodeType() != ComputationNodeType.MATRIX) {
                    isStillResolvable = false;
                    break;
                }
            }
            // Only compute if all children are concrete matrices
            if (isStillResolvable) {
                loadAndCompute(resolver);
            }
        }
        try {
            executor.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        // 1. Load operand matrices into shared memory
        for (int i = 0; i < node.getChildren().size(); i++) {
            ComputationNode child = node.getChildren().get(i);
            double[][] childMatrix = child.getMatrix();

            if (i == 0) {
                // Left matrix is always loaded in Row-Major order
                leftMatrix.loadRowMajor(childMatrix);
            } 
            else if (i == 1) {
                if (node.getNodeType() == ComputationNodeType.MULTIPLY) {
                    rightMatrix.loadColumnMajor(childMatrix);
                } else {
                    rightMatrix.loadRowMajor(childMatrix);
                }
            }
        }

        // 2. Create computation tasks based on the operator
        List<Runnable> tasks = null;
        if (node.getNodeType() == ComputationNodeType.ADD) {
            tasks = createAddTasks();
        }
        else if (node.getNodeType() == ComputationNodeType.NEGATE) {
            tasks = createNegateTasks();
        }
        else if (node.getNodeType() == ComputationNodeType.MULTIPLY) {
            tasks = createMultiplyTasks();
        }
        else if (node.getNodeType() == ComputationNodeType.TRANSPOSE) {
            tasks = createTransposeTasks();
        }

        // 3. Submit tasks to the thread pool and wait for completion
        if (tasks != null && !tasks.isEmpty()) executor.submitAll(tasks);
        
        // 4. Retrieve the result and resolve the current node
        node.resolve(leftMatrix.readRowMajor());
    }
    
    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        List<Runnable> tasks = new java.util.ArrayList<>();

        // 1. Validation: Check if matrices have compatible dimensions and orientation
        if (leftMatrix.length() != rightMatrix.length()) {
            throw new IllegalArgumentException("Matrix row count mismatch");
        }
        
        if (leftMatrix.length() > 0) {
            if (leftMatrix.get(0).getOrientation() != rightMatrix.get(0).getOrientation()) {
                throw new IllegalArgumentException("Orientation mismatch: Both matrices must have the same orientation");
            }
            if (leftMatrix.get(0).length() != rightMatrix.get(0).length()) {
                throw new IllegalArgumentException("Matrix column count mismatch");
            }
        }

        // 2. Task Creation: Create a task for each vector (row/column)
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIndex = i;
            
            Runnable task = () -> {
                SharedVector leftVector = leftMatrix.get(rowIndex);
                SharedVector rightVector = rightMatrix.get(rowIndex);

                // Locking: We are modifying 'left' (WriteLock) and reading 'right' (ReadLock)
                leftVector.writeLock();
                try {
                    rightVector.readLock();
                    try {
                        leftVector.add(rightVector);
                    } finally {
                        rightVector.readUnlock();
                    }
                } finally {
                    leftVector.writeUnlock();
                }
            };
            
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        List<Runnable> tasks = new java.util.ArrayList<>();

        // 1. Validation
        if (leftMatrix.length() == 0 || rightMatrix.length() == 0) return tasks;

        // Check inner dimensions (Left Width == Right Height)
        if (leftMatrix.get(0).length() != rightMatrix.get(0).length()) {
            throw new IllegalArgumentException("Matrix dimension mismatch: Inner dimensions must agree.");
        }

        int rowCount = leftMatrix.length();

        // 2. Task Creation: One task per row of the left matrix
        for (int i = 0; i < rowCount; i++) {
            final int rowIdx = i;

            Runnable worker = () -> {
                SharedVector currentRow = leftMatrix.get(rowIdx);

                // Acquire Write Lock on the target row (since it will be modified in-place)
                currentRow.writeLock();
                try {
                    int rLength = rightMatrix.length();

                    // Acquire Read Locks for ALL vectors in the right matrix
                    for (int j = 0; j < rLength; j++) {
                        rightMatrix.get(j).readLock();
                    }

                    try {
                        currentRow.vecMatMul(rightMatrix);

                    } finally {
                        // Release Read Locks for all vectors in the right matrix
                        for (int j = 0; j < rLength; j++) {
                            rightMatrix.get(j).readUnlock();
                        }
                    }
                } finally {
                    // Release Write Lock on the target row
                    currentRow.writeUnlock();
                }
            };
            tasks.add(worker);
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        List<Runnable> tasks = new java.util.ArrayList<>();

        // Task Creation: Create a task for each vector (row/column)
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIndex = i;
            
            Runnable task = () -> {
                SharedVector vector = leftMatrix.get(rowIndex);
                // Locking: We are modifying 'left' (WriteLock)
                vector.writeLock();
                try {
                    vector.negate();
                } finally {
                    vector.writeUnlock();
                }
            };
            
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        List<Runnable> tasks = new ArrayList<>();
        // 1. Validation
        if (leftMatrix.length() == 0) return tasks;
        int originalRows = leftMatrix.length();
        int originalCols = leftMatrix.get(0).length();
        // 2. Prepare a temporary array to hold the transposed data.
        // This array is local to the function scope but effectively final, so tasks can access it.
        double[][] tempMatrix = new double[originalCols][originalRows];
        // 3. Atomic counter to track completion of tasks.
        // This ensures thread-safe coordination to update the main matrix only once.
        AtomicInteger completionCounter = new AtomicInteger(0);
        int totalTasks = originalCols;
        // 4. Task Creation: Create one task per NEW row (which corresponds to an ORIGINAL column)
        for (int i = 0; i < originalCols; i++) {
            final int newRowIndex = i;
            Runnable task = () -> {
                double[] newRowData = new double[originalRows];
                // Harvest data: collect the i-th element from every original row
                for (int r = 0; r < originalRows; r++) {
                    SharedVector oldRow = leftMatrix.get(r);
                    oldRow.readLock();
                    try {
                        newRowData[r] = oldRow.get(newRowIndex);
                    } finally {
                        oldRow.readUnlock();
                    }
                }

                // Store the result in the shared temporary matrix.
                // No locking needed here as each task writes to a unique index.
                tempMatrix[newRowIndex] = newRowData;

                // 5. Finalize: The last task to finish updates the main matrix.
                if (completionCounter.incrementAndGet() == totalTasks) {
                    // Critical Section: Replace the internal structure of leftMatrix with the transposed one.
                    leftMatrix.loadRowMajor(tempMatrix);
                }
            };
            tasks.add(task);
        }
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }
}
