/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.processing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

public abstract class JobManager implements Runnable {

	/* queue of jobs to execute */
	protected List<IJob> awaitingJobs = new LinkedList<>();

	protected volatile boolean executing;

	/* background processing */
	protected volatile Thread processingThread;
	protected Job progressJob;

	/* counter indicating whether job execution is enabled or not, disabled if <= 0
	    it cannot go beyond 1 */
	private int enableCount = 1;

	public static boolean VERBOSE = false;
	/* flag indicating that the activation has completed */
	public boolean activated = false;

	private int awaitingClients = 0;

	private final Object idleMonitor = new Object();

	/**
	 * Invoked exactly once, in background, before starting processing any job
	 */
	public void activateProcessing() {
		this.activated = true;
	}
	/**
	 * Answer the amount of awaiting jobs.
	 */
	public synchronized int awaitingJobsCount() {
		// pretend busy in case concurrent job attempts performing before activated
		return this.activated ? this.awaitingJobs.size() : 1;
	}
	/**
	 * Answers the first job in the queue, or null if there is no job available
	 * Until the job has completed, the job manager will keep answering the same job.
	 */
	public synchronized IJob currentJob() {
		if (this.enableCount > 0 && !this.awaitingJobs.isEmpty()) {
			return this.awaitingJobs.get(0);
		}
		return null;
	}
	public synchronized void disable() {
		this.enableCount--;
		if (VERBOSE)
			Util.verbose("DISABLING background indexing"); //$NON-NLS-1$
	}

	/**
	 * @return {@code true} if the job manager is enabled
	 */
	public synchronized boolean isEnabled() {
		return this.enableCount > 0;
	}

