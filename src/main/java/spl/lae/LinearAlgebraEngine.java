package spl.lae;


import java.util.List;

import memory.SharedMatrix;
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

        // TODO: resolve computation tree step by step until final matrix is produced

        ComputationNodeType nodeType = computationRoot.getNodeType();
        if (nodeType == ComputationNodeType.MATRIX) {
            return computationRoot;

        }  else {

            computationRoot.associativeNesting();
            ComputationNode Resolver = computationRoot.findResolvable();
            Resolver.getNodeType();
            // לסנכרן פה את המטריצות המוחזרות לילדיי הצומת
            while (Resolver != null && Resolver.getNodeType() != ComputationNodeType.MATRIX) {
                loadAndCompute(Resolver);
                computationRoot.associativeNesting();
                Resolver = computationRoot.findResolvable();
            }
            return Resolver;
        }

    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor

        for (int i = 0; i < node.getChildren().size(); i++) {
            ComputationNode child = node.getChildren().get(i);
            double[][] childMatrix = child.getMatrix();
            if (i == 0) {
                leftMatrix.loadRowMajor(childMatrix);
            } 
            else if (i == 1) {
                rightMatrix.loadRowMajor(childMatrix);
            }
        }

        if (node.getNodeType() == ComputationNodeType.ADD) {
            List<Runnable> tasks = createAddTasks();
            executor.submitAll(tasks);
        }

        else if (node.getNodeType() == ComputationNodeType.NEGATE) {
            List<Runnable> tasks = createNegateTasks();
            executor.submitAll(tasks);
        }
        else if (node.getNodeType() == ComputationNodeType.TRANSPOSE) {
            List<Runnable> tasks = createTransposeTasks();
            executor.submitAll(tasks);
        }
        else if (node.getNodeType() == ComputationNodeType.MULTIPLY) {
            List<Runnable> tasks = createMultiplyTasks();
            executor.submitAll(tasks);
            
        }
        // לסנכרן פה את המטריצות המוחזרות לילדיי הצומת
        for (int i = 0; i < node.getChildren().size(); i++) {
            if(i == 0){
                node.getChildren().get(i).resolve(leftMatrix.readRowMajor());
            }
            else if (i == 1){
                node.getChildren().get(i).resolve(rightMatrix.readRowMajor());
            }            
        }

    }
    
    public List<Runnable> createAddTasks() {

        // TODO: return tasks that perform row-wise addition


        return null;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication

        return null;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        return null;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows

        return null;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }
}
