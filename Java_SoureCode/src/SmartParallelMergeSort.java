import java.util.concurrent.RecursiveAction;

public class SmartParallelMergeSort extends RecursiveAction {
    private static final int MAX = 1 << 13;
    private final String[] array;
    private final String[] helper;
    private final int low;
    private final int high;
    private final int max;

    public SmartParallelMergeSort(final String[] array, final int low, final int high, final int max) {
        this.array = array;
        helper = new String[array.length];
        this.low = low;
        this.high = high;
        this.max = max;
    }

    @Override
    protected void compute() {
        if (low < high) {
            if (high - low <= max) {
                mergesort(array, helper, low, high);
            } else {
                final int middle = (low + high) / 2;
                final SmartParallelMergeSort left = new SmartParallelMergeSort(array, low, middle, max);
                final SmartParallelMergeSort right = new SmartParallelMergeSort(array, middle + 1, high, max);
                invokeAll(left, right);
                merge(array, helper, low, middle, high);
            }
        }
    }

    static void merge(final String[] array, final String[] helper, final int low, final int middle, final int high) {
        for (int i = low; i <= high; i++) {
            helper[i] = array[i];
        }

        int helperLeft = low;
        int helperRight = middle + 1;
        int current = low;

        while (helperLeft <= middle && helperRight <= high) {
            if (compare(helper[helperLeft],helper[helperRight])) {
                array[current] = helper[helperLeft++];
            } else {
                array[current] = helper[helperRight++];
            }
            current++;
        }

        while (helperLeft <= middle) {
            array[current++] = helper[helperLeft++];
        }
    }

    public static void mergesort(final String[] array, final String[] helper, final int low, final int high) {
        if (low < high) {
            final int middle = (low + high) / 2;
            mergesort(array, helper, low, middle);
            mergesort(array, helper, middle + 1, high);
            merge(array, helper, low, middle, high);
        }
    }

    static boolean compare(String x, String y){
        //x lexicographically less than y, return negative value
        if(x.compareTo(y) < 0){
            return true;
        }
        else if (x.compareTo(y) > 0){
            return false;
        }
        return true;
    }
}
