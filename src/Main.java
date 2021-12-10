import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.io.FileWriter;

public class Main {
    public static void main(final String[] args) throws IOException {
        final int SIZE = 5757;
        String word[] = new String[SIZE];
        long startTime = 0, difference = 0, sum = 0;

        //read word list into array
        ReadFile(word);

        //show number of core available
        System.out.println("Number of core:" + Runtime.getRuntime().availableProcessors());

        //find the best limit to stop parallel merge sort
        int best = 0;
        long high = 99999999;
        int num[] = {1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192};
        System.out.println("\nFind least number of elements to stop parallel merge sort");
        for (int y = 0; y < 14; y++){
            sum = 0;
            for (int i =0; i<5; i++){
                String parSmartMergeList[] = word.clone();
                startTime = System.nanoTime();

                final ForkJoinPool forkJoinPoolSmart = new ForkJoinPool(Runtime.getRuntime().availableProcessors() - 1);
                forkJoinPoolSmart.invoke(new SmartParallelMergeSort(parSmartMergeList,0,parSmartMergeList.length-1, num[y]));
                Arrays.parallelSort(parSmartMergeList);

                difference = System.nanoTime() - startTime;
                if(i<2){
                    sum = sum + difference;
                }
            }
            if (sum/3 < high){
                high = sum/3;
                best = y;
            }
        }
        System.out.println("Best limit: " + num[best]);
        System.out.println("Average execution time: " + high + " ns");

        //clone array to perform smart parallel merge sort performance test
        sum = 0;
        System.out.println("\nSmart Parallel Merge Sort");
        for (int i =0; i<11; i++){
            String parSmartMergeList[] = word.clone();
            startTime = System.nanoTime();

            final ForkJoinPool forkJoinPoolSmart = new ForkJoinPool(Runtime.getRuntime().availableProcessors() - 1);
            forkJoinPoolSmart.invoke(new SmartParallelMergeSort(parSmartMergeList,0,parSmartMergeList.length-1, num[best]));
            Arrays.parallelSort(parSmartMergeList);

            difference = System.nanoTime() - startTime;
            if(i!=0){
                System.out.println(i + "th iteration: " + difference + " ns");
                sum = sum + difference;
            }
            else {
                System.out.println("Warm up iteration: " + difference + " ns");
                WriteFile(parSmartMergeList);
            }
        }
        System.out.println("Average execution time: " + sum/10 + " ns\n");


    }

    public static void ReadFile(String[] array) {
        int x = 0;
        try {
            File myObj = new File("words.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                array[x] = data;
                x++;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void WriteFile(String[] array) throws IOException {
        FileWriter writer = new FileWriter("output.txt");
        for(String str: array) {
            writer.write(str + System.lineSeparator());
        }
        writer.close();
    }
}
