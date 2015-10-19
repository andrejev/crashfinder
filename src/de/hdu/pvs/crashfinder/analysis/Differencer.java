package de.hdu.pvs.crashfinder.analysis;

public class Differencer {
	 
/*    //private List<String> originalList = Arrays.asList("aaa", "bbb", "ccc");

    // Helper method for get the file content
    private static List<String> fileToLines(String filename) {
            List<String> lines = new LinkedList<String>();
            String line = "";
            try {
                    BufferedReader in = new BufferedReader(new FileReader(filename));
                    while ((line = in.readLine()) != null) {
                            lines.add(line);
                    }
            } catch (IOException e) {
                    e.printStackTrace();
            }
            return lines;
    }

    public static void main(String[] args) {
            // At first, parse the unified diff file and get the patch
            Patch patch = DiffUtils.parseUnifiedDiff(fileToLines("/home/felix/.jenkins/jobs/fail/workspace/diff2.diff"));
            
        	System.out.println(patch.getDeltas().get(0).getRevised());
            for (Delta item:patch.getDeltas()){
            	Delta x= item;
            	System.out.println(x);
            }
            // Then apply the computed patch to the given text
            //List result = DiffUtils.patch(original, patch);
            /// Or we can call patch.applyTo(original). There is no difference.
    }*/
}