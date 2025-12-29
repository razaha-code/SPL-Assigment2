package spl.lae;
import java.io.IOException;

import parser.ComputationNode;
import parser.OutputWriter;

public class Main {
    public static void main(String[] args) throws IOException {
      // TODO: main
      LinearAlgebraEngine engine = new LinearAlgebraEngine(4);
      System.out.println(engine.getWorkerReport());
      ComputationNode answerNode = null;
      OutputWriter outputWriter = new OutputWriter();
      parser.InputParser parserworker = new parser.InputParser();
      try { 
          parser.ComputationNode root = parserworker.parse("example6.json");
          answerNode = engine.run(root);
          System.out.println(engine.getWorkerReport());
      } catch (java.text.ParseException e) {

          outputWriter.write("Error parsing input: " + e.getMessage(), "output.json");
      }   

      if (answerNode != null) {
            double[][] answerMatrix = answerNode.getMatrix();
            outputWriter.write(answerMatrix, "output.json");
      }
  
    }
} 