	/**
	 * Remove the index from cache for a given project.
	 * Passing null as a job family discards them all.
	 */
	public void discardJobs(String jobFamily) {

		if (VERBOSE)
			Util.verbose("DISCARD   background job family - " + jobFamily); //$NON-NLS-1$

		try {
			IJob currentJob;
			// cancel current job if it belongs to the given family
			synchronized(this){
				currentJob = currentJob();
				disable();
			}
			if (currentJob != null && (jobFamily == null || currentJob.belongsTo(jobFamily))) {
				currentJob.cancel();

				// wait until current active job has finished
				while (this.processingThread != null && this.executing){
					try {
						if (VERBOSE)
							Util.verbose("-> waiting end of current background job - " + currentJob); //$NON-NLS-1$
						Thread.sleep(50);
					} catch(InterruptedException e){
						// ignore
					}
				}
			}

			synchronized(this) {
				Iterator<IJob> it = this.awaitingJobs.iterator();
				while (it.hasNext()) {
					currentJob = it.next();
					if (jobFamily == null || currentJob.belongsTo(jobFamily)) {
						if (VERBOSE) {
							Util.verbose("-> discarding background job  - " + currentJob); //$NON-NLS-1$
						}
						currentJob.cancel();
						it.remove();
					}
				}
			}
		} finally {
			enable();
		}
		if (VERBOSE)
			Util.verbose("DISCARD   DONE with background job family - " + jobFamily); //$NON-NLS-1$
	}
	public synchronized void enable() {
		this.enableCount++;
		if (VERBOSE)
			Util.verbose("ENABLING  background indexing"); //$NON-NLS-1$
		notifyAll(); // wake up the background thread if it is waiting (context must be synchronized)
	}
	protected synchronized boolean isJobWaiting(IJob request) {
		if(this.awaitingJobs.size() <= 1) {
			return false;
		}
		// Start at the end and go backwards
		ListIterator<IJob> iterator = this.awaitingJobs.listIterator(this.awaitingJobs.size());
		IJob first = this.awaitingJobs.get(0);
		while (iterator.hasPrevious()) {
			IJob job = iterator.previous();
			// don't check first job, as it may have already started
			if(job == first) {
				break;
			}
			if (request.equals(job)) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Advance to the next available job, once the current one has been completed.
	 * Note: clients awaiting until the job count is zero are still waiting at this point.
	 */
	protected synchronized void moveToNextJob() {
		//if (!enabled) return;

		if (!this.awaitingJobs.isEmpty()) {
			this.awaitingJobs.remove(0);
			if (awaitingJobsCount() == 0) {
				synchronized (this) {
					this.notifyAll();
				}
			}
		}
	}
	/**
	 * When idle, give chance to do something
	 */
	protected void notifyIdle(long idlingTime) {
		// do nothing
	}
	/**
	 * This API is allowing to run one job in concurrence with background processing.
	 * Indeed since other jobs are performed in background, resource sharing might be
	 * an issue.Therefore, this functionality allows a given job to be run without
	 * colliding with background ones.
	 * Note: multiple thread might attempt to perform concurrent jobs at the same time,
	 *            and should synchronize (it is deliberately left to clients to decide whether
	 *            concurrent jobs might interfere or not. In general, multiple read jobs are ok).
	 *
	 * Waiting policy can be:
	 * 		IJobConstants.ForceImmediateSearch
	 * 		IJobConstants.CancelIfNotReadyToSearch
	 * 		IJobConstants.WaitUntilReadyToSearch
	 *
	 */
	public boolean performConcurrentJob(IJob searchJob, int waitingPolicy, IProgressMonitor monitor) {
		if (VERBOSE)
			Util.verbose("STARTING  concurrent job - " + searchJob); //$NON-NLS-1$

		searchJob.ensureReadyToRun();

		boolean status = IJob.FAILED;
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor);
			if (awaitingJobsCount() > 0) {
				switch (waitingPolicy) {

					case IJob.ForceImmediate :
						if (VERBOSE)
							Util.verbose("-> NOT READY - forcing immediate - " + searchJob);//$NON-NLS-1$
						try {
							disable(); // pause indexing
							status = searchJob.execute(subMonitor);
						} finally {
							enable();
						}
						if (VERBOSE)
							Util.verbose("FINISHED  concurrent job - " + searchJob); //$NON-NLS-1$
						return status;

					case IJob.CancelIfNotReady :
						if (VERBOSE)
							Util.verbose("-> NOT READY - cancelling - " + searchJob); //$NON-NLS-1$
						if (VERBOSE)
							Util.verbose("CANCELED concurrent job - " + searchJob); //$NON-NLS-1$
						throw new OperationCanceledException();

					case IJob.WaitUntilReady :
						int totalWork = 1000;
						SubMonitor waitMonitor = subMonitor.setWorkRemaining(10).split(8).setWorkRemaining(totalWork);
						// use local variable to avoid potential NPE (see bug 20435 NPE when searching java method
						// and bug 42760 NullPointerException in JobManager when searching)
						Thread t = this.processingThread;
						int originalPriority = t == null ? -1 : t.getPriority();
						try {
							if (t != null)
								t.setPriority(Thread.currentThread().getPriority());
							synchronized(this) {
								this.awaitingClients++;
							}
							IJob previousJob = null;
							int awaitingJobsCount;
							int lastJobsCount = totalWork;
							float lastWorked = 0;
							float totalWorked = 0;
							while ((awaitingJobsCount = awaitingJobsCount()) > 0) {
								if (waitMonitor.isCanceled() || this.processingThread == null)
									throw new OperationCanceledException();
								IJob currentJob = currentJob();
								// currentJob can be null when jobs have been added to the queue but job manager is not enabled
								if (currentJob != null ) {
									synchronized (this.idleMonitor) {
										this.idleMonitor.notifyAll(); // wake up idle sleepers
									}
								}
								if (currentJob != null && currentJob != previousJob) {
									if (VERBOSE)
										Util.verbose("-> NOT READY - waiting until ready - " + searchJob);//$NON-NLS-1$
									String indexing = Messages.bind(Messages.jobmanager_filesToIndex, currentJob.getJobFamily(), Integer.toString(awaitingJobsCount));
									waitMonitor.subTask(indexing);
									// ratio of the amount of work relative to the total work
									float ratio = awaitingJobsCount < totalWork ? 1 : ((float) totalWork) / awaitingJobsCount;
									if (lastJobsCount > awaitingJobsCount) {
										totalWorked += (lastJobsCount - awaitingJobsCount) * ratio;
									} else {
										// more jobs were added, just increment by the ratio
										totalWorked += ratio;
									}
									if (totalWorked - lastWorked >= 1) {
										waitMonitor.worked((int) (totalWorked - lastWorked));
										lastWorked = totalWorked;
									}
									lastJobsCount = awaitingJobsCount;
									previousJob = currentJob;
								}
								synchronized (this) {
									if (awaitingJobsCount() > 0) {
										try {
											this.wait(50); // avoid Thread.sleep! wait is informed by notifyAll
										} catch (InterruptedException e) {
											// ignore
										}
									}
								}
							}
						} finally {
							synchronized(this) {
								this.awaitingClients--;
							}
							if (t != null && originalPriority > -1 && t.isAlive())
								t.setPriority(originalPriority);
						}
				}
			}
			status = searchJob.execute(subMonitor);
		} finally {
			SubMonitor.done(monitor);
			if (VERBOSE)
				Util.verbose("FINISHED  concurrent job - " + searchJob); //$NON-NLS-1$
		}
		return status;
	}
	public abstract String processName();

