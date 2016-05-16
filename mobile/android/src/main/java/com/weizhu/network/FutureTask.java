package com.weizhu.network;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public final class FutureTask<V> extends java.util.concurrent.FutureTask<V> implements Future<V>, Comparable<FutureTask<V>> {

	private final int priorityNum;
	
	public FutureTask(Callable<V> callable, int priorityNum) {
		super(callable);
		this.priorityNum = priorityNum;
	}

	public FutureTask(Runnable runnable, V result, int priorityNum) {
		super(runnable, result);
		this.priorityNum = priorityNum;
	}

	@Override
	public int compareTo(FutureTask<V> other) {
		if (this == other) {
			return 0;
		}
		int cmp = compare(this.priorityNum, other.priorityNum);
		return cmp != 0 ? cmp : compare(this.hashCode(), other.hashCode());
	}
	
	private static int compare(int a, int b) {
		return (a > b) ? -1 : ((a < b) ? 1 : 0);
	}
	
	private final Object lock = new Object();
	private boolean isDone = false;
	
	private CallbackNode<V> callbackList = null;

	@Override
	public void addCallback(Callback<V> callback) {
	
		synchronized (lock) {
			if (!isDone) {
				callbackList = new CallbackNode<V>(callback, callbackList);
				return;
			}
		}
	
		if (this.isCancelled()) {
			callback.onCancel();
		} else {
			try {
				V result = this.get();
				callback.onSucc(result);
			} catch (InterruptedException e) {
				callback.onFail(e);
			} catch (ExecutionException e) {
				callback.onFail(e.getCause());
			}
		}
	}
	
	@Override
	protected void done() {
		
		CallbackNode<V> list = null;
		synchronized (lock) {
			if (isDone) {
				return;
			}
			isDone = true;
			list = callbackList;
			callbackList = null;
		}
		
		if (this.isCancelled()) {
			while (list != null) {
				CallbackNode<V> tmp = list;
				list = list.next;
				tmp.next = null;
				try {
					tmp.callback.onCancel();
				} catch (Throwable th) {
				}
			}
		} else {
			Throwable throwable;
			try {
				V result = this.get();
				while (list != null) {
					CallbackNode<V> tmp = list;
					list = list.next;
					tmp.next = null;
					try {
						tmp.callback.onSucc(result);
					} catch (Throwable th) {
					}
				}
				return;
			} catch (InterruptedException e) {
				throwable = e;
			} catch (ExecutionException e) {
				throwable = e.getCause();
			}
			while (list != null) {
				CallbackNode<V> tmp = list;
				list = list.next;
				tmp.next = null;
				try {
					tmp.callback.onFail(throwable);
				} catch (Throwable th) {
				}
			}
		}
	}
	
	private static class CallbackNode<V> {
		final Callback<V> callback;
		CallbackNode<V> next;
		
		CallbackNode(Callback<V> callback, CallbackNode<V> next) {
			this.callback = callback;
			this.next = next;
		}
	}

}
