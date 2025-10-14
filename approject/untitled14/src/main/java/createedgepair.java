import java.io.*;
import java.util.*;

public class createedgepair {
    public ArrayList<ArrayList<Integer>> createpairedges() throws  IOException {
        int[][] edges = new int[61][3];
        File file = new File("src/edges.txt");
        FileReader reader = new FileReader(file);
        int i;
        Scanner sc = new Scanner(file);
        for (i = 0; i < 61; i++) {
            String a = sc.nextLine();
            String[] b = a.split(" ");
            for (int j = 0; j < 3; j++) {
                b[j] = b[j].substring(1);
                edges[i][j] = Integer.parseInt(b[j]);
            }
        }
        ArrayList<ArrayList<Integer>> pairedges = new ArrayList<>();
        int s = 0;
        for (i = 0; i < 61; i++) {
            for (int j = 0; j < 61; j++) {
                if (edges[i][2] == edges[j][1]) {
                    ArrayList<Integer> pair = new ArrayList<>();
                    s++;
                    pair.add(s);
                    pair.add(edges[i][0]);
                    pair.add(edges[j][0]);
                    pair.add(edges[i][1]);
                    pair.add(edges[j][1]);
                    pair.add(edges[j][2]);
                    pairedges.add(pair);
                }

            }
        }
        return pairedges;
    }
}


       /// for (i=0;i< pairedges.size();i++) {
          ///  for (int j=0;j< pairedges.get(i).size();j++) {
           ///     System.out.print(pairedges.get(i).get(j)+" ");
       ///     }
         ///   System.out.println();
    ///    }
      ///  for (i=0;i<61;i++) {
         ///   for (int j=0;j<3;j++) {
             ///   System.out.print(edges[i][j]+" ");
         ///   }
         ///   System.out.println();
      ///  }