	/**
	 * Schedules given job for execution is there is no equal jobs waiting in the queue already
	 *
	 * @see JobManager#isJobWaiting(IJob)
	 * @param job
	 *            a job to schedule (or not)
	 */
	public synchronized void requestIfNotWaiting(IJob job) {
		if(!isJobWaiting(job)) {
			request(job);
		}
	}

	public synchronized void request(IJob job) {

		job.ensureReadyToRun();
		// append the job to the list of ones to process later on
		this.awaitingJobs.add(job);
		if (VERBOSE) {
			Util.verbose("REQUEST   background job - " + job); //$NON-NLS-1$
			Util.verbose("AWAITING JOBS count: " + awaitingJobsCount()); //$NON-NLS-1$
		}
		notifyAll(); // wake up the background thread if it is waiting
	}
	/**
	 * Flush current state
	 */
	public void reset() {
		if (VERBOSE)
			Util.verbose("Reset"); //$NON-NLS-1$

		Thread thread;
		synchronized (this) {
			thread = this.processingThread;
		}

		if (thread != null) {
			discardJobs(null); // discard all jobs
		} else {
			synchronized (this) {
				/* initiate background processing */
				this.processingThread = new Thread(this, processName());
				this.processingThread.setDaemon(true);
				// less prioritary by default, priority is raised if clients are actively waiting on it
				this.processingThread.setPriority(Thread.NORM_PRIORITY-1);
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=296343
				// set the context loader to avoid leaking the current context loader
				this.processingThread.setContextClassLoader(this.getClass().getClassLoader());
				this.processingThread.start();
			}
		}
	}
	/**
	 * Infinite loop performing resource indexing
	 */
	@Override
	public void run() {

		long idlingStart = -1;
		activateProcessing();
		try {
			class ProgressJob extends Job {
				ProgressJob(String name) {
					super(name);
				}
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					IJob job = currentJob();
					while (!monitor.isCanceled() && job != null) {
						 String taskName = new StringBuffer(Messages.jobmanager_indexing)
							.append(Messages.bind(Messages.jobmanager_filesToIndex, job.getJobFamily(), Integer.toString(awaitingJobsCount())))
							.toString();
						monitor.subTask(taskName);
						setName(taskName);
						synchronized (JobManager.this) {
							if (currentJob() != null) {
								try {
									JobManager.this.wait(500);
								} catch (InterruptedException e) {
									// ignore
								}
							}
						}
						job = currentJob();
					}
					return Status.OK_STATUS;
				}
			}
			this.progressJob = null;
			while (this.processingThread != null) {
				try {
					IJob job;
					synchronized (this) {
						// handle shutdown case when notifyAll came before the wait but after the while loop was entered
						if (this.processingThread == null) continue;

						// must check for new job inside this sync block to avoid timing hole
						if ((job = currentJob()) == null) {
							if (this.progressJob != null) {
								this.progressJob.cancel();
								this.progressJob = null;
							}
							if (idlingStart < 0)
								idlingStart = System.currentTimeMillis();
							else
								notifyIdle(System.currentTimeMillis() - idlingStart);
							this.wait(); // wait until a new job is posted (or reenabled:38901)
						} else {
							idlingStart = -1;
						}
					}
					if (job == null) {
						notifyIdle(System.currentTimeMillis() - idlingStart);
						// just woke up, delay before processing any new jobs, allow some time for the active thread to finish
						synchronized (this.idleMonitor) {
							this.idleMonitor.wait(500); // avoid sleep fixed time
						}
						continue;
					}
					if (VERBOSE) {
						Util.verbose(awaitingJobsCount() + " awaiting jobs"); //$NON-NLS-1$
						Util.verbose("STARTING background job - " + job); //$NON-NLS-1$
					}
					try {
						this.executing = true;
						if (this.progressJob == null) {
							this.progressJob = new ProgressJob(Messages.bind(Messages.jobmanager_indexing, "", "")); //$NON-NLS-1$ //$NON-NLS-2$
							this.progressJob.setPriority(Job.LONG);
							this.progressJob.setSystem(true);
							this.progressJob.schedule();
						}
						/*boolean status = */job.execute(null);
						//if (status == FAILED) request(job);
					} finally {
						this.executing = false;
						if (VERBOSE)
							Util.verbose("FINISHED background job - " + job); //$NON-NLS-1$
						moveToNextJob();
						if (this.awaitingClients == 0 && job.waitNeeded()) {
							if (VERBOSE) {
								Util.verbose("WAITING after job - " + job); //$NON-NLS-1$
							}
							synchronized (this.idleMonitor) {
								this.idleMonitor.wait(5); // avoid sleep fixed time
							}
						}
					}
				} catch (InterruptedException e) { // background indexing was interrupted
				}
			}
		} catch (RuntimeException e) {
			if (this.processingThread != null) { // if not shutting down
				// log exception
				Util.log(e, "Background Indexer Crash Recovery"); //$NON-NLS-1$

				// keep job manager alive
				discardJobs(null);
				this.processingThread = null;
				reset(); // this will fork a new thread with no waiting jobs, some indexes will be inconsistent
			}
			throw e;
		} catch (Error e) {
			if (this.processingThread != null && !(e instanceof ThreadDeath)) {
				// log exception
				Util.log(e, "Background Indexer Crash Recovery"); //$NON-NLS-1$

				// keep job manager alive
				discardJobs(null);
				this.processingThread = null;
				reset(); // this will fork a new thread with no waiting jobs, some indexes will be inconsistent
			}
			throw e;
		}
	}
	/**
	 * Stop background processing, and wait until the current job is completed before returning
	 */
	public void shutdown() {

		if (VERBOSE)
			Util.verbose("Shutdown"); //$NON-NLS-1$

		disable();
		discardJobs(null); // will wait until current executing job has completed
		Thread thread = this.processingThread;
		try {
			if (thread != null) { // see http://bugs.eclipse.org/bugs/show_bug.cgi?id=31858
				synchronized (this) {
					this.processingThread = null; // mark the job manager as shutting down so that the thread will stop by itself
					notifyAll(); // ensure its awake so it can be shutdown
				}
				// in case processing thread is handling a job
				thread.join();
			}
			Job job = this.progressJob;
			if (job != null) {
				job.cancel();
				job.join();
			}
		} catch (InterruptedException e) {
			// ignore
		}
	}
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(10);
		buffer.append("Enable count:").append(this.enableCount).append('\n'); //$NON-NLS-1$
		int numJobs = this.awaitingJobs.size();
		buffer.append("Jobs in queue:").append(numJobs).append('\n'); //$NON-NLS-1$
		for (int i = 0; i < numJobs && i < 15; i++) {
			buffer.append(i).append(" - job["+i+"]: ").append(this.awaitingJobs.get(i)).append('\n'); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return buffer.toString();
	}
}
