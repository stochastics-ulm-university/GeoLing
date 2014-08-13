package geoling.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class for working on a to-do list of jobs (objects) in parallel.
 * <p>
 * Note that <code>ThreadedTodoWorker.ENABLED</code> has to be set for parallel
 * execution of code. The number of threads used is automatically determined and
 * may be explicitly controlled by setting the environment variable
 * <code>NUMBER_OF_THREADS</code> or the class variable <code>ThreadedTodoWorker.NUMBER_OF_THREADS</code>.
 * <p>
 * Example (density estimation):
 * <code>
 * // input: (list of) locations, data (passed to an exemplary densityEstimation object)
 * 
 * // hash map where the results will be stored
 * HashMap<Location,Double> resultMap = new HashMap<Location,Double>();
 * 
 * // synchronized result map, required because of parallel write-accesses to the map
 * final Map<Location,Double> resultMapSynchronized = Collections.synchronizedMap(resultMap);
 * 
 * // process locations in parallel, one entry of the to-do list (a location)
 * // is the job to estimate the density for that location using the given data
 * ThreadedTodoWorker.workOnTodoList(locations, new ThreadedTodoWorker.SimpleTodoWorker<Location>() {
 *     public void processTodoItem(Location location) {
 *         double value = densityEstimation.estimate(data, location);
 *         resultMapSynchronized.put(location, new Double(value));
 *     }
 * });
 * 
 * // output: resultMap
 * </code>
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @version 1.3.2, 11.06.2013
 */
public class ThreadedTodoWorker {
	
	/** Determines whether usage of threads is enabled. Disabled by default. */
	public static boolean ENABLED = false;
	
	/** Number of threads that may be used, has to be changed (if necessary) before the first call to <code>getThreadExecutor</code>. */
	public static int NUMBER_OF_THREADS = getDefaultNumberOfThreads();
	
	/** Worker class for <code>workOnTodoList</code>, a single object is used in several threads, so avoid e.g. instance variables! */
	public interface SimpleTodoWorker<E> {
		public void processTodoItem(E todo);
	}
	
	/** Marks whether we have currently running threads. */
	private static boolean threadsCurrentlyRunning = false;
	
	/**
	 * Starts threads that work on items of a to-do list.
	 * This method doesn't use threads if the number of threads is set
	 * to one, or, if we are already in a thread created by this class.
	 * 
	 * @param todos   the items to process
	 * @param worker  the worker object used to process an item
	 */
	public static <E> void workOnTodoList(Collection<E> todos, SimpleTodoWorker<E> worker) {
		workOnTodoList(todos, worker, new ProgressOutput());
	}
	
	/**
	 * Starts threads that work on items of a to-do list.
	 * This method doesn't use threads if the number of threads is set
	 * to one, or, if we are already in a thread created by this class.
	 * 
	 * @param todos    the items to process
	 * @param worker   the worker object used to process an item
	 * @param progress output object for progress messages, e.g.
	 *                 <code>new ProgressOutput(System.out)</code>
	 */
	public static <E> void workOnTodoList(Collection<E> todos, final SimpleTodoWorker<E> worker, final ProgressOutput progress) {
		int todoCount  = todos.size();
		int maxThreads = Math.min(todoCount, ENABLED ? NUMBER_OF_THREADS : 1);
		
		// prepare progress output
		progress.reset(todoCount);
		progress.initCurrent();
		
		// are we allowed to use threads (and there aren't already threads running...)?
		boolean lockObtained;
		if (maxThreads > 1) {
			lockObtained = getLockOnThreadsCurrentlyRunning();
		} else {
			lockObtained = false;
		}
		
		// now begin, either with threads or without
		if (lockObtained) {
			try {
				ExecutorService threadPool = Executors.newCachedThreadPool();
				try {
					// variable to hold the thread references
					ArrayList<Future<?>> futures = new ArrayList<Future<?>>(maxThreads);
					
					// synchronized copy of to-do list
					final List<E> todosSynchronized = Collections.synchronizedList(new LinkedList<E>(todos));
					
					// now start all the threads
					for (int i = 0; i < maxThreads; i++) {
						Runnable thread = new Runnable() {
							public void run() {
								while (!todosSynchronized.isEmpty()) {
									E todo = null;
									try {
										todo = todosSynchronized.remove(0);
									} catch (IndexOutOfBoundsException e) {
										// maybe another thread has fetched the last todo entry since the isEmpty() call
									}
									if (todo != null) {
										worker.processTodoItem(todo);
										progress.incrementCurrent();
									}
								}
							}
						};
						futures.add(threadPool.submit(thread));
					}
					
					try {
						// wait until all threads are finished
						for (Future<?> future : futures) {
							future.get();
						}
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} catch (ExecutionException e) {
						if (e.getCause() != null) {
							// throw original exception created in the thread, wrap it with a
							// RuntimeException only if necessary
							if (e.getCause() instanceof RuntimeException) {
								throw (RuntimeException)e.getCause();
							} else {
								throw new RuntimeException(e.getCause());
							}
						} else {
							throw new RuntimeException(e);
						}
					}
				} finally {
					threadPool.shutdown();
				}
			} finally {
				releaseLockOnThreadsCurrentlyRunning();
			}
		} else {
			// no multi-threading or already in a thread: don't start a new thread
			for (E todo : todos) {
				worker.processTodoItem(todo);
				progress.incrementCurrent();
			}
		}
	}
	
