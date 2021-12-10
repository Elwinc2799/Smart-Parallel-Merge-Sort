from __future__ import print_function
import time
from contextlib import contextmanager
from multiprocessing import Manager, Pool
import multiprocessing
import json


class Timer(object):
    """
    Record timing information.
    """

    def __init__(self, *steps):
        self._time_per_step = dict.fromkeys(steps)

    def __getitem__(self, item):
        return self.time_per_step[item]

    @property
    def time_per_step(self):
        return {
            step: elapsed_time
            for step, elapsed_time in self._time_per_step.items()
            if elapsed_time is not None and elapsed_time > 0
        }

    def start_for(self, step):
        self._time_per_step[step] = -time.time()

    def stop_for(self, step):
        self._time_per_step[step] += time.time()


def merge_sort_multiple(results, array):
    results.append(merge_sort(array))


def merge_multiple(results, array_part_left, array_part_right):
    results.append(merge(array_part_left, array_part_right))


def merge_sort(array):
    array_length = len(array)

    if array_length <= 1:
        return array

    middle_index = int(array_length / 2)
    left = array[0:middle_index]
    right = array[middle_index:]

    left = merge_sort(left)
    right = merge_sort(right)
    return merge(left, right)


def merge(left, right):
    sorted_list = []
    # We create shallow copies so that we do not mutate
    # the original objects.
    left = left[:]
    right = right[:]
    # We do not have to actually inspect the length,
    # as empty lists truth value evaluates to False.
    # This is for algorithm demonstration purposes.
    while len(left) > 0 or len(right) > 0:
        if len(left) > 0 and len(right) > 0:
            if left[0] <= right[0]:
                sorted_list.append(left.pop(0))
            else:
                sorted_list.append(right.pop(0))
        elif len(left) > 0:
            sorted_list.append(left.pop(0))
        elif len(right) > 0:
            sorted_list.append(right.pop(0))
    return sorted_list


@contextmanager
def process_pool(size):
    """Create a process pool and block until
    all processes have completed.
    Note: see also concurrent.futures.ProcessPoolExecutor"""
    pool = Pool(size)
    yield pool
    pool.close()
    pool.join()


def parallel_merge_sort(array, process_counts):
    timer = Timer('sort', 'merge', 'total')
    timer.start_for('total')
    timer.start_for('sort')

    # Divide the list in chunks
    step = int(length / process_counts)

    # Instantiate a multiprocessing.Manager object to
    # store the output of each process.
    # See example here
    # http://docs.python.org/library/multiprocessing.html#sharing-state-between-processes
    manager = Manager()
    results = manager.list()

    with process_pool(size=process_counts) as pool:
        for n in range(process_counts):
            # We create a new Process object and assign the
            # merge_sort_multiple function to it,
            # using as input a sublist
            if n < process_counts - 1:
                chunk = array[n * step:(n + 1) * step]
            else:
                # Get the remaining elements in the list
                chunk = array[n * step:]
            pool.apply_async(merge_sort_multiple, (results, chunk))

    timer.stop_for('sort')

    print('Performing final merge.')

    timer.start_for('merge')

    # For a core count greater than 2, we can use multiprocessing
    # again to merge sub-lists in parallel.
    while len(results) > 1:
        with process_pool(size=process_counts) as pool:
            pool.apply_async(merge_multiple, (results, results.pop(0), results.pop(0)))

    timer.stop_for('merge')
    timer.stop_for('total')

    final_sorted_list = results[0]

    return timer, final_sorted_list


if __name__ == '__main__':

    process_count = multiprocessing.cpu_count()

    with open('datasets.txt') as f:
        datasets = f.readlines()
    data_unsorted = datasets

    main_timer = Timer('single_core', 'list_generation')

    # Randomize the length of our list
    length = len(data_unsorted)

    print(f'List length: {length}')

    # Start timing the single-core procedure
    main_timer.start_for('single_core')
    single = merge_sort(data_unsorted)
    main_timer.stop_for('single_core')

    # Create a copy first due to mutation
    data_sorted = data_unsorted[:]
    data_sorted.sort()

    # Comparison with Python list sort method
    # serves also as validation of our implementation.
    print('Verification of sorting algorithm:', data_sorted == single)
    print('Single Core elapsed time: %4.6f sec' % main_timer['single_core'])

    print('Starting parallel sort.')

    total = 0

    for i in range(0, 11):
        parallel_timer, parallel_sorted_list = \
            parallel_merge_sort(data_unsorted, process_count)

        if i == 0:
            print("Warming up:")

            textFile = open("sorted.txt", "w")
            for element in parallel_sorted_list:
                textFile.write(element)

        else:
            print(f"Iteration {i}:")
            total = total + parallel_timer['total']

        print('Final merge duration: %4.6f sec' % parallel_timer['merge'])
        print('Parallel Sorted arrays equal:', parallel_sorted_list == data_sorted)
        print('%d-Core elapsed time: %4.6f sec' % (process_count, parallel_timer['total']))

    print(f'Average Time: {total / 10}')