	/**
	 * Starts threads which work on a single index each, which may
	 * be e.g. a coordinate of a slice in a 3D image.
	 * This method doesn't use threads if the number of threads is set
	 * to one, or, if we are already in a thread created by this class.
	 * 
	 * @param startIndex  the first index
	 * @param endIndex    the last index
	 * @param increment   the increment for the indices
	 * @param worker      the worker object used to process a single index
	 */
	public static <E> void workOnIndices(int startIndex, int endIndex, int increment, SimpleTodoWorker<Integer> worker) {
		workOnIndices(startIndex, endIndex, increment, worker, new ProgressOutput());
	}
	
	/**
	 * Starts threads which work on a single index each, which may
	 * be e.g. a coordinate of a slice in a 3D image.
	 * This method doesn't use threads if the number of threads is set
	 * to one, or, if we are already in a thread created by this class.
	 * 
	 * @param startIndex  the first index
	 * @param endIndex    the last index
	 * @param increment   the increment for the indices
	 * @param worker      the worker object used to process a single index
	 * @param progress    output object for progress messages, e.g.
	 *                    <code>new ProgressOutput(System.out)</code>
	 */
	public static <E> void workOnIndices(int startIndex, int endIndex, int increment, SimpleTodoWorker<Integer> worker, ProgressOutput progress) {
		if (increment <= 0) {
			throw new IllegalArgumentException("The increment has to be positive!");
		}
		ArrayList<Integer> list = new ArrayList<Integer>((endIndex-startIndex)/increment + 1);
		for (int i = startIndex; i <= endIndex; i += increment) {
			list.add(i);
		}
		workOnTodoList(list, worker, progress);
	}
	
	/**
	 * Checks whether the lock for using threads is already assigned.
	 * 
	 * @return <code>true</code> if the lock exists,
	 *         <code>false</code> if the lock is available
	 */
	public static synchronized boolean hasLockOnThreadsCurrentlyRunning() {
		return threadsCurrentlyRunning;
	}
	
	/**
	 * Tries to get a lock on using threads.
	 * 
	 * @return <code>true</code> if the lock was obtained
	 */
	private static synchronized boolean getLockOnThreadsCurrentlyRunning() {
		if (threadsCurrentlyRunning) {
			return false;
		} else {
			threadsCurrentlyRunning = true;
			return true;
		}
	}
	
	/**
	 * Releases the lock on using threads.
	 * Call this method only if you obtained the lock before with <code>getLockOnThreadsCurrentlyRunning</code>!
	 */
	private static synchronized void releaseLockOnThreadsCurrentlyRunning() {
		threadsCurrentlyRunning = false;
	}
	
	/**
	 * Detects the number of threads that should be used by default, i.e.,
	 * it is the number of processors in the system, which can be overwritten
	 * by an environment variable <code>NUMBER_OF_THREADS</code>.
	 * 
	 * @return the default number of threads
	 */
	public static int getDefaultNumberOfThreads() {
		int result = Runtime.getRuntime().availableProcessors();
		
		String envNumberOfThreads = System.getenv("NUMBER_OF_THREADS");
		if ((envNumberOfThreads != null) && !envNumberOfThreads.isEmpty()) {
			try {
				result = Integer.parseInt(envNumberOfThreads);
			} catch (NumberFormatException e) {
				System.err.println("Could not parse the NUMBER_OF_THREADS environment variable, it will be ignored.");
			}
		}
		
		return result;
	}
	
}